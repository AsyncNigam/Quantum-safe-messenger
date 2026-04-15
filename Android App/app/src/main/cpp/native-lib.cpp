#include <jni.h>
#include <string>

// The function name must follow this exact pattern:
// Java_<packageName>_<ClassName>_<methodName>
// Replace "com_yourname_quantummessenger" with your actual package name
// using underscores instead of dots

extern "C" [[maybe_unused]] JNIEXPORT jstring JNICALL
Java_com_nigdroid_quantummessenger_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string hello = "C++ engine is alive. NDK is wired correctly.";
    return env->NewStringUTF(hello.c_str());
}