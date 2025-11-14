#include <jni.h>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_MyNativeLib_processFrameNV21(
        JNIEnv *env,
        jclass clazz,
        jbyteArray inputNV21,
        jint width,
        jint height,
        jobject outDirectBuffer,
        jboolean useCanny


) {
    // TEMP: just a log to test
    __android_log_print(ANDROID_LOG_INFO, "NativeLib", "JNI function reached!");
}
