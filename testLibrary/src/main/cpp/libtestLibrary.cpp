#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_imf_testLibrary_NativeTestLibrary_stringFromJNI(JNIEnv *env, jclass clazz) {
    std::string hello = "from NativeTestLibrary C++";
    return env->NewStringUTF(hello.c_str());
}