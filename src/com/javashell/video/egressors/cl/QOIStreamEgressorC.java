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
	private static byte[] encodedBuffer0, encodedBuffer1;
	private static long lastTime;
	private static boolean isRunning;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private Thread serverThread, egressThread;

	static {
		try {
			InputStream libQOIEncoderStream = QOIStreamEgressor.class.getResourceAsStream("/libQOIEncoder.so");
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

		encodedBuffer0 = encodedBuffer1;
		byte[] frameBytes = convertBufferedImageToByteArray(frame);
		encodedBuffer1 = encode(frameBytes, getResolution().width, getResolution().height, 4, 0);

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
			egressThread = new Thread(new EgressRunnable());
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

	private class EgressRunnable implements Runnable {
		public void run() {
			lastTime = System.nanoTime();
			while (isRunning) {
				if (System.nanoTime() - lastTime >= frameRateInterval) {
					try {
						if (encodedBuffer0 == null) {
							Thread.sleep(10);
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
					} catch (Exception e) {
						e.printStackTrace();
						;
					}
				}
			}
		}
	}

}
