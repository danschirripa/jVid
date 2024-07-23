package com.javashell.video.digestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.javashell.video.VideoDigestor;

public class ScalingDigestor extends VideoDigestor {
	private final Dimension outputResolution;
	private BufferedImage resizedFrame;

	public ScalingDigestor(Dimension resolution, Dimension outputResolution) {
		super(resolution);
		this.outputResolution = outputResolution;
		this.resizedFrame = new BufferedImage(outputResolution.width, outputResolution.height,
				BufferedImage.TYPE_4BYTE_ABGR);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		resizedFrame.getGraphics().drawImage(frame, 0, 0, outputResolution.width, outputResolution.height, null);
		return resizedFrame;
	}

	@Override
	public boolean open() {
		return false;
	}

	@Override
	public boolean close() {
		return false;
	}

}
