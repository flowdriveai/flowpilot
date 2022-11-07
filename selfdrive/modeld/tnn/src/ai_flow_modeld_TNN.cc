#include "ai_flow_modeld_TNN.h"
#include "tnn_api.h"
#include <string.h>

std::shared_ptr<TNNAPI> model = std::make_shared<TNNAPI>();
std::map<std::string, void*> input_map;

char* jstring2string(JNIEnv* env, jstring jstr)
{
    char* rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0)
    {
        rtn = (char*)malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}

jstring string2jstring(JNIEnv* env, const char* pat) {
    jclass strClass = (env)->FindClass("java/lang/String");
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = (env)->NewByteArray(strlen(pat));
    (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*) pat);
    jstring encoding = (env)->NewStringUTF("GB2312");
    jstring r = (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
    env->DeleteLocalRef( strClass );
    env->DeleteLocalRef( bytes );
    env->DeleteLocalRef( encoding );
    return r;
}

void JavaHashMapToStlMap(JNIEnv *env, jobject javaMap, std::map<std::string, void*> &mapOut) {
  // Get the Map's entry Set.
  jclass mapClass = env->FindClass("java/util/Map");
  if (mapClass == NULL) {
    return;
  }
  jmethodID entrySet =
    env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");
  if (entrySet == NULL) {
    return;
  }
  jobject set = env->CallObjectMethod(javaMap, entrySet);
  if (set == NULL) {
    return;
  }
  // Obtain an iterator over the Set
  jclass setClass = env->FindClass("java/util/Set");
  if (setClass == NULL) {
    return;
  }
  jmethodID iterator =
    env->GetMethodID(setClass, "iterator", "()Ljava/util/Iterator;");
  if (iterator == NULL) {
    return;
  }
  jobject iter = env->CallObjectMethod(set, iterator);
  if (iter == NULL) {
    return;
  }
  // Get the Iterator method IDs
  jclass iteratorClass = env->FindClass("java/util/Iterator");
  if (iteratorClass == NULL) {
    return;
  }
  jmethodID hasNext = env->GetMethodID(iteratorClass, "hasNext", "()Z");
  if (hasNext == NULL) {
    return;
  }
  jmethodID next =
    env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;");
  if (next == NULL) {
    return;
  }
  // Get the Entry class method IDs
  jclass entryClass = env->FindClass("java/util/Map$Entry");
  if (entryClass == NULL) {
    return;
  }
  jmethodID getKey =
    env->GetMethodID(entryClass, "getKey", "()Ljava/lang/Object;");
  if (getKey == NULL) {
    return;
  }
  jmethodID getValue =
    env->GetMethodID(entryClass, "getValue", "()Ljava/lang/Object;");
  if (getValue == NULL) {
    return;
  }
  // Iterate over the entry Set
  while (env->CallBooleanMethod(iter, hasNext)) {
    jobject entry = env->CallObjectMethod(iter, next);
    jstring key = (jstring) env->CallObjectMethod(entry, getKey);
    jobject value = env->CallObjectMethod(entry, getValue);
    const char* keyStr = env->GetStringUTFChars(key, NULL);
    if (!keyStr) {  // Out of memory
      return;
    }

    mapOut[std::string(keyStr)] = env->GetDirectBufferAddress(value);

    env->DeleteLocalRef(entry);
    env->ReleaseStringUTFChars(key, keyStr);
    env->DeleteLocalRef(key);
    env->DeleteLocalRef(value);
  }
}


JNIEXPORT jboolean JNICALL Java_ai_flow_modeld_TNN_init(JNIEnv *env, jobject thiz, jstring modelPath, jstring deviceType, jstring IODeviceType){
  tnn::DeviceType device_type_tnn;
  tnn::DeviceType device_type_io;
  std::string dev_type_str = jstring2string(env, deviceType);
  std::string io_dev_type_str = jstring2string(env, IODeviceType);
  if (dev_type_str == "OPENCL")
    device_type_tnn = tnn::DeviceType::DEVICE_OPENCL;
  else if (dev_type_str == "X86")
    device_type_tnn = tnn::DeviceType::DEVICE_X86;
  else if (dev_type_str == "ARM")
    device_type_tnn = tnn::DeviceType::DEVICE_ARM;
  else if (dev_type_str == "NAIVE")
    device_type_tnn = tnn::DeviceType::DEVICE_NAIVE;

  if (io_dev_type_str == "OPENCL")
    device_type_io = tnn::DeviceType::DEVICE_OPENCL;
  else if (io_dev_type_str == "X86")
    device_type_io = tnn::DeviceType::DEVICE_X86;
  else if (io_dev_type_str == "ARM")
    device_type_io = tnn::DeviceType::DEVICE_ARM;
  else if (io_dev_type_str == "NAIVE")
    device_type_io = tnn::DeviceType::DEVICE_NAIVE;
  
  return model->init(jstring2string(env, modelPath), device_type_tnn, device_type_io);
}

JNIEXPORT void JNICALL Java_ai_flow_modeld_TNN_createInput(JNIEnv *env, jobject thiz, jstring name, jintArray shape_arr){
  jsize size = env->GetArrayLength(shape_arr);
  std::vector<int> shape_vec(size);
  env->GetIntArrayRegion(shape_arr, 0, size, &shape_vec[0]);
  model->create_input(jstring2string(env, name), shape_vec);
}

JNIEXPORT void JNICALL Java_ai_flow_modeld_TNN_createOutput(JNIEnv *env, jobject thiz, jstring name, jintArray shape_arr){
  jsize size = env->GetArrayLength(shape_arr);
  std::vector<int> shape_vec(size);
  env->GetIntArrayRegion(shape_arr, 0, size, &shape_vec[0]);
  model->create_output(jstring2string(env, name), shape_vec);
}

JNIEXPORT void JNICALL Java_ai_flow_modeld_TNN_forward__Ljava_util_Map_2(JNIEnv *env, jobject thiz, jobject container){
 
  JavaHashMapToStlMap(env, container, input_map);
  model->forward(input_map);
}

JNIEXPORT void JNICALL Java_ai_flow_modeld_TNN_forward__Ljava_util_Map_2Ljava_lang_String_2_3F(JNIEnv *env, jobject thiz, jobject container, jstring out_name, jfloatArray out){
  JavaHashMapToStlMap(env, container, input_map);
  model->forward(input_map);
  float* output_native_buffer = model->get_output(jstring2string(env, out_name));
  jsize size = env->GetArrayLength(out);
  env->SetFloatArrayRegion(out, 0, size, output_native_buffer);
}

JNIEXPORT jfloatArray JNICALL Java_ai_flow_modeld_TNN_forward__Ljava_util_Map_2Ljava_lang_String_2(JNIEnv *env, jobject thiz, jobject container, jstring out_name){
  std::string out_name_str = jstring2string(env, out_name);
  JavaHashMapToStlMap(env, container, input_map);
  model->forward(input_map);
  float* output_native_buffer = model->get_output(out_name_str);

  double out_size = model->output_elements[out_name_str];
  jfloatArray out = env->NewFloatArray(out_size); //TODO: check for memory leaks
  env->SetFloatArrayRegion(out, 0, out_size, output_native_buffer);
  return out;
}

JNIEXPORT jobject JNICALL Java_ai_flow_modeld_TNN_getOutputBuffer(JNIEnv *env, jobject thiz, jstring out_name){
  std::string out_name_str = jstring2string(env, out_name);
  double out_size = model->output_elements[out_name_str];
  float* output_native_buffer = model->get_output(out_name_str);
  return env->NewDirectByteBuffer(output_native_buffer, out_size*sizeof(float));
}

JNIEXPORT jfloatArray JNICALL Java_ai_flow_modeld_TNN_getOutput__Ljava_lang_String_2(JNIEnv *env, jobject thiz, jstring out_name){
  std::string out_name_str = jstring2string(env, out_name);
  double out_size = model->output_elements[out_name_str];
  float* output_native_buffer = model->get_output(out_name_str);
  jfloatArray out = env->NewFloatArray(out_size); //TODO: check for memory leaks
  env->SetFloatArrayRegion(out, 0, out_size, output_native_buffer);
  return out;
}


JNIEXPORT void JNICALL Java_ai_flow_modeld_TNN_getOutput__Ljava_lang_String_2_3F(JNIEnv *env, jobject thiz, jstring out_name, jfloatArray out){
  std::string out_name_str = jstring2string(env, out_name);
  double out_size = model->output_elements[out_name_str];
  float* output_native_buffer = model->get_output(out_name_str);
  env->SetFloatArrayRegion(out, 0, out_size, output_native_buffer);
}
