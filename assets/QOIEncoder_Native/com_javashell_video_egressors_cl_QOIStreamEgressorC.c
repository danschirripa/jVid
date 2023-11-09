#define  SIMPLIFIED_QOI_IMPLEMENTATION
#include "sQOI.h"
#include "com_javashell_video_egressors_cl_QOIStreamEgressorC.h"
#include <jni.h>
#include <cstdlib>

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_cl_QOIStreamEgressorC_encode
  (JNIEnv* env, jobject object, jbyteArray input, jint width, jint height, jint channels, jint colorspace){
    qoi_desc_t desc;
    qoi_enc_t enc;
    uint8_t* pixel_seek;
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(input, NULL);
    uint8_t* qoi_output;

    qoi_desc_init(&desc);
    qoi_set_dimensions(&desc, width, height);
    qoi_set_channels(&desc, channels);
    qoi_set_colorspace(&desc, colorspace);
    qoi_output = (uint8_t*)malloc(((size_t)desc.width * (size_t)desc.height * ((size_t)desc.channels + 1)) + 14 + 8 + sizeof(size_t));


    write_qoi_header(&desc, qoi_output);

    pixel_seek = data;
    qoi_enc_init(&desc, &enc, qoi_output);

    while(!qoi_enc_done(&enc)){
        qoi_encode_chunk(&desc, &enc, pixel_seek);
        pixel_seek += desc.channels;
    }

    qoi_output = (uint8_t*)realloc(qoi_output, enc.offset - enc.data);

    jbyteArray encodedData = env->NewByteArray((jsize)(enc.offset - enc.data));
    env->SetByteArrayRegion(encodedData, 0, (jsize)(enc.offset - enc.data), (jbyte*)qoi_output);
    free(data);
    free(qoi_output);
    return encodedData;
  }