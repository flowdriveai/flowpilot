source .env

export USE_GPU="1"
export PASSIVE="0"
export ROAD_CAMERA_SOURCE="0" # no affect on android
#export USE_PARAMS_NATIVE="1" # use java params over socket or native
#export MSGQ="1"
export ZMQ_MESSAGING_PROTOCOL="TCP" # TCP, INTER_PROCESS, SHARED_MEMORY
export ZMQ_MESSAGING_ADDRESS="127.0.0.1"
export SIMULATION="1"
export FINGERPRINT="HONDA CIVIC 2016"

flowinit
