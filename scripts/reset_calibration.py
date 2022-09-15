from common.params import Params
import cereal.messaging as messaging

p = Params()

msg = messaging.new_message('liveCalibration')
msg.liveCalibration.rpyCalib = [0.0, 0.0, 0.0]
p.put("CalibrationParams", msg.to_bytes())

print("done")
