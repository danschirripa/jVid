package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import com.javashell.video.VideoEgress;

public class UVCEgressor extends VideoEgress {
	private BufferedImage curFrame;

	public UVCEgressor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = frame;
		return frame;
	}

	@Override
	public boolean open() {
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

	private native void initUVC();

	private byte[] retrieveImage() {
		return ((DataBufferByte) curFrame.getRaster().getDataBuffer()).getData();
	}

}
