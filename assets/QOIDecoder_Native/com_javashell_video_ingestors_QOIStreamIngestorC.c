#define  SIMPLIFIED_QOI_IMPLEMENTATION
#include "sQOI.h"
#include "com_javashell_video_ingestors_QOIStreamIngestorC.h"
#include <jni.h>
#include <cstdlib>

JNIEXPORT jbyteArray JNICALL Java_com_javashell_video_ingestors_QOIStreamIngestorC_decode
  (JNIEnv* env, jobject obj, jbyteArray encodedData, jint encodedDataSize){
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(encodedData, NULL);
    qoi_desc_t desc;
    qoi_dec_t dec;
    qoi_pixel_t px;

    uint8_t* bytes;
    size_t raw_image_length, seek;

    qoi_desc_init(&desc);
    
    if (!read_qoi_header(&desc, data))
    {
        printf("The file you opened is not a QOIF file\n");
        return encodedData;
    }
    seek = 0;

    qoi_dec_init(&desc, &dec, data, encodedDataSize);



    raw_image_length = (size_t)desc.width * (size_t)desc.height * (size_t)desc.channels;
    size_t bytes_length = (size_t) (raw_image_length * sizeof(unsigned char));

    bytes = (unsigned char*)malloc(bytes_length);

    while(!qoi_dec_done(&dec)){
        px = qoi_decode_chunk(&dec);

        if(desc.channels > 3){
            bytes[seek] = px.alpha;
        } else {
            bytes[seek] = 255;
        }

        bytes[seek + 3] = px.red;
        bytes[seek + 2] = px.green;
        bytes[seek + 1] = px.blue;

        seek += desc.channels;
    }

    jbyteArray decodedData = env->NewByteArray((jsize)(bytes_length));
    env->SetByteArrayRegion(decodedData, 0, (jsize)(bytes_length), (jbyte*)bytes);
    free(data);
    free(bytes);
    return decodedData;
  }