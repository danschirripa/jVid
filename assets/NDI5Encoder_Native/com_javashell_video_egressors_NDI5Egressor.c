#include <cstdlib>
#include <jni.h>
#include "com_javashell_video_egressors_NDI5Egressor.h"
#include <chrono>
#include <cstdint>
#include <string.h>
#include <Processing.NDI.Lib.h>

#ifdef _WIN32
#ifdef _WIN64
#pragma comment(lib, "Processing.NDI.Lib.x64.lib")
#else // _WIN64
#pragma comment(lib, "Processing.NDI.Lib.x86.lib")
#endif // _WIN64
#endif // _WIN32

using namespace std;

NDIlib_send_instance_t pNDI_send;
NDIlib_video_frame_v2_t NDI_frame;
int frameWidth, frameHeight;

void convert4Channel(uint8_t* input, uint8_t* output, size_t totalSize){
  size_t currIndex = 0;

  while(currIndex < totalSize){
    output[currIndex] = input[currIndex + 3];
    output[currIndex + 1] = input[currIndex + 2];
    output[currIndex + 2] = input[currIndex + 1];
    output[currIndex + 3] = input[currIndex + 0];
    currIndex += 4;
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

JNIEXPORT void JNICALL Java_com_javashell_video_egressors_NDI5Egressor_initializeNDI
  (JNIEnv *env, jobject obj, jint width, jint height, jstring ndiNameString){  
    NDIlib_send_create_t pNDI_desc;
    pNDI_desc.p_ndi_name = env->GetStringUTFChars(ndiNameString, 0);
    pNDI_send = NDIlib_send_create(&pNDI_desc);
    NDI_frame.xres = width;
    NDI_frame.yres = height;
    NDI_frame.FourCC = NDIlib_FourCC_type_RGBA;
    frameWidth = width;
    frameHeight = height;
}

JNIEXPORT void JNICALL Java_com_javashell_video_egressors_NDI5Egressor_sendFrameB(JNIEnv *env, jobject thisObj, jbyteArray img_data, jint channels){
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(img_data, NULL);
    size_t totalSize = frameWidth * frameHeight * 4;
    uint8_t* output = (uint8_t*)malloc(totalSize * sizeof(uint8_t));

    if(channels == 3){
        convert3Channel(data, output, totalSize);
    } else {
        convert4Channel(data, output, totalSize);
    }

    NDI_frame.p_data = (output);

    NDIlib_send_send_video_v2(pNDI_send, &NDI_frame);
    free(data);
    free(output);
}

JNIEXPORT void JNICALL Java_com_javashell_video_egressors_NDI5Egressor_sendFrameI(JNIEnv *env, jobject thisObj, jintArray img_data, jint channels){
    uint8_t* data = (uint8_t*) env->GetIntArrayElements(img_data, NULL);
    size_t totalSize = frameWidth * frameHeight * 4;
    uint8_t* output = (uint8_t*)malloc(totalSize * sizeof(uint8_t));

    if(channels == 3){
        convert3Channel(data, output, totalSize);
    } else {
        convert4Channel(data, output, totalSize);
    }

    NDI_frame.p_data = (output);

    NDIlib_send_send_video_v2(pNDI_send, &NDI_frame);
    free(data);
    free(output);
}