package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.VideoIngestor;

public class LocalScreenIngestor extends VideoIngestor {
	private FFmpegFrameGrabber grabber;
	private Java2DFrameConverter conv;
	private BufferedImage nullFrame;
	private Thread captureThread;
	private boolean isOpen = false;
	private static BufferedImage curFrame, bufFrame;
	private long lastFPS;
	private int frameDelay = 16;

	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}

	public LocalScreenIngestor(Dimension resolution) {
		super(resolution);
		grabber = new FFmpegFrameGrabber(":0.0+" + 0 + "," + 0);
		grabber.setFormat("x11grab");
		grabber.setImageWidth(resolution.width);
		grabber.setImageHeight(resolution.height);
		grabber.setFrameRate(60);
		init();
	}

	private void init() {
		captureThread = new Thread(new Runnable() {
			public void run() {
				Frame javacvFrame;
				while (isOpen) {
					try {
						long startTime = System.nanoTime();
						javacvFrame = grabber.grab();
						while (javacvFrame.image == null) {
							javacvFrame.close();
							javacvFrame = grabber.grab();
						}
						bufFrame = conv.convert(javacvFrame);
						long endTime = System.nanoTime();
						lastFPS = (1000000000 / (endTime - startTime));
						Thread.sleep(frameDelay);
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
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			grabber.start();
			frameDelay = (int) (1 / grabber.getFrameRate() * 1000);
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
