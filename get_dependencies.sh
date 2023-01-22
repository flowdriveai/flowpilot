sudo apt-get update

# these need to be installed before requirements.txt 
pip install pkgconfig==1.5.5
pip install Cython==0.29.32

sudo apt-get install -y rsync clang capnproto libcapnp-dev libzmq3-dev cmake libjson11-1 libjson11-1-dev liblmdb-dev libusb-1.0-0-dev
sudo apt-get install -y dfu-util gcc-arm-none-eabi libcurl4-openssl-dev libssl-dev

SCRIPT=$(realpath "$0")
DIR=$(dirname "$SCRIPT")

# install capnpc-java
if ! command -v capnpc-java --version &> /dev/null  # TODO: Running through scons misses this
then
    sh $DIR/libs/capnpc-java/build.sh
fi

# pycapnp without wheel build can fail on some systems, in this case, its built from scratch later in the process.
pip install pycapnp==1.0.0 --install-option="--force-system-libcapnp" > /dev/null 2>&1
pip install -r requirements.txt

# target for scons
touch .dep_update

