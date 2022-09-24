# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/car/ford/values.py
from selfdrive.car import dbc_dict
from cereal import car
Ecu = car.CarParams.Ecu

MAX_ANGLE = 87.  # make sure we never command the extremes (0xfff) which cause latching fault

class CAR:
  FUSION = "FORD FUSION 2018"

DBC = {
  CAR.FUSION: dbc_dict('ford_fusion_2018_pt', 'ford_fusion_2018_adas'),
}
