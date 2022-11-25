# these need to be installed before requirements.txt 
pip install pkgconfig==1.5.5
pip install Cython==0.29.30

sudo apt-get install -y rsync clang capnproto libcapnp-dev libzmq3-dev cmake libjson11-1 libjson11-1-dev liblmdb-dev libusb-1.0-0-dev
sudo apt-get install -y dfu-util gcc-arm-none-eabi libcurl4-openssl-dev libssl-dev

# install capnpc-java
SCRIPT=$(realpath "$0")
DIR=$(dirname "$SCRIPT")
sh $DIR/libs/capnpc-java/build.sh

pip install pycapnp==1.0.0 --install-option="--force-system-libcapnp"
pip install -r requirements.txt

