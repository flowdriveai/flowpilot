source .env

export ROAD_CAMERA_SOURCE="tmp" # no affect on android
export USE_GPU="1" # no affect on android, gpu always used on android
export PASSIVE="0"
#export MSGQ="1"
export ZMQ_MESSAGING_PROTOCOL="TCP" # TCP, INTER_PROCESS, SHARED_MEMORY
export ZMQ_MESSAGING_ADDRESS="127.0.0.1"

export SIMULATION="1"
#export FINGERPRINT="HONDA CIVIC 2016"

## android specific ##
export USE_SNPE="0" # only works for snapdragon devices.

flowinit
