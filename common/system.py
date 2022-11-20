import os
import platform
from functools import lru_cache
from .params import Params

def cache(user_function):
  return lru_cache(maxsize=None)(user_function)

class System:
    linux = "Linux"
    mac = "Darwin"
    windows = "Windows"
    unknown = "Unknown"
    android = "Android"

@cache
def is_android():
    return "ANDROID_ROOT" in os.environ

@cache
def is_android_rooted():
    if not is_android():
        return False
    try: 
        os.listdir("/sys") # TODO: is there any better way ?
        return True
    except PermissionError:
        return False
@cache
def is_desktop():
    return get_platform() != System.android
@cache
def get_platform():
    system = platform.system()
    if system == System.linux:
        if is_android(): return System.android
        else: return System.linux
    return system

def is_registered_device() -> bool:
    # TODO: shift fucntion to better place.
    params = Params()
    dongle_id = params.get("DongleId", encoding='utf8')
    return dongle_id not in (None, "UnregisteredDevice")
