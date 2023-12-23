package com.javashell.video.egressors.cl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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
	private static int lastBuf = 1, subSegments = 12;
	private static long lastTime;
	private static boolean isRunning;
	private static final long frameRateInterval = (long) 16.3 * 1000000;
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

		bufFrame0 = bufFrame1;
		bufFrame1 = frame;

		if (bufFrame0 == null) {
			bufFrame0 = new BufferedImage(getResolution().width, getResolution().height, BufferedImage.TYPE_INT_ARGB);
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

	private static byte[] convertBufferedImageToByteArray(BufferedImage image) {
		if (image == null) {
			return null;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int numComponents = 4; // Assuming 4 components (RGBA) per pixel

		// Create a byte array to hold the image data
		byte[] imageData = new byte[width * height * numComponents];

		// Get the image's raster data
		int[] pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);

		int index = 0;
		for (int pixel : pixels) {
			// Extract the individual color components (RGBA) from the pixel
			int alpha = (pixel >> 24) & 0xFF;
			int red = (pixel >> 16) & 0xFF;
			int green = (pixel >> 8) & 0xFF;
			int blue = pixel & 0xFF;

			// Store the components in the byte array (assuming RGBA order)
			imageData[index++] = (byte) red;
			imageData[index++] = (byte) green;
			imageData[index++] = (byte) blue;
			imageData[index++] = (byte) alpha;
		}

		return imageData;
	}

	@Override
	public boolean close() {
		isRunning = false;
		return true;
	}

	private native byte[] encode(byte[] data, byte[] preData, int width, int height, int channels, int colorspace);

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	private class EncoderThread1 implements Runnable {

		public void run() {
			long lastTime = System.nanoTime();
			while (true) {
				try {
					long curTime = System.nanoTime();
					if (curTime - lastTime >= frameRateInterval) {
						System.err.println("Encoder1 took too long, " + (curTime - lastTime));
					}
					if (bufFrame1 == null) {
						System.out.println("1 null");
						continue;
					}
					lastTime = System.nanoTime();

					final int width = getResolution().width;
					final int yDelta = getResolution().height / subSegments;
					byte[][] encodedSubImages = new byte[subSegments][];

					ExecutorService es = Executors.newCachedThreadPool();

					for (int i = 0; i < subSegments; i++) {
						final BufferedImage preImage = bufFrame0.getSubimage(0, yDelta * i, width, yDelta);
						final BufferedImage subImage = bufFrame1.getSubimage(0, yDelta * i, width, yDelta);
						final int index = i;
						es.execute(new Runnable() {
							public void run() {
								final long startTime = System.nanoTime();
								final byte[] frameBytes = convertBufferedImageToByteArray(subImage);
								final byte[] prevFrameB = convertBufferedImageToByteArray(preImage);
								encodedSubImages[index] = encode(frameBytes, prevFrameB, width, yDelta, 4, 0);
							}
						});
					}
					es.shutdown();
					es.awaitTermination(1, TimeUnit.SECONDS);
					encodedBuffer0 = encodedSubImages;
					long encodeTime = System.nanoTime() - lastTime;
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
						Thread.sleep(frameRateMS, frameRateNS);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
