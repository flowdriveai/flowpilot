set -e
. ./.env

export WIDE_ROAD_CAMERA_SOURCE="selfdrive/assets/fcam.avi" # no affect on android
export ROAD_CAMERA_SOURCE="selfdrive/assets/tmp" # no affect on android
export USE_GPU="0" # no affect on android, gpu always used on android
export PASSIVE="0"
#export MSGQ="1"
#export USE_PARAMS_NATIVE="1"
export ZMQ_MESSAGING_PROTOCOL="TCP" # TCP, INTER_PROCESS, SHARED_MEMORY

#export DISCOVERABLE_PUBLISHERS="1" # if enabled, other devices on same network can access sup/pub data.
#export DEVICE_ADDR="127.0.0.1" # connect to external device running flowpilot over same network. useful for livestreaming.

export SIMULATION="0"
export FINGERPRINT="HONDA CIVIC 2016"

## android specific ##
export USE_SNPE="0" # only works for snapdragon devices.

if ! command -v tmux &> /dev/null
then
    echo "tmux could not be found, installing.."
    sudo apt-get update
    sudo apt-get install tmux
    echo "set -g mouse on" >> .tmux.conf # enable mouse scrolling in tmux
fi


if pgrep -x "flowinit" > /dev/null
    then
        echo "another instance of flowinit is already running"
        exit
    else
        # start a tmux pane
        tmux new-session -d -s "flowpilot" "scons && flowinit"
        tmux attach -t flowpilot
fi

# while true; do sleep 1; done
scons && flowinit