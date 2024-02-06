package com.javashell.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.UUID;

public abstract class MultiplexedVideoProcessor implements VideoProcessor {

	@Override
	public abstract BufferedImage processFrame(BufferedImage frame);

	public abstract void ingestFrame(UUID sourceID, BufferedImage frame);

	@Override
	public abstract boolean open();

	@Override
	public abstract boolean close();

	@Override
	public abstract Dimension getResolution();

}
