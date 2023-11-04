package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;

import com.javashell.flow.FlowNode;
import com.javashell.video.VideoEgress;
import com.javashell.video.VideoProcessor;

import me.saharnooby.qoi.QOIImage;
import me.saharnooby.qoi.QOIUtil;
import me.saharnooby.qoi.QOIUtilAWT;

public class QOIStreamEgressor extends VideoEgress {
	private ServerSocket server;
	private static HashSet<Socket> clients;
	private static BufferedImage bufFrame0, bufFrame1;
	private int lastFrameThread = 0;
	private static QOIImage curFrame;
	private static long lastTime;
	private static boolean isRunning;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private Thread serverThread, egressThread, frameConverterThread0, frameConverterThread1;

	public QOIStreamEgressor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(final BufferedImage frame) {
		if (frame == null)
			return frame;

		if (lastFrameThread == 0) {
			lastFrameThread = 1;
			bufFrame1 = frame;
		} else {
			lastFrameThread = 0;
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
			egressThread = new Thread(new EgressRunnable());
			frameConverterThread0 = new Thread(new Frame0EncoderRunnable());
			frameConverterThread1 = new Thread(new Frame1EncoderRunnable());

			serverThread.start();
			egressThread.start();

			frameConverterThread0.start();
			frameConverterThread1.start();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	@Override
	public boolean close() {
		isRunning = false;
		return true;
	}

	private class Frame0EncoderRunnable implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;

		public void run() {
			long lastTime = System.nanoTime();
			while (true) {
				if (System.nanoTime() - lastTime >= localizedFrameRateInterval) {
					lastTime = System.nanoTime();
					if (bufFrame0 == null) {
						System.out.println("0 null");
						continue;
					}
					lastTime = System.nanoTime();
					curFrame = QOIUtilAWT.createFromBufferedImage(bufFrame0);
				}
			}
		}
	}

	private class Frame1EncoderRunnable implements Runnable {
		private long localizedFrameRateInterval = frameRateInterval * 2;

		public void run() {
			try {
				Thread.sleep(localizedFrameRateInterval);
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
					curFrame = QOIUtilAWT.createFromBufferedImage(bufFrame1);
				}
			}
		}
	}

	private class EgressRunnable implements Runnable {
		public void run() {
			lastTime = System.nanoTime();
			while (isRunning) {
				if (System.nanoTime() - lastTime >= frameRateInterval) {
					try {
						final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
						QOIUtil.writeImage(curFrame, bOut);
						bOut.flush();
						final byte[] qoiBytes = bOut.toByteArray();
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
