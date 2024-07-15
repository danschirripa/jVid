package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.VideoIngestor;

public class TestPatternIngestor extends VideoIngestor {

	public enum TestPattern {
		SMPTE_COLORBARS, LINEAR_ACCURACY, HD_TEST, UHD_TEST
	}

	private FFmpegFrameGrabber grabber;
	private Java2DFrameConverter conv;
	private BufferedImage nullFrame;
	private Thread captureThread;
	private boolean isOpen = false;
	private BufferedImage curFrame, bufFrame;
	private long lastFPS;
	private int frameDelay = 16;
	private double frameRate = -1;
	private final String lock = "";
	private final TestPattern patternType;

	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
		// FFmpegLogCallback.set();
	}

	public TestPatternIngestor(Dimension resolution, TestPattern type) {
		super(resolution);
		patternType = type;
		try {
			if (type == TestPattern.SMPTE_COLORBARS) {
				final BufferedImage loadedFrame = ImageIO
						.read(TestPatternIngestor.class.getResourceAsStream("/test_patterns/SMPTE-COLOR-BARS.png"));
				curFrame = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_4BYTE_ABGR);
				curFrame.getGraphics().drawImage(loadedFrame, 0, 0, resolution.width, resolution.height, null);

				bufFrame = curFrame;
			} else if (type == TestPattern.LINEAR_ACCURACY) {
				final BufferedImage loadedFrame = ImageIO
						.read(TestPatternIngestor.class.getResourceAsStream("/test_patterns/BELLE-NUIT.png"));
				curFrame = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_4BYTE_ABGR);
				curFrame.getGraphics().drawImage(loadedFrame, 0, 0, resolution.width, resolution.height, null);

				bufFrame = curFrame;
			} else if (type == TestPattern.HD_TEST) {
				grabber = new FFmpegFrameGrabber(TestPatternIngestor.class.getResource("/test_patterns/MIRE_60p.mp4"));
				init();
			} else if (type == TestPattern.UHD_TEST) {
				grabber = new FFmpegFrameGrabber(TestPatternIngestor.class.getResource("/test_patterns/MIRE_60p.mp4"));
				init();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init() {
		grabber.setOption("filter_complex", "loop=loop=-1");
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
		if (patternType == TestPattern.HD_TEST || patternType == TestPattern.UHD_TEST)
			synchronized (lock) {
				lock.notify();
			}
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			if (patternType == TestPattern.HD_TEST || patternType == TestPattern.UHD_TEST) {
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
			}
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
