#!/usr/bin/env python3
import bz2
import io
import os
import sys
import argparse
import random
import threading
import time
import traceback
import boto3
import logging

from cereal import log
import cereal.messaging as messaging
from common.api import Api
from common.params import Params
from common.realtime import set_core_affinity
from selfdrive.loggerd.xattr_cache import getxattr, setxattr
from selfdrive.loggerd.config import ROOT
from selfdrive.swaglog import cloudlog

logger = logging.getLogger(__name__)

NetworkType = log.DeviceState.NetworkType
UPLOAD_ATTR_NAME = 'user.upload'
UPLOAD_ATTR_VALUE = b'1'

UPLOAD_QLOG_QCAM_MAX_SIZE = 100 * 1e6  # MB

allow_sleep = bool(os.getenv("UPLOADER_SLEEP", "1"))
force_wifi = os.getenv("FORCEWIFI") is not None
fake_upload = os.getenv("FAKEUPLOAD") is not None


def get_directory_sort(d):
  return list(map(lambda s: s.rjust(10, '0'), d.rsplit('--', 1)))

def listdir_by_creation(d):
  try:
    paths = os.listdir(d)
    paths = sorted(paths, key=get_directory_sort)
    return paths
  except OSError:
    cloudlog.exception("listdir_by_creation failed")
    logger.warning("listdir_by_creation failed")
    return list()

def clear_locks(root):
  for logname in os.listdir(root):
    path = os.path.join(root, logname)
    try:
      for fname in os.listdir(path):
        if fname.endswith(".lock"):
          os.unlink(os.path.join(path, fname))
    except OSError:
      cloudlog.exception("clear_locks failed")
      logger.warning("clear_locks failed")

class FakeResponse():
        def __init__(self):
          self.status_code = 200

class Uploader():
  def __init__(self, dongle_id, root):
    self.dongle_id = dongle_id
    self.api = Api()
    self.root = root

    self.upload_thread = None

    self.last_resp = None
    self.last_exc = None

    self.immediate_size = 0
    self.immediate_count = 0

    # stats for last successfully uploaded file
    self.last_time = 0.0
    self.last_speed = 0.0
    self.last_filename = ""

    self.immediate_folders = ["crash/", "boot/"]
    self.immediate_priority = {"qlog": 0, "qlog.bz2": 0, "qcamera.ts": 1}

  def get_upload_sort(self, name):
    if name in self.immediate_priority:
      return self.immediate_priority[name]
    return 1000

  def list_upload_files(self):
    if not os.path.isdir(self.root):
      return

    self.immediate_size = 0
    self.immediate_count = 0

    for logname in listdir_by_creation(self.root):
      path = os.path.join(self.root, logname)
      try:
        names = os.listdir(path)
      except OSError:
        continue

      if any(name.endswith(".lock") for name in names):
        continue

      for name in sorted(names, key=self.get_upload_sort):
        key = os.path.join(logname, name)
        fn = os.path.join(path, name)
        # skip files already uploaded
        try:
          is_uploaded = getxattr(fn, UPLOAD_ATTR_NAME)
        except OSError:
          cloudlog.event("uploader_getxattr_failed", exc=self.last_exc, key=key, fn=fn)
          is_uploaded = True  # deleter could have deleted
        if is_uploaded:
          continue

        try:
          if name in self.immediate_priority:
            self.immediate_count += 1
            self.immediate_size += os.path.getsize(fn)
        except OSError:
          pass
        
        yield (name, key, fn)

  def next_file_to_upload(self):
    upload_files = list(self.list_upload_files())

    for name, key, fn in upload_files:
      if any(f in fn for f in self.immediate_folders):
        return (name, key, fn)

    for name, key, fn in upload_files:
      if name in self.immediate_priority:
        return (name, key, fn)

    return None

  def do_upload(self, key, fn):
    try:
      credentials = self.api.get_credentials()

      access_key = credentials["access_key"]
      secret_access_key = credentials["secret_access_key"]
      session_token = credentials["session_token"]

      s3=boto3.client(
          's3',
          aws_access_key_id=access_key,
          aws_secret_access_key=secret_access_key,
          aws_session_token=session_token,
      )

      if fake_upload:
        cloudlog.debug(f"*** WARNING, THIS IS A FAKE UPLOAD ***")
        self.last_resp = FakeResponse()

      else:
        with open(fn, "rb") as f:
          if key.endswith('.bz2') and not fn.endswith('.bz2'):
            data = bz2.compress(f.read())
            data = io.BytesIO(data)
          else:
            data = f

          # Params are fetched as bytes, have to convert them to string
          user_id_san = self.api.user_id.decode("utf-8")
          dongle_id_san = self.api.dongle_id.decode("utf-8")

          # api.get_credentials should populate api.email field, saving us a DB call
          object_name = f"unprocessed/{user_id_san}/{dongle_id_san}/{key}"
          bucket = "fdusermedia"

          self.last_resp = FakeResponse()
          s3.upload_fileobj(data, bucket, object_name)
          s3.Object(bucket, object_name).wait_until_exists()

    except Exception as e:
      self.last_exc = (e, traceback.format_exc())
      raise e

  def normal_upload(self, key, fn):
    self.last_resp = None
    self.last_exc = None

    try:
      self.do_upload(key, fn)
      logger.debug(f"S3 event successful for {key}")
    except Exception as e:
      logger.warning(f"S3 event failed for {key}: {e}")

    return self.last_resp

  def upload(self, name, key, fn, network_type, metered):
    try:
      sz = os.path.getsize(fn)
    except OSError:
      cloudlog.exception("upload: getsize failed")
      logger.warning("upload: getsize failed")
      return False

    cloudlog.event("upload_start", key=key, fn=fn, sz=sz, network_type=network_type, metered=metered)

    if sz == 0:
      # tag files of 0 size as uploaded
      success = True
    elif name in self.immediate_priority and sz > UPLOAD_QLOG_QCAM_MAX_SIZE:
      cloudlog.event("uploader_too_large", key=key, fn=fn, sz=sz)
      success = True
    else:
      start_time = time.monotonic()
      stat = self.normal_upload(key, fn)
      if stat is not None and stat.status_code in (200, 201, 401, 403, 412):
        self.last_filename = fn
        self.last_time = time.monotonic() - start_time
        self.last_speed = (sz / 1e6) / self.last_time
        success = True
        cloudlog.event("upload_success" if stat.status_code != 412 else "upload_ignored", key=key, fn=fn, sz=sz, network_type=network_type, metered=metered)
      else:
        success = False
        cloudlog.event("upload_failed", stat=stat, exc=self.last_exc, key=key, fn=fn, sz=sz, network_type=network_type, metered=metered)

    if success:
      # tag file as uploaded
      try:
        setxattr(fn, UPLOAD_ATTR_NAME, UPLOAD_ATTR_VALUE)
        logger.debug(f"Successfully set attr on {key}")
      except OSError:
        logger.warning(f"Successfully set attr on {key}")
        cloudlog.event("uploader_setxattr_failed", exc=self.last_exc, key=key, fn=fn, sz=sz)

    return success

  def get_msg(self):
    msg = messaging.new_message("uploaderState")
    us = msg.uploaderState
    us.immediateQueueSize = int(self.immediate_size / 1e6)
    us.immediateQueueCount = self.immediate_count
    us.lastTime = self.last_time
    us.lastSpeed = self.last_speed
    us.lastFilename = self.last_filename
    return msg


def uploader_fn(exit_event):
  try:
    set_core_affinity([0, 1, 2, 3])
  except Exception:
    cloudlog.exception("failed to set core affinity")
    logger.warning("failed to set core affinity")

  clear_locks(ROOT)

  params = Params()
  dongle_id = params.get("DongleId", encoding='utf8')

  if dongle_id is None:
    cloudlog.info("uploader missing dongle_id")

  sm = messaging.SubMaster(['deviceState'])
  pm = messaging.PubMaster(['uploaderState'])
  uploader = Uploader(dongle_id, ROOT)

  backoff = 0.1
  while not exit_event.is_set():
    sm.update(0)
    offroad = params.get_bool("IsOffroad")
    offroad = False
   
    d = uploader.next_file_to_upload()
    if d is None:  # Nothing to upload
      if allow_sleep:
        time.sleep(60 if offroad else 5)
      continue

    name, key, fn = d

    # qlogs and bootlogs need to be compressed before uploading
    if key.endswith(('qlog', 'rlog')) or (key.startswith('boot/') and not key.endswith('.bz2')):
      key += ".bz2"

    success = uploader.upload(name, key, fn, sm['deviceState'].networkType.raw, sm['deviceState'].networkMetered)
    if success:
      print("\033[92m" + f"Uploaded {key} !" + "\033[0m")
      backoff = 0.1
    elif allow_sleep:
      logger.warning(f"Failed to upload {key}, retrying with a backoff")
      cloudlog.info("upload backoff %r", backoff)
      time.sleep(backoff + random.uniform(0, backoff))
      backoff = min(backoff*2, 120)

    pm.send("uploaderState", uploader.get_msg())


def main():
  parser = argparse.ArgumentParser(prog='Flowpilot loggerd')
  parser.add_argument("-v", "--verbose", action="store_true", default=False, help="Get verbose logs",)

  args = parser.parse_args()

  log_level = logging.DEBUG if args.verbose else logging.ERROR
  logging.basicConfig(
      level=log_level,
      format="%(asctime)s %(filename)s [%(levelname)s] %(message)s",
  )

  uploader_fn(threading.Event())


if __name__ == "__main__":
  main()
