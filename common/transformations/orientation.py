import numpy as np
from scipy.spatial.transform import Rotation

def isRotationMatrix(R) :
    Rt = np.transpose(R)
    I = np.identity(3, dtype = R.dtype)
    n = np.linalg.norm(I - np.dot(Rt, R))
    return n < 1e-6

def euler_from_rot(R) : 
    assert(isRotationMatrix(R))
    sy = np.sqrt(R[0,0] * R[0,0] +  R[1,0] * R[1,0])
    singular = sy < 1e-6
    if  not singular :
        x = np.arctan2(R[2,1] , R[2,2])
        y = np.arctan2(-R[2,0], sy)
        z = np.arctan2(R[1,0], R[0,0])
    else :
        x = np.arctan2(-R[1,2], R[1,1])
        y = np.arctan2(-R[2,0], sy)
        z = 0
    return np.array([x, y, z])


def rot_from_euler(rpy):
    return Rotation.from_euler('xyz', rpy, degrees=False).as_matrix()