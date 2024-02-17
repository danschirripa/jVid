package com.javashell.video.digestors;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.opencv.core.Core;
import org.opencv.core.Rect;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoDigestor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.FaceSet;
import com.javashell.video.camera.PTZControlInterface;
import com.javashell.video.camera.PTZMove;

public class OpenCVDeepLearningAutoTrackerDigestor extends VideoDigestor implements ControlInterface {
	private static Net net = null;
	private Point lastCenterPoint;
	private Rect faceRect;
	private boolean doTrack = false;
	private HashSet<PTZControlInterface> controllers;
	private Camera cam;
	private final double totalArea;
	private double desiredFaceToFramePercentage = 10;
	private long lastDetectionTime = 0;
	private long detectionTimeout = 6000;
	private double distanceRange = 300;
	private Mat frame = null;
	private int digestionOffset = 10;
	private int digestionOffsetIndex = 0;
	private Thread analyzationThread;

	private Runnable autoTrackRunnable = new Runnable() {
		public void run() {
			autoTrack(frame);
		}
	};

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String protoFile = "", caffeFile = "";

		try {
			InputStream in = OpenCVDeepLearningAutoTrackerDigestor.class.getResourceAsStream("/caffe/deploy.prototxt");
			File protoTxtFile = File.createTempFile("deploy", ".prototxt");
			FileOutputStream protoTxtOutStream = new FileOutputStream(protoTxtFile);
			protoTxtOutStream.write(in.readAllBytes());
			protoTxtOutStream.flush();
			protoTxtOutStream.close();
			in.close();

			in = OpenCVDeepLearningAutoTrackerDigestor.class
					.getResourceAsStream("/caffe/res10_300x300_ssd_iter_140000.caffemodel");
			File caffeModelFile = File.createTempFile("caffe", ".caffemodel");
			FileOutputStream caffeModelOutStream = new FileOutputStream(caffeModelFile);
			caffeModelOutStream.write(in.readAllBytes());
			caffeModelOutStream.flush();
			caffeModelOutStream.close();
			in.close();

			protoFile = protoTxtFile.getAbsolutePath();
			caffeFile = caffeModelFile.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		}

		net = readNetFromCaffe(protoFile, caffeFile);
	}

	public OpenCVDeepLearningAutoTrackerDigestor(Dimension resolution, Camera cam) {
		super(resolution);
		this.totalArea = resolution.width * resolution.height;
		this.cam = cam;
		controllers = new HashSet<>();
		distanceRange = resolution.width / 2.5;
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

	private int numFramesUntilZoom = 10, numFramesToZoom = 10;

	private void autoTrack(Mat frame) {
		resize(frame, frame, new Size(300, 300));
		Mat blob = blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0), false, false,
				CV_32F);

		net.setInput(blob);
		Mat output = net.forward();

		Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr());
		FloatIndexer srcIndexer = ne.createIndexer();
		HashSet<Rect> faces = new HashSet<Rect>();
		for (int i = 0; i < output.size(3); i++) {
			float confidence = srcIndexer.get(i, 2);
			float f1 = srcIndexer.get(i, 3);
			float f2 = srcIndexer.get(i, 4);
			float f3 = srcIndexer.get(i, 5);
			float f4 = srcIndexer.get(i, 6);
			if (confidence > .6) {
				float tx = f1 * getResolution().width;
				float ty = f2 * getResolution().height;
				float bx = f3 * getResolution().width;
				float by = f4 * getResolution().height;
				faces.add(new Rect((int) tx, (int) ty, (int) (bx - tx), (int) (by - ty)));
			}
		}
		Rect[] facesArray = new Rect[faces.size()];
		facesArray = faces.toArray(facesArray);
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
		Mat imgMat = Java2DFrameUtils.toMat(sourceImg);
		return imgMat;
	}

}
