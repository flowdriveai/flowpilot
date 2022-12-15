from common.params import Params
from cereal.messaging import log
import numpy as np

p = Params()

live_calib_bytes = p.get("CalibrationParams")
if live_calib_bytes is not None:
    print("extrinsic calibration rpy deg:", np.rad2deg(log.Event.from_bytes(live_calib_bytes).liveCalibration.rpyCalib))
else:
    print("extrinsic calibration not yet done")

intrinsic_calib_bytes = p.get("CameraMatrix")
if intrinsic_calib_bytes is not None:
    print("intrinsic calibration:", np.frombuffer(intrinsic_calib_bytes, dtype="float32"))
else:
    print("extrriintrinsicnsic calibration not yet done")
