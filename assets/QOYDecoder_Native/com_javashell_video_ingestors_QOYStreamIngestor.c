#include <jni.h>

#define QOY_IMPLEMENTATION

#include "qoy.h"
#include "com_javashell_video_ingestors_QOYStreamIngestor.h"
#include <stdint.h>
#include <cstdlib>
/*
 * Class:     com_javashell_video_ingestors_QOYStreamIngestor
 * Method:    decode
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_ingestors_QOYStreamIngestor_decode
  (JNIEnv* env, jobject obj, jbyteArray encodedData, jint encodedDataSize){
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(encodedData, NULL);
    qoy_desc desc;
    int bytesRead;
    uint8_t* bytes = (uint8_t*) qoy_decode(data, encodedDataSize, &desc, 4, QOY_FORMAT_RGBA);

    size_t raw_image_length = (size_t)desc.width * (size_t)desc.height * (size_t)desc.channels;
    size_t bytes_length = (size_t) (raw_image_length * sizeof(unsigned char));

    jbyteArray decodedData = env->NewByteArray((jsize)(bytes_length));
    env->SetByteArrayRegion(decodedData, 0, (jsize)(bytes_length), (jbyte*)bytes);
    free(data);
    free(bytes);
    return decodedData;
  }