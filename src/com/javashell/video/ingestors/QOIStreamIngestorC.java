package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.javashell.video.VideoIngestor;

public class QOIStreamIngestorC extends VideoIngestor {
	private String ip;
	private int port;
	private Socket sock;
	private BufferedImage curFrame, bufFrame;
	private Runnable unicastRunner, multicastRunner;
	private Thread decoderThread0, decoderThread1;
	private static boolean isOpen;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private static byte[] bufBytes0, bufBytes1;
	private static int lastBufByte = 1;
	private boolean isMulticast = false;
	private static final String wait0 = "0", wait1 = "1";

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			InputStream libQOIDecoderStream = QOIStreamIngestorC.class
					.getResourceAsStream("/" + prefix + "/libQOIDecoder.so");
			File libQOIDecoderFile = File.createTempFile("libQOIDecoder", ".so");
			FileOutputStream libQOIDecoderOutputStream = new FileOutputStream(libQOIDecoderFile);
			libQOIDecoderOutputStream.write(libQOIDecoderStream.readAllBytes());
			libQOIDecoderOutputStream.flush();
			libQOIDecoderOutputStream.close();
			libQOIDecoderStream.close();
			System.load(libQOIDecoderFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public QOIStreamIngestorC(Dimension resolution, String ip, int port, boolean isMulticast) {
		super(resolution);
		this.ip = ip;
		this.port = port;
		this.sock = new Socket();
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration();
			bufFrame = gc.createCompatibleImage(getResolution().width, getResolution().height);
			sock.connect(new InetSocketAddress(ip, port));
			Thread captureThread;
			isOpen = true;
			if (isMulticast) {
				captureThread = new Thread(new MulticastRunnable());
			} else {
				captureThread = new Thread(new UnicastRunnable());
			}
			captureThread.start();
			decoderThread0 = new Thread(new Decoder0Runnable());
			decoderThread1 = new Thread(new Decoder1Runnable());
			decoderThread0.setName("Dec0");
			decoderThread1.setName("Dec1");
			decoderThread0.start();
			decoderThread1.start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			isOpen = false;
			return false;
		}
	}

	@Override
	public boolean close() {
		try {
			sock.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private int bytesToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}

	private class Decoder0Runnable implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;
		private long localizedFrameRateMS = 0;
		private int localizedFrameRateNS = 0;
		private int width = getResolution().width, height = getResolution().height;

		public void run() {
			if (localizedFrameRateInterval > 999999) {
				localizedFrameRateMS = localizedFrameRateInterval / 1000000;
				localizedFrameRateNS = (int) (localizedFrameRateInterval % 1000000);
			} else
				localizedFrameRateNS = (int) localizedFrameRateInterval;
			long lastTime = System.nanoTime();
			while (true) {
				if (System.nanoTime() - lastTime > localizedFrameRateInterval) {
					System.err.println("Decoder 0 took too long between frames");
				}
				if (bufBytes0 == null) {
					System.out.println("0 null");
					continue;
				}
				lastTime = System.nanoTime();
				byte[] decodedImage = decode(bufBytes0, bufBytes0.length);
				final BufferedImage tmpFrame = toBufferedImageAbgr(width, height, decodedImage);
				synchronized (bufFrame) {
					bufFrame = tmpFrame;
				}
				decodedImage = null;
				synchronized (wait1) {
					wait1.notify();
				}
				synchronized (wait0) {
					try {
						wait0.wait();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				lastTime = System.nanoTime();
			}
		}
	}

	private class Decoder1Runnable implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;
		private long localizedFrameRateMS = 0;
		private int localizedFrameRateNS = 0;
		private int width = getResolution().width, height = getResolution().height;

		public void run() {
			if (localizedFrameRateInterval > 999999) {
				localizedFrameRateMS = localizedFrameRateInterval / 1000000;
				localizedFrameRateNS = (int) (localizedFrameRateInterval % 1000000);
			} else
				localizedFrameRateNS = (int) localizedFrameRateInterval;

			long firstRunDelayMS = 0;
			int firstRunDelayNS = 0;
			long firstRunDelayTotal = localizedFrameRateInterval / 2;

			if (firstRunDelayTotal > 999999) {
				firstRunDelayMS = firstRunDelayTotal / 1000000;
				firstRunDelayNS = (int) (firstRunDelayTotal % 1000000);
			} else
				firstRunDelayNS = (int) localizedFrameRateInterval;

			try {
				Thread.sleep(firstRunDelayMS, firstRunDelayNS);
			} catch (Exception e) {
				e.printStackTrace();
			}

			long lastTime = System.nanoTime();
			while (true) {
				long deltaTime = System.nanoTime() - lastTime;
				if (deltaTime > localizedFrameRateInterval) {
					System.err.println("Decoder 1 took too long between frames - "
							+ (deltaTime - localizedFrameRateInterval) + "ns");
				}
				if (bufBytes1 == null) {
					System.out.println("1 null");
					continue;
				}
				lastTime = System.nanoTime();
				byte[] decodedImage = decode(bufBytes1, bufBytes1.length);
				final BufferedImage tmpFrame = toBufferedImageAbgr(width, height, decodedImage);
				synchronized (bufFrame) {
					bufFrame = tmpFrame;
				}
				decodedImage = null;
				synchronized (wait0) {
					wait0.notify();
				}
				synchronized (wait1) {
					try {
						wait1.wait();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				lastTime = System.nanoTime();
			}
		}
	}

	BufferedImage toBufferedImageAbgr(int width, int height, byte[] abgrData) {
		final DataBuffer dataBuffer = new DataBufferByte(abgrData, width * height * 4, 0);
		final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
				new int[] { 8, 8, 8, 8 }, true, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		final WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 4, 4,
				new int[] { 3, 2, 1, 0 }, null);
		final BufferedImage image = new BufferedImage(colorModel, raster, false, null);
		return image;
	}

	private native byte[] decode(byte[] image, int dataSize);

	private class UnicastRunnable implements Runnable {

		@Override
		public void run() {
			try {
				BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
				OutputStream out = sock.getOutputStream();
				long lastTime = System.currentTimeMillis();
				while (!sock.isClosed()) {
					try {
						final byte[] sizeBytes = new byte[4];
						in.read(sizeBytes);
						if (System.currentTimeMillis() - lastTime > frameRateInterval) {
							System.err.println("Stream not being served at expected rate");
						}
						final int size = bytesToInt(sizeBytes);
						final byte[] imageBytes = in.readNBytes(size);
						if (lastBufByte == 0) {
							bufBytes1 = imageBytes;
							lastBufByte = 1;
						} else {
							bufBytes0 = imageBytes;
							lastBufByte = 0;
						}
					} catch (Exception e) {
						e.printStackTrace();
						in.readAllBytes();
						out.write((byte) 0);
					}
					lastTime = System.currentTimeMillis();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private class MulticastRunnable implements Runnable {

		@Override
		public void run() {
		}

	}

}
