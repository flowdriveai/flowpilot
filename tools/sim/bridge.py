#!/usr/bin/env python3
import argparse
import math
import signal
import threading
import time
from multiprocessing import Process, Queue
from typing import Any

import carla  # pylint: disable=import-error
import numpy as np

import cereal.messaging as messaging
from cereal import log
from common.basedir import BASEDIR
from common.numpy_fast import clip
from common.params import Params
from common.realtime import DT_DMON, Ratekeeper
from selfdrive.car.honda.values import CruiseButtons
from tools.sim.lib.can import can_function

W, H = 1164, 874
REPEAT_COUNTER = 5
PRINT_DECIMATION = 100
STEER_RATIO = 15.
K = [831, 0,  582,
      0, 831, 437,
      0,  0,   1 ]

pm = messaging.PubMaster(['roadCameraState', 'sensorEvents', 'can', "gpsLocationExternal"])
sm = messaging.SubMaster(['carControl', 'controlsState'])

def parse_args(add_args=None):
  parser = argparse.ArgumentParser(description='Bridge between CARLA and openpilot.')
  parser.add_argument('--joystick', action='store_true')
  parser.add_argument('--high_quality', action='store_true')
  parser.add_argument('--town', type=str, default='Town04_Opt')
  parser.add_argument('--spawn_point', dest='num_selected_spawn_point', type=int, default=16)

  return parser.parse_args(add_args)


class VehicleState:
  def __init__(self):
    self.speed = 0.0
    self.angle = 0.0
    self.bearing_deg = 0.0
    self.vel = carla.Vector3D()
    self.cruise_button = 0
    self.is_engaged = False
    self.ignition = True


def steer_rate_limit(old, new):
  # Rate limiting to 0.5 degrees per step
  limit = 0.5
  if new > old + limit:
    return old + limit
  elif new < old - limit:
    return old - limit
  else:
    return new


class Camerad:
  def __init__(self):
    self.frame_road_id = 0

  def cam_callback_road(self, image):
    self._cam_callback(image, self.frame_road_id, 'roadCameraState')
    self.frame_road_id += 1

  def _cam_callback(self, image, frame_id, topic):
    img = np.frombuffer(image.raw_data, dtype=np.dtype("uint8"))
    img = np.reshape(img, (H, W, 4))
    img = img[:, :, [0, 1, 2]].copy()

    dat = messaging.new_message(topic)
    msg = {
      "frameId": frame_id,
      "intrinsics": K,
      "image": img.tobytes()
    }
    setattr(dat, topic, msg)
    pm.send(topic, dat)

def imu_callback(imu, vehicle_state):
  vehicle_state.bearing_deg = math.degrees(imu.compass)
  dat = messaging.new_message('sensorEvents', 2)
  dat.sensorEvents[0].sensor = 4
  dat.sensorEvents[0].type = 0x10
  dat.sensorEvents[0].init('acceleration')
  dat.sensorEvents[0].acceleration.v = [imu.accelerometer.x, imu.accelerometer.y, imu.accelerometer.z]
  # copied these numbers from locationd
  dat.sensorEvents[1].sensor = 5
  dat.sensorEvents[1].type = 0x10
  dat.sensorEvents[1].init('gyroUncalibrated')
  dat.sensorEvents[1].gyroUncalibrated.v = [imu.gyroscope.x, imu.gyroscope.y, imu.gyroscope.z]
  pm.send('sensorEvents', dat)

def panda_state_function(vs: VehicleState, exit_event: threading.Event):
  pm = messaging.PubMaster(['pandaStates'])
  while not exit_event.is_set():
    dat = messaging.new_message('pandaStates', 1)
    dat.valid = True
    dat.pandaStates[0] = {
      'ignitionLine': vs.ignition,
      'pandaType': "blackPanda",
      'controlsAllowed': True,
      'safetyModel': 'hondaNidec',
    }
    pm.send('pandaStates', dat)
    time.sleep(0.5)


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


def gps_callback(gps, vehicle_state):
  dat = messaging.new_message('gpsLocationExternal')

  # transform vel from carla to NED
  # north is -Y in CARLA
  velNED = [
    -vehicle_state.vel.y,  # north/south component of NED is negative when moving south
    vehicle_state.vel.x,  # positive when moving east, which is x in carla
    vehicle_state.vel.z,
  ]

  dat.gpsLocationExternal = {
    "unixTimestampMillis": int(time.time() * 1000),
    "flags": 1,  # valid fix
    "accuracy": 1.0,
    "verticalAccuracy": 1.0,
    "speedAccuracy": 0.1,
    "bearingAccuracyDeg": 0.1,
    "vNED": velNED,
    "bearingDeg": vehicle_state.bearing_deg,
    "latitude": gps.latitude,
    "longitude": gps.longitude,
    "altitude": gps.altitude,
    "speed": vehicle_state.speed,
    "source": log.GpsLocationData.SensorSource.ublox,
  }

  pm.send('gpsLocationExternal', dat)


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


def can_function_runner(vs: VehicleState, exit_event: threading.Event):
  i = 0
  while not exit_event.is_set():
    can_function(pm, vs.speed, vs.angle, i, vs.cruise_button, vs.is_engaged)
    time.sleep(0.01)
    i += 1


def connect_carla_client():
  client = carla.Client("127.0.0.1", 2000)
  client.set_timeout(5)
  return client


class CarlaBridge:

  def __init__(self, arguments):
    msg = messaging.new_message('liveCalibration')
    msg.liveCalibration.validBlocks = 20
    msg.liveCalibration.rpyCalib = [0.0, 0.0, 0.0]
    Params().put("CalibrationParams", msg.to_bytes())

    self._args = arguments
    self._carla_objects = []
    self._camerad = None
    self._exit_event = threading.Event()
    self._threads = []
    self._keep_alive = True
    self.started = False
    signal.signal(signal.SIGTERM, self._on_shutdown)
    self._exit = threading.Event()

  def _on_shutdown(self, signal, frame):
    self._keep_alive = False

  def bridge_keep_alive(self, q: Queue, retries: int):
    try:
      while self._keep_alive:
        try:
          self._run(q)
          break
        except RuntimeError as e:
          self.close()
          if retries == 0:
            raise

          # Reset for another try
          self._carla_objects = []
          self._threads = []
          self._exit_event = threading.Event()

          retries -= 1
          if retries <= -1:
            print(f"Restarting bridge. Error: {e} ")
          else:
            print(f"Restarting bridge. Retries left {retries}. Error: {e} ")
    finally:
      # Clean up resources in the opposite order they were created.
      self.close()

  def _run(self, q: Queue):
    client = connect_carla_client()
    world = client.load_world(self._args.town)

    settings = world.get_settings()
    settings.synchronous_mode = True  # Enables synchronous mode
    settings.fixed_delta_seconds = 0.05
    world.apply_settings(settings)

    world.set_weather(carla.WeatherParameters.ClearSunset)

    if not self._args.high_quality:
      world.unload_map_layer(carla.MapLayer.Foliage)
      world.unload_map_layer(carla.MapLayer.Buildings)
      world.unload_map_layer(carla.MapLayer.ParkedVehicles)
      world.unload_map_layer(carla.MapLayer.Props)
      world.unload_map_layer(carla.MapLayer.StreetLights)
      world.unload_map_layer(carla.MapLayer.Particles)

    blueprint_library = world.get_blueprint_library()

    world_map = world.get_map()

    vehicle_bp = blueprint_library.filter('vehicle.tesla.*')[1]
    spawn_points = world_map.get_spawn_points()
    assert len(spawn_points) > self._args.num_selected_spawn_point, f'''No spawn point {self._args.num_selected_spawn_point}, try a value between 0 and
      {len(spawn_points)} for this town.'''
    spawn_point = spawn_points[self._args.num_selected_spawn_point]
    vehicle = world.spawn_actor(vehicle_bp, spawn_point)
    self._carla_objects.append(vehicle)
    max_steer_angle = vehicle.get_physics_control().wheels[0].max_steer_angle

    # make tires less slippery
    # wheel_control = carla.WheelPhysicsControl(tire_friction=5)
    physics_control = vehicle.get_physics_control()
    physics_control.mass = 2326
    # physics_control.wheels = [wheel_control]*4
    physics_control.torque_curve = [[20.0, 500.0], [5000.0, 500.0]]
    physics_control.gear_switch_time = 0.0
    vehicle.apply_physics_control(physics_control)

    transform = carla.Transform(carla.Location(x=0.8, z=1.13))

    def create_camera(fov, callback):
      blueprint = blueprint_library.find('sensor.camera.rgb')
      blueprint.set_attribute('image_size_x', str(W))
      blueprint.set_attribute('image_size_y', str(H))
      blueprint.set_attribute('fov', str(fov))
      if not self._args.high_quality:
        blueprint.set_attribute('enable_postprocess_effects', 'False')
      camera = world.spawn_actor(blueprint, transform, attach_to=vehicle)
      camera.listen(callback)
      return camera

    self._camerad = Camerad()

    road_camera = create_camera(fov=70, callback=self._camerad.cam_callback_road)  
    self._carla_objects.append(road_camera)

    vehicle_state = VehicleState()

    # reenable IMU
    imu_bp = blueprint_library.find('sensor.other.imu')
    imu = world.spawn_actor(imu_bp, transform, attach_to=vehicle)
    imu.listen(lambda imu: imu_callback(imu, vehicle_state))

    gps_bp = blueprint_library.find('sensor.other.gnss')
    gps = world.spawn_actor(gps_bp, transform, attach_to=vehicle)
    gps.listen(lambda gps: gps_callback(gps, vehicle_state))

    self._carla_objects.extend([imu, gps])
    # launch fake car threads
    self._threads.append(threading.Thread(target=panda_state_function, args=(vehicle_state, self._exit_event,)))
    self._threads.append(threading.Thread(target=peripheral_state_function, args=(self._exit_event,)))
    self._threads.append(threading.Thread(target=fake_driver_monitoring, args=(self._exit_event,)))
    self._threads.append(threading.Thread(target=can_function_runner, args=(vehicle_state, self._exit_event,)))
    for t in self._threads:
      t.setDaemon(True)    
      t.start()

    # init
    throttle_ease_out_counter = REPEAT_COUNTER
    brake_ease_out_counter = REPEAT_COUNTER
    steer_ease_out_counter = REPEAT_COUNTER

    vc = carla.VehicleControl(throttle=0, steer=0, brake=0, reverse=False)

    is_openpilot_engaged = False
    throttle_out = steer_out = brake_out = 0.
    throttle_op = steer_op = brake_op = 0.
    throttle_manual = steer_manual = brake_manual = 0.

    old_steer = old_brake = old_throttle = 0.
    throttle_manual_multiplier = 0.7  # keyboard signal is always 1
    brake_manual_multiplier = 0.7  # keyboard signal is always 1
    steer_manual_multiplier = 45 * STEER_RATIO  # keyboard signal is always 1

    # Simulation tends to be slow in the initial steps. This prevents lagging later
    for _ in range(20):
      world.tick()

    # loop
    rk = Ratekeeper(100, print_delay_threshold=None)

    while self._keep_alive:
      # 1. Read the throttle, steer and brake from op or manual controls
      # 2. Set instructions in Carla
      # 3. Send current carstate to op via can

      cruise_button = 0
      throttle_out = steer_out = brake_out = 0.0
      throttle_op = steer_op = brake_op = 0.0
      throttle_manual = steer_manual = brake_manual = 0.0

      # --------------Step 1-------------------------------
      if not q.empty():
        message = q.get()
        m = message.split('_')
        if m[0] == "steer":
          steer_manual = float(m[1])
          is_openpilot_engaged = False
        elif m[0] == "throttle":
          throttle_manual = float(m[1])
          is_openpilot_engaged = False
        elif m[0] == "brake":
          brake_manual = float(m[1])
          is_openpilot_engaged = False
        elif m[0] == "reverse":
          cruise_button = CruiseButtons.CANCEL
          is_openpilot_engaged = False
        elif m[0] == "cruise":
          if m[1] == "down":
            cruise_button = CruiseButtons.DECEL_SET
            is_openpilot_engaged = True
          elif m[1] == "up":
            cruise_button = CruiseButtons.RES_ACCEL
            is_openpilot_engaged = True
          elif m[1] == "cancel":
            cruise_button = CruiseButtons.CANCEL
            is_openpilot_engaged = False
        elif m[0] == "ignition":
          vehicle_state.ignition = not vehicle_state.ignition
        elif m[0] == "quit":
          break

        throttle_out = throttle_manual * throttle_manual_multiplier
        steer_out = steer_manual * steer_manual_multiplier
        brake_out = brake_manual * brake_manual_multiplier

        old_steer = steer_out
        old_throttle = throttle_out
        old_brake = brake_out

      if is_openpilot_engaged:
        sm.update(0)

        # TODO gas and brake is deprecated
        throttle_op = clip(sm['carControl'].actuators.accel / 1.6, 0.0, 1.0)
        brake_op = clip(-sm['carControl'].actuators.accel / 4.0, 0.0, 1.0)
        steer_op = sm['carControl'].actuators.steeringAngleDeg

        throttle_out = throttle_op
        steer_out = steer_op
        brake_out = brake_op

        steer_out = steer_rate_limit(old_steer, steer_out)
        old_steer = steer_out

      else:
        if throttle_out == 0 and old_throttle > 0:
          if throttle_ease_out_counter > 0:
            throttle_out = old_throttle
            throttle_ease_out_counter += -1
          else:
            throttle_ease_out_counter = REPEAT_COUNTER
            old_throttle = 0

        if brake_out == 0 and old_brake > 0:
          if brake_ease_out_counter > 0:
            brake_out = old_brake
            brake_ease_out_counter += -1
          else:
            brake_ease_out_counter = REPEAT_COUNTER
            old_brake = 0

        if steer_out == 0 and old_steer != 0:
          if steer_ease_out_counter > 0:
            steer_out = old_steer
            steer_ease_out_counter += -1
          else:
            steer_ease_out_counter = REPEAT_COUNTER
            old_steer = 0

      # --------------Step 2-------------------------------
      steer_carla = steer_out / (max_steer_angle * STEER_RATIO * -1)

      steer_carla = np.clip(steer_carla, -1, 1)
      steer_out = steer_carla * (max_steer_angle * STEER_RATIO * -1)
      old_steer = steer_carla * (max_steer_angle * STEER_RATIO * -1)

      vc.throttle = throttle_out / 0.6
      vc.steer = steer_carla
      vc.brake = brake_out
      vehicle.apply_control(vc)

      # --------------Step 3-------------------------------
      vel = vehicle.get_velocity()
      speed = math.sqrt(vel.x ** 2 + vel.y ** 2 + vel.z ** 2)  # in m/s
      vehicle_state.speed = speed
      vehicle_state.vel = vel
      vehicle_state.angle = steer_out
      vehicle_state.cruise_button = cruise_button
      vehicle_state.is_engaged = is_openpilot_engaged

      if rk.frame % PRINT_DECIMATION == 0:
        print("frame: ", "engaged:", is_openpilot_engaged, "; throttle: ", round(vc.throttle, 3), "; steer(c/deg): ",
              round(vc.steer, 3), round(steer_out, 3), "; brake: ", round(vc.brake, 3))

      if rk.frame % 5 == 0:
        world.tick()
      rk.keep_time()
      self.started = True

  def close(self):
    self.started = False
    self._exit_event.set()

    for s in self._carla_objects:
      try:
        s.destroy()
      except Exception as e:
        print("Failed to destroy carla object", e)
    for t in reversed(self._threads):
      t.join()

  def run(self, queue, retries=-1):
    bridge_p = Process(target=self.bridge_keep_alive, args=(queue, retries), daemon=True)
    bridge_p.start()
    return bridge_p


if __name__ == "__main__":
  q: Any = Queue()
  args = parse_args()

  try:
    carla_bridge = CarlaBridge(args)
  
    if args.joystick:
      # start input poll for joystick
      from tools.sim.lib.manual_ctrl import wheel_poll_thread
      threading.Thread(target=wheel_poll_thread, args=(q,), daemon=True).start()
    else:
      # start input poll for keyboard
      from tools.sim.lib.keyboard_ctrl import keyboard_poll_thread
      threading.Thread(target=keyboard_poll_thread, args=(q,), daemon=True).start()
  
    p = carla_bridge.bridge_keep_alive(q, retries=-1)
    p.join()
  finally:
    print("exiting")
