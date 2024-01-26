#include <cstdlib>
#include <jni.h>
#include "com_javashell_video_ingestors_NDI5Ingestor.h"
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

NDIlib_video_frame_v2_t video_frame;
NDIlib_recv_instance_t pRecv = NDIlib_recv_create_v3();


//From RGBA to BGRA
void convert4Channel(uint8_t* input, uint8_t* output, size_t totalSize){
  size_t currIndex = 0;

  while(currIndex < totalSize){
    output[currIndex + 1] = input[currIndex + 2];
    output[currIndex + 2] = input[currIndex + 1];
    output[currIndex + 3] = input[currIndex];
    output[currIndex + 0] = input[currIndex + 3];
    currIndex += 4;
  }
}

void convert3Channel(uint8_t* input, uint8_t* output, size_t totalSize){
  size_t prevIndex = 0;
  size_t currIndex = 0;

  while(currIndex < totalSize){
    output[currIndex + 1] = input[prevIndex + 2];
    output[currIndex + 2] = input[prevIndex + 1];
    output[currIndex + 3] = input[prevIndex];
    output[currIndex + 0] = 255;
    currIndex += 4;
    prevIndex += 3;
  }
}


JNIEXPORT void JNICALL Java_com_javashell_video_ingestors_NDI5Ingestor_initializeNDI(JNIEnv *env, jobject obj, jstring ndiNameString, jstring sourceNameString){
  if (!NDIlib_initialize()){
    printf("NDI not initialized");
		return;
  }
  NDIlib_source_t ndiSource = NDIlib_source_t();
  ndiSource.p_ndi_name = env->GetStringUTFChars(sourceNameString, 0);

  NDIlib_recv_create_v3_t ndiDesc = NDIlib_recv_create_v3_t();
  ndiDesc.p_ndi_recv_name = env->GetStringUTFChars(ndiNameString, 0);
  ndiDesc.color_format = NDIlib_recv_color_format_RGBX_RGBA;
  ndiDesc.allow_video_fields = false;
  
	pRecv = NDIlib_recv_create_v3(&ndiDesc);
	// Connect to our sources
	NDIlib_recv_connect(pRecv, &ndiSource);

  jclass cls = env->GetObjectClass(obj);
  jmethodID mid = env->GetMethodID(cls, "setResolution", "(II)V");

  while(true){
     switch(NDIlib_recv_capture_v3(pRecv, &video_frame, nullptr, nullptr, 1000)){
      case NDIlib_frame_type_none:
        break;
      case NDIlib_frame_type_video:
        printf("Received frame\n");
        env->CallVoidMethod(obj, mid, (jint) video_frame.xres, (jint) video_frame.yres);
        NDIlib_recv_free_video_v2(pRecv, &video_frame);
        return;
        break;
    }
  }
}

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_ingestors_NDI5Ingestor_grabFrame(JNIEnv *env, jobject obj){
  switch(NDIlib_recv_capture_v3(pRecv, &video_frame, nullptr, nullptr, 1000)){
    case NDIlib_frame_type_none:
      return NULL;
      break;
    case NDIlib_frame_type_video:
      size_t totalSize = video_frame.xres * video_frame.yres * 4;

      uint8_t* output = (uint8_t*)malloc(video_frame.xres * video_frame.yres * 4 * sizeof(uint8_t));
      convert4Channel(video_frame.p_data, output, totalSize);

      jbyteArray decodedData = env->NewByteArray((jsize)(totalSize * sizeof(uint8_t)));
      env->SetByteArrayRegion(decodedData, 0, (jsize)(totalSize * sizeof(uint8_t)), (jbyte*)output);
      NDIlib_recv_free_video_v2(pRecv, &video_frame);
      free(output);
      return decodedData;
      break;
  }
  return NULL;
}


