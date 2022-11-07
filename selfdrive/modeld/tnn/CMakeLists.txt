project(tnnJNI)
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")

option(BUILD_ANDROID "Cross compile for android" OFF)

if(BUILD_ANDROID)
    set(TNN_LIB_PATH ${CMAKE_SOURCE_DIR}/../../../android/libs/arm64-v8a)
    find_library(log-lib log)
else()
    set(TNN_LIB_PATH ${CMAKE_SOURCE_DIR}/../../../libs/linux/${CMAKE_HOST_SYSTEM_PROCESSOR})
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})
endif()

include_directories($ENV{TNN_ROOT}/include)
include_directories($ENV{TNN_ROOT}/third_party/opencl/include)
include_directories(src)

file(GLOB_RECURSE SRC src/*.cc)
add_library(tnnjni SHARED ${SRC})

target_link_directories(tnnjni PUBLIC ${TNN_LIB_PATH})
target_link_libraries( # Specifies the target library.
                        tnnjni
                        -Wl,--whole-archive TNN -Wl,--no-whole-archive
                      )

if(BUILD_ANDROID)
    target_link_libraries(tnnjni ${log-lib})
endif()

