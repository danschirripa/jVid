package com.javashell.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class NullEndpoint implements VideoProcessor {

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		return null;
	}

	@Override
	public boolean open() {
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

	@Override
	public Dimension getResolution() {
		return null;
	}

}
