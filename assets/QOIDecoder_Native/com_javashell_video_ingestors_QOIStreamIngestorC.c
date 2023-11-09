#define  SIMPLIFIED_QOI_IMPLEMENTATION
#include "sQOI.h"
#include "com_javashell_video_ingestors_QOIStreamIngestorC.h"
#include <jni.h>
#include <cstdlib>

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_ingestors_QOIStreamIngestorC_decode
  (JNIEnv* env, jobject obj, jbyteArray encodedData){
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(encodedData, NULL);
    qoi_desc_t desc;
    qoi_dec_t dec;
    qoi_pixel_t px;

    uint8_t* bytes;
    size_t raw_image_length, seek;

    qoi_desc_init(&desc);
    read_qoi_header(&desc, data);
    raw_image_length = (size_t)desc.width * (size_t)desc.height * (size_t)desc.channels;
    seek = 0;

    qoi_dec_init(&desc, &dec, data, sizeof(data));
    bytes = (unsigned char*)malloc(raw_image_length * sizeof(unsigned char) + 4);

    while(!qoi_dec_done(&dec)){
        px = qoi_decode_chunk(&dec);
        bytes[seek] = px.red;
        bytes[seek + 1] = px.green;
        bytes[seek + 2] = px.blue;

        if(desc.channels > 3)
            bytes[seek + 3] = px.alpha;

        seek += desc.channels;
    }
    jbyteArray decodedData = env->NewByteArray((jsize)(raw_image_length));
    env->SetByteArrayRegion(decodedData, 0, (jsize)(raw_image_length), (jbyte*)bytes);
    free(data);
    free(bytes);
    return decodedData;
  }