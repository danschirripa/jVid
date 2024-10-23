package com.javashell.video.converters;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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

		final int inputWidth = bytesToInt(fin.readNBytes(4));
		final int inputHeight = bytesToInt(fin.readNBytes(4));

		int frameWidth = inputWidth;
		int frameHeight = inputHeight;

		Rectangle maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

		if (maxSize.width < inputWidth) {
			int ratio = inputWidth / maxSize.width;
			frameWidth = maxSize.width;
			frameHeight = inputHeight * ratio;
		} else if (maxSize.height < inputHeight) {
			int ratio = inputHeight / maxSize.height;
			frameHeight = maxSize.height;
			frameWidth = inputWidth * ratio;
		}

		final int width = inputWidth;
		final int height = inputHeight;

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
					long lastTime;
					long delta = 0;
					long nextSleep = waitTime;
					while (fin.available() > 4) {
						lastTime = System.currentTimeMillis();
						final byte[][] encoded = new byte[subSegments][];
						for (int i = 0; i < subSegments; i++) {
							final int encodedLength = bytesToInt(fin.readNBytes(4));
							encoded[i] = fin.readNBytes(encodedLength);
						}

						synchronized (bufFrame) {
							bufFrame = qoy.convert(encoded, width, height, subSegments);
						}
						delta = System.currentTimeMillis() - lastTime;

						if (delta > waitTime)
							nextSleep = waitTime - delta;
						else
							nextSleep = waitTime;

						if (nextSleep < 0)
							nextSleep = 0;

						Thread.sleep(nextSleep, 0);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		previewFrame.setSize(new Dimension(frameWidth, frameHeight));

		previewFrame.setVisible(true);
		previewFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		long renderTime = System.currentTimeMillis();
		long delta;
		long nextSleep;
		decoderThread0.start();
		while (previewFrame.isVisible()) {
			delta = System.currentTimeMillis() - renderTime;
			renderTime = System.currentTimeMillis();
			if (delta > 1000 / 60)
				System.err.println("Render took " + delta + "ms too long");
			nextSleep = (1000 / 60) - delta;
			if (nextSleep < 0)
				nextSleep = 0;
			Thread.sleep(nextSleep, 0);
			previewFrame.repaint();
		}
		fin.close();
	}

	private int bytesToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}

}
