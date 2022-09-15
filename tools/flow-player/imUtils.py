import numpy as np
import cv2
import functools


def get_centre_crop(img, h=874, w=1164):
    '''
    crops image to target dimenstions from
    centre of the image. the dims image should
    be greater than the target dims.
    '''
    x = (img.shape[1] - w) // 2
    y = (img.shape[0] - h) // 2
    assert x >= 0 and y >= 0
    return img[y:y+h, x:x+w]

def strip_float(num, decimal_places=2):
    '''
    stips float digits to given decimal
    places.
    '''
    return float(("{:." + str(decimal_places) + "f}").format(num))

@functools.lru_cache(maxsize=100, typed=False)
def change_aspect(height, width, target_aspect, min_dim=300):
    '''
    approximates new dimensions for converting
    curerent image aspect to target one.
    all aspects are approximated to 2 decimal places.
    errors of 1-2 don't matter much.
    '''
    target_aspect = strip_float(target_aspect)
    curr_aspect = strip_float(width / height)

    if curr_aspect == target_aspect:
        return (height, width)

    elif curr_aspect > target_aspect:
        # decrease curr_aspect
        for i in range(width, min_dim, -1):
            if strip_float(i/height) == target_aspect:
                return (height, i)

    elif curr_aspect < target_aspect:
        # increase curr_aspect
        for i in range(height, min_dim, -1):
            if strip_float(width/i) == target_aspect:
                return (i, width)
    else:
        # no valid dimensions found
        return None

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

def get_new_intrinsics(K, K_dash):
    return K_dash @ K

@functools.lru_cache(maxsize=5, typed=False)
def get_K_dash(prev_dims, new_dims):
    '''
    resizing image changes the camera intrinsic matrix.
    K' is computed which should be multiplied with the
    original camera matrix to obtain new camera matrix
    after resize operation.
    '''
    scale_x = new_dims[1] / prev_dims[1]
    scale_y = new_dims[0] / prev_dims[0]
    K_dash = np.eye(3)
    K_dash[0] *= scale_x
    K_dash[1] *= scale_y
    return K_dash

def smart_resize(img, target_height=874, target_width=1164, K=None):
    '''
    resizes image to target resolution without distortion.
    if the image dimensions are greater than target dims,
    the centre crop is returned, else new dims that match
    the target aspect ratio are calculated and image is
    centre cropped to new_dims. this cropped portion can be
    resized to target dims because now the aspect ratio is
    same.
    '''
    if img.shape[0] > target_height and img.shape[1] > target_width:
        resized = get_centre_crop(img, h=target_height, w=target_width)
        return resized, K
    new_dims = change_aspect(img.shape[0], img.shape[1], target_width/target_height)
    cropped = get_centre_crop(img, h=new_dims[0], w=new_dims[1])
    resized = cv2.resize(cropped, (target_width, target_height))
    K_dash = get_K_dash(img.shape, (target_height, target_width))
    if K is not None: return resized, K_dash @ K
    else: return resized, K
