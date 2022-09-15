import numpy as np

def intrinsic_from_fov(height, width, fov=90):
    '''
    computes camera intrinsic matrix K given the field
    of view can image dimensions. fx and fy is in meters.
    '''
    px, py = (width / 2, height / 2)
    hfov = fov / 360. * 2. * np.pi
    fx = width / (2. * np.tan(hfov / 2.))

    vfov = 2. * np.arctan(np.tan(hfov / 2) * height / width)
    fy = height / (2. * np.tan(vfov / 2.))

    return np.array([[fx, 0, px],
                     [0, fy, py],
                     [0, 0, 1.],])

