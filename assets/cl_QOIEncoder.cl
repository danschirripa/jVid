// Define the qoi_pixel_t type
typedef union
{
    struct {
        int8 red;
        int8 green;
        int8 blue;
        int8 alpha;
    };
    uint8 channels[4];
    uint concatenated_pixel_values;
} qoi_pixel_t;

// Define the qoi_enc_t object
typedef struct
{
    /*
        A running array[64] (zero-initialized) of previously seen pixel
        values is maintained by the encoder and decoder. Each pixel that is
        seen by the encoder and decoder is put into this array at the
        position formed by a hash function of the color value. 
    */
    qoi_pixel_t buffer[64];

    qoi_pixel_t prev_pixel;

    size_t pixel_offset, len;

    uint8* data;
    uint8* offset;

    uint run;
} qoi_enc_t;

#define QOI_TAG      0xC0
#define QOI_TAG_MASK 0x3F

#define QOI_OP_RGB   0xFE
#define QOI_OP_RGBA  0xFF

#define QOI_OP_INDEX 0x00
#define QOI_OP_DIFF  0x40
#define QOI_OP_LUMA  0x80
#define QOI_OP_RUN   0xC0

__kernel void qoi_encode_kernel(__global const uint8* input,
                                __global uint8* output,
                                int width,
                                int height,
                                __global int* encodedDataSize ) {

    uint8* pixel_seek;
    qoi_enc_t* enc;

    uint8 MAGIC[4] = {'q', 'o', 'i', 'f'};
    uint8 QOI_PADDING[8] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};

    uint8 *byte = (uint8*) output;

    byte[0] = MAGIC[0];
    byte[1] = MAGIC[1];
    byte[2] = MAGIC[2];
    byte[3] = MAGIC[3];

    uint* dimension_ptr = (uint*)&byte[4];

    dimension_ptr[0] = (uint)width;
    dimension_ptr[1] = (uint)height;

    byte[12] = (uint8) 3;
    byte[13] = (uint8) 0;

    qoi_pixel_t* pix;
    for(int element = 0; element < 64; element ++){
        pix = (qoi_pixel_t*) &enc->buffer[element];
        pix->red = 0;
        pix->green = 0;
        pix->blue = 0;
        pix->alpha = 0;
    }

    enc->len = (size_t)width * (size_t)height;
    enc->run = 0;
    enc->pixel_offset = 0;

    qoi_pixel_t prev_pixel;
    prev_pixel.red = 0;
    prev_pixel.green = 0;
    prev_pixel.blue = 0;
    prev_pixel.alpha = 0;

    enc->prev_pixel = prev_pixel;
    enc->data = input;
    enc->offset = enc->data + 14;

    while(!(enc->pixel_offset >= enc->len)){
        qoi_pixel_t cur_pixel = *((qoi_pixel_t*)pixel_seek);
        cur_pixel.alpha = 255;

        int index_pos = (int)(cur_pixel.red * 3 + cur_pixel.green * 5 + cur_pixel.blue * 7 + cur_pixel.alpha * 11) % 64;
        if(cur_pixel.concatenated_pixel_values == enc->prev_pixel.concatenated_pixel_values){
            if((++enc->run >= 62) || (enc->pixel_offset >= enc->len)){
                    uint8 tag = QOI_OP_RUN | (enc->run - 1);
                    enc->run = 0;
                    enc->offset++[0] = tag;
            }
        }

        if(((qoi_pixel_t)enc->buffer[index_pos]).concatenated_pixel_values == cur_pixel.concatenated_pixel_values){
                /* The run-length is stored with a bias of -1 */
            uint8 tag = QOI_OP_INDEX | index_pos;
            enc->offset++[0] = tag;
        } else {
            enc->buffer[index_pos] = cur_pixel;

            int8 red_diff, green_diff, blue_diff;
            int8 dr_dg, db_dg;

            red_diff = (int8)cur_pixel.red - (int8)enc->prev_pixel.red;
            green_diff = (int8)cur_pixel.green - (int8)enc->prev_pixel.green;
            blue_diff = (int8)cur_pixel.blue - (int8)enc->prev_pixel.blue;

            dr_dg = red_diff - green_diff;
            db_dg = blue_diff - green_diff;

            if (
                red_diff >= -2 && red_diff <= 1 &&
                green_diff >= -2 && green_diff <= 1 &&
                blue_diff >= -2 && blue_diff <= 1
                ) {
                        uint8 tag =
                        QOI_OP_DIFF |
                        (uint8)((int8)(red_diff + 2)) << 4 |
                        (uint8)((int8)(green_diff + 2)) << 2 |
                        (uint8)((int8)(blue_diff + 2));

                        enc->offset[0] = tag;
        
                        enc->offset++;
                } else if (
                    dr_dg >= -8 && dr_dg <= 7 &&
                    green_diff >= -32 && green_diff <= 31 &&
                    db_dg >= -8 && db_dg <= 7
                ) {
                    uint8 tag[2] = {
                        QOI_OP_LUMA | (uint8)(green_diff + 32), 
                        (uint8)((int8)(dr_dg + 8)) << 4 | (uint8)((int8)(db_dg + 8))
                    };

                    enc->offset[0] = tag[0];
                    enc->offset[1] = tag[1];

                    enc->offset += 2;
                } else {
                    uint8 tag[4] = {
                        QOI_OP_RGB,
                        cur_pixel.red,
                        cur_pixel.green,
                        cur_pixel.blue
                    };

                    enc->offset[0] = tag[0]; /* RGB opcode */
                    enc->offset[1] = tag[1]; /* Red */
                    enc->offset[2] = tag[2]; /* Green */
                    enc->offset[3] = tag[3]; /* Blue */

                    enc->offset += 4;
                }
        }
        enc->prev_pixel = cur_pixel;
        enc->pixel_offset++;

        pixel_seek += 3;
    }
    enc->offset[0] = QOI_PADDING[0];
    enc->offset[1] = QOI_PADDING[1];
    enc->offset[2] = QOI_PADDING[2];
    enc->offset[3] = QOI_PADDING[3];
    enc->offset[4] = QOI_PADDING[4];
    enc->offset[5] = QOI_PADDING[5];
    enc->offset[6] = QOI_PADDING[6];
    enc->offset[7] = QOI_PADDING[7];

    enc->offset += 8;
    *encodedDataSize = enc->offset;
}