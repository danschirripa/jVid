package com.javashell.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public abstract class VideoIngestor implements VideoProcessor {

	private final Dimension resolution;

	public VideoIngestor(Dimension resolution) {
		this.resolution = resolution;
	}

	@Override
	public abstract BufferedImage processFrame(BufferedImage frame);

	@Override
	public abstract boolean open();

	@Override
	public abstract boolean close();

	@Override
	public Dimension getResolution() {
		// TODO Auto-generated method stub
		return resolution;
	}

}
