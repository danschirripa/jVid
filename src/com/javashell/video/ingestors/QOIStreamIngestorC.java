package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.javashell.video.VideoIngestor;
import com.javashell.video.egressors.cl.QOIStreamEgressor;

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
	private static int lastBufByte = 0;
	private boolean isMulticast = false;

	static {
		try {
			InputStream libQOIDecoderStream = QOIStreamIngestorC.class.getResourceAsStream("/libQOIDecoder.so");
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

		public void run() {
			long lastTime = System.nanoTime();
			while (true) {
				if (System.nanoTime() - lastTime >= localizedFrameRateInterval) {
					lastTime = System.nanoTime();
					if (bufBytes0 == null)
						continue;
					ByteArrayInputStream bin = new ByteArrayInputStream(decode(bufBytes0));
					try {
						bufFrame = ImageIO.read(bin);
						bin.close();
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
	}

	private class Decoder1Runnable implements Runnable {
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
					if (bufBytes1 == null)
						continue;
					ByteArrayInputStream bin = new ByteArrayInputStream(decode(bufBytes1));
					try {
						bufFrame = ImageIO.read(bin);
						bin.close();
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
	}

	private native byte[] decode(byte[] image);

	private class UnicastRunnable implements Runnable {

		@Override
		public void run() {
			try {
				BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
				OutputStream out = sock.getOutputStream();
				while (!sock.isClosed()) {
					try {
						final byte[] sizeBytes = new byte[4];
						in.read(sizeBytes);
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