package com.javashell.video.egressors.experimental;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.javashell.video.VideoEgress;

public class AV1StreamEgressor extends VideoEgress {

	public AV1StreamEgressor(Dimension resolution) {
		super(resolution);
	}

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

	private native byte[] encodeFrame(byte[] rgb);

}
