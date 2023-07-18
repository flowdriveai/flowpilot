import cereal.messaging as messaging
from cereal import log
import threading
from opendbc.can.packer import CANPacker
from selfdrive.boardd.boardd import can_list_to_can_capnp
from selfdrive.car import crc8_pedal
from common.realtime import DT_DMON
from selfdrive.car.honda.values import CruiseButtons
import time

packer = CANPacker("honda_civic_touring_2016_can_generated")
rpacker = CANPacker("acura_ilx_2016_nidec")

def can_function(pm, speed, angle, idx, cruise_button, is_engaged):

  msg = []

  # *** powertrain bus ***

  speed = speed * 3.6 # convert m/s to kph
  msg.append(packer.make_can_msg("ENGINE_DATA", 0, {"XMISSION_SPEED": speed}))
  msg.append(packer.make_can_msg("WHEEL_SPEEDS", 0, {
    "WHEEL_SPEED_FL": speed,
    "WHEEL_SPEED_FR": speed,
    "WHEEL_SPEED_RL": speed,
    "WHEEL_SPEED_RR": speed
  }))

  msg.append(packer.make_can_msg("SCM_BUTTONS", 0, {"CRUISE_BUTTONS": cruise_button}))

  values = {"COUNTER_PEDAL": idx & 0xF}
  checksum = crc8_pedal(packer.make_can_msg("GAS_SENSOR", 0, {"COUNTER_PEDAL": idx & 0xF})[2][:-1])
  values["CHECKSUM_PEDAL"] = checksum
  msg.append(packer.make_can_msg("GAS_SENSOR", 0, values))

  msg.append(packer.make_can_msg("GEARBOX", 0, {"GEAR": 4, "GEAR_SHIFTER": 8}))
  msg.append(packer.make_can_msg("GAS_PEDAL_2", 0, {}))
  msg.append(packer.make_can_msg("SEATBELT_STATUS", 0, {"SEATBELT_DRIVER_LATCHED": 1}))
  msg.append(packer.make_can_msg("STEER_STATUS", 0, {}))
  msg.append(packer.make_can_msg("STEERING_SENSORS", 0, {"STEER_ANGLE": angle}))
  msg.append(packer.make_can_msg("VSA_STATUS", 0, {}))
  msg.append(packer.make_can_msg("STANDSTILL", 0, {"WHEELS_MOVING": 1 if speed >= 1.0 else 0}))
  msg.append(packer.make_can_msg("STEER_MOTOR_TORQUE", 0, {}))
  msg.append(packer.make_can_msg("EPB_STATUS", 0, {}))
  msg.append(packer.make_can_msg("DOORS_STATUS", 0, {}))
  msg.append(packer.make_can_msg("CRUISE_PARAMS", 0, {}))
  msg.append(packer.make_can_msg("CRUISE", 0, {}))
  msg.append(packer.make_can_msg("SCM_FEEDBACK", 0, {"MAIN_ON": 1}))
  msg.append(packer.make_can_msg("POWERTRAIN_DATA", 0, {"ACC_STATUS": int(is_engaged)}))
  msg.append(packer.make_can_msg("HUD_SETTING", 0, {}))

  # *** cam bus ***
  msg.append(packer.make_can_msg("STEERING_CONTROL", 2, {}))
  msg.append(packer.make_can_msg("ACC_HUD", 2, {}))
  msg.append(packer.make_can_msg("BRAKE_COMMAND", 2, {}))

  if idx % 5 == 0:
    msg.append(rpacker.make_can_msg("RADAR_DIAGNOSTIC", 1, {"RADAR_STATE": 0x79}))
    for i in range(16):
      msg.append(rpacker.make_can_msg("TRACK_%d" % i, 1, {"LONG_DIST": 255.5}))

  pm.send('can', can_list_to_can_capnp(msg))

pm = messaging.PubMaster(["can"])

def can_function_runner(exit_event: threading.Event):
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
    threads.append(threading.Thread(target=imu_callback, args=(exit_event,)))
    threads.append(threading.Thread(target=gps_callback, args=(exit_event,)))

    for t in threads:
      t.start()

    for t in reversed(threads):
        t.join()

