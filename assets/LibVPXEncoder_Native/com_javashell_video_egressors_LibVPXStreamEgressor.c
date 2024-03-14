
#include <cstdlib>
#include <chrono>
#include <cstdint>
#include <string.h>
#include <jni.h>

#include <vpx/vpx_encoder.h>
#include <com_javashell_video_egressors_LibVPXStreamEgressor.h">

struct VPXStruct {
  vpx_codec_ctx_t codec;
  vpx_codec_enc_cfg_t cfg;
  vpx_image_t raw;
  vpx_codec_err_t res;
  vpx_codec_iter_t iter;
  int frame_index;
}


/*
 * Class:     com_javashell_video_egressors_LibVPXStreamEgressor
 * Method:    initializeVPX
 * Signature: (IILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_javashell_video_egressors_LibVPXStreamEgressor_initializeVPX
  (JNIEnv *env, jobject obj, jint width, jint height, jstring codec){
    struct VPXStruct *instance = (struct VPXStruct*) malloc(sizeof(struct VPXStruct));

    VpxVideoInfo info = { 0, 0, 0, { 0, 0 } };
    const VpxInterface *encoder = NULL;
    encoder = get_vpx_encoder_by_name(codec);

    info.codec_fourcc = encoder->fourcc;
    info.frame_width = width;
    info.frame_height = height;
    info.time_base.numerator = 1;
    info.time_base.denominator = 60;

    vpx_img_alloc(&instance->raw, VPX_IMG_FMT_I420, info.frame_width, info.frame_height, 1);
    instance->res = vpx_codec_enc_config_default(encoder->codec_interface(), &instance->cfg, 0);

    instance->cfg.g_w = info.frame_width;
    instance->cfg.g_h = info.frame_height;
    instance->cfg.g_timebase.num = info.time_base.numerator;
    instance->cfg.g_timebase.den = info.time_base.denominator;
    instance->cfg.rc_target_bitrate = 1000;

    vpx_codec_enc_init(&instance->codec, encoder->codec_interface(), &instance->cfg, 0);

    instance->frame_index = 0;
    instance->iter = NULL;
    return (long) instance;
  }

/*
 * Class:     com_javashell_video_egressors_LibVPXStreamEgressor
 * Method:    sendFrameB
 * Signature: (J[BI)V
 */
JNIEXPORT void JNICALL Java_com_javashell_video_egressors_LibVPXStreamEgressor_sendFrameB
  (JNIEnv *env, jobject obj, jlong ptr, jbyteArray rawImage, jint channels){
    uint8_t* data = (uint8_t*) env->GetByteArrayElements(img_data, NULL);
    struct VPXStruct *instance = (struct VPXStruct*) ptr;
  }

/*
 * Class:     com_javashell_video_egressors_LibVPXStreamEgressor
 * Method:    sendFrameI
 * Signature: (J[II)V
 */
JNIEXPORT void JNICALL Java_com_javashell_video_egressors_LibVPXStreamEgressor_sendFrameI
  (JNIEnv *env, jobject obj, jlong ptr, jintArray rawImage, jint channels){
    uint8_t* data = (uint8_t*) env->GetIntArrayElements(img_data, NULL);
    struct VPXStruct *instance = (struct VPXStruct*) ptr;
  }
