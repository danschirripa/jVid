package com.javashell.video.digestors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.UUID;

import com.javashell.video.MultiplexedVideoProcessor;

public class MultiviewDigestor extends MultiplexedVideoProcessor {
	private BufferedImage multiplexedFrame;
	private final Dimension resolution, viewportResolution;
	private int viewports, rows, cols;
	private HashMap<UUID, BufferedImage> imageMapping = new HashMap<UUID, BufferedImage>();
	private final UUID[] idIndices;
	private int idIndicesOffset = 0;

	public MultiviewDigestor(Dimension resolution, int viewports, int rows, int col) {
		this.resolution = resolution;
		this.viewportResolution = new Dimension(resolution.width / col, resolution.height / col);
		this.viewports = viewports;
		this.rows = rows;
		this.cols = col;
		idIndices = new UUID[viewports];
		multiplexedFrame = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		int posx = 0, posy = 0;
		Graphics g = multiplexedFrame.getGraphics();
		for (int i = 0; i < idIndices.length; i++) {
			int xoffset = viewportResolution.width * posx;
			int yoffset = viewportResolution.height * posy;
			UUID u = idIndices[i];
			if (u == null) {
				posx++;
				if (posx == cols) {
					posx = 0;
					posy++;
				}
				continue;
			}
			g.drawImage(imageMapping.get(u), xoffset, yoffset, viewportResolution.width, viewportResolution.height,
					null);
			posx++;
			if (posx == cols) {
				posx = 0;
				posy++;
			}
		}
		g = null;
		frame = null;
		return multiplexedFrame;
	}

	@Override
	public void ingestFrame(UUID sourceID, BufferedImage frame) {
		if (!imageMapping.containsKey(sourceID)) {
			if (idIndices.length == idIndicesOffset)
				return;
			idIndices[idIndicesOffset] = sourceID;
			idIndicesOffset++;
		}
		imageMapping.put(sourceID, frame);
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public boolean close() {
		return true;
	}

	@Override
	public Dimension getResolution() {
		return resolution;
	}

}
