import os
import shutil


def get_flowpilot_root():
    return os.path.dirname(os.path.dirname(os.path.realpath(__file__)))

def internal(path):
    return os.path.join(get_flowpilot_root(), path)

def init_data_dir():
    data_dir = internal('data')
    if not os.path.exists(data_dir):
        os.mkdir(data_dir)

def clear_data_dir():
    try:
        data_dir = internal('data')
        if os.path.exists(data_dir):
            shutil.rmtree(internal('data'))
            init_data_dir()
    except OSError as e:
        print ("Error: %s - %s." % (e.filename, e.strerror))

def is_fp_root_cwd():
    return get_flowpilot_root() == os.getcwd()

BASEDIR = get_flowpilot_root()
