import os
import subprocess
import psutil
from typing import Optional, List, ValuesView

from selfdrive.swaglog import cloudlog
from cereal import log, car

LOGPATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), "logfiles")
LOG_TO_FILES = os.getenv("LOG_TO_FILES")

class ManagerProcess:
    def __init__(
        self, name: str, command: str, args: List[str]=[], enabled=True, onroad=True, offroad=False, 
        callback=None, unkillable=False, platform=["android", "desktop"], rename=False):

        self.name: str = name
        self.command: str = command
        self.args: List[str] = args
        self.enabled = enabled
        self.onroad = onroad
        self.offroad = offroad
        self.callback = callback
        self.unkillable = unkillable
        self.platform = platform
        self.shell = rename

        self.phandler = None
        self.proc = None
        self.exitcode = None
        self.communicated = False

        if rename:
          self.command = "LD_PRELOAD=libprocname.so " + self.command
    
    def is_alive(self):
        # python subprocesses go into zombie state after terminating
        # need to use polling to determine if alive or not.
        if self.proc is not None:
            poll = self.proc.poll()
            return True if poll is None else False
        else:
            return self.phandler.is_running()
    
    def communicate(self):
        """Handles communication with the service and returns errors"""
        if self.proc is not None:
            _, stderr = self.proc.communicate()
            self.communicated = True
            return stderr
        return None
 
    def start(self):
        """Starts the service"""
        if self.phandler is not None or not self.enabled:
            return
        cloudlog.info("Starting " + self.name)

        # pipe only stderr for sentry
        stdout, stderr = None, subprocess.PIPE 
        if LOG_TO_FILES:
            with open(
                os.path.join(LOGPATH, f"{self.name}.stdout"), "a"
            ) as stdout, open(
                os.path.join(LOGPATH, f"{self.name}.stderr"), "a"
            ) as stderr:
                stdout, stderr = stdout, stderr

        self.proc = subprocess.Popen(
                    [self.command] + self.args, stdout=stdout, stderr=stderr, shell=self.shell
                )
        self.pid = self.proc.pid
        self.phandler = psutil.Process(self.pid)

    def stop(self):
        """Handles how the service ends"""
        if self.phandler is None or self.unkillable:
          return
        if self.is_alive():
            cloudlog.info("killing " + self.name)
            self.exitcode = self.proc.terminate()
            self.proc.wait()
        self.phandler = None
        self.proc = None

    def get_proc_msg(self):
        """Packages a Capn'Proto message for proc logs"""
        proc_msg = log.ProcLog.Process.new_message()
        if self.is_alive():
            proc_msg.pid=self.pid
            proc_msg.name=self.name
            proc_msg.state=self.phandler.status()
            proc_msg.nice=self.phandler.nice()
            proc_msg.numThreads=self.phandler.num_threads()
            proc_msg.startTime=self.phandler.create_time()
            proc_msg.processor=self.phandler.cpu_affinity()
            proc_msg.cpuPercent=self.phandler.cpu_percent()
            proc_msg.cpuTimes=self.phandler.cpu_times().user
            proc_msg.memoryUsage=self.phandler.memory_percent()
            proc_msg.cmdline=self.phandler.cmdline()
            proc_msg.exe=self.phandler.exe()
        return proc_msg

    def get_process_state_msg(self):
        state = log.ManagerState.ProcessState.new_message()
        state.name = self.name
        if self.phandler:
            state.running = self.is_alive()
            state.shouldBeRunning = self.phandler is not None
            state.pid = self.pid or 0
            state.exitCode = self.exitcode or 0
        return state

def manager_cleanup(services: List[ManagerProcess]):
    """Kills all the services recieved as argument"""
    cloudlog.info("Killing services.. ")
    for service in services:
        service.stop()

def ensure_running(procs: ValuesView[ManagerProcess], started: bool, params=None, CP: car.CarParams=None,
                   not_run: Optional[List[str]]=None) -> List[ManagerProcess]:
  if not_run is None:
    not_run = []

  running = []
  for p in procs:
    # Conditions that make a process run
    run = any((
      p.offroad and not started,
      p.onroad and started,
    ))
    if p.callback is not None and None not in (params, CP):
      run = run or p.callback(started, params, CP)

    # Conditions that block a process from starting
    run = run and not any((
      not p.enabled,
      p.name in not_run,
    ))

    if run:
      p.start()
      running.append(p)
    else:
      p.stop()

  return running
