from cereal import log
from common.realtime import sec_since_boot

def can_list_to_can_capnp(can_msgs, msgtype='can', valid=True):
  dat = log.Event.new_message()
  dat.init(msgtype, len(can_msgs))
  dat.valid = True
  dat.logMonoTime = sec_since_boot() * 1e9

  for i, can_msg in enumerate(can_msgs):
    if msgtype == 'sendcan':
      cc = dat.sendcan[i]
    else:
      cc = dat.can[i]

    cc.address = can_msg[0]
    cc.busTime = can_msg[1]
    cc.dat = bytes(can_msg[2])
    cc.src = can_msg[3]

  return dat.to_bytes()
