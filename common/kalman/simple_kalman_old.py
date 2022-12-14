# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/common/kalman/simple_kalman_old.py
import numpy as np


class KF1D:
  # this EKF assumes constant covariance matrix, so calculations are much simpler
  # the Kalman gain also needs to be precomputed using the control module

  def __init__(self, x0, A, C, K):
    self.x = x0
    self.A = A
    self.C = np.atleast_2d(C)
    self.K = K

    self.A_K = self.A - np.dot(self.K, self.C)

    # K matrix needs to  be pre-computed as follow:
    # import control
    # (x, l, K) = control.dare(np.transpose(self.A), np.transpose(self.C), Q, R)
    # self.K = np.transpose(K)

  def update(self, meas):
    self.x = np.dot(self.A_K, self.x) + np.dot(self.K, meas)
    return self.x
