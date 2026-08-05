#!/bin/bash
set -e

echo "Installing Linux system dependencies..."
sudo apt-get update
sudo apt-get install -y build-essential clang scons capnproto libcapnp-dev libusb-1.0-0-dev python3-pip python3-dev

echo "Installing Python requirements..."
pip install -r requirements.txt
pip install -e .

echo "Setup completed successfully."
