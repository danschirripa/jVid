/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_javashell_video_egressors_NDI5Egressor */

#ifndef _Included_com_javashell_video_egressors_NDI5Egressor
#define _Included_com_javashell_video_egressors_NDI5Egressor
#ifdef __cplusplus
extern "C" {
#endif
#undef com_javashell_video_egressors_NDI5Egressor_frameRateInterval
#define com_javashell_video_egressors_NDI5Egressor_frameRateInterval 16000000LL
/*
 * Class:     com_javashell_video_egressors_NDI5Egressor
 * Method:    initializeNDI
 * Signature: (IILjava/lang/String;)V
 */
JNIEXPORT jlong JNICALL Java_com_javashell_video_egressors_NDI5Egressor_initializeNDI
  (JNIEnv *, jobject, jint, jint, jstring);

/*
 * Class:     com_javashell_video_egressors_NDI5Egressor
 * Method:    sendFrameB
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_com_javashell_video_egressors_NDI5Egressor_sendFrameB
  (JNIEnv *, jobject, jlong, jbyteArray, jint);

/*
 * Class:     com_javashell_video_egressors_NDI5Egressor
 * Method:    sendFrameI
 * Signature: ([II)V
 */
JNIEXPORT void JNICALL Java_com_javashell_video_egressors_NDI5Egressor_sendFrameI
  (JNIEnv *, jobject, jlong, jintArray, jint);

#ifdef __cplusplus
}
#endif
#endif
