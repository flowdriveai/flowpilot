# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/boardd/pandad.py
# simple boardd wrapper that updates the panda first
import os
import usb1
import time
import subprocess
from typing import List, NoReturn
from functools import cmp_to_key

from panda import Panda, PandaDFU, FW_PATH
from common.basedir import BASEDIR
from common.params import Params
from system.hardware import HARDWARE
from system.swaglog import cloudlog

from common.system import is_android, is_android_rooted


def get_expected_signature(panda: Panda) -> bytes:
  try:
    fn = os.path.join(FW_PATH, panda.get_mcu_type().config.app_fn)
    return Panda.get_signature_from_firmware(fn)
  except Exception:
    cloudlog.exception("Error computing expected signature")
    return b""


def flash_panda(panda_serial: str) -> Panda:
  panda = Panda(panda_serial)

  fw_signature = get_expected_signature(panda)
  internal_panda = panda.is_internal()

  panda_version = "bootstub" if panda.bootstub else panda.get_version()
  panda_signature = b"" if panda.bootstub else panda.get_signature()
  cloudlog.warning(f"Panda {panda_serial} connected, version: {panda_version}, signature {panda_signature.hex()[:16]}, expected {fw_signature.hex()[:16]}")

  if panda.bootstub or panda_signature != fw_signature:
    cloudlog.info("Panda firmware out of date, update required")
    panda.flash()
    cloudlog.info("Done flashing")

  if panda.bootstub:
    bootstub_version = panda.get_version()
    cloudlog.info(f"Flashed firmware not booting, flashing development bootloader. {bootstub_version=}, {internal_panda=}")
    if internal_panda:
      HARDWARE.recover_internal_panda()
    panda.recover(reset=(not internal_panda))
    cloudlog.info("Done flashing bootloader")

  if panda.bootstub:
    cloudlog.info("Panda still not booting, exiting")
    raise AssertionError

  panda_signature = panda.get_signature()
  if panda_signature != fw_signature:
    cloudlog.info("Version mismatch after flashing, exiting")
    raise AssertionError

  return panda


def panda_sort_cmp(a: Panda, b: Panda):
  a_type = a.get_type()
  b_type = b.get_type()

  # make sure the internal one is always first
  if a.is_internal() and not b.is_internal():
    return -1
  if not a.is_internal() and b.is_internal():
    return 1

  # sort by hardware type
  if a_type != b_type:
    return a_type < b_type

  # last resort: sort by serial number
  return a.get_usb_serial() < b.get_usb_serial()

def is_panda(usb_fd):
  os.chdir(os.path.join(BASEDIR, "selfdrive/boardd"))
  try:
    ret = eval(subprocess.check_output(["termux-usb", "-r", "-e", "./ispanda", usb_fd], encoding='utf8').rstrip())
    return ret
  except Exception as e:
    return False 

def main_android_no_root() -> NoReturn:
  # android termux-usb implementation.
  cloudlog.info(f"Running pandad in no-root mode")

  print("listing usb devices.. if this hangs here, restart termux.")
  while True:
    try:
      panda_descriptors = [] 
      usb_fd_list = eval(subprocess.check_output(["termux-usb", "-l"], encoding='utf8').rstrip())

      for usb_fd in usb_fd_list:
        if is_panda(usb_fd):
          panda_descriptors.append(usb_fd)      
      if len(panda_descriptors) == 0:
        print("no panda found, retrying..")
        time.sleep(0.5)
        continue
      panda_descriptor = panda_descriptors[0] # pickup first panda
      print(f"connecting to panda {panda_descriptor}..")

    except Exception as e:
      cloudlog.exception("Panda USB exception while setting up: " + str(e))
      continue
    
    # run boardd with file descriptors as arguments
    os.environ['MANAGER_DAEMON'] = 'boardd'
    os.chdir(os.path.join(BASEDIR, "selfdrive/boardd"))
    subprocess.run(["termux-usb", "-r", "-e", "./boardd", panda_descriptor], check=True)

def main() -> NoReturn:
  first_run = True
  params = Params()

  while True:
    try:
      params.remove("PandaSignatures")

      # Flash all Pandas in DFU mode
      dfu_serials = PandaDFU.list()
      if len(dfu_serials) > 0:
        for serial in dfu_serials:
          cloudlog.info(f"Panda in DFU mode found, flashing recovery {serial}")
          PandaDFU(serial).recover()
        time.sleep(1)

      panda_serials = Panda.list()
      if len(panda_serials) == 0:
        if first_run:
          cloudlog.info("No pandas found, resetting internal panda")
          HARDWARE.reset_internal_panda()
          time.sleep(2)  # wait to come back up
        continue

      cloudlog.info(f"{len(panda_serials)} panda(s) found, connecting - {panda_serials}")

      # Flash pandas
      pandas: List[Panda] = []
      for serial in panda_serials:
        pandas.append(flash_panda(serial))

      # check health for lost heartbeat
      for panda in pandas:
        health = panda.health()
        if health["heartbeat_lost"]:
          params.put_bool("PandaHeartbeatLost", True)
          cloudlog.event("heartbeat lost", deviceState=health, serial=panda.get_usb_serial())

        if first_run:
          cloudlog.info(f"Resetting panda {panda.get_usb_serial()}")
          panda.reset()

      # sort pandas to have deterministic order
      pandas.sort(key=cmp_to_key(panda_sort_cmp))
      panda_serials = list(map(lambda p: p.get_usb_serial(), pandas))  # type: ignore

      # log panda fw versions
      params.put("PandaSignatures", b','.join(p.get_signature() for p in pandas))

      # close all pandas
      for p in pandas:
        p.close()
    except (usb1.USBErrorNoDevice, usb1.USBErrorPipe):
      # a panda was disconnected while setting everything up. let's try again
      cloudlog.exception("Panda USB exception while setting up")
      continue

    first_run = False

    # run boardd with all connected serials as arguments
    os.environ['MANAGER_DAEMON'] = 'boardd'
    os.chdir(os.path.join(BASEDIR, "selfdrive/boardd"))
    subprocess.run(["./boardd", *panda_serials], check=True)

def run():
    if is_android() and not is_android_rooted():
      main_android_no_root()    
    else:
      main()

if __name__ == "__main__":
  run()
