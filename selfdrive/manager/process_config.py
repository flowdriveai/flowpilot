from cereal import car
from common.params import Params
from selfdrive.manager.process import ManagerProcess
from common.system import is_android

def driverview(started: bool, params: Params, CP: car.CarParams) -> bool:
  return params.get_bool("IsDriverViewEnabled")  # type: ignore

def notcar(started: bool, params: Params, CP: car.CarParams) -> bool:
  return CP.notCar  # type: ignore

def logging(started, params, CP: car.CarParams) -> bool:
  run = (not CP.notCar) or not params.get_bool("DisableLogging")
  return started and run

def is_f3():
  return Params().get_bool("F3")
    
  # ai.flow.app:
  #   command: "am start --user 0 -n ai.flow.android/ai.flow.android.AndroidLauncher"
  #   nowait: true
  #   nomonitor: true
  #   platforms: ["android"]

procs = [
  # due to qualcomm kernel bugs SIGKILLing camerad sometimes causes page table corruption
  ManagerProcess("calibrationd", "calibrationd"),
  ManagerProcess("plannerd", "plannerd"),
  ManagerProcess("modelparsed", "./selfdrive/modeld/modelparsed", enabled=is_f3()),
  ManagerProcess("controlsd", "controlsd"),
  ManagerProcess("proclogd", "./system/proclogd/proclogd"),
  ManagerProcess("logmessaged", "logmessaged", offroad=True),
  ManagerProcess("thermald_", "thermald_", offroad=True),
  ManagerProcess("statsd", "statsd", offroad=True),
  ManagerProcess("keyvald", "keyvald", offroad=True),
  ManagerProcess("flowpilot", "./gradlew", args=["desktop:run"], rename=False, offroad=True, platform=["desktop"]),
  ManagerProcess("pandad", "pandad", offroad=True),
  ManagerProcess("loggerd", "./selfdrive/loggerd/loggerd", enabled=False, onroad=False, callback=logging),
  ManagerProcess("uploader", "uploader", enabled=False, offroad=True),
  ManagerProcess("deleter", "deleter", enabled=False, offroad=True),
]

platform = "android" if is_android() else "desktop" 
managed_processes = {p.name: p for p in procs if platform in p.platform}
