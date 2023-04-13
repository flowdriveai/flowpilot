from common.params import Params
from common.transformations.camera import tici_f_focal_length, tici_e_focal_length, eon_f_focal_length,\
                                          eon_f_frame_size, tici_f_frame_size, tici_e_frame_size
import numpy as np

def get_intrinsic_matrix(focal_length, frame_size):
    return np.array([
    [focal_length,  0.0,  float(frame_size[0])/2],
    [0.0,  focal_length,  float(frame_size[1])/2],
    [0.0,  0.0,                              1.0]]).astype("float32")

p = Params()

p.put("WideCameraMatrix", get_intrinsic_matrix(tici_e_focal_length, tici_e_frame_size).flatten().tobytes())
p.put("F3CameraMatrix", get_intrinsic_matrix(tici_f_focal_length, tici_f_frame_size).flatten().tobytes())
p.put("CameraMatrix", get_intrinsic_matrix(eon_f_focal_length, eon_f_frame_size).flatten().tobytes())

p.put_bool('EndToEndToggle', False)
p.put_bool('EnableWideCamera', False)
p.put_bool('JoystickDebugMode', False)
p.put_bool('DisengageOnAccelerator', True)
p.put_bool('IsMetric', False)
p.put_bool('IsLdwEnabled', False)
p.put_bool('Passive', False)
p.put_bool('FlowpilotEnabledToggle', True)
p.put_bool('WideCameraOnly', True)

print("done")
