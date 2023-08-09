# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/controls/plannerd.py
import cereal.messaging as messaging
from cereal import car
from common.params import Params
from common.realtime import Priority, config_realtime_process
from system.swaglog import cloudlog
from selfdrive.controls.lib.longitudinal_planner import LongitudinalPlanner
from selfdrive.controls.lib.lateral_planner import LateralPlanner


def plannerd_thread(sm=None, pm=None):
  config_realtime_process(5, Priority.CTRL_LOW)

  cloudlog.info("plannerd is waiting for CarParams")
  params = Params()
  CP = car.CarParams.from_bytes(params.get("CarParams", block=True))
  cloudlog.info("plannerd got CarParams: %s", CP.carName)

  longitudinal_planner = LongitudinalPlanner(CP)

  use_lanelines = not params.get_bool('EndToEndToggle')
  lateral_planner = LateralPlanner(CP, use_lanelines=use_lanelines)

  if sm is None:
    sm = messaging.SubMaster(['carControl', 'carState', 'controlsState', 'radarState', 'modelV2'],
                             poll=['radarState', 'modelV2'], ignore_avg_freq=['radarState'])

  if pm is None:
    pm = messaging.PubMaster(['longitudinalPlan', 'lateralPlan'])

  while True:
    sm.update()

    if sm.updated['modelV2']:
      lateral_planner.update(sm)
      lateral_planner.publish(sm, pm)
      longitudinal_planner.update(sm)
      longitudinal_planner.publish(sm, pm)


def main(sm=None, pm=None):
  plannerd_thread(sm, pm)


if __name__ == "__main__":
  main()
