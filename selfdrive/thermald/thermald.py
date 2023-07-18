#!/usr/bin/env python3
import datetime
import queue
import threading
import time
from collections import OrderedDict, namedtuple
from typing import Dict, Optional, Tuple

import psutil

import cereal.messaging as messaging
from cereal import log
from common.dict_helpers import strip_deprecated_keys
from common.filter_simple import FirstOrderFilter
from common.params import Params
from common.realtime import DT_TRML, sec_since_boot
from common.system import is_android, is_android_rooted
from selfdrive.controls.lib.alertmanager import set_offroad_alert
from system.hardware import HARDWARE
from selfdrive.loggerd.config import get_available_percent
from selfdrive.statsd import statlog
from system.swaglog import cloudlog

ThermalStatus = log.DeviceState.ThermalStatus
NetworkType = log.DeviceState.NetworkType
NetworkStrength = log.DeviceState.NetworkStrength
CURRENT_TAU = 15.   # 15s time constant
TEMP_TAU = 5.   # 5s time constant
DISCONNECT_TIMEOUT = 5.  # wait 5 seconds before going offroad after disconnect so you get an alert
PANDA_STATES_TIMEOUT = int(1000 * 1.5 * DT_TRML)  # 1.5x the expected pandaState frequency

ThermalBand = namedtuple("ThermalBand", ['min_temp', 'max_temp'])
HardwareState = namedtuple("HardwareState", ['network_type', 'network_info', 'network_strength', 'network_stats', 'network_metered', 'nvme_temps', 'modem_temps'])

# List of thermal bands. We will stay within this region as long as we are within the bounds.
# When exiting the bounds, we'll jump to the lower or higher band. Bands are ordered in the dict.
THERMAL_BANDS = OrderedDict({
  ThermalStatus.green: ThermalBand(None, 80.0),
  ThermalStatus.yellow: ThermalBand(75.0, 96.0),
  ThermalStatus.red: ThermalBand(80.0, 107.),
  ThermalStatus.danger: ThermalBand(94.0, None),
})

# Override to highest thermal band when offroad and above this temp
OFFROAD_DANGER_TEMP = 79.5

prev_offroad_states: Dict[str, Tuple[bool, Optional[str]]] = {}

def get_device_state():
  # System utilization
  msg = messaging.new_message("deviceState")
  
  msg.deviceState.freeSpacePercent = get_available_percent(default=100.0)
  msg.deviceState.memoryUsagePercent = int(round(psutil.virtual_memory().percent))
  msg.deviceState.cpuUsagePercent = [int(round(n)) for n in psutil.cpu_percent(percpu=True)]

  # Power
  if (not is_android()) or (is_android_rooted()):
    try: # TODO: causes crash on android when providing power via panda.
      battery = psutil.sensors_battery()
      msg.deviceState.batteryPercent = int(battery.percent)
      msg.deviceState.chargingDisabled = not battery.power_plugged
    except:
      pass

  # Device Thermals
  temps = psutil.sensors_temperatures()
  if temps.get("coretemp", None) is not None:
    msg.deviceState.cpuTempC = [cpu.current for cpu in temps['coretemp']]
  elif temps.get("battery", None) is not None:
    msg.deviceState.cpuTempC = [bms.current for bms in temps['battery']]
  else:
    msg.deviceState.cpuTempC = [0.0]*8 # TODO: find a better way to get temps that works across platforms.
  
  # desktops have bad temperature readings causing false positives.
  if not is_android():
    msg.deviceState.cpuTempC = [0.0]*8

  msg.deviceState.networkType = log.DeviceState.NetworkType.none
  msg.deviceState.networkStrength = log.DeviceState.NetworkStrength.unknown

  return msg


def set_offroad_alert_if_changed(offroad_alert: str, show_alert: bool, extra_text: Optional[str]=None):
  if prev_offroad_states.get(offroad_alert, None) == (show_alert, extra_text):
    return
  prev_offroad_states[offroad_alert] = (show_alert, extra_text)
  set_offroad_alert(offroad_alert, show_alert, extra_text)


def thermald_thread(end_event, hw_queue):
  pm = messaging.PubMaster(['deviceState'])
  sm = messaging.SubMaster(["peripheralState", "gpsLocationExternal", "controlsState", "pandaStates"], poll=["pandaStates"])

  count = 0

  onroad_conditions: Dict[str, bool] = {
    "ignition": False,
  }
  startup_conditions: Dict[str, bool] = {}
  startup_conditions_prev: Dict[str, bool] = {}

  off_ts = None
  started_ts = None
  started_seen = False
  thermal_status = ThermalStatus.green

  all_temp_filter = FirstOrderFilter(0., TEMP_TAU, DT_TRML)
  offroad_temp_filter = FirstOrderFilter(0., TEMP_TAU, DT_TRML)
  should_start_prev = False
  in_car = False
  engaged_prev = False

  params = Params()

  fan_controller = None

  while not end_event.is_set():
    sm.update(PANDA_STATES_TIMEOUT)

    pandaStates = sm['pandaStates']
    peripheralState = sm['peripheralState']

    msg = get_device_state()

    if sm.updated['pandaStates'] and len(pandaStates) > 0:

      # Set ignition based on any panda connected
      onroad_conditions["ignition"] = any(ps.ignitionLine or ps.ignitionCan for ps in pandaStates if ps.pandaType != log.PandaState.PandaType.unknown)
      
      pandaState = pandaStates[0]

      in_car = pandaState.harnessStatus != log.PandaState.HarnessStatus.notConnected

    elif (sec_since_boot() - sm.rcv_time['pandaStates']) > DISCONNECT_TIMEOUT:
      if onroad_conditions["ignition"]:
        onroad_conditions["ignition"] = False
        cloudlog.error("panda timed out onroad")

    try:
      last_hw_state = hw_queue.get_nowait()
    except queue.Empty:
      pass

    # this one is only used for offroad
    temp_sources = [
      msg.deviceState.memoryTempC,
      sum(msg.deviceState.cpuTempC)/len(msg.deviceState.cpuTempC),
      #max(msg.deviceState.gpuTempC),
    ]
    offroad_comp_temp = offroad_temp_filter.update(max(temp_sources))

    # this drives the thermal status while onroad
    #temp_sources.append(max(msg.deviceState.pmicTempC))
    all_comp_temp = all_temp_filter.update(max(temp_sources))

    if fan_controller is not None:
      msg.deviceState.fanSpeedPercentDesired = fan_controller.update(all_comp_temp, onroad_conditions["ignition"])

    is_offroad_for_5_min = (started_ts is None) and ((not started_seen) or (off_ts is None) or (sec_since_boot() - off_ts > 60 * 5))
    if is_offroad_for_5_min and offroad_comp_temp > OFFROAD_DANGER_TEMP:
      # If device is offroad we want to cool down before going onroad
      # since going onroad increases load and can make temps go over 107
      thermal_status = ThermalStatus.danger
    else:
      current_band = THERMAL_BANDS[thermal_status]
      band_idx = list(THERMAL_BANDS.keys()).index(thermal_status)
      if current_band.min_temp is not None and all_comp_temp < current_band.min_temp:
        thermal_status = list(THERMAL_BANDS.keys())[band_idx - 1]
      elif current_band.max_temp is not None and all_comp_temp > current_band.max_temp:
        thermal_status = list(THERMAL_BANDS.keys())[band_idx + 1]

    # **** starting logic ****

    # Ensure date/time are valid
    now = datetime.datetime.utcnow()
    startup_conditions["time_valid"] = (now.year > 2020) or (now.year == 2020 and now.month >= 10)
    set_offroad_alert_if_changed("Offroad_InvalidTime", (not startup_conditions["time_valid"]))

    startup_conditions["up_to_date"] = params.get("Offroad_ConnectivityNeeded") is None or params.get_bool("DisableUpdates") or params.get_bool("SnoozeUpdate")
    startup_conditions["not_uninstalling"] = not params.get_bool("DoUninstall")
    startup_conditions["accepted_terms"] = params.get_bool("HasAcceptedTerms")
    startup_conditions["offroad_min_time"] = (not started_seen) or ((off_ts is not None) and (sec_since_boot() - off_ts) > 5.)

    # with 2% left, we killall, otherwise the phone will take a long time to boot
    startup_conditions["free_space"] = msg.deviceState.freeSpacePercent > 2
    startup_conditions["completed_training"] = params.get_bool("CompletedTrainingVersion") or \
                                               params.get_bool("Passive")
    # if any CPU gets above 107 or the battery gets above 63, kill all processes
    # controls will warn with CPU above 95 or battery above 60
    onroad_conditions["device_temp_good"] = thermal_status < ThermalStatus.danger
    set_offroad_alert_if_changed("Offroad_TemperatureTooHigh", (not onroad_conditions["device_temp_good"]))

    # Handle offroad/onroad transition
    should_start = all(onroad_conditions.values())
    if started_ts is None:
      should_start = should_start and all(startup_conditions.values())
    
    # for debug
    if not should_start and count % 10 == 0:
      for startup_condition in startup_conditions:
        if not startup_conditions[startup_condition]:
          print(startup_condition, startup_conditions[startup_condition])
      for onroad_condition in onroad_conditions:
        if not onroad_conditions[onroad_condition]:
          print(onroad_condition, onroad_conditions[onroad_condition])
        
    if should_start != should_start_prev or (count == 0):
      params.put_bool("IsOnroad", should_start)
      params.put_bool("IsOffroad", not should_start)

      params.put_bool("IsEngaged", False)
      engaged_prev = False
      HARDWARE.set_power_save(not should_start)

    if sm.updated['controlsState']:
      engaged = sm['controlsState'].enabled
      if engaged != engaged_prev:
        params.put_bool("IsEngaged", engaged)
        engaged_prev = engaged

      try:
        with open('/dev/kmsg', 'w') as kmsg:
          kmsg.write(f"<3>[thermald] engaged: {engaged}\n")
      except Exception:
        pass

    if should_start:
      off_ts = None
      if started_ts is None:
        started_ts = sec_since_boot()
        started_seen = True
    else:
      if onroad_conditions["ignition"] and (startup_conditions != startup_conditions_prev):
        cloudlog.event("Startup blocked", startup_conditions=startup_conditions, onroad_conditions=onroad_conditions, error=True)
        startup_conditions_prev = startup_conditions.copy()

      started_ts = None
      if off_ts is None:
        off_ts = sec_since_boot()

    # TODO: implement this
    # Offroad power monitoring
    # current_power_draw = HARDWARE.get_current_power_draw()
    # statlog.sample("power_draw", current_power_draw)
    # msg.deviceState.powerDrawW = current_power_draw

    # TODO: implement this
    # Check if we need to shut down
    # if power_monitor.should_shutdown(onroad_conditions["ignition"], in_car, off_ts, started_seen):
    #   cloudlog.warning(f"shutting device down, offroad since {off_ts}")
    #   params.put_bool("DoShutdown", True)

    msg.deviceState.started = started_ts is not None
    msg.deviceState.startedMonoTime = int(1e9*(started_ts or 0))

    msg.deviceState.thermalStatus = thermal_status
    pm.send("deviceState", msg)

    should_start_prev = should_start

    # Log to statsd
    statlog.gauge("free_space_percent", msg.deviceState.freeSpacePercent)
    statlog.gauge("memory_usage_percent", msg.deviceState.memoryUsagePercent)
    for i, usage in enumerate(msg.deviceState.cpuUsagePercent):
      statlog.gauge(f"cpu{i}_usage_percent", usage)
    for i, temp in enumerate(msg.deviceState.cpuTempC):
      statlog.gauge(f"cpu{i}_temperature", temp)

    # report to server once every 10 minutes
    if (count % int(600. / DT_TRML)) == 0:
      cloudlog.event("STATUS_PACKET",
                     count=count,
                     pandaStates=[strip_deprecated_keys(p.to_dict()) for p in pandaStates],
                     peripheralState=strip_deprecated_keys(peripheralState.to_dict()),
                     location=(strip_deprecated_keys(sm["gpsLocationExternal"].to_dict()) if sm.alive["gpsLocationExternal"] else None),
                     deviceState=strip_deprecated_keys(msg.to_dict()))

    count += 1


def main():
  hw_queue = queue.Queue(maxsize=1)
  end_event = threading.Event()

  threads = [
    threading.Thread(target=thermald_thread, args=(end_event, hw_queue)),
  ]

  for t in threads:
    t.start()

  try:
    while True:
      time.sleep(1)
      if not all(t.is_alive() for t in threads):
        break
  finally:
    end_event.set()

  for t in threads:
    t.join()


if __name__ == "__main__":
  main()
