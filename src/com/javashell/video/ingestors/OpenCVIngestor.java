package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.stream.Stream;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import com.javashell.video.VideoIngestor;

public class OpenCVIngestor extends VideoIngestor {
	private static BufferedImage nullFrame;
	private Thread captureThread;
	private boolean isOpen = true;
	private static BufferedImage curFrame, bufFrame;
	private static VideoCapture cap;
	private int device;
	private long lastFPS = 0;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		if (com.sun.jna.Platform.isWindows()) {
			String gstPath = System.getProperty("gstreamer.path",
					Stream.of("GSTREAMER_1_0_ROOT_MSVC_X86_64", "GSTREAMER_1_0_ROOT_MINGW_X86_64",
							"GSTREAMER_1_0_ROOT_X86_64").map(System::getenv).filter(p -> p != null)
							.map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\").findFirst().orElse(""));
			if (!gstPath.isEmpty()) {
				String systemPath = System.getenv("PATH");
				if (systemPath == null || systemPath.trim().isEmpty()) {
					com.sun.jna.platform.win32.Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath);
				} else {
					com.sun.jna.platform.win32.Kernel32.INSTANCE.SetEnvironmentVariable("PATH",
							gstPath + File.pathSeparator + systemPath);
				}
			}
		} else if (com.sun.jna.Platform.isMac()) {
			String gstPath = System.getProperty("gstreamer.path", "/Library/Frameworks/GStreamer.framework/Libraries/");
			if (!gstPath.isEmpty()) {
				String jnaPath = System.getProperty("jna.library.path", "").trim();
				if (jnaPath.isEmpty()) {
					System.setProperty("jna.library.path", gstPath);
				} else {
					System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
				}
			}

		}

		Gst.init(Version.BASELINE, "jVid");

	}

	public OpenCVIngestor(Dimension resolution, int device) {
		super(resolution);

		generateNullFrame();

		this.device = device;

		curFrame = nullFrame;

		captureThread = new Thread(new Runnable() {
			public void run() {
				Mat javacvFrame = new Mat();
				while (isOpen) {
					try {
						long startTime = System.nanoTime();
						javacvFrame = new Mat();
						cap.read(javacvFrame);
						javacvFrame = javacvFrame.clone();
						bufFrame = matToBufferedImage(javacvFrame);
						javacvFrame.release();
						long endTime = System.nanoTime();
						lastFPS = (1000000000 / (endTime - startTime));
						Thread.sleep(16);
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		});
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = null;
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			cap = new VideoCapture();
			cap.open(device);
			isOpen = true;
			cap.set(Videoio.CAP_PROP_FRAME_WIDTH, this.getResolution().getWidth());
			cap.set(Videoio.CAP_PROP_FRAME_HEIGHT, this.getResolution().getHeight());
			captureThread.start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean close() {
		try {
			isOpen = false;
			cap.release();
			cap = null;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void generateNullFrame() {
		if (nullFrame == null) {
			nullFrame = new BufferedImage(this.getResolution().width, this.getResolution().height,
					BufferedImage.TYPE_INT_RGB);
			nullFrame.getGraphics().setColor(java.awt.Color.CYAN);
			nullFrame.getGraphics().fill3DRect(0, 0, getResolution().width, getResolution().height, true);
		}
	}

	/**
	 * Convert a MAT object to a BufferedImage for processing
	 * 
	 * @param original Mat to convert
	 * @return Converted BufferedImage
	 */
	private static BufferedImage matToBufferedImage(Mat original) {
		BufferedImage image = null;
		final int width = original.width(), height = original.height(), channels = original.channels();
		final byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		return image;
	}
}
