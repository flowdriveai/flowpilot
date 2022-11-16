import logging
import os
import subprocess
from typing import List

import psutil

from flowinit.config import Config
from cereal import log

logger = logging.getLogger(__name__)    


class Service:
    def __init__(
        self, name: str, command: str=None, args: List[str]=[], restart: bool=False, monitor_only: bool=False,
        pid: int=None, nomonitor: bool=False, nowait: bool=False
    ):
        self.name: str = name
        self.command: str = command
        self.args: List[str] = args
        self.monitor_only = monitor_only
        self.nowait = nowait
        self.restart: bool = restart or False
        self.pid = pid
        self.nomonitor = nomonitor
        self.phandler = None
        self.proc = None
        self.exitcode = None
        self.proc = None

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)

    def __repr__(self):
        return self.__str__() + "\n"
    
    def is_alive(self):
        # python subprocesses go into zombie state after terminating
        # need to use polling to determine if alive or not.
        if self.proc is not None:
            poll = self.proc.poll()

            if poll is None:
                return True, None
            else:
                _, error = self.proc.communicate()
                return False, error
        else:
            return self.phandler.is_running(), None   
 
    def start(self):
        """Starts the service"""
        if self.phandler is not None:
            return
        if not self.monitor_only:
            logger.info("Starting " + self.name)
            stdout, stderr = subprocess.PIPE, subprocess.PIPE
            if Config.LOGPATH:
                with open(
                    os.path.join(Config.LOGPATH, f"{self.name}.stdout"), "a"
                ) as stdout, open(
                    os.path.join(Config.LOGPATH, f"{self.name}.stderr"), "a"
                ) as stderr:
                    stdout, stderr = stdout, stderr

            self.proc = subprocess.Popen(
                        [self.command] + self.args, stdout=stdout, stderr=stderr, shell=True
                    )
            self.pid = self.proc.pid
        self.phandler = psutil.Process(self.pid)

    def stop(self):
        """Handles how the service ends"""
        if self.is_alive()[0]:
            logger.info("killing " + self.name)
            self.exitcode = self.phandler.terminate()
            self.phandler.wait()

    def get_proc_msg(self):
        """Packages a Capn'Proto message for proc logs"""
        proc_msg = log.ProcLog.Process.new_message()
        if self.is_alive()[0]:
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
            state.running = self.is_alive()[0]
            state.shouldBeRunning = self.phandler is not None
            state.pid = self.pid or 0
            state.exitCode = self.exitcode or 0
        return state

def killswitch(services: List[Service]):
    """Kills all the services recieved as argument"""
    logger.info("Killing services.. ")
    for service in services:
        service.stop()
