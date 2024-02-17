package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.javashell.video.VideoEgress;

public class NDI5Egressor extends VideoEgress {
	private final String ndiName;
	private static final long frameRateInterval = (long) 16.3 * 1000000;
	private Thread egressThread;
	private BufferedImage curFrame;
	private String lock1 = "";

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			System.out.println("Prefix: " + prefix);

			InputStream libNDIEncoderStream = NDI5Egressor.class.getResourceAsStream("/" + prefix + "/libndi.so");
			File libNDIEncoderFile = File.createTempFile("libndi", ".so");
			FileOutputStream libNDIEncoderOutputStream = new FileOutputStream(libNDIEncoderFile);
			libNDIEncoderOutputStream.write(libNDIEncoderStream.readAllBytes());
			libNDIEncoderOutputStream.flush();
			libNDIEncoderOutputStream.close();
			libNDIEncoderStream.close();
			System.load(libNDIEncoderFile.getAbsolutePath());

			libNDIEncoderStream = NDI5Egressor.class.getResourceAsStream("/" + prefix + "/libNDIEncoder.so");
			libNDIEncoderFile = File.createTempFile("libNDIEncoder", ".so");
			libNDIEncoderOutputStream = new FileOutputStream(libNDIEncoderFile);
			libNDIEncoderOutputStream.write(libNDIEncoderStream.readAllBytes());
			libNDIEncoderOutputStream.flush();
			libNDIEncoderOutputStream.close();
			libNDIEncoderStream.close();
			System.load(libNDIEncoderFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public NDI5Egressor(Dimension resolution, String name) {
		super(resolution);
		ndiName = name;
	}

	public NDI5Egressor(Dimension resolution) {
		this(resolution, "jVid");
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = frame;
		synchronized (lock1) {
			lock1.notify();
		}
		return frame;
	}

	@Override
	public boolean open() {
		initializeNDI(getResolution().width, getResolution().height, ndiName);
		egressThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						synchronized (lock1) {
							lock1.wait();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					final int channels = (curFrame.getAlphaRaster() != null) ? 4 : 3;
					final DataBuffer currentBuf = curFrame.getRaster().getDataBuffer();
					if (currentBuf instanceof DataBufferByte) {
						sendFrameB(((DataBufferByte) currentBuf).getData(), channels);
					} else {
						sendFrameI(((DataBufferInt) currentBuf).getData(), channels);
					}
				}
			}
		});
		egressThread.start();
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

	private native void initializeNDI(int width, int height, String name);

	private native void sendFrameB(byte[] frame, int channels);

	private native void sendFrameI(int[] frame, int channels);

}
