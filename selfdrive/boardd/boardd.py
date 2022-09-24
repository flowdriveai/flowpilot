# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/boardd/boardd.py
# pylint: skip-file

# Cython, now uses scons to build
from selfdrive.boardd.boardd_api_impl import can_list_to_can_capnp
assert can_list_to_can_capnp

def can_capnp_to_can_list(can, src_filter=None):
  ret = []
  for msg in can:
    if src_filter is None or msg.src in src_filter:
      ret.append((msg.address, msg.busTime, msg.dat, msg.src))
  return ret
