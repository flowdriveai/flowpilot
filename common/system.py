import os
import platform

class System:
    linux = "Linux"
    mac = "Darwin"
    windows = "Windows"
    unknown = "Unknown"
    android = "Android"


def is_android():
    return "ANDROID_ROOT" in os.environ
    
def is_android_rooted():
    if not is_android():
        return False
    try: 
        os.listdir("/sys") # TODO: is there any better way ?
        return True
    except PermissionError:
        return False
    
def is_desktop():
    return get_platform() != System.android

def get_platform():
    system = platform.system()
    if system == System.linux:
        if is_android(): return System.android
        else: return System.linux
    return system
