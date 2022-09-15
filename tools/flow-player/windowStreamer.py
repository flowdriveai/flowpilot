import time
import os
import time
import sys, getopt
import numpy as np
import threading
import cv2
import win32gui, win32ui, win32con, win32api
import ctypes
from threadUtils import synchronized
from imUtils import smart_resize, intrinsic_from_fov
from rateLimiter import Ratelimiter
import cereal.messaging as messaging

class WindowStreamer():

    def __init__(self, target_resolution=None):
        self.scaling_factor = ctypes.windll.shcore.GetScaleFactorForDevice(0) / 100
        self.win_handles = {}
        self._streaming = False
        self.K = None
        self.target_resolution = target_resolution
        self._event = threading.Event()
        self.errors = []
        self.rl = Ratelimiter(20)
        self.pm = messaging.PubMaster()

    @staticmethod
    def getWindowsName(hwnd, result):
        win_name = win32gui.GetWindowText(hwnd)
        result[win_name] = hwnd

    @staticmethod
    def getWindowDims(hwin, scaling_factor):
        # (x1, y1): top-left, (x2, y2): bottom-right
        x1, y1 ,x2, y2 = win32gui.GetWindowRect(hwin)
        width = x2 - x1
        height = y2 - y1
        return (np.array((x1, y1, width, height))*scaling_factor).astype('int')

    def updateAvailableWindows(self):
        self.win_handles.clear()
        win32gui.EnumWindows(WindowStreamer.getWindowsName, self.win_handles)

    def searchWindow(self, query):
        result = {}
        if not self.win_handles: self.updateAvailableWindows()
        for window_name in self.win_handles.keys():
            if query.lower() in window_name.lower():
                result[window_name] = self.win_handles[window_name]
        return result

    def isStreaming(self):
        return self._streaming

    @synchronized
    def startStreaming(self, query, topic):
        if self._streaming:
            print("Stream Already Running. Stop It first")
            return False
        if self.win_handles.get(query, None) is None: return False
        self._thread = threading.Thread(target=self._runStreamThread, args=(self.win_handles[query], topic))
        self._thread.daemon = True
        self._streaming = True
        self._event.set()
        self._thread.start()
        return True

    @synchronized
    def stopStreaming(self):
        if not self._streaming:
            return
        self._event.clear()
        self._streaming = False

    @synchronized
    def pause(self):
        self._event.clear()

    @synchronized
    def resume(self):
        self._event.set()

    def _runStreamThread(self, hwin, topic):
        self.updateAvailableWindows()
        if hwin not in self.win_handles.values():
            print("Invalid Window")
            return

        hwind = win32gui.GetDesktopWindow()
        dims = WindowStreamer.getWindowDims(hwin, self.scaling_factor)
        width = int(dims[2])
        height = int(dims[3])
        self.K = intrinsic_from_fov(height, width)
        self.rl.reset()
        self.K = np.array([[960.,           0.,  582.,        ],
                             [  0.,         960., 437.,        ],
                             [  0.,           0.,  1.        ]]
                            )

        hwindc = win32gui.GetWindowDC(hwind)
        srcdc = win32ui.CreateDCFromHandle(hwindc)
        memdc = srcdc.CreateCompatibleDC()
        bmp = win32ui.CreateBitmap()
        bmp.CreateCompatibleBitmap(srcdc, width, height)
        memdc.SelectObject(bmp)

        self.pm.createPublisher(topic)
        frameId = 1

        self.rl.reset()
        while self._streaming:
            self._event.wait()
            start = time.time()
            memdc.BitBlt((0, 0), (width, height), srcdc, (dims[0], dims[1]), win32con.SRCCOPY)
            signedIntsArray = bmp.GetBitmapBits(True)
            if len(signedIntsArray) != height*width*4:
                self.errors.append("Can't retrieve window, try bringing the target window to focus and stream again.")
                self.stopStreaming()
                return
            img = np.frombuffer(signedIntsArray, dtype='uint8')
            img.shape = (height,width,4)
            img = img[:, :, :3]
            if self.target_resolution:
                img, new_K = smart_resize(img, *(self.target_resolution), K=self.K)
            msg_frame = messaging.new_message("roadCameraState")
            msg_frame.image = img.tobytes()
            msg_frame.intrinsics = self.K.flatten().tolist()
            msg_frame.frameId = frameId
            self.pm.publish({topic:msg_frame.to_segments()[0]})
            end = time.time()
            #cv2.imshow('stream', img)
            #cv2.waitKey(1)
            frameId += 1
            self.rl.keep_time()

        self.K = None
        self.pm.releaseAll()
        cv2.destroyAllWindows()
        srcdc.DeleteDC()
        memdc.DeleteDC()
        win32gui.ReleaseDC(hwin, hwindc)
        win32gui.DeleteObject(bmp.GetHandle())

    def getErrors(self):
        errs = self.errors.copy()
        self.errors.clear()
        return errs

    def __del__(self):
        self.stopStreaming()
        self.pm.releaseAll()
