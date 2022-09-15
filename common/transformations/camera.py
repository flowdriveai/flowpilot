from common.transformations.orientation import rot_from_euler
import numpy as np

def get_view_frame_from_road_frame(roll, pitch, yaw, height): 
    R = rot_from_euler([roll, pitch, yaw])
    t = np.array([[0.0, height, 0.0]])
    return np.vstack([R, t]).T