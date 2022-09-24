// hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/common/params.h
#pragma once

#include <map>
#include <string>

#include "common/util.h"
#include <common/lmdb++.h>

enum ParamKeyType {
  PERSISTENT = 0x02,
  CLEAR_ON_MANAGER_START = 0x04,
  CLEAR_ON_IGNITION_ON = 0x08,
  CLEAR_ON_IGNITION_OFF = 0x10,
  DONT_LOG = 0x20,
  ALL = 0xFFFFFFFF
};

class Params {
public:
  Params(const std::string &path = {});
  bool checkKey(const std::string &key);
  ParamKeyType getKeyType(const std::string &key);
  inline std::string getParamPath(const std::string &key = {}) {
    return util::getenv("HOME", "/home") + "/.flowdrive" + "/params";
  }

  // Delete a value
  int remove(const std::string &key);
  void clearAll(ParamKeyType type);

  // helpers for reading values
  std::string get(const std::string &key, bool block = false);
  inline bool getBool(const std::string &key) {
    return get(key) == "1";
  }
  std::map<std::string, std::string> readAll();

  // helpers for writing values
  int put(const std::string &key, const std::string &val);
  inline int putBool(const std::string &key, bool val) {
    return put(key.c_str(), val ? "1" : "0");
  }

private:
  static lmdb::env env;
};
