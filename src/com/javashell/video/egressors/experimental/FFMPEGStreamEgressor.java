package com.javashell.video.egressors.experimental;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.VideoEgress;

public class FFMPEGStreamEgressor extends VideoEgress {
	private FFmpegFrameRecorder recorder;
	private static Java2DFrameConverter conv;
	private static ServerSocket ss;
	private static HashSet<Socket> socks;
	private boolean hasStarted = false;

	public FFMPEGStreamEgressor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (frame == null || !hasStarted)
			return frame;
		try {
			recorder.record(conv.convert(frame));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean open() {
		socks = new HashSet<Socket>();
		conv = new Java2DFrameConverter();
		FFmpegLogCallback.set();

		recorder = new FFmpegFrameRecorder("udp://10.42.0.1:7896", getResolution().width, getResolution().height, 0);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H265);
		recorder.setFormat("mpegts");
		recorder.setFrameRate(30);
		recorder.setGopSize(30);
		recorder.setOption("localaddr", "10.42.0.1");
		try {
			recorder.start();
			hasStarted = true;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean close() {
		try {
			hasStarted = false;
			ss.close();
			recorder.stop();
			conv.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
