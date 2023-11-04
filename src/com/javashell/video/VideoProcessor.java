package com.javashell.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public interface VideoProcessor {

	public BufferedImage processFrame(BufferedImage frame);

	public boolean open();

	public boolean close();

	public Dimension getResolution();

}
