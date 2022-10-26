// hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/common/params.cc
#include <iostream>
#include <unordered_map>

#include <common/lmdb++.h>
#include "common/util.h"
#include "common/params.h"

volatile sig_atomic_t params_do_exit = 0;
void params_sig_handler(int signal) {
  params_do_exit = 1;
}

std::unordered_map<std::string, uint32_t> keys = {
    {"DongleId", PERSISTENT},
    {"DeviceManufacturer", PERSISTENT},
    {"DeviceModel", PERSISTENT},
    {"Version", PERSISTENT},
    {"TermsVersion", PERSISTENT},
    {"TrainingVersion", PERSISTENT},
    {"GitCommit", PERSISTENT},
    {"GitBranch", PERSISTENT},
    {"GitRemote", PERSISTENT},
    {"FlowpilotPID", CLEAR_ON_MANAGER_START},
    {"FlowinitReady", CLEAR_ON_MANAGER_START},
    {"PandaSignatures", CLEAR_ON_MANAGER_START},
    {"CarVin", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_ON},
    {"ControlsReady", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_ON},
    {"CarParams", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_ON},
    {"CarParamsCache", CLEAR_ON_MANAGER_START},
    {"CalibrationParams", PERSISTENT},
    {"UserID", PERSISTENT},
    {"CameraMatrix", PERSISTENT},
    {"DistortionCoefficients", PERSISTENT},
    {"ModelDReady", CLEAR_ON_MANAGER_START},
    {"RecordFrontLock", PERSISTENT},
    {"RecordFront", PERSISTENT},
    {"DisableRadar_Allow", PERSISTENT},
    {"DisableRadar", PERSISTENT},
    {"Passive", PERSISTENT},
    {"CompletedTrainingVersion", PERSISTENT},
    {"DisengageOnAccelerator", PERSISTENT},
    {"HasAcceptedTerms", PERSISTENT},
    {"OpenpilotEnabledToggle", PERSISTENT},
    {"PandaHeartbeatLost", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_OFF},
    {"PandaSignatures", CLEAR_ON_MANAGER_START},
    {"ResetExtrinsicCalibration", CLEAR_ON_MANAGER_START},
    {"FlowpilotEnabledToggle", PERSISTENT},
    {"IsLdwEnabled", PERSISTENT},
    {"IsRHD", PERSISTENT},
    {"IsMetric", PERSISTENT},
    {"EndToEndToggle", PERSISTENT},
    {"RecordRoad", CLEAR_ON_MANAGER_START},  
    {"EnableWideCamera", PERSISTENT},  
    {"JoystickDebugMode", PERSISTENT},   
    {"Offroad_BadNvme", CLEAR_ON_MANAGER_START},
    {"Offroad_CarUnrecognized", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_ON},
    {"Offroad_ConnectivityNeeded", CLEAR_ON_MANAGER_START},
    {"Offroad_ConnectivityNeededPrompt", CLEAR_ON_MANAGER_START},
    {"Offroad_InvalidTime", CLEAR_ON_MANAGER_START},
    {"Offroad_IsTakingSnapshot", CLEAR_ON_MANAGER_START},
    {"Offroad_NeosUpdate", CLEAR_ON_MANAGER_START},
    {"Offroad_NoFirmware", CLEAR_ON_MANAGER_START | CLEAR_ON_IGNITION_ON},
    {"Offroad_StorageMissing", CLEAR_ON_MANAGER_START},
    {"Offroad_TemperatureTooHigh", CLEAR_ON_MANAGER_START},
    {"Offroad_UnofficialHardware", CLEAR_ON_MANAGER_START},
    {"Offroad_UpdateFailed", CLEAR_ON_MANAGER_START}, 
};

lmdb::env Params::env = nullptr;

Params::Params(const std::string &path) {
    if (env!=nullptr)
        return;
    std::string db_path = getParamPath();
    util::create_directories(db_path, 0775);
    env = lmdb::env::create();
    env.open(db_path.c_str(), 0);
}

bool Params::checkKey(const std::string &key) {
  return keys.find(key) != keys.end();
}

ParamKeyType Params::getKeyType(const std::string &key) {
  return static_cast<ParamKeyType>(keys[key]);
}

int Params::put(const std::string &key, const std::string &value) {
    lmdb::txn txn = lmdb::txn::begin(env);
    lmdb::dbi dbi = lmdb::dbi::open(txn, nullptr);
    dbi.put(txn, key, value);
    txn.commit();
    return 1;
}

std::string Params::get(const std::string &key, bool block) {
    bool ret = false;
    std::string_view val;
    if (!block){
        {
            lmdb::txn txn = lmdb::txn::begin(env, nullptr, MDB_RDONLY);
            lmdb::dbi dbi = lmdb::dbi::open(txn, nullptr);
            dbi.get(txn, key, val);
        }
        return std::string(val); 
    }

    params_do_exit = 0;
    void (*prev_handler_sigint)(int) = std::signal(SIGINT, params_sig_handler);
    void (*prev_handler_sigterm)(int) = std::signal(SIGTERM, params_sig_handler);
    
    while (!params_do_exit){
        {
            lmdb::txn txn = lmdb::txn::begin(env, nullptr, MDB_RDONLY);
            lmdb::dbi dbi = lmdb::dbi::open(txn, nullptr);
            ret = dbi.get(txn, key, val);
            if (ret)
                break;
        }
    }
    std::signal(SIGINT, prev_handler_sigint);
    std::signal(SIGTERM, prev_handler_sigterm);
    return std::string(val);
}

std::map<std::string, std::string> Params::readAll() {
    std::map<std::string, std::string> ret;
    auto txn = lmdb::txn::begin(env, nullptr, MDB_RDONLY);
    lmdb::dbi dbi = lmdb::dbi::open(txn, nullptr);
    {
        auto cursor = lmdb::cursor::open(txn, dbi);

        std::string_view key, value;
        if (cursor.get(key, value, MDB_FIRST)) {
            do {
                ret[std::string(key)] = std::string(value);
            } while (cursor.get(key, value, MDB_NEXT));
        }
    }
    return ret;
}

int Params::remove(const std::string &key) {
    lmdb::txn txn = lmdb::txn::begin(env);
    lmdb::dbi dbi = lmdb::dbi::open(txn, nullptr);
    dbi = lmdb::dbi::open(txn, nullptr);
    dbi.del(txn, key);
    txn.commit();
    return 1;
}

void Params::clearAll(ParamKeyType key_type) {
  for (auto &[key, type] : keys) {
      if (type & key_type) {
          remove(key);
    }
  } 
}
