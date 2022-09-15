import time
import multiprocessing

class Ratelimiter():
  def __init__(self, rate, verbose=False):
    """Rate in Hz for ratekeeping."""
    self.interval = 1. / rate
    self.next_target = time.time() + self.interval
    self.lagging = False
    self.verbose = verbose
    self._process_name = multiprocessing.current_process().name

  # Maintain loop rate by calling this at the end of each loop
  def keep_time(self):
    sleep = self.next_target - time.time()
    self.next_target += self.interval
    if sleep < 0:
      self.lagging = True
      if self.verbose: print('lagging: ', abs(sleep), 'ms.')
    else:
      time.sleep(sleep)
      self.lagging = False

  def reset(self):
    self.next_target = time.time() + self.interval
