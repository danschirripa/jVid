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

import com.javashell.video.VideoEgress;

public class QOIStreamEgressorC extends VideoEgress {
	private ServerSocket server;
	private static HashSet<Socket> clients;
	private static byte[] encodedBuffer0;
	private static BufferedImage bufFrame0, bufFrame1;
	private static int lastBuf = 0;
	private static long lastTime;
	private static boolean isRunning;
	private static final long frameRateInterval = (long) 16.3 * 1000000;
	private Thread serverThread, egressThread, encoderThread0, encoderThread1;

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			InputStream libQOIEncoderStream = QOIStreamEgressor.class
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

		if (lastBuf == 0) {
			lastBuf = 1;
			bufFrame1 = frame;
		} else {
			lastBuf = 0;
			bufFrame0 = frame;
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
			encoderThread0 = new Thread(new EncoderThread0());
			encoderThread1 = new Thread(new EncoderThread1());
			encoderThread0.setName("QOI_Enc0");
			encoderThread1.setName("QOI_Enc1");
			egressThread = new Thread(new EgressRunnable());
			egressThread.setName("QOI_Egress");
			encoderThread0.start();
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

	private native byte[] encode(byte[] data, int width, int height, int channels, int colorspace);

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	private class EncoderThread0 implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;
		private long localizedFrameRateMS = 0;
		private int localizedFrameRateNS = 0;

		public void run() {
			if (localizedFrameRateInterval > 999999) {
				localizedFrameRateMS = localizedFrameRateInterval / 1000000;
				localizedFrameRateNS = (int) (localizedFrameRateInterval % 1000000);
			} else
				localizedFrameRateNS = (int) localizedFrameRateInterval;
			long lastTime = System.nanoTime();
			while (true) {
				if (System.nanoTime() - lastTime >= localizedFrameRateInterval) {
					lastTime = System.nanoTime();
					if (bufFrame0 == null) {
						System.out.println("0 null");
						continue;
					}
					lastTime = System.nanoTime();
					final byte[] frameBytes = convertBufferedImageToByteArray(bufFrame0);
					encodedBuffer0 = encode(frameBytes, getResolution().width, getResolution().height, 4, 0);
					try {
						Thread.sleep(localizedFrameRateMS, localizedFrameRateNS);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

	private class EncoderThread1 implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;
		private long localizedFrameRateMS = 0;
		private int localizedFrameRateNS = 0;

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
				if (System.nanoTime() - lastTime >= localizedFrameRateInterval) {
					lastTime = System.nanoTime();
					if (bufFrame1 == null) {
						System.out.println("1 null");
						continue;
					}
					lastTime = System.nanoTime();
					final byte[] frameBytes = convertBufferedImageToByteArray(bufFrame1);
					encodedBuffer0 = encode(frameBytes, getResolution().width, getResolution().height, 4, 0);
					try {
						Thread.sleep(localizedFrameRateMS, localizedFrameRateNS);
					} catch (Exception e) {
						e.printStackTrace();
					}
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
							System.out.println("null");
							continue;
						}
						final byte[] qoiBytes = encodedBuffer0;
						final byte[] size = intToBytes(qoiBytes.length);
						for (Socket client : clients) {
							try {
								client.getOutputStream().write(size);
								client.getOutputStream().write(qoiBytes);
							} catch (Exception e) {
								clients.remove(client);
							}

						}
						Thread.sleep(frameRateMS, frameRateNS);
					} catch (Exception e) {
						e.printStackTrace();
						;
					}
				}
			}
		}
	}

}
