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
import com.javashell.video.camera.FaceSet;

public class OpenCVDeepLearningFaceDetectorDigestor extends VideoDigestor implements ControlInterface {
	private int digestionOffset = 10;
	private int digestionOffsetIndex = 0;
	private Thread analyzationThread;
	private Mat frame = null;
	private double distanceRange = 300;
	private HashSet<ControlInterface> controllers;
	private Point lastCenterPoint;
	private boolean doTrack = false;
	private final Net net;
	private static String protoFile, caffeFile;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

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
	}

	private Runnable autoTrackRunnable = new Runnable() {
		public void run() {
			autoTrack(frame);
		}
	};

	public OpenCVDeepLearningFaceDetectorDigestor(Dimension resolution) {
		super(resolution);
		net = readNetFromCaffe(protoFile, caffeFile);
		distanceRange = resolution.width / 2.5;
		controllers = new HashSet<>();
	}

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
		if (facesArray.length == 0) {
			FaceSet faceSet = new FaceSet(facesArray, null);
			for (ControlInterface cf : controllers) {
				cf.processControl(faceSet);
			}
			frame.release();
			return;
		}
		Rect face = facesArray[0];
		Point centerPoint = null;
		// Identify which rectangles centerpoint most likely correlates with the last
		// detected face, prevents points from jumping around erroneously
		for (Rect f : facesArray) {
			Point nCenterPoint = new Point(f.x + (f.width / 2), f.y + (f.height / 2));
			if (isInRange(nCenterPoint)) {
				face = f;
				centerPoint = nCenterPoint;
				break;
			}
		}
		lastCenterPoint = centerPoint;
		FaceSet faceSet = new FaceSet(facesArray, face);
		for (ControlInterface cf : controllers) {
			cf.processControl(faceSet);
		}
		frame.release();

	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
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

	private Mat bufferedImageToMat(BufferedImage sourceImg) {
		Mat imgMat = Java2DFrameUtils.toMat(sourceImg);
		return imgMat;
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
		// System.out.println("DISTANCE " + dist);
		if (dist < distanceRange)
			return true;
		return false;
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		controllers.add(cf);
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
