package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.VideoEgress;

public class FFMPEGStreamEgressor extends VideoEgress {
	private FFmpegFrameRecorder recorder;
	private Java2DFrameConverter conv;

	public FFMPEGStreamEgressor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (frame == null)
			return frame;
		try {
			recorder.record(conv.getFrame(frame));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean open() {
		recorder = new FFmpegFrameRecorder("rtmp://", getResolution().width, getResolution().height);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H265);
		recorder.setFormat("mp4");
		recorder.setFrameRate(60);
		recorder.setGopSize(60);
		try {
			recorder.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

}
