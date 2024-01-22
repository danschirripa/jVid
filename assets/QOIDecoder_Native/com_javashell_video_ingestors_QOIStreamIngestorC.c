#define  QOI_IMPLEMENTATION
#include "qoi.h"
#include "com_javashell_video_ingestors_QOIStreamIngestorC.h"
#include <jni.h>
#include <cstdlib>

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_ingestors_QOIStreamIngestorC_decode
  (JNIEnv* env, jobject obj, jbyteArray encodedData, jint encodedDataSize){
    void* data = (void*) env->GetByteArrayElements(encodedData, NULL);
    qoi_desc* desc;

    void* bytes;
    bytes = qoi_decode(data, encodedDataSize, desc, 4);

    size_t raw_image_length = (size_t)desc->width * (size_t)desc->height * (size_t)desc->channels;
    size_t bytes_length = (size_t) (raw_image_length * sizeof(unsigned char));

    jbyteArray decodedData = env->NewByteArray((jsize)(bytes_length));
    env->SetByteArrayRegion(decodedData, 0, (jsize)(bytes_length), (jbyte*)bytes);
    free(data);
    free(bytes);
    return decodedData;
  }