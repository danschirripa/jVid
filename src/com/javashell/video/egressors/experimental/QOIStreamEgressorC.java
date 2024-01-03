package com.javashell.video.egressors.experimental;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.javashell.video.VideoEgress;

public class QOIStreamEgressorC extends VideoEgress {
	private ServerSocket server;
	private static HashSet<Socket> clients;
	private static byte[][] encodedBuffer0;
	private static BufferedImage bufFrame0, bufFrame1;
	private static final String lock = "";
	private static int lastBuf = 1, subSegments = 12;
	private static long lastTime;
	private static boolean isRunning;
	private static final long frameRateInterval = (long) 16.3 * 1000000;
	private static int keyFrameInterval = 60;
	private Thread serverThread, egressThread, encoderThread1;

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			InputStream libQOIEncoderStream = QOIStreamEgressorC.class
					.getResourceAsStream("/" + prefix + "/libQOIEncoder.so");
			File libQOIEncoderFile = File.createTempFile("libQOIEncoder", ".so");
			FileOutputStream libQOIEncoderOutputStream = new FileOutputStream(libQOIEncoderFile);
			libQOIEncoderOutputStream.write(libQOIEncoderStream.readAllBytes());
			libQOIEncoderOutputStream.flush();
			libQOIEncoderOutputStream.close();
			libQOIEncoderStream.close();
			System.load(libQOIEncoderFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public QOIStreamEgressorC(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(final BufferedImage frame) {
		if (frame == null)
			return frame;

		bufFrame1 = frame;

		return frame;
	}

	@Override
	public boolean open() {
		try {
			isRunning = true;
			clients = new HashSet<Socket>();
			server = new ServerSocket(4500);
			serverThread = new Thread(new Runnable() {
				public void run() {
					while (!server.isClosed()) {
						try {
							final Socket sock = server.accept();
							clients.add(sock);
							System.out.println("New client " + sock.getInetAddress().getCanonicalHostName());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			serverThread.setName("QOI_Server");
			encoderThread1 = new Thread(new EncoderThread1());
			encoderThread1.setName("QOI_Enc1");
			egressThread = new Thread(new EgressRunnable());
			egressThread.setName("QOI_Egress");
			encoderThread1.start();
			serverThread.start();
			egressThread.start();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private native byte[] encode(byte[] data, int width, int height, int channels, int colorspace, boolean isKeyFrame);
	
	private native static byte[] _convertAndCompareB(byte[] current, byte[] prev, int width, int height, int channels);

	private native static byte[] _convertAndCompareI(int[] current, int[] prev, int width, int height, int channels);

	private native static byte[] _convertDontCompareB(byte[] current, int width, int height, int channels);

	private native static byte[] _convertDontCompareI(int[] current, int width, int height, int channels);

	// Create the individual segments to be encoded, comparing them to the previous
	// image with the intent of setting duplicate pixels to transparent
	private static byte[][] convertAndCompare(BufferedImage current, BufferedImage previous, int width, int height,
			int subSegments, boolean isKey) {

		if (current == null) {
			return null;
		}
		if (previous == null) {
			previous = new BufferedImage(width, height, current.getType());
		}

		final int yDelta = height / subSegments;

		byte[][] rawData = new byte[subSegments][];

		final DataBuffer currentBuf = current.getRaster().getDataBuffer();
		final DataBuffer previousBuf = previous.getRaster().getDataBuffer();

		byte[] currentBytes = null;
		int[] currentInts = null;
		byte[] previousBytes = null;
		int[] previousInts = null;

		if (currentBuf instanceof DataBufferByte) {
			currentBytes = ((DataBufferByte) currentBuf).getData();
		} else {
			currentInts = ((DataBufferInt) currentBuf).getData();
		}
		if (previousBuf instanceof DataBufferByte) {
			previousBytes = ((DataBufferByte) previousBuf).getData();
		} else {
			previousInts = ((DataBufferInt) previousBuf).getData();
		}

		int channels = (current.getAlphaRaster() != null) ? 4 : 3;
		byte[] initalizedImage;
		if (!isKey) {
			if (currentBuf instanceof DataBufferInt) {
				initalizedImage = _convertAndCompareI(currentInts, previousInts, width, height, channels);
			} else {
				initalizedImage = _convertAndCompareB(currentBytes, previousBytes, width, height, channels);
			}
		} else {
			if (currentBuf instanceof DataBufferInt) {
				initalizedImage = _convertDontCompareI(currentInts, width, height, channels);
			} else {
				initalizedImage = _convertDontCompareB(currentBytes, width, height, channels);
			}
		}

		int subSize = width * yDelta * 4;
		for (int nSegments = 0; nSegments < subSegments; nSegments++) {
			rawData[nSegments] = Arrays.copyOfRange(initalizedImage, nSegments * subSize,
					(nSegments * subSize) + subSize);
		}
		return rawData;
	}

	private static byte[] convertBufferedImageToByteArray(final BufferedImage image, int segment) {
		if (image == null) {
			return null;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int numComponents = 4; // Assuming 4 components (RGBA) per pixel
		boolean hasAlpha = image.getAlphaRaster() != null;
		final byte[] imageData = new byte[width * height * numComponents];

		System.out.println("" + width + "x" + height);

		final DataBuffer buf = image.getRaster().getDataBuffer();

		if (buf instanceof DataBufferInt) {
			final int[] pixelData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			System.out.println(imageData.length + " - " + pixelData.length);
			if (!hasAlpha) {
				var pixIndex = 0;
				var imageIndex = 0;
				while (imageIndex < imageData.length) {
					imageData[imageIndex + 3] = (byte) 255;
					imageData[imageIndex] = (byte) pixelData[pixIndex + 2];
					imageData[imageIndex + 1] = (byte) pixelData[pixIndex + 1];
					imageData[imageIndex + 2] = (byte) pixelData[pixIndex];
					imageIndex += 4;
					pixIndex += 3;
				}
			} else {
				for (int i = 0; i < imageData.length; i += 4) {
					imageData[i + 3] = (byte) pixelData[i];
					imageData[i] = (byte) pixelData[i + 3];
					imageData[i + 1] = (byte) pixelData[i + 2];
					imageData[i + 2] = (byte) pixelData[i + 1];
				}
			}
		} else {
			final byte[] pixelData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
			System.out.println(imageData.length + " - " + pixelData.length);
			if (!hasAlpha) {
				var pixIndex = 0;
				var imageIndex = 0;
				while (imageIndex < imageData.length) {
					imageData[imageIndex + 3] = (byte) 255;
					imageData[imageIndex] = pixelData[pixIndex + 2];
					imageData[imageIndex + 1] = pixelData[pixIndex + 1];
					imageData[imageIndex + 2] = pixelData[pixIndex];
					imageIndex += 4;
					pixIndex += 3;
				}
			} else {
				for (int i = 0; i < imageData.length; i += 4) {
					imageData[i + 3] = pixelData[i];
					imageData[i] = pixelData[i + 3];
					imageData[i + 1] = pixelData[i + 2];
					imageData[i + 2] = pixelData[i + 1];
				}
			}
		}

		return imageData;
	}

	@Override
	public boolean close() {
		isRunning = false;
		return true;
	}

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	private class EncoderThread1 implements Runnable {
		private int framesSinceKey = 0;

		public void run() {
			long lastTime = System.nanoTime();
			while (true) {
				try {
					long curTime = System.nanoTime();
					if (curTime - lastTime >= frameRateInterval) {
						// System.err.println("Encoder1 took too long, " + (curTime - lastTime));
					}
					if (bufFrame1 == null) {
						System.out.println("1 null");
						continue;
					}
					lastTime = System.nanoTime();

					final int width = getResolution().width;
					final int yDelta = getResolution().height / subSegments;
					byte[][] encodedSubImages = new byte[subSegments][];

					final boolean isKey = framesSinceKey == keyFrameInterval;
					byte[][] rgbImages = convertAndCompare(bufFrame1, bufFrame0, width, getResolution().height,
							subSegments, isKey);

					ExecutorService es = Executors.newCachedThreadPool();

					for (int i = 0; i < subSegments; i++) {
						final int index = i;
						final byte[] frameBytes = rgbImages[index];
						es.execute(new Runnable() {
							public void run() {
								encodedSubImages[index] = encode(frameBytes, width, yDelta, 4, 0, isKey);
							}
						});
					}
					es.shutdown();
					es.awaitTermination(1, TimeUnit.SECONDS);
					synchronized (lock) {
						encodedBuffer0 = encodedSubImages;
						lock.notify();
						lock.wait();
					}
					bufFrame0 = bufFrame1;
					if (framesSinceKey == keyFrameInterval)
						framesSinceKey = 0;
					else
						framesSinceKey++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class EgressRunnable implements Runnable {
		private long frameRateMS = 0;
		private int frameRateNS = 0;

		public void run() {

			if (frameRateInterval > 999999) {
				frameRateMS = frameRateInterval / 1000000;
				frameRateNS = (int) (frameRateInterval % 1000000);
			} else
				frameRateNS = (int) frameRateInterval;

			lastTime = System.nanoTime();
			while (isRunning) {
				if (System.nanoTime() - lastTime >= frameRateInterval) {
					try {
						synchronized (lock) {
							lock.wait();
						}
						if (encodedBuffer0 == null) {
							Thread.sleep(10);
							System.out.println("egress null");
							continue;
						}
						final byte[][] qoiBytes = encodedBuffer0;
						for (Socket client : clients) {
							try {
								for (int i = 0; i < subSegments; i++) {
									final byte[] size = intToBytes(qoiBytes[i].length);
									if (qoiBytes[i].length <= 0) {
										System.err.println("Size of 0 detected");
										i--;
										continue;
									}
									client.getOutputStream().write(size);
									client.getOutputStream().write(qoiBytes[i]);
								}
							} catch (Exception e) {
								e.printStackTrace();
								clients.remove(client);
							}

						}
						synchronized (lock) {
							lock.notify();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
