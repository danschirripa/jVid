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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.javashell.video.VideoIngestor;

public class QOIStreamIngestorC extends VideoIngestor {
	private String ip;
	private int port;
	private Socket sock;
	private BufferedImage curFrame, bufFrame;
	private Runnable unicastRunner, multicastRunner;
	private Thread decoderThread0, decoderThread1;
	private Thread[] decoderThreads;
	private static boolean isOpen;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private static byte[][] bufBytes0, bufBytes1;
	private static byte[][] bufBytes;
	private static int lastBufByte = 1, subSegments = 12;
	private boolean isMulticast = false;
	private static final String wait0 = "0", wait1 = "1";
	private static String[] wait;

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
		this(resolution, ip, port, isMulticast, 2);
	}

	public QOIStreamIngestorC(Dimension resolution, String ip, int port, boolean isMulticast, int numThreads) {
		super(resolution);
		this.ip = ip;
		this.port = port;
		this.sock = new Socket();
		this.decoderThreads = new Thread[numThreads];
		bufBytes = new byte[numThreads][];
		wait = new String[numThreads];

		for (int i = 0; i < numThreads; i++) {
			wait[i] = "" + i;

			Thread decThread = new Thread(new DecoderRunnable(i));
			decThread.setName("Dec" + i);
			decoderThreads[i] = decThread;
		}
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

			for (Thread t : decoderThreads) {
				t.start();
			}
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

	private class DecoderRunnable implements Runnable {
		private int width = getResolution().width, height = getResolution().height;
		private int decNum, nextNum;

		public DecoderRunnable(int num) {
			decNum = num;
			if (decNum == decoderThreads.length) {
				nextNum = 0;
			} else {
				nextNum = decNum + 1;
			}

		}

		public void run() {
			if (decNum != 0) {
				synchronized (wait[decNum]) {
					try {
						wait[decNum].wait();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			long lastTime = System.nanoTime();
			while (true) {
				try {
					if (System.nanoTime() - lastTime > frameRateInterval) {
						System.err.println("Decoder " + decNum + " took too long between frames");
					}
					if (bufBytes[decNum] == null) {
						System.out.println(decNum + " null");
						continue;
					}
					lastTime = System.nanoTime();

					byte[][] decodedRGB = new byte[subSegments][];
					ExecutorService es = Executors.newCachedThreadPool();

					for (int i = 0; i < subSegments; i++) {
						final int index = i;
						es.execute(new Runnable() {
							public void run() {
								decodedRGB[index] = decode(bufBytes[decNum], bufBytes[decNum].length);
							}
						});
					}
					es.shutdown();
					es.awaitTermination(1, TimeUnit.SECONDS);

					int totalSize = 0;
					for (int i = 0; i < subSegments; i++) {
						totalSize += decodedRGB[i].length;
					}
					final byte[] decodedImage = new byte[totalSize];
					ByteBuffer buf = ByteBuffer.wrap(decodedImage);
					for (int i = 0; i < subSegments; i++)
						buf.put(decodedRGB[i]);

					final BufferedImage tmpFrame = toBufferedImageAbgr(width, height, decodedImage);
					synchronized (bufFrame) {
						bufFrame = tmpFrame;
					}
					synchronized (wait[nextNum]) {
						wait[nextNum].notify();
					}
					synchronized (wait[decNum]) {
						try {
							wait[decNum].wait();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					lastTime = System.nanoTime();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
						if (lastBufByte == decoderThreads.length) {
							lastBufByte = 0;
						} else {
							lastBufByte++;
						}
						bufBytes[lastBufByte] = imageBytes;
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
