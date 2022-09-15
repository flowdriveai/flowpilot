from cereal import messaging
import cv2
import numpy as np
import time


topic = 'roadCameraState'
sm = messaging.SubMaster([topic])

while True:
    sm.update()
    if sm.updated[topic]:
        image_arr = np.frombuffer(sm[topic].image, dtype='uint8')
        image_arr.shape = (874, 1164, 3)
        cv2.imshow('img', image_arr)
        cv2.waitKey(1)

