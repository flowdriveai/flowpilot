# distutils: language = c++
# cython: language_level = 3
from posix.time cimport clock_gettime, timespec, CLOCK_MONOTONIC_RAW, CLOCK_REALTIME, clockid_t

# warning, this was changed for compatibility with python and java.
CLOCK_BOOTTIME = CLOCK_REALTIME

cdef double readclock(clockid_t clock_id):
  cdef timespec ts
  cdef double current

  clock_gettime(clock_id, &ts)
  current = ts.tv_sec + (ts.tv_nsec / 1000000000.)
  return current

def monotonic_time():
  return readclock(CLOCK_MONOTONIC_RAW)

def sec_since_boot():
  return readclock(CLOCK_BOOTTIME) 
