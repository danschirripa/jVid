package com.javashell.video.digestors;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.opencv.core.Rect;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoDigestor;
import com.javashell.video.camera.FaceSet;

public class FaceSetPaintingDigestor extends VideoDigestor implements ControlInterface {
	private FaceSet curFaces;

	public FaceSetPaintingDigestor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		return false;
	}

	@Override
	public void processControl(Object obj) {
		if (obj instanceof FaceSet) {
			curFaces = (FaceSet) obj;
		}
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (curFaces != null && curFaces.size() != 0) {
			final Graphics2D g = (Graphics2D) frame.getGraphics();
			g.setStroke(new BasicStroke(2.5f));
			Color original = g.getColor();
			g.setColor(Color.RED);
			final Rect primary = curFaces.getPrimary();
			g.drawRect(primary.x, primary.y, primary.width, primary.height);
			g.setColor(Color.WHITE);
			for (Rect f : curFaces) {
				g.drawRect(f.x, f.y, f.width, f.height);
			}
			g.setColor(original);
		}
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

}
