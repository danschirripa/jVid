package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.extras.AmcrestCameraInterface;
import com.javashell.video.digestors.AutoFramingDigestor;
import com.javashell.video.digestors.FaceSetPaintingDigestor;
import com.javashell.video.digestors.OpenCVDeepLearningFaceDetectorDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.NDI5Egressor;

public class Amcrest_to_local_Eggressor {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
		Camera IP2M_841 = Camera.getCamera("IP2M-841");

		Dimension resolution = new Dimension(1920, 1080);

		AmcrestCameraInterface amc = new AmcrestCameraInterface(resolution, "admin", "Enohpoxas98*", "10.42.0.144",
				4096000, IP2M_841);
		// OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(new
		// Dimension(1920, 1080), IP2M_841);

		OpenCVDeepLearningFaceDetectorDigestor faces = new OpenCVDeepLearningFaceDetectorDigestor(resolution);
		FaceSetPaintingDigestor facePaint = new FaceSetPaintingDigestor(resolution);
		AutoFramingDigestor autoFrame = new AutoFramingDigestor(resolution);

		NDI5Egressor ndi5 = new NDI5Egressor(new Dimension(1920, 1080));
		LocalWindowEgressor egress = new LocalWindowEgressor(new Dimension(1920, 1080), false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(amc, null, null);
		// FlowNode<VideoProcessor> autoNode = new VideoFlowNode(auto, ingressNode,
		// null);

		FlowNode<VideoProcessor> faceNode = new VideoFlowNode(faces, ingressNode, null);
		FlowNode<VideoProcessor> facePaintNode = new VideoFlowNode(facePaint, faceNode, null);
		FlowNode<VideoProcessor> cropNode = new VideoFlowNode(autoFrame, facePaintNode, null);

		FlowNode<VideoProcessor> ndiNode = new VideoFlowNode(ndi5, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ndiNode, null);

		ingressNode.setEgressDestinationNode(faceNode);

		// autoNode.setEgressDestinationNode(faceNode);

		faceNode.setEgressDestinationNode(facePaintNode);
		facePaintNode.setEgressDestinationNode(cropNode);
		cropNode.setEgressDestinationNode(ndiNode);
		ndiNode.setEgressDestinationNode(egressNode);
		// autoNode.setEgressDestinationNode(egressNode);

		// auto.addSubscriber(amc);

		FlowController.registerFlowNode(ingressNode);

		amc.open();
		faces.open();
		facePaint.open();
		autoFrame.open();
		ndi5.open();
		egress.open();
		// auto.open();

		faces.addSubscriber(facePaint);
		faces.addSubscriber(autoFrame);

		FlowController.startFlowControl();

	}
}
