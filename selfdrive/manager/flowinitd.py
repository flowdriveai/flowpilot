import logging
import time
from typing import List
from pathlib import Path
import traceback
import subprocess
import os

import psutil
from common.params import Params, ParamKeyType
from common.basedir import BASEDIR
from common import system
from common.path import external_android_storage
import cereal.messaging as messaging

from selfdrive.manager.daemon import Daemon, DaemonSig
from selfdrive.manager.filelock import FileLock
from selfdrive.manager.process import ensure_running
from selfdrive.manager.process_config import managed_processes

from system.version import is_dirty, get_commit, get_version, get_origin, get_short_branch, \
                              terms_version, training_version
from system.swaglog import cloudlog
from selfdrive.sentry import sentry_init, capture_error

os.chdir(BASEDIR)

POSSIBLE_PNAME_MATRIX = [
    "java",  # linux
    "ai.flow.android",  # android
    "java.exe",  # windows
]
ANDROID_APP = "ai.flow.app"
ENV_VARS = ["USE_GPU", "ZMQ_MESSAGING_PROTOCOL", "ZMQ_MESSAGING_ADDRESS",
            "SIMULATION", "FINGERPRINT", "MSGQ", "PASSIVE"]
UNREGISTERED_DONGLE_ID = "UnregisteredDevice"

params = Params()

def flowpilot_running():
    ret = False
    pid_bytes = params.get("FlowpilotPID")
    pid = int.from_bytes(pid_bytes, "little") if pid_bytes is not None else None
    try:
        if pid is not None and psutil.pid_exists(pid):
            p = psutil.Process(pid)
            if p.name() in POSSIBLE_PNAME_MATRIX:
                ret = True
    except:
        pass
    return ret


def append_extras(command: str):
    for var in ENV_VARS:
        val = os.environ.get(var, None)
        if val is not None:
            command += f" -e '{var}' '{val}'"
    return command


def manager_cleanup() -> None:
  # send signals to kill all procs
  for p in managed_processes.values():
    p.stop()


def main():
    params.clear_all(ParamKeyType.CLEAR_ON_MANAGER_START)

    with FileLock("flowinit"):        
        # Parse the services yaml file

        for proc in psutil.process_iter():
            if proc.name() in managed_processes.keys() and \
                not managed_processes[proc.name()].unkillable:
                cloudlog.warning(f"{proc.name()} already alive, restarting..")
                proc.kill()

        default_params = [
                        ("CompletedTrainingVersion", "0"),
                        ("DisengageOnAccelerator", "1"),
                        ("HasAcceptedTerms", "0"),
                        ("FlowpilotEnabledToggle", "0"),
                        ("WideCameraOnly", "1"),
                         ]

        if params.get_bool("RecordFrontLock"):
            params.put_bool("RecordFront", True)

        if not params.get_bool("DisableRadar_Allow"):
            params.remove("DisableRadar")
        
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

        ignore: List[str] = []
        if params.get("UserID", encoding='utf8') in (None, ):
            ignore.append("uploader")
        if os.getenv("NOBOARD") is not None:
            ignore.append("pandad")
        ignore += [x for x in os.getenv("BLOCK", "").split(",") if len(x) > 0]

        sm = messaging.SubMaster(['deviceState', 'carParams'], poll=['deviceState'])
        pm = messaging.PubMaster(['managerState'])

        ensure_running(managed_processes.values(), False, params=params, CP=sm['carParams'], not_run=ignore)
        
        try:
            while True:
                sm.update()

                started = sm['deviceState'].started
                ensure_running(managed_processes.values(), started, params=params, CP=sm['carParams'], not_run=ignore)

                running_daemons = []
                for service in managed_processes.values():
                    if service.phandler is None:
                        continue
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
                running_daemons.append("%s%s\u001b[0m" % ("\u001b[32m", "flowinitd"))
                if flowpilot_running():
                    running_daemons.append("%s%s\u001b[0m" % ("\u001b[32m", "modeld camerad sensord ui soundd"))

                print(" ".join(running_daemons))
                cloudlog.debug(running_daemons)

                # send managerState
                manager_state_msg = messaging.new_message('managerState')
                manager_state_msg.managerState.processes = [p.get_process_state_msg() for p in managed_processes.values()]
                pm.send('managerState', manager_state_msg)

                # Exit main loop when uninstall/shutdown/reboot is needed
                shutdown = False
                for param in ("DoUninstall", "DoShutdown", "DoReboot"):
                    if params.get_bool(param):
                        shutdown = True
                        params.put("LastManagerExitReason", param)
                        cloudlog.warning(f"Shutting down manager - {param} set")

                if shutdown:
                    break

                time.sleep(2)
                   
        except Exception as e:
            print(traceback.format_exc())
        finally:
            cloudlog.info("cleaning up..")
            params.put_bool("FlowinitReady", False)
            manager_cleanup()
            