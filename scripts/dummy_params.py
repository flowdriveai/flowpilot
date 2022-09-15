from common.params import Params
import numpy as np

p = Params()

intrinsics = np.array([[910, 0,   1164/2],
                [0,   910,  874/2],
                [0,    0,   1    ]], dtype="float32")

p.put("CameraMatrix", intrinsics.flatten().tobytes())
p.put_bool('EndToEndToggle', False)
p.put_bool('EnableWideCamera', False)
p.put_bool('JoystickDebugMode', False)
p.put_bool('DisengageOnAccelerator', True)
p.put_bool('IsMetric', False)
p.put_bool('IsLdwEnabled', False)
p.put_bool('Passive', False)
p.put_bool('FlowpilotEnabledToggle', True)

print("done")
