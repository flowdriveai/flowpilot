// hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/boardd/boardd.h
#pragma once

#include "selfdrive/boardd/panda.h"

bool safety_setter_thread(std::vector<Panda *> pandas);
void boardd_main_thread(const int fd);
void boardd_main_thread(std::vector<std::string> serials);
