package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.javashell.video.VideoIngestor;

public class QOIFileIngestor extends VideoIngestor {

	public QOIFileIngestor(Dimension resolution) {
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

}
