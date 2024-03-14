package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.fazecast.jSerialComm.SerialPort;
import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.MultiplexedVideoFlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.VISCAControlInterface;
import com.javashell.video.camera.extras.AmcrestCameraInterface;
import com.javashell.video.digestors.AutoFramingDigestor;
import com.javashell.video.digestors.AutoTrackingDigestor;
import com.javashell.video.digestors.FaceSetPaintingDigestor;
import com.javashell.video.digestors.MatrixDigestor;
import com.javashell.video.digestors.MultiviewDigestor;
import com.javashell.video.digestors.OpenCVDeepLearningFaceDetectorDigestor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;
import com.javashell.video.ingestors.OpenCVIngestor;

public class DualCamera_to_NDI5Egressor {

	public static void main(String[] args)
			throws NoSuchAlgorithmException, IOException, URISyntaxException, InterruptedException {
		Dimension resolution = new Dimension(1920, 1080);

		// INPUT DEFINITION
		Camera IP2M_841 = Camera.getCamera("IP2M-841");
		AmcrestCameraInterface amc = new AmcrestCameraInterface(resolution, "admin", "Enohpoxas98*", "10.42.0.143",
				4096000, IP2M_841);
		FFMPEGIngestor camIn = new FFMPEGIngestor(new Dimension(720, 576), new File("/dev/video2"));
		FFMPEGIngestor huddlyIn = new FFMPEGIngestor(resolution, new File("/dev/video1"));

		// DIGEST DEFINITION
		Camera EVI_D70 = Camera.getCamera("Sony EVI D-70");
		OpenCVDeepLearningFaceDetectorDigestor amcAuto = new OpenCVDeepLearningFaceDetectorDigestor(
				new Dimension(1920, 1080));

		OpenCVDeepLearningFaceDetectorDigestor huddlyAuto = new OpenCVDeepLearningFaceDetectorDigestor(resolution);

		OpenCVDeepLearningFaceDetectorDigestor sonyAuto = new OpenCVDeepLearningFaceDetectorDigestor(
				new Dimension(720, 576));

		AutoTrackingDigestor atd = new AutoTrackingDigestor(new Dimension(720, 576), EVI_D70);
		FaceSetPaintingDigestor facePaint = new FaceSetPaintingDigestor(new Dimension(720, 576));
		AutoFramingDigestor autoFrame = new AutoFramingDigestor(resolution);
		AutoFramingDigestor autoFrameHuddly = new AutoFramingDigestor(resolution);

		MultiviewDigestor mv = new MultiviewDigestor(resolution, 3, 2, 2);

		NDI5Egressor ndi5Out = new NDI5Egressor(resolution, "Multiview Out");
		NDI5Egressor ndi5Out2 = new NDI5Egressor(resolution, "Amcrest");
		NDI5Egressor ndi5Out3 = new NDI5Egressor(new Dimension(720, 576), "Sony");
		NDI5Egressor ndi5Out4 = new NDI5Egressor(resolution, "Huddly");

		MatrixDigestor matrix = new MatrixDigestor();

		SerialEgressor serial = new SerialEgressor(SerialPort.getCommPort("/dev/ttyUSB0"));
		VISCAControlInterface visca = new VISCAControlInterface();

		VideoFlowNode amcIngress = new VideoFlowNode(amc, null, null);
		VideoFlowNode sonyIngress = new VideoFlowNode(camIn, null, null);
		VideoFlowNode huddlyIngress = new VideoFlowNode(huddlyIn, null, null);

		VideoFlowNode amcFaceDigest = new VideoFlowNode(amcAuto, amcIngress, null);
		VideoFlowNode amcAutoFrame = new VideoFlowNode(autoFrame, amcFaceDigest, null);

		VideoFlowNode huddlyFaceDigest = new VideoFlowNode(huddlyAuto, huddlyIngress, null);
		VideoFlowNode huddlyAutoFrame = new VideoFlowNode(autoFrameHuddly, huddlyFaceDigest, null);

		VideoFlowNode sonyFaceDigest = new VideoFlowNode(sonyAuto, sonyIngress, null);
		VideoFlowNode sonyFacePaint = new VideoFlowNode(facePaint, sonyFaceDigest, null);

		FlowNode<VideoProcessor> multiviewDigest = new MultiplexedVideoFlowNode(mv, null, null);

		VideoFlowNode ndiEgress = new VideoFlowNode(ndi5Out, multiviewDigest, null);

		VideoFlowNode ndiEgress2 = new VideoFlowNode(ndi5Out2, null, null);
		VideoFlowNode ndiEgress3 = new VideoFlowNode(ndi5Out3, null, null);
		VideoFlowNode ndiEgress4 = new VideoFlowNode(ndi5Out4, null, null);

		FlowController.registerFlowNode(amcIngress);
		FlowController.registerFlowNode(sonyIngress);
		FlowController.registerFlowNode(huddlyIngress);

		amcIngress.setEgressDestinationNode(amcFaceDigest);
		amcFaceDigest.setEgressDestinationNode(amcAutoFrame);

		sonyIngress.setEgressDestinationNode(sonyFacePaint);
		sonyFacePaint.setEgressDestinationNode(sonyFaceDigest);

		huddlyIngress.setEgressDestinationNode(huddlyFaceDigest);
		huddlyFaceDigest.setEgressDestinationNode(huddlyAutoFrame);

		matrix.createCrossPoint(sonyFaceDigest, multiviewDigest);
		matrix.createCrossPoint(amcAutoFrame, multiviewDigest);
		matrix.createCrossPoint(huddlyAutoFrame, multiviewDigest);

		matrix.createCrossPoint(sonyFaceDigest, ndiEgress3);
		matrix.createCrossPoint(amcAutoFrame, ndiEgress2);

		matrix.createCrossPoint(huddlyAutoFrame, ndiEgress4);

		multiviewDigest.setEgressDestinationNode(ndiEgress);

		amcAuto.addSubscriber(autoFrame);

		huddlyAuto.addSubscriber(autoFrameHuddly);

		sonyAuto.addSubscriber(atd);
		atd.addSubscriber(visca);
		visca.addSubscriber(serial);

		amc.open();
		camIn.open();
		huddlyIn.open();

		amcAuto.open();
		sonyAuto.open();
		huddlyAuto.open();

		facePaint.open();

		autoFrame.open();
		autoFrameHuddly.open();

		mv.open();
		ndi5Out.open();
		ndi5Out2.open();
		ndi5Out3.open();
		ndi5Out4.open();

		FlowController.startFlowControl();
	}
}
