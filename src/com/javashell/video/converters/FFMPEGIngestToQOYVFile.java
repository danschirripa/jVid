package com.javashell.video.converters;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.egressors.QOYStreamEgressor;
import com.javashell.video.egressors.cl.QOIStreamEgressorC;

public class FFMPEGIngestToQOYVFile {
	private File inputFile;
	private String outputFile;
	private final String fileExtension = ".qov";
	private FFmpegFrameGrabber grab;
	private final QOYStreamEgressor qoyOut;
	private Java2DFrameConverter conv;
	private int subSegments = 12;

	public FFMPEGIngestToQOYVFile(File inputFile, String outputFile) {
		qoyOut = new QOYStreamEgressor(new Dimension(1920, 1080));
		this.outputFile = outputFile;
		this.inputFile = inputFile;
	}

	public void runConversion() throws IOException {
		conv = new Java2DFrameConverter();
		grab = new FFmpegFrameGrabber(inputFile);
		grab.setNumBuffers(0);
		grab.start();

		System.out.println(grab.getFrameRate());
		System.out.println(grab.getImageWidth() + "x" + grab.getImageHeight());
		System.out.println(grab.getVideoCodecName());

		int length = grab.getLengthInVideoFrames();
		int curVideoFrame = 0;
		FileOutputStream fOut = new FileOutputStream(new File(outputFile + fileExtension));
		JFrame progressFrame = new JFrame("Converting...");
		JProgressBar progress = new JProgressBar(0, length);
		progress.setValue(0);
		progress.setStringPainted(true);
		progress.setVisible(true);
		progressFrame.add(progress);
		progressFrame.setSize(150, 50);
		progressFrame.setVisible(true);
		Frame curFrame;
		BufferedImage preImg;

		final byte[] frameRateDoubleString = (grab.getFrameRate() + "").getBytes();
		final byte[] frameRateDoubleStringLength = intToBytes(frameRateDoubleString.length);

		final byte[] imageWidthInt = intToBytes(grab.getImageWidth());
		final byte[] imageHeightInt = intToBytes(grab.getImageHeight());

		final byte[] sugSegmentsInt = intToBytes(subSegments);

		fOut.write(frameRateDoubleStringLength);
		fOut.write(frameRateDoubleString);
		fOut.write(imageWidthInt);
		fOut.write(imageHeightInt);
		fOut.write(sugSegmentsInt);
		fOut.flush();

		curFrame = grab.grabImage();
		preImg = conv.getBufferedImage(curFrame);

		int keyFrameInterval = 120;
		int keyFrameIndex = 0;

		while (true) {
			curFrame = grab.grabImage();
			curVideoFrame++;
			if (curVideoFrame == length || curFrame == null)
				break;
			final BufferedImage bufFrame = conv.convert(curFrame);
			boolean isKey = false;
			final byte[][] nextEncodedFrame = qoyOut.convertFromBufferedImage(bufFrame, preImg, curVideoFrame == 1, 12);
			for (int i = 0; i < subSegments; i++) {
				final byte[] nextEncodedFrameLengthInt = intToBytes(nextEncodedFrame[i].length);
				fOut.write(nextEncodedFrameLengthInt);
				fOut.write(nextEncodedFrame[i]);
			}
			progress.setValue(curVideoFrame);
			System.out.println(curVideoFrame);
			preImg = null;
			preImg = Java2DFrameConverter.cloneBufferedImage(bufFrame);
			curFrame.close();
		}
		grab.stop();
		fOut.flush();
		fOut.close();
		progressFrame.dispose();
	}

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

}
