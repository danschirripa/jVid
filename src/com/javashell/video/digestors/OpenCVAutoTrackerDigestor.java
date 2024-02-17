package com.javashell.video.digestors;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoDigestor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.FaceSet;
import com.javashell.video.camera.PTZControlInterface;
import com.javashell.video.camera.PTZMove;

public class OpenCVAutoTrackerDigestor extends VideoDigestor implements ControlInterface {
	private int digestionOffset = 10;
	private int digestionOffsetIndex = 0;
	private Thread analyzationThread;
	private Mat frame = null;
	private double distanceRange = 300;
	private CascadeClassifier classifier = new CascadeClassifier();
	private HashSet<PTZControlInterface> controllers;
	private Point lastCenterPoint;
	private Rect faceRect;
	private boolean doTrack = false;
	private Camera cam;
	private static final Size minFaceSize = new Size(1080 * 0.15f, 1080 * 0.15f), maxFaceSize = new Size();
	private double desiredFaceToFramePercentage = 20;
	private final double totalArea;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	private Runnable autoTrackRunnable = new Runnable() {
		public void run() {
			autoTrack(frame);
		}
	};

	public OpenCVAutoTrackerDigestor(Dimension resolution, Camera cam) {
		super(resolution);
		this.cam = cam;
		InputStream cascadeInput = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");

		distanceRange = resolution.width / 2.5;
		// Read and load the "haarcascasde" classifier
		try {
			File tmpFile = File.createTempFile("cascade", ".xml");
			FileOutputStream tmpOut = new FileOutputStream(tmpFile);
			tmpOut.write(cascadeInput.readAllBytes());
			tmpOut.flush();
			tmpOut.close();
			classifier.load(tmpFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to load cascade classifier");
			System.exit(-1);
		}
		controllers = new HashSet<>();
		totalArea = resolution.getWidth() * resolution.getHeight();
	}

	private long lastDetectionTime = 0;
	private long detectionTimeout = 6000;

	private void autoTrack(Mat frame) {
		MatOfRect faces = new MatOfRect();
		Mat gray = new Mat();

		Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(gray, gray);

		classifier.detectMultiScale(gray, faces, 1.1, 3, 0 | Objdetect.CASCADE_SCALE_IMAGE, minFaceSize, maxFaceSize);

		Rect[] facesArray = faces.toArray();
		if (facesArray.length == 0)
			return;
		Rect face = facesArray[0];
		Point centerPoint = null;
		// Identify which rectangles centerpoint most likely correlates with the last
		// detected face, prevents points from jumping around erroneously
		for (Rect f : facesArray) {
			Point nCenterPoint = new Point(f.x + (f.width / 2), f.y + (f.height / 2));
			if (isInRange(nCenterPoint)) {
				face = f;
				this.faceRect = f;
				centerPoint = nCenterPoint;
				break;
			}
		}
		FaceSet faceSet = new FaceSet(facesArray, face);
		for (ControlInterface cf : controllers) {
			cf.processControl(faceSet);
		}
		if (!(centerPoint == null)) {
			if (lastCenterPoint == null) {
				lastCenterPoint = centerPoint;
			}
			System.out.println("Face @ " + centerPoint.toString());
			double faceDist = centerPoint.distance(lastCenterPoint);
			System.out.println("Face dist: " + faceDist);

			int[] ptAdjustment = cam.calculatePTZAdjustment(centerPoint);
			System.out.println("PAN:" + ptAdjustment[0] + " - " + ptAdjustment[2] + "TILT: " + ptAdjustment[1] + " - "
					+ ptAdjustment[3]);

			final double faceFramePercentage = (face.area() / totalArea) * 100;
			int zoom = 0;

			double difference = desiredFaceToFramePercentage - faceFramePercentage;

			if (Math.abs(difference) > 15) {
				if (difference > 15) {
					zoom = 1;
				} else if (difference < -12) {
					zoom = -1;
				}
			}

			lastDetectionTime = System.currentTimeMillis();

			System.out.println("FaceFrame percentage is " + faceFramePercentage + " diff: " + difference);

			PTZMove move = new PTZMove(ptAdjustment[2], ptAdjustment[3], zoom, ptAdjustment[0], ptAdjustment[1]);
			move.setCamera(cam);

			for (PTZControlInterface cf : controllers) {
				cf.PTZ(ptAdjustment[2] * 2, ptAdjustment[3] * 2, zoom);
				cf.processControl(move);
			}
			lastCenterPoint = centerPoint;
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
				lastCenterPoint = null;
				lastDetectionTime = System.currentTimeMillis();
			}
		}
		frame.release();

	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		/**
		 * if (lastCenterPoint != null) { Color old = frame.getGraphics().getColor();
		 * Graphics frameg = frame.getGraphics(); frameg.setColor(Color.red);
		 * frameg.fillOval(lastCenterPoint.x, lastCenterPoint.y, 10, 10);
		 * frameg.drawRect(faceRect.x, faceRect.y, faceRect.width, faceRect.height); //
		 * frame.getGraphics().setColor(old); }
		 **/
		if (frame == null || !doTrack)
			return frame;
		if (digestionOffsetIndex == digestionOffset) {
			this.frame = bufferedImageToMat(frame);
			analyzationThread = new Thread(autoTrackRunnable);
			analyzationThread.start();
			digestionOffsetIndex = 0;
			return frame;
		}
		digestionOffsetIndex++;
		return frame;
	}

	@Override
	public boolean open() {
		doTrack = true;
		digestionOffsetIndex = 0;
		return true;
	}

	@Override
	public boolean close() {
		doTrack = false;
		return true;
	}

	private boolean isInRange(Point p) {
		if (lastCenterPoint == null)
			return true;
		double dist = Math.abs(Point.distance(p.getX(), p.getY(), lastCenterPoint.getX(), lastCenterPoint.getY()));
		System.out.println("DISTANCE " + dist);
		if (dist < distanceRange)
			return true;
		return false;
	}

	private Mat bufferedImageToMat(BufferedImage sourceImg) {

		final DataBuffer dataBuffer = sourceImg.getRaster().getDataBuffer();
		byte[] imgPixels = null;
		Mat imgMat = null;

		int width = sourceImg.getWidth();
		int height = sourceImg.getHeight();

		if (dataBuffer instanceof DataBufferByte) {
			imgPixels = ((DataBufferByte) dataBuffer).getData();
		}

		if (dataBuffer instanceof DataBufferInt) {

			int byteSize = width * height;
			imgPixels = new byte[byteSize * 3];

			final int[] imgIntegerPixels = ((DataBufferInt) dataBuffer).getData();

			for (int p = 0; p < byteSize; p++) {
				imgPixels[p * 3 + 0] = (byte) ((imgIntegerPixels[p] & 0x00FF0000) >> 16);
				imgPixels[p * 3 + 1] = (byte) ((imgIntegerPixels[p] & 0x0000FF00) >> 8);
				imgPixels[p * 3 + 2] = (byte) (imgIntegerPixels[p] & 0x000000FF);
			}
		}

		if (imgPixels != null) {
			imgMat = new Mat(height, width, CvType.CV_8UC3);
			imgMat.put(0, 0, imgPixels);
		}
		imgPixels = null;

		return imgMat;
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
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
		controllers.remove(cf);
	}

}
