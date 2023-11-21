package com.javashell.video.egressors;

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
	private static SplitOutputStream bOut;
	private Thread serverThread;
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
		serverThread = new Thread(new ServerRunnable());
		bOut = new SplitOutputStream();
		socks = new HashSet<Socket>();
		conv = new Java2DFrameConverter();
		FFmpegLogCallback.set();

		recorder = new FFmpegFrameRecorder(bOut, getResolution().width, getResolution().height, 0);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setFormat("flv");
		recorder.setFrameRate(60);
		recorder.setGopSize(60);
		try {
			recorder.start();
			serverThread.start();
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

	private class SplitOutputStream extends OutputStream {

		@Override
		public void write(int arg0) throws IOException {
			for (Socket s : socks) {
				s.getOutputStream().write(arg0);
			}
		}

		@Override
		public void write(byte[] bytes) {
			for (Socket s : socks) {
				try {
					s.getOutputStream().write(bytes);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class ServerRunnable implements Runnable {

		@Override
		public void run() {
			try {
				ss = new ServerSocket(7896);
				while (ss.isClosed() == false) {
					try {
						Socket s = ss.accept();
						socks.add(s);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
