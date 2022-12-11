import argparse
import logging
import sys
import time
from pathlib import Path
import traceback
import subprocess
import os

import psutil
import yaml
from common.params import Params, ParamKeyType
from common.basedir import BASEDIR
from common import system
from common.path import external_android_storage
import cereal.messaging as messaging

from selfdrive.manager.config import Config
from selfdrive.manager.daemon import Daemon, DaemonSig
from selfdrive.manager.filelock import FileLock
from selfdrive.manager.utils import get_cpu_times, get_device_state_msg, get_memory_logs
from selfdrive.manager.services import Service, killswitch

from selfdrive.version import is_dirty, get_commit, get_version, get_origin, get_short_branch, \
                              terms_version, training_version
from selfdrive.swaglog import cloudlog
from selfdrive.sentry import sentry_init, capture_error

logger = logging.getLogger(__name__)
os.chdir(BASEDIR)

POSSIBLE_PNAME_MATRIX = [
    "java",  # linux
    "ai.flow.android",  # android
    "java.exe",  # windows
]
ANDROID_APP = "ai.flow.app"
ENV_VARS = ["USE_GPU", "ZMQ_MESSAGING_PROTOCOL", "ZMQ_MESSAGING_ADDRESS",
            "SIMULATION", "FINGERPRINT", "MSGQ", "PASSIVE"]

params = Params()

def flowpilot_running():
    logger.debug("Checking if flowpilot is running")
    ret = False
    
    pid_bytes = params.get("FlowpilotPID")
    pid = int.from_bytes(pid_bytes, "little") if pid_bytes is not None else None
    if pid is not None and psutil.pid_exists(pid):
        p = psutil.Process(pid)
        if p.name() in POSSIBLE_PNAME_MATRIX:
            logger.debug("flowpilot is running")
            ret = True

    return ret


def wait_for_start_signal(daemon):
    logger.info("Waiting for the start signal")
    while (not flowpilot_running()) and daemon.recv() != DaemonSig.START:
        time.sleep(Config.FREQUENCY)
    logger.debug("Got the start signal")


def parse_services(service_file):
    """Parses the service.yaml file and returns a list of Services"""

    services: list[Service] = []
    nomonitor_services: list[Service] = []

    platform = "android" if system.is_android() else "desktop" 

    with open(service_file) as f:
        parsed = yaml.safe_load(f)

        for sname, sdata in parsed["services"].items():
            target_platforms = sdata.get("platforms", None)
            if target_platforms is not None:
                if platform not in target_platforms:
                    continue

            service = Service(
                    sname,
                    sdata["command"],
                    sdata.get("args", []),
                    sdata.get("restart", False),
                    nomonitor=sdata.get("nomonitor", False),
                    nowait = sdata.get("nowait", False)
                )
            if sdata.get("nomonitor", False):
                nomonitor_services.append(service)
            else:
                services.append(service)
    return services, nomonitor_services


def parse_args(args):
    """Argument Parsing"""
    parser = argparse.ArgumentParser(
        description="A lightweight init system for flowpilot"
    )

    parser.add_argument(
        "-c",
        "--config",
        action="store",
        type=str,
        default=f"{Config.FLOWINIT_ROOT}/services.yaml",
        help="Path to the service yaml file",
    )

    parser.add_argument(
        "-o",
        "--logpath",
        action="store",
        type=str,
        default=Config.LOGPATH,
        help="Path to where the stdout will be redirected",
    )

    parser.add_argument(
        "--logfile",
        action="store_true",
        default=False,
        help="Print STDOUT to logfile",
    )

    parser.add_argument(
        "-v",
        "--verbose",
        action="count",
        default=0,
        help="Increase logging level. Defaults to logging.INFO",
    )

    return parser.parse_args(args)

def append_extras(command: str):
    for var in ENV_VARS:
        val = os.environ.get(var, None)
        if val is not None:
            command += f" -e '{var}' '{val}'"
    return command

def main():
    params.clear_all(ParamKeyType.CLEAR_ON_MANAGER_START)
    with FileLock("flowinit"):
        # Argument Parsing
        args = parse_args(sys.argv[1:])

        # Make the logpath if not already done
        Path(args.logpath).mkdir(parents=True, exist_ok=True)
        Config.LOGPATH = "" if not args.logfile else args.logpath

        # Set up logging
        log_levels: list = [logging.INFO, logging.DEBUG]
        verbosity: int = min(args.verbose, len(log_levels) - 1)
        log_level = log_levels[verbosity]
        logging.basicConfig(
            level=log_level,
            format="%(asctime)s %(filename)s [%(levelname)s] %(message)s",
        )

        # Parse the services yaml file
        services, nomonitor_services = parse_services(args.config)

        services_names = [service.name for service in services+nomonitor_services] 
        for proc in psutil.process_iter():
            if proc.name() in services_names:
                logger.warning(f"{proc.name()} already alive, restarting..")
                proc.kill()
            
        for service in services+nomonitor_services: 

            # explicitly need to pass env variables to android app through extras.
            if service.name == ANDROID_APP:
                service.command = append_extras(service.command)

            if service.nowait:
                service.start()

        logger.debug(f"Got services: \n {services}")

        default_params = [
                        ("CompletedTrainingVersion", "0"),
                        ("DisengageOnAccelerator", "1"),
                        ("HasAcceptedTerms", "0"),
                        ("OpenpilotEnabledToggle", "1"),
                         ]

        if params.get_bool("RecordFrontLock"):
            params.put_bool("RecordFront", True)

        if not params.get_bool("DisableRadar_Allow"):
            params.delete("DisableRadar")
        
        # android specififc
        if system.is_android():
            if os.environ.get("USE_SNPE", None) == "1":
                params.put_bool("UseSNPE", True)
            else:
                params.put_bool("UseSNPE", False)
            
            # android app cannot access internal termux files, need to copy them over 
            # to external storage. rsync is used to copy only modified files.
            internal_assets_dir = os.path.join(BASEDIR, "selfdrive/assets")
            external_android_flowpilot_assets_dir = os.path.join(external_android_storage(), "flowpilot/selfdrive")
            Path(external_android_flowpilot_assets_dir).mkdir(parents=True, exist_ok=True)
            subprocess.check_output(["rsync", "-r", "-u", internal_assets_dir, external_android_flowpilot_assets_dir])

        for k, v in default_params:
            if params.get(k) is None:
                params.put(k, v)
        
        # is this dashcam?
        if os.getenv("PASSIVE") is not None:
            params.put_bool("Passive", bool(int(os.getenv("PASSIVE", "0"))))

        if params.get("Passive") is None:
            raise Exception("Passive must be set to continue")

        # set version params
        params.put("Version", get_version())
        params.put("TermsVersion", terms_version)
        params.put("TrainingVersion", training_version)
        params.put("GitCommit", get_commit(default=""))
        params.put("GitBranch", get_short_branch(default=""))
        params.put("GitRemote", get_origin(default=""))

        if not is_dirty():
            os.environ['CLEAN'] = '1'
        
        sentry_init()
        
        cloudlog.bind_global(dongle_id="", version=get_version(), dirty=is_dirty(), # TODO
                            device="todo")

        # Get a daemon instance
        daemon = Daemon()

        # Get the green flag from the FlowPilot app to start the services
        wait_for_start_signal(daemon)

        # on android without root, we cannot monitor external processes
        flowpilot_pid = int.from_bytes(params.get("FlowpilotPID"), "little")
        if psutil.pid_exists(flowpilot_pid):
            flowpilot_process = Service("modeld camerad ui sensord", pid=flowpilot_pid,
                                        monitor_only=True)
            services.append(flowpilot_process)
        else:
            cloudlog.warning("Unable to monitor flowpilot app (modeld camerad ui sensord)")

        try:
            # Start all services
            for service in services:
                service.start()

            pm = messaging.PubMaster(["procLog", "deviceState", "managerState"])
            params.put_bool("FlowinitReady", True)
            
            # Event loop
            while True:
                running_daemons = []
                for service in services:
                    is_running = service.is_alive()
                    if is_running:
                        running_daemons.append("%s%s\u001b[0m" % ("\u001b[32m", service.name))
                    else:
                        running_daemons.append("%s%s\u001b[0m" % ("\u001b[31m", service.name))
                        if service.communicated:
                            continue
                        stderr = service.communicate()
                        if stderr is not None:
                            stderr = stderr.decode("utf-8")
                            print("%s%s\u001b[0m" % ("\u001b[31m", f"[{service.name}] " + stderr))
                            if "KeyboardInterrupt" not in stderr:
                                capture_error(stderr, level="error")

                print(" ".join(running_daemons))
                cloudlog.debug(running_daemons)

                # send managerState
                manager_state_msg = messaging.new_message('managerState')
                manager_state_msg.managerState.processes = [p.get_process_state_msg() for p in services]
                pm.send('managerState', manager_state_msg)

                # Generate a new procLogList Message
                proc_log_msg = messaging.new_message("procLog")
                proc_log_msg.procLog.procs = [service.get_proc_msg() for service in services]
                proc_log_msg.procLog.cpuTimes = get_cpu_times()
                proc_log_msg.procLog.mem = get_memory_logs()

                device_state_msg = get_device_state_msg()

                # Publishing topics
                pm.send("procLog", proc_log_msg)
                pm.send("deviceState", device_state_msg)
                
                # Try not to hog all the CPU cycles
                time.sleep(Config.FREQUENCY)
               
        except Exception as e:
            print(traceback.format_exc())
        finally:
            logger.info("cleaning up..")
            params.put_bool("FlowinitReady", False)
            killswitch(services)
            killswitch(nomonitor_services)
            
