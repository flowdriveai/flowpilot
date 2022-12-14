# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/car/ford/carstate.py
from cereal import car
from common.conversions import Conversions as CV
from common.numpy_fast import mean
from opendbc.can.parser import CANParser
from selfdrive.car.interfaces import CarStateBase
from selfdrive.car.ford.values import DBC

WHEEL_RADIUS = 0.33

class CarState(CarStateBase):
  def update(self, cp):
    ret = car.CarState.new_message()

    ret.wheelSpeeds = self.get_wheel_speeds(
      cp.vl["WheelSpeed_CG1"]["WhlFl_W_Meas"],
      cp.vl["WheelSpeed_CG1"]["WhlFr_W_Meas"],
      cp.vl["WheelSpeed_CG1"]["WhlRl_W_Meas"],
      cp.vl["WheelSpeed_CG1"]["WhlRr_W_Meas"],
      unit=WHEEL_RADIUS,
    )
    ret.vEgoRaw = mean([ret.wheelSpeeds.rr, ret.wheelSpeeds.rl, ret.wheelSpeeds.fr, ret.wheelSpeeds.fl])
    ret.vEgo, ret.aEgo = self.update_speed_kf(ret.vEgoRaw)
    ret.standstill = not ret.vEgoRaw > 0.001
    ret.steeringAngleDeg = cp.vl["Steering_Wheel_Data_CG1"]["SteWhlRelInit_An_Sns"]
    ret.steeringPressed = not cp.vl["Lane_Keep_Assist_Status"]["LaHandsOff_B_Actl"]
    ret.steerFaultPermanent = cp.vl["Lane_Keep_Assist_Status"]["LaActDeny_B_Actl"] == 1
    ret.cruiseState.speed = cp.vl["Cruise_Status"]["Set_Speed"] * CV.MPH_TO_MS
    ret.cruiseState.enabled = not (cp.vl["Cruise_Status"]["Cruise_State"] in (0, 3))
    ret.cruiseState.available = cp.vl["Cruise_Status"]["Cruise_State"] != 0
    ret.gas = cp.vl["EngineData_14"]["ApedPosScal_Pc_Actl"] / 100.
    ret.gasPressed = ret.gas > 1e-6
    ret.brakePressed = bool(cp.vl["Cruise_Status"]["Brake_Drv_Appl"])
    ret.genericToggle = bool(cp.vl["Steering_Buttons"]["Dist_Incr"])
    # TODO: we also need raw driver torque, needed for Assisted Lane Change
    self.lkas_state = cp.vl["Lane_Keep_Assist_Status"]["LaActAvail_D_Actl"]

    return ret

  @staticmethod
  def get_can_parser(CP):
    signals = [
      # sig_name, sig_address
      ("WhlRr_W_Meas", "WheelSpeed_CG1"),
      ("WhlRl_W_Meas", "WheelSpeed_CG1"),
      ("WhlFr_W_Meas", "WheelSpeed_CG1"),
      ("WhlFl_W_Meas", "WheelSpeed_CG1"),
      ("SteWhlRelInit_An_Sns", "Steering_Wheel_Data_CG1"),
      ("Cruise_State", "Cruise_Status"),
      ("Set_Speed", "Cruise_Status"),
      ("LaActAvail_D_Actl", "Lane_Keep_Assist_Status"),
      ("LaHandsOff_B_Actl", "Lane_Keep_Assist_Status"),
      ("LaActDeny_B_Actl", "Lane_Keep_Assist_Status"),
      ("ApedPosScal_Pc_Actl", "EngineData_14"),
      ("Dist_Incr", "Steering_Buttons"),
      ("Brake_Drv_Appl", "Cruise_Status"),
    ]
    checks = []
    return CANParser(DBC[CP.carFingerprint]["pt"], signals, checks, 0, enforce_checks=False)
