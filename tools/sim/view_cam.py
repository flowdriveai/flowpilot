from cereal import messaging
import cv2
import numpy as np
from common.params import Params
from common.transformations.camera import eon_f_frame_size, tici_f_frame_size

p = Params()
if p.get_bool("F3"):
    topic = 'wideRoadCameraBuffer'
    frame_size = tici_f_frame_size
else:
    topic = 'roadCameraBuffer'
    frame_size = eon_f_frame_size
sm = messaging.SubMaster([topic])
W, H = frame_size

while True:
    sm.update()
    if sm.updated[topic]:
        image_arr = np.frombuffer(sm[topic].image, dtype='uint8')
        image_arr.shape = (H, W, 3)
        cv2.imshow('img', image_arr)
        cv2.waitKey(1)

