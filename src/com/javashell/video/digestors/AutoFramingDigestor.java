package com.javashell.video.digestors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.opencv.core.Rect;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoDigestor;
import com.javashell.video.camera.FaceSet;

public class AutoFramingDigestor extends VideoDigestor implements ControlInterface {
	private FaceSet curFaces;
	private Rect overallRect;
	private boolean isTransitioning = false;
	private int xpadding = 100, ypadding = 200, transitionalFrames = 60, nTransitionalFrame = 0,
			transitionalStepSizeW = 0, transitionalStepSizeH = 0, transitionalStepSizeX = 0, transitionalStepSizeY = 0;
	private final float ratio;

	public AutoFramingDigestor(Dimension resolution) {
		super(resolution);
		ratio = ((float) resolution.width) / ((float) resolution.height);
		overallRect = new Rect(0, 0, resolution.width, resolution.height);
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
		analyzeFaceSet();
	}

	private void analyzeFaceSet() {
		if (isTransitioning || curFaces.size() == 0) {
			return;
		}
		int minx, maxx, miny, maxy;
		final Rect prim = curFaces.getPrimary();
		minx = prim.x;
		miny = prim.y;
		maxx = prim.x + prim.width;
		maxy = prim.y + prim.height;
		for (Rect r : curFaces) {
			if (r.x < minx)
				minx = r.x;
			if (r.x + r.width > maxx)
				maxx = r.x + r.width;
			if (r.y < miny)
				miny = r.y;
			if (r.y + r.height > maxy)
				maxy = r.y + r.height;
		}
		if (minx - xpadding < 0) {
			minx = 0;
		} else
			minx = minx - xpadding;

		if (miny - ypadding < 0) {
			miny = 0;
		} else
			miny = miny - ypadding;

		if (maxx + xpadding > getResolution().width) {
			maxx = getResolution().width;
		} else
			maxx = maxx + xpadding;

		if (maxy + ypadding > getResolution().height) {
			maxy = getResolution().height;
		} else
			maxy = maxy + ypadding;

		int width, height;
		width = maxx - minx;
		height = maxy - miny;
		float scale;

		scale = ((float) getResolution().width * (float) height) / ((float) getResolution().height * (float) width);

		int newWidth = (int) (width * scale);
		int widthDelta = newWidth - width;
		width = newWidth;

		minx = (int) (((minx - (widthDelta / 2)) > 0) ? minx = minx - (widthDelta / 2) : 0);

		double currentArea = overallRect.area();
		double nextArea = width * height;

		if (nextArea > currentArea) {
			transitionalStepSizeW = (int) ((width - overallRect.width) / transitionalFrames);
			transitionalStepSizeH = (int) ((height - overallRect.height) / transitionalFrames);
		} else {
			transitionalStepSizeW = (int) -((overallRect.width - width) / transitionalFrames);
			transitionalStepSizeH = (int) -((overallRect.height - height) / transitionalFrames);
		}

		transitionalStepSizeX = (int) ((overallRect.x - minx) / transitionalFrames);
		transitionalStepSizeY = (int) ((overallRect.y - miny) / transitionalFrames);

		isTransitioning = true;
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (overallRect != null) {
			if (isTransitioning) {
				int x, y, width, height;
				x = overallRect.x - transitionalStepSizeX;
				y = overallRect.y - transitionalStepSizeY;
				width = overallRect.width + transitionalStepSizeW;
				height = overallRect.height + transitionalStepSizeH;

				if (x + width > getResolution().width) {
					int widthDelta = getResolution().width - width;
					width = width - widthDelta;
					x = x - widthDelta;
				}

				overallRect.x = x;
				overallRect.y = y;
				overallRect.width = width;
				overallRect.height = height;

				nTransitionalFrame++;
				if (nTransitionalFrame == transitionalFrames) {
					isTransitioning = false;
					nTransitionalFrame = 0;
				}
			}

			final BufferedImage crop = new BufferedImage(getResolution().width, getResolution().height,
					BufferedImage.TYPE_4BYTE_ABGR);
			final Graphics g = crop.getGraphics();
			try {
				g.drawImage(frame.getSubimage(overallRect.x, overallRect.y, overallRect.width, overallRect.height), 0,
						0, getResolution().width, getResolution().height, null);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			frame = null;
			return crop;
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
