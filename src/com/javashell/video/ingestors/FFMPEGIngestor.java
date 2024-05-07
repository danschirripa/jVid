package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.VideoIngestor;

public class FFMPEGIngestor extends VideoIngestor {
	private final FFmpegFrameGrabber grabber;
	private Java2DFrameConverter conv;
	private BufferedImage nullFrame;
	private Thread captureThread;
	private boolean isOpen = false;
	private BufferedImage curFrame, bufFrame;
	private long lastFPS;
	private int frameDelay = 16;
	private double frameRate = -1;
	private final String lock = "";

	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
		//FFmpegLogCallback.set();
	}

	public FFMPEGIngestor(Dimension resolution, InputStream videoInput, String format) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(videoInput, 0);
		grabber.setVideoCodecName(format);
		grabber.setFormat(format);
		init();
	}

	public FFMPEGIngestor(Dimension resolution, URL videoInput) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(videoInput);
		init();
	}

	public FFMPEGIngestor(Dimension resolution, File videoInput) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(videoInput);
		init();
	}

	public FFMPEGIngestor(Dimension resolution, File videoInput, String format) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(videoInput);
		grabber.setVideoCodecName(format);
		grabber.setFormat(format);
		init();
	}

	public FFMPEGIngestor(Dimension resolution, String input, String format) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(input);
		grabber.setVideoCodecName(format);
		grabber.setFormat(format);
		init();
	}

	public FFMPEGIngestor(Dimension resolution, String input) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(input);
		init();
	}

	public void setVideoCodec(String codec) {
		grabber.setVideoCodecName(codec);
	}

	public void setVideoFormat(String format) {
		grabber.setFormat(format);
	}

	public void setFrameRate(double rate) {
		if (rate > 0)
			this.frameRate = rate;
	}

	public void setVideoOption(String key, String value) {
		grabber.setVideoOption(key, value);
	}

	public void setOption(String key, String value) {
		grabber.setOption(key, value);
	}

	private void init() {
		captureThread = new Thread(new Runnable() {
			public void run() {
				Frame javacvFrame;
				while (isOpen) {
					try {
						synchronized (lock) {
							lock.wait();
						}
						long startTime = System.nanoTime();
						javacvFrame = grabber.grabImage();
						while (javacvFrame.image == null) {
							javacvFrame.close();
							javacvFrame = grabber.grabImage();
						}
						bufFrame = conv.convert(javacvFrame);
						long endTime = System.nanoTime();
						lastFPS = (1000000000 / (endTime - startTime));
					} catch (Exception e) {
						e.printStackTrace();
					}
					curFrame = nullFrame;
				}
			}
		});
		conv = new Java2DFrameConverter();
		generateNullFrame();
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		synchronized (lock) {
			lock.notify();
		}
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			grabber.setImageWidth(getResolution().width);
			grabber.setImageHeight(getResolution().height);
			grabber.setNumBuffers(0);
			if (frameRate != -1)
				grabber.setFrameRate(frameRate);
			grabber.start();
			frameDelay = (int) (1 / grabber.getFrameRate() * 1000);
			System.out.println(grabber.getFrameRate());
			System.out.println(grabber.getImageWidth() + "x" + grabber.getImageHeight());
			System.out.println(grabber.getVideoCodecName());
			isOpen = true;
			captureThread.start();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean close() {
		try {
			grabber.stop();
			isOpen = false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void generateNullFrame() {
		nullFrame = new BufferedImage(this.getResolution().width, this.getResolution().height,
				BufferedImage.TYPE_INT_RGB);
		nullFrame.getGraphics().setColor(java.awt.Color.CYAN);
		nullFrame.getGraphics().fill3DRect(0, 0, getResolution().width, getResolution().height, true);
	}

}
