#define  SIMPLIFIED_QOI_IMPLEMENTATION
#include "sQOI.h"
#include "com_javashell_video_egressors_cl_QOIStreamEgressorC.h"
#include <jni.h>
#include <cstdlib>

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_cl_QOIStreamEgressorC_encode
  (JNIEnv* env, jobject object, jbyteArray input, jbyteArray prev, jint width, jint height, jint channels, jint colorspace){
    qoi_desc_t desc;
    qoi_enc_t enc;
    uint8_t* pixel_seek;
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(input, NULL);
    uint8_t* prevDat = (uint8_t*) env->GetByteArrayElements(prev, NULL);
    uint8_t* qoi_output;

    for(int i = 0; i < (width * height); i = i+4){
      uint8_t nRed = data[i];
      uint8_t nBlue = data[i+1];
      uint8_t nGreen = data[i+2];
      uint8_t nAlpha = data[i+3];
      
      uint8_t pRed = prevDat[i];
      uint8_t pBlue = prevDat[i+1];
      uint8_t pGreen = prevDat[i+2];
      uint8_t pAlpha = prevDat[i+3];

      if(nRed == pRed && nBlue == pBlue && nGreen == pGreen && nAlpha == pAlpha){
        data[i] = 0;
        data[i+1] = 0;
        data[i+2] = 0;
        data[i+3] = 0;
      }
    }
    free(prevDat);

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