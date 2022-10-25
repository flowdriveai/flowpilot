source .env

#export USE_CUDA="1" # Has no affect on android
export PASSIVE="0"
export ROAD_CAMERA_SOURCE="0" # no affect on android
export USE_PARAMS_CLIENT="1"
#export MSGQ="1"
export ZMQ_MESSAGING_PROTOCOL="TCP" # TCP, INTER_PROCESS, SHARED_MEMORY
export ZMQ_MESSAGING_ADDRESS="127.0.0.1"
export SIMULATION="1"
export FINGERPRINT="HONDA CIVIC 2016"

flowinit

