import os

def flowpilot_root():
    return os.path.dirname(os.path.dirname(os.path.realpath(__file__)))

def internal(path):
    return os.path.join(flowpilot_root(), path)

def external_android_storage():
    return "/storage/emulated/0"

BASEDIR = flowpilot_root()
