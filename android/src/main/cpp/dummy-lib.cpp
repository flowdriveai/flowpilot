#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
whatever(
        JNIEnv *env,
        jobject /* this */){
    std::string hello = "Hello";
    return env->NewStringUTF(hello.c_str());
};
