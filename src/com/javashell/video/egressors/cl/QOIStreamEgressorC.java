package com.javashell.video.egressors.cl;

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
	private static final String lock = "", lock1 = "";
	private static int subSegments = 20;
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
		synchronized (lock) {
			lock.notify();
		}

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
							sock.setSendBufferSize(87380);
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

	private native byte[] encodeB(byte[] data, byte[] prev, int width, int height, int channels, int colorspace,
			boolean isKeyFrame);

	private native byte[] encodeI(int[] data, int[] prev, int width, int height, int channels, int colorspace,
			boolean isKeyFrame);

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
						System.err.println("Encoder1 took too long, " + (curTime - lastTime));
					}
					synchronized (lock) {
						lock.wait();
					}
					if (bufFrame1 == null) {
						System.out.println("1 null");
						continue;
					}
					lastTime = System.nanoTime();

					final int width = getResolution().width;
					final int yDelta = getResolution().height / subSegments;
					byte[][] encodedSubImages = new byte[subSegments][];

					boolean isKey = framesSinceKey == keyFrameInterval;
					//boolean isKey = true;

					final int channels = (bufFrame1.getAlphaRaster() != null) ? 4 : 3;
					final int subSize = width * yDelta * channels;

					if (bufFrame0 == null) {
						bufFrame0 = new BufferedImage(width, getResolution().height, bufFrame1.getType());
					}

					final DataBuffer currentBuf = bufFrame1.getRaster().getDataBuffer();
					final DataBuffer previousBuf = bufFrame0.getRaster().getDataBuffer();

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

					ExecutorService es = Executors.newFixedThreadPool(subSegments);

					for (int i = 0; i < subSegments; i++) {
						final int index = i;
						if (currentBuf instanceof DataBufferByte) {
							final byte[] frameBytes = Arrays.copyOfRange(currentBytes, index * subSize,
									(index * subSize) + subSize);
							final byte[] prevBytes = Arrays.copyOfRange(previousBytes, index * subSize,
									(index * subSize) + subSize);
							es.execute(new Runnable() {
								public void run() {
									encodedSubImages[index] = encodeB(frameBytes, prevBytes, width, yDelta, channels, 0,
											isKey);
								}
							});
						} else {
							final int[] frameBytes = Arrays.copyOfRange(currentInts, index * subSize,
									(index * subSize) + subSize);
							final int[] prevBytes = Arrays.copyOfRange(previousInts, index * subSize,
									(index * subSize) + subSize);
							es.execute(new Runnable() {
								public void run() {
									encodedSubImages[index] = encodeI(frameBytes, prevBytes, width, yDelta, channels, 0,
											isKey);
								}
							});
						}
					}
					es.shutdown();
					es.awaitTermination(1, TimeUnit.SECONDS);
					encodedBuffer0 = encodedSubImages;

					bufFrame0 = bufFrame1;
					synchronized (lock1) {
						lock1.notify();
					}
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
		public void run() {
			lastTime = System.nanoTime();
			while (isRunning) {
				long diff = System.nanoTime() - lastTime;
				if (diff > frameRateInterval) {
					System.err.println("Egress took too long " + diff);
				}
				try {
					synchronized (lock1) {
						lock1.wait();
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
					lastTime = System.nanoTime();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
