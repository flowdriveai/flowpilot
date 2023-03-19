from common.params import Params
from common.transformations.camera import tici_fcam_intrinsics, tici_ecam_intrinsics, eon_fcam_intrinsics
import numpy as np

p = Params()

intrinsics = tici_ecam_intrinsics if p.get_bool("F3") else eon_fcam_intrinsics

p.put("CameraMatrix", intrinsics.astype("float32").flatten().tobytes())
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
