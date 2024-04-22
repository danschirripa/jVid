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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.javashell.video.VideoIngestor;

public class QOYStreamIngestor extends VideoIngestor {
	private String ip;
	private int port;
	private Socket sock;
	private BufferedImage curFrame, bufFrame;
	private Runnable unicastRunner, multicastRunner;
	private static Thread[] decoderThreads;
	private static boolean isOpen;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private static byte[][] bufBytes0, bufBytes1;
	private static byte[][][] bufBytes;
	private static int lastBufByte = 1, subSegments = 20;
	private boolean isMulticast = false;
	private final String lock = "", lock1 = "";

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			InputStream libQOYDecoderStream = QOYStreamIngestor.class
					.getResourceAsStream("/" + prefix + "/libQOYDecoder.so");
			File libQOYDecoderFile = File.createTempFile("libQOYDecoder", ".so");
			FileOutputStream libQOYDecoderOutputStream = new FileOutputStream(libQOYDecoderFile);
			libQOYDecoderOutputStream.write(libQOYDecoderStream.readAllBytes());
			libQOYDecoderOutputStream.flush();
			libQOYDecoderOutputStream.close();
			libQOYDecoderStream.close();
			System.load(libQOYDecoderFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public QOYStreamIngestor(Dimension resolution, String ip, int port, boolean isMulticast) {
		this(resolution, ip, port, isMulticast, 4);
	}

	public QOYStreamIngestor() {
		super(new Dimension(1920, 1080));
	}

	public QOYStreamIngestor(Dimension resolution, String ip, int port, boolean isMulticast, int numThreads) {
		super(resolution);
		this.ip = ip;
		this.port = port;
		this.sock = new Socket();
		decoderThreads = new Thread[numThreads];
		bufBytes = new byte[numThreads][subSegments][];

		if (numThreads == 1) {
			decoderThreads[0] = new Thread(new SingleDecoderRunnable());
		} else {
			final DecoderRunnable[] decRunners = new DecoderRunnable[numThreads];

			for (int i = 0; i < numThreads; i++) {
				decRunners[i] = new DecoderRunnable(i);
			}

			for (int i = 0; i < numThreads; i++) {
				final DecoderRunnable decRun = decRunners[i];
				final DecoderRunnable nextDec = (i + 1 == numThreads) ? decRunners[0] : decRunners[i + 1];
				decRun.setThisThread(decRun);
				decRun.setNextThread(nextDec);
			}
			for (int i = 0; i < numThreads; i++) {
				Thread decThread = new Thread(decRunners[i]);
				decThread.setName("Dec" + i);
				decoderThreads[i] = decThread;
			}
		}
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		return bufFrame;
	}

	@Override
	public boolean open() {
		try {
			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration();
			bufFrame = new BufferedImage(getResolution().width, getResolution().height, BufferedImage.TYPE_4BYTE_ABGR);
			curFrame = new BufferedImage(getResolution().width, getResolution().height, BufferedImage.TYPE_4BYTE_ABGR);
			sock.setReceiveBufferSize(87380);
			sock.connect(new InetSocketAddress(ip, port));
			Thread captureThread;
			isOpen = true;
			if (isMulticast) {
				captureThread = new Thread(new MulticastRunnable());
			} else {
				captureThread = new Thread(new UnicastRunnable());
			}
			captureThread.setName("QOIIngress");
			captureThread.start();

			for (Thread t : decoderThreads) {
				t.setName("QOIDec");
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

	public BufferedImage convert(byte[][] encoded, int width, int height, int subSegments) {
		BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		ExecutorService es = Executors.newCachedThreadPool();
		final byte[][] decodedRGB = new byte[subSegments][];

		for (int i = 0; i < subSegments; i++) {
			final int index = i;
			es.execute(new Runnable() {
				public void run() {
					decodedRGB[index] = decode(encoded[index], encoded[index].length);
				}
			});
		}
		es.shutdown();
		try {
			es.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int totalSize = 0;
		for (int i = 0; i < subSegments; i++) {
			totalSize += decodedRGB[i].length;
		}
		final byte[] decodedImage = new byte[totalSize];
		ByteBuffer buf = ByteBuffer.wrap(decodedImage);
		for (int i = 0; i < subSegments; i++)
			buf.put(decodedRGB[i]);
		System.arraycopy(decodedImage, 0, ((DataBufferByte) output.getRaster().getDataBuffer()).getData(), 0,
				decodedImage.length);
		return output;
	}

	public byte[] convertToBytes(byte[][] encoded, int width, int height, int subSegments) {
		BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		ExecutorService es = Executors.newCachedThreadPool();
		final byte[][] decodedRGB = new byte[subSegments][];

		for (int i = 0; i < subSegments; i++) {
			final int index = i;
			es.execute(new Runnable() {
				public void run() {
					decodedRGB[index] = decode(encoded[index], encoded[index].length);
				}
			});
		}
		es.shutdown();
		try {
			es.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int totalSize = 0;
		for (int i = 0; i < subSegments; i++) {
			totalSize += decodedRGB[i].length;
		}
		final byte[] decodedImage = new byte[totalSize];
		ByteBuffer buf = ByteBuffer.wrap(decodedImage);
		for (int i = 0; i < subSegments; i++)
			buf.put(decodedRGB[i]);
		return decodedImage;
	}

	private int bytesToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}

	private class DecoderRunnable implements Runnable {
		private int width = getResolution().width, height = getResolution().height;
		private int decNum;
		private Object thisThread, nextThread;

		public DecoderRunnable(int num) {
			decNum = num;
		}

		public void setNextThread(Object nextThread) {
			this.nextThread = nextThread;
		}

		public void setThisThread(Object thisThread) {
			this.thisThread = thisThread;
		}

		public void run() {
			if (decNum != 0)
				synchronized (thisThread) {
					try {
						thisThread.wait();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			long lastTime = System.nanoTime();
			while (true) {
				try {

					if (bufBytes[decNum] == null) {
						System.out.println(decNum + " null");
						continue;
					}
					if (bufBytes[decNum].length < subSegments) {
						System.out.println("Bad subsegs");
						continue;
					}
					if (bufBytes[decNum][0] == null) {
						System.out.println("No data");
						continue;
					}
					lastTime = System.nanoTime();

					byte[][] decodedRGB = new byte[subSegments][];
					ExecutorService es = Executors.newCachedThreadPool();

					for (int i = 0; i < subSegments; i++) {
						final int index = i;
						es.execute(new Runnable() {
							public void run() {
								decodedRGB[index] = decode(bufBytes[decNum][index], bufBytes[decNum][index].length);
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

					synchronized (bufFrame) {
						System.arraycopy(decodedImage, 0,
								((DataBufferByte) bufFrame.getRaster().getDataBuffer()).getData(), 0,
								decodedImage.length);
					}
					synchronized (nextThread) {
						nextThread.notify();
					}
					synchronized (thisThread) {
						try {
							thisThread.wait();
						} catch (Exception e) {
							e.printStackTrace();
						}
						lastTime = System.nanoTime();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class SingleDecoderRunnable implements Runnable {
		public void run() {
			long lastTime = System.nanoTime();
			while (true) {
				try {
					synchronized (lock1) {
						lock1.wait();
					}
					if (bufBytes[0] == null) {
						System.out.println(1 + " null");
						continue;
					}
					if (bufBytes[0].length < subSegments) {
						System.out.println("Bad subsegs");
						continue;
					}
					if (bufBytes[0][0] == null) {
						System.out.println("No data");
						continue;
					}
					lastTime = System.nanoTime();

					final byte[][] decodedRGB = new byte[subSegments][];
					ExecutorService es = Executors.newFixedThreadPool(subSegments);

					for (int i = 0; i < subSegments; i++) {
						final int index = i;
						es.execute(new Runnable() {
							public void run() {
								decodedRGB[index] = decode(bufBytes[0][index], bufBytes[0][index].length);
							}
						});
					}
					es.shutdown();
					es.awaitTermination(1, TimeUnit.SECONDS);
					if (System.nanoTime() - lastTime > frameRateInterval) {
						System.err.println("Decode took too long");
					}

					int totalSize = 0;
					for (int i = 0; i < subSegments; i++) {
						totalSize += decodedRGB[i].length;
					}
					final byte[] decodedImage = new byte[totalSize];
					final ByteBuffer buf = ByteBuffer.wrap(decodedImage);
					for (int i = 0; i < subSegments; i++)
						buf.put(decodedRGB[i]);

					synchronized (bufFrame) {
						System.arraycopy(decodedImage, 0,
								((DataBufferByte) bufFrame.getRaster().getDataBuffer()).getData(), 0,
								decodedImage.length);
						//bufFrame.getGraphics().drawImage(curFrame, 0, 0, null);
					}

					lastTime = System.nanoTime();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private native byte[] decode(byte[] image, int dataSize);

	private class UnicastRunnable implements Runnable {

		@Override
		public void run() {
			try {
				BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
				OutputStream out = sock.getOutputStream();
				while (!sock.isClosed()) {
					try {
						if (lastBufByte == decoderThreads.length) {
							lastBufByte = 0;
						}
						for (int i = 0; i < subSegments; i++) {
							final byte[] sizeBytes = in.readNBytes(4);
							final int size = bytesToInt(sizeBytes);
							final byte[] imageBytes = in.readNBytes(size);
							bufBytes[lastBufByte][i] = imageBytes;
						}
						lastBufByte++;
						synchronized (lock1) {
							lock1.notify();
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
