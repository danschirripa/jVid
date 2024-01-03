package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.javashell.video.VideoEgress;

public class ImageSeriesEgressor extends VideoEgress {
	private File outputFolder;
	private int numFrames, curFrames = 0;

	public ImageSeriesEgressor(Dimension resolution, File outputFolder, int numFrames) {
		super(resolution);
		this.outputFolder = outputFolder;
		this.numFrames = Math.abs(numFrames);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (curFrames == numFrames) {
			return frame;
		}
		try {
			ImageIO.write(frame, "png", new File(outputFolder, curFrames + "_out.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		curFrames++;
		return frame;
	}

	@Override
	public boolean open() {
		if (outputFolder.exists() && outputFolder.isDirectory()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

}
