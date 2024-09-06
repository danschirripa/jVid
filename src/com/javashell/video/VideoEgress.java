package com.javashell.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.javashell.audio.JackAudioDigestor;

public abstract class VideoEgress implements VideoProcessor {
	private final Dimension resolution;

	public VideoEgress(Dimension resolution) {
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
		return resolution;
	}

}
