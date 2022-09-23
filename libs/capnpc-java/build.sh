SCRIPT=$(realpath "$0")
DIR=$(dirname "$SCRIPT")
ARCHNAME=$(arch)

if [ ! -d capnproto-java/ ]; then
  git clone https://github.com/capnproto/capnproto-java.git $DIR/capnproto-java
fi
cd $DIR/capnproto-java

git checkout 81d18463a8f3c98f6d21d4eae27caaca6bace4f7

make
sudo make install

cd ..

