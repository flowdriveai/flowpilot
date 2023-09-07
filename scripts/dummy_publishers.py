import cereal.messaging as messaging
from cereal import log
import threading
from opendbc.can.packer import CANPacker
from common.realtime import DT_DMON
from selfdrive.car.honda.values import CruiseButtons
from tools.sim.lib.can import can_function
from common.system import is_android

import time

packer = CANPacker("honda_civic_touring_2016_can_generated")
rpacker = CANPacker("acura_ilx_2016_nidec")

def can_function_runner(exit_event: threading.Event):
  pm = messaging.PubMaster(["can"])
  i = 0
  while not exit_event.is_set():
    cruise_button = CruiseButtons.RES_ACCEL
    if i % 500 == 0:
        cruise_button = 0
    can_function(pm, 20, 0.1, i, cruise_button, True)
    time.sleep(0.009)
    i += 1

def panda_state_function(exit_event: threading.Event):
  pm = messaging.PubMaster(['pandaStates'])
  while not exit_event.is_set():
    dat = messaging.new_message('pandaStates', 1)
    dat.valid = True
    dat.pandaStates[0] = {
      'ignitionLine': True,
      'pandaType': "blackPanda",
      'controlsAllowed': True,
      'safetyModel': 'hondaNidec',
    }
    pm.send('pandaStates', dat)
    time.sleep(0.5)

def gps_callback(exit_event):
  pm = messaging.PubMaster(['gpsLocationExternal'])
  while not exit_event.is_set():
    dat = messaging.new_message('gpsLocationExternal')
    velNED = [0, 0, 0]
    dat.gpsLocationExternal = {
      "unixTimestampMillis": int(time.time() * 1000),
      "flags": 1,  # valid fix
      "accuracy": 1.0,
      "verticalAccuracy": 1.0,
      "speedAccuracy": 0.1,
      "bearingAccuracyDeg": 0.1,
      "vNED": velNED,
      "bearingDeg": 0,
      "latitude": 20,
      "longitude": 30,
      "altitude": 1000,
      "speed": 20,
      "source": log.GpsLocationData.SensorSource.ublox,
    }

    pm.send('gpsLocationExternal', dat)
    time.sleep(0.1)

def imu_callback(exit_event):
  # send 5x since 'sensor_tick' doesn't seem to work. limited by the world tick?
  pm = messaging.PubMaster(['accelerometer', 'gyroscope'])
  while not exit_event.is_set():
    dat = messaging.new_message('accelerometer')
    dat.accelerometer.sensor = 4
    dat.accelerometer.type = 0x10
    dat.accelerometer.timestamp = dat.logMonoTime  # TODO: use the IMU timestamp
    dat.accelerometer.init('acceleration')
    dat.accelerometer.acceleration.v = [0, 0, 0]
    pm.send('accelerometer', dat)

    # copied these numbers from locationd
    dat = messaging.new_message('gyroscope')
    dat.gyroscope.sensor = 5
    dat.gyroscope.type = 0x10
    dat.gyroscope.timestamp = dat.logMonoTime  # TODO: use the IMU timestamp
    dat.gyroscope.init('gyroUncalibrated')
    dat.gyroscope.gyroUncalibrated.v = [0, 0, 0]
    pm.send('gyroscope', dat)
    time.sleep(0.01)

def peripheral_state_function(exit_event: threading.Event):
  pm = messaging.PubMaster(['peripheralState'])
  while not exit_event.is_set():
    dat = messaging.new_message('peripheralState')
    dat.valid = True
    # fake peripheral state data
    dat.peripheralState = {
      'pandaType': log.PandaState.PandaType.blackPanda,
      'voltage': 12000,
      'current': 5678,
      'fanSpeedRpm': 1000
    }
    pm.send('peripheralState', dat)
    time.sleep(0.5)

def fake_driver_monitoring(exit_event: threading.Event):
  pm = messaging.PubMaster(['driverState', 'driverMonitoringState'])
  while not exit_event.is_set():
    # dmonitoringmodeld output
    dat = messaging.new_message('driverState')
    dat.driverState.faceProb = 1.0
    pm.send('driverState', dat)

    # dmonitoringd output
    dat = messaging.new_message('driverMonitoringState')
    dat.driverMonitoringState = {
      "faceDetected": True,
      "isDistracted": False,
      "awarenessStatus": 1.,
    }
    pm.send('driverMonitoringState', dat)

    time.sleep(DT_DMON)


if __name__ == "__main__":
    threads = []
    exit_event = threading.Event()
    threads.append(threading.Thread(target=panda_state_function, args=(exit_event,)))
    threads.append(threading.Thread(target=peripheral_state_function, args=(exit_event,)))
    threads.append(threading.Thread(target=fake_driver_monitoring, args=(exit_event,)))
    threads.append(threading.Thread(target=can_function_runner, args=(exit_event,)))
    threads.append(threading.Thread(target=gps_callback, args=(exit_event,)))
    if is_android():
      threads.append(threading.Thread(target=imu_callback, args=(exit_event,)))

    for t in threads:
      t.start()

    for t in reversed(threads):
        t.join()

