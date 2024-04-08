package com.javashell.video.converters;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.swing.JFrame;

import com.javashell.video.ingestors.QOYStreamIngestor;

public class QOYVFileViewer {
	private File inputFile;
	private final QOYStreamIngestor qoy;
	private BufferedImage bufFrame;

	public QOYVFileViewer(File inputFile) {
		this.inputFile = inputFile;
		qoy = new QOYStreamIngestor();
	}

	public void playback() throws IOException, InterruptedException {
		final FileInputStream fin = new FileInputStream(inputFile);
		int frameRateDoubleStringLength = bytesToInt(fin.readNBytes(4));
		String frameRateDoubleString = new String(fin.readNBytes(frameRateDoubleStringLength));
		double frameRate = Double.parseDouble(frameRateDoubleString);
		int waitTime = (int) (1000 / frameRate);

		final int width = bytesToInt(fin.readNBytes(4));
		final int height = bytesToInt(fin.readNBytes(4));

		final int subSegments = bytesToInt(fin.readNBytes(4));

		System.out.println(width + "x" + height);
		System.out.println(frameRateDoubleString + " fps");

		bufFrame = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

		JFrame previewFrame = new JFrame(inputFile.getName()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void update(Graphics g) {
				paint(g);
			}

			@Override
			public void paint(Graphics g) {
				try {
					g.drawImage(bufFrame, 0, 0, getWidth(), getHeight(), this);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};

		Thread decoderThread0 = new Thread(new Runnable() {
			public void run() {
				try {
					while (fin.available() > 4) {
						final byte[][] encoded = new byte[subSegments][];
						for (int i = 0; i < subSegments; i++) {
							final int encodedLength = bytesToInt(fin.readNBytes(4));
							encoded[i] = fin.readNBytes(encodedLength);
						}

						final byte[] decodedImage = qoy.convertToBytes(encoded, width, height, subSegments);
						synchronized (bufFrame) {
							System.arraycopy(decodedImage, 0,
									((DataBufferByte) bufFrame.getRaster().getDataBuffer()).getData(), 0,
									decodedImage.length);
						}
						Thread.sleep(waitTime - 1, 599999);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		decoderThread0.start();

		previewFrame.setSize(new Dimension(width, height));

		previewFrame.setVisible(true);
		previewFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		long renderTime = System.currentTimeMillis();
		long delta;
		while (previewFrame.isVisible()) {
			delta = System.currentTimeMillis() - renderTime;
			renderTime = System.currentTimeMillis();
			if (delta > 1000 / 60)
				System.err.println("Render took " + delta + "ms too long");
			Thread.sleep(1000 / 60, 0);
			previewFrame.repaint();
		}
		fin.close();
	}

	private int bytesToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}

}
