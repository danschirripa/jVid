/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_javashell_video_egressors_QOYStreamEgressor */

#ifndef _Included_com_javashell_video_egressors_QOYStreamEgressor
#define _Included_com_javashell_video_egressors_QOYStreamEgressor
#ifdef __cplusplus
extern "C" {
#endif
#undef com_javashell_video_egressors_QOYStreamEgressor_frameRateInterval
#define com_javashell_video_egressors_QOYStreamEgressor_frameRateInterval 16000000LL
/*
 * Class:     com_javashell_video_egressors_QOYStreamEgressor
 * Method:    encodeB
 * Signature: ([B[BIIIIZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_QOYStreamEgressor_encodeB
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jint, jint, jint, jint, jboolean);

/*
 * Class:     com_javashell_video_egressors_QOYStreamEgressor
 * Method:    encodeI
 * Signature: ([I[IIIIIZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_QOYStreamEgressor_encodeI
  (JNIEnv *, jobject, jintArray, jintArray, jint, jint, jint, jint, jboolean);

#ifdef __cplusplus
}
#endif
#endif