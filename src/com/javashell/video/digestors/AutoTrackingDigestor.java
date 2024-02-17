package com.javashell.video.digestors;

import java.awt.Dimension;
import java.awt.Point;
import java.util.HashSet;

import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.core.Rect;

import com.javashell.video.ControlInterface;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.FaceSet;
import com.javashell.video.camera.PTZControlInterface;
import com.javashell.video.camera.PTZMove;

public class AutoTrackingDigestor implements ControlInterface {
	private HashSet<PTZControlInterface> controllers;
	private Camera cam;
	private final double totalArea;
	private double desiredFaceToFramePercentage = 10;
	private long lastDetectionTime = 0;
	private long detectionTimeout = 6000;
	private FaceSet curFaces;

	public AutoTrackingDigestor(Dimension resolution, Camera cam) {
		this.totalArea = resolution.width * resolution.height;
		this.cam = cam;
		controllers = new HashSet<>();
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		if (cf instanceof PTZControlInterface) {
			controllers.add((PTZControlInterface) cf);
			System.out.println("Added interface");
			return true;
		}
		return false;
	}

	@Override
	public void processControl(Object obj) {
		if (obj instanceof FaceSet) {
			this.curFaces = (FaceSet) obj;
			autoTrack();
		}
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
		controllers.remove(cf);
	}

	private int numFramesUntilZoom = 10, numFramesToZoom = 10;

	private void autoTrack() {
		if (curFaces.size() != 0) {
			Rect face = curFaces.getPrimary();
			Point centerPoint = new Point(face.x + (face.width / 2), face.y + (face.height / 2));
			// Identify which rectangles centerpoint most likely correlates with the last
			// detected face, prevents points from jumping around erroneously

			int[] ptAdjustment = cam.calculatePTZAdjustment(centerPoint);
			System.out.println("PAN:" + ptAdjustment[0] + " - " + ptAdjustment[2] + "TILT: " + ptAdjustment[1] + " - "
					+ ptAdjustment[3]);

			final double faceFramePercentage = (face.area() / totalArea) * 100;
			int zoom = 0;

			double difference = desiredFaceToFramePercentage - faceFramePercentage;

			if (Math.abs(difference) > 5) {
				numFramesUntilZoom--;
				if (numFramesUntilZoom == 0) {
					if (difference > 5) {
						zoom = 1;
					} else if (difference < -5) {
						zoom = -1;
					}
					numFramesUntilZoom = numFramesToZoom;
				}
			} else {
				numFramesUntilZoom = numFramesToZoom;
			}

			lastDetectionTime = System.currentTimeMillis();

			System.out.println("FaceFrame percentage is " + faceFramePercentage + " diff: " + difference);

			PTZMove move = new PTZMove(ptAdjustment[2], ptAdjustment[3], zoom, ptAdjustment[0], ptAdjustment[1]);
			move.setCamera(cam);

			for (PTZControlInterface cf : controllers) {
				cf.PTZ(ptAdjustment[2] * 2, ptAdjustment[3] * 2, zoom);
				cf.processControl(move);
			}
		} else {
			PTZMove move = new PTZMove(0, 0, 0, 0, 0);
			move.setCamera(cam);
			for (PTZControlInterface cf : controllers) {
				cf.PTZ(0, 0, 0);
				cf.processControl(move);
			}
			if (System.currentTimeMillis() - lastDetectionTime >= detectionTimeout) {
				for (PTZControlInterface cf : controllers) {
					cf.HOME();
				}
				lastDetectionTime = System.currentTimeMillis();
			}
		}

	}
}
