
#define  SIMPLIFIED_QOI_IMPLEMENTATION
#include "sQOI.h"
#include "com_javashell_video_egressors_cl_QOIStreamEgressorC.h"
#include <jni.h>
#include <cstdlib>
#include <string.h>
#include <omp.h>

void convertCompare3Channel(uint8_t* current, uint8_t* previous, uint8_t* output, size_t totalSize){
  size_t prevIndex = 0;
  size_t currIndex = 0;

  for(size_t i = 0; i < totalSize; i += 4){
    if(memcmp((current + prevIndex), (previous + prevIndex), sizeof(uint8_t) * 3) == 0){
      output[currIndex] = 0;
      output[currIndex + 1] = 0;
      output[currIndex + 2] = 0;
      output[currIndex + 3] = 0;
    } else {
      output[currIndex] = current[prevIndex + 2];
      output[currIndex + 1] = current[prevIndex + 1];
      output[currIndex + 2] = current[prevIndex];
      output[currIndex + 3] = 255;
    }
    currIndex += 4;
    prevIndex += 3;
  }
}

void convert3Channel(uint8_t* input, uint8_t* output, size_t totalSize){
  size_t prevIndex = 0;
  size_t currIndex = 0;

  while(currIndex < totalSize){
    output[currIndex] = input[prevIndex + 2];
    output[currIndex + 1] = input[prevIndex + 1];
    output[currIndex + 2] = input[prevIndex];
    output[currIndex + 3] = 255;
    currIndex += 4;
    prevIndex += 3;
  }
}

void convertCompare4Channel(uint8_t* current, uint8_t* previous, uint8_t* output, size_t totalSize){
  size_t currIndex = 0;

  for(size_t i = 0; i < totalSize; i +=4){
    if(memcmp((current + currIndex), (previous + currIndex), sizeof(uint8_t) * 4)){
      output[currIndex] = 0;
      output[currIndex + 1] = 0;
      output[currIndex + 2] = 0;
      output[currIndex + 3] = 0;
    } else {
      output[currIndex] = current[currIndex + 2];
      output[currIndex + 1] = current[currIndex + 1];
      output[currIndex + 2] = current[currIndex];
      output[currIndex + 3] = current[currIndex + 3];
    }

    currIndex += 4;
  }
}

void convert4Channel(uint8_t* input, uint8_t* output, size_t totalSize){
  size_t currIndex = 0;

  while(currIndex < totalSize){
    output[currIndex] = input[currIndex + 2];
    output[currIndex + 1] = input[currIndex + 1];
    output[currIndex + 2] = input[currIndex];
    output[currIndex + 3] = input[currIndex + 3];
    currIndex += 4;
  }
}

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_cl_QOIStreamEgressorC_encodeB
  (JNIEnv* env, jobject object, jbyteArray input, jbyteArray previous, jint width, jint height, jint channels, jint colorspace, jboolean isKeyFrame){
    qoi_desc_t desc;
    qoi_enc_t enc;
    uint8_t* pixel_seek;
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(input, NULL);
    uint8_t* previousData = (uint8_t*) env->GetByteArrayElements(previous, NULL);
    uint8_t* qoi_output;

    size_t totalSize = width * height * 4;
    uint8_t* output = (uint8_t*)malloc(totalSize * sizeof(uint8_t));

    if(isKeyFrame){
      if(channels == 3){
        convert3Channel(data, output, totalSize);
      } else {
        convert4Channel(data, output, totalSize);
      }
    } else {
      if(channels == 3){
        convertCompare3Channel(data, previousData, output, totalSize);
      } else {
        convertCompare4Channel(data, previousData, output, totalSize);
      }
    }

    qoi_desc_init(&desc);
    qoi_set_dimensions(&desc, width, height);
    qoi_set_channels(&desc, 4);
    qoi_set_colorspace(&desc, colorspace);
    qoi_output = (uint8_t*)malloc(((size_t)desc.width * (size_t)desc.height * ((size_t)desc.channels + 1)) + 14 + 8 + sizeof(size_t));


    write_qoi_header(&desc, qoi_output);

    pixel_seek = output;
    qoi_enc_init(&desc, &enc, qoi_output);

    while(!qoi_enc_done(&enc)){
        qoi_encode_chunk(&desc, &enc, pixel_seek);
        pixel_seek += desc.channels;
    }

    qoi_output = (uint8_t*)realloc(qoi_output, enc.offset - enc.data);

    jbyteArray encodedData = env->NewByteArray((jsize)(enc.offset - enc.data));
    env->SetByteArrayRegion(encodedData, 0, (jsize)(enc.offset - enc.data), (jbyte*)qoi_output);
    free(data);
    free(previousData);
    free(output);
    free(qoi_output);
    return encodedData;
  }

  JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_egressors_cl_QOIStreamEgressorC_encodeI
  (JNIEnv* env, jobject object, jintArray input, jintArray previous, jint width, jint height, jint channels, jint colorspace, jboolean isKeyFrame){
    qoi_desc_t desc;
    qoi_enc_t enc;
    uint8_t* pixel_seek;
    uint8_t* data = (uint8_t*) env->GetIntArrayElements(input, NULL);
    uint8_t* previousData = (uint8_t*) env->GetIntArrayElements(previous, NULL);
    uint8_t* qoi_output;

    size_t totalSize = width * height * 4;
    uint8_t* output = (uint8_t*)malloc(totalSize * sizeof(uint8_t));

    if(isKeyFrame){
      if(channels == 3){
        convert3Channel(data, output, totalSize);
      } else {
        convert4Channel(data, output, totalSize);
      }
    } else {
      if(channels == 3){
        convertCompare3Channel(data, previousData, output, totalSize);
      } else {
        convertCompare4Channel(data, previousData, output, totalSize);
      }
    }

    qoi_desc_init(&desc);
    qoi_set_dimensions(&desc, width, height);
    qoi_set_channels(&desc, 4);
    qoi_set_colorspace(&desc, colorspace);
    qoi_output = (uint8_t*)malloc(((size_t)desc.width * (size_t)desc.height * ((size_t)desc.channels + 1)) + 14 + 8 + sizeof(size_t));


    write_qoi_header(&desc, qoi_output);

    pixel_seek = output;
    qoi_enc_init(&desc, &enc, qoi_output);

    while(!qoi_enc_done(&enc)){
        qoi_encode_chunk(&desc, &enc, pixel_seek);
        pixel_seek += desc.channels;
    }

    qoi_output = (uint8_t*)realloc(qoi_output, enc.offset - enc.data);

    jbyteArray encodedData = env->NewByteArray((jsize)(enc.offset - enc.data));
    env->SetByteArrayRegion(encodedData, 0, (jsize)(enc.offset - enc.data), (jbyte*)qoi_output);
    free(data);
    free(previousData);
    free(output);
    free(qoi_output);
    return encodedData;
  }