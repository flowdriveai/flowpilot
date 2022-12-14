# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/car/vin.py
import traceback

import cereal.messaging as messaging
from panda.python.uds import FUNCTIONAL_ADDRS
from selfdrive.car.isotp_parallel_query import IsoTpParallelQuery

VIN_REQUEST = b'\x09\x02'
VIN_RESPONSE = b'\x49\x02\x01'
VIN_UNKNOWN = "0" * 17


def get_vin(logcan, sendcan, bus, timeout=0.1, retry=5, debug=False):
  for i in range(retry):
    try:
      query = IsoTpParallelQuery(sendcan, logcan, bus, FUNCTIONAL_ADDRS, [VIN_REQUEST], [VIN_RESPONSE], functional_addr=True, debug=debug)
      for addr, vin in query.get_data(timeout).items():
        return addr[0], vin.decode()
      print(f"vin query retry ({i+1}) ...")
    except Exception:
      print(f"VIN query exception: {traceback.format_exc()}")

  return 0, VIN_UNKNOWN


if __name__ == "__main__":
  import time
  sendcan = messaging.pub_sock('sendcan')
  logcan = messaging.sub_sock('can')
  time.sleep(1)
  addr, vin = get_vin(logcan, sendcan, 1, debug=False)
  print(hex(addr), vin)
