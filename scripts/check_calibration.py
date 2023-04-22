from common.params import Params
from cereal.messaging import log
import numpy as np

p = Params()

live_calib_bytes = p.get("CalibrationParams")
if live_calib_bytes is not None:
    print("extrinsic calibration rpy deg:", np.rad2deg(log.Event.from_bytes(live_calib_bytes).liveCalibration.rpyCalib))
else:
    print("extrinsic calibration not yet done")

for camera_matrix in ["CameraMatrix", "F3CameraMatrix", "WideCameraMatrix"]:
    intrinsic_calib_bytes = p.get(camera_matrix)
    if intrinsic_calib_bytes is not None:
        print(camera_matrix, ":", np.frombuffer(intrinsic_calib_bytes, dtype="float32").astype(int))
