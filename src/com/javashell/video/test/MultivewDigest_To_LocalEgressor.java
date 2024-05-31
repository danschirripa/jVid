package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
import com.javashell.video.digestors.FaceSetPaintingDigestor;
import com.javashell.video.digestors.MultiviewDigestor;
import com.javashell.video.digestors.OpenCVAutoTrackerDigestor;
import com.javashell.video.digestors.OpenCVFaceDetectorDigestor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.PTZLocalWindowEgressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;
import com.javashell.video.ingestors.NDI5Ingestor;
import com.javashell.video.ingestors.OpenCVIngestor;

public class MultivewDigest_To_LocalEgressor {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {

		String ndiName = "";
		for (String arg : args) {
			ndiName += arg + " ";
		}
		ndiName = ndiName.stripTrailing();

		Dimension resolution = new Dimension(1920, 1080);

		Camera IP2M_841 = Camera.getCamera("IP2M-841");
		AmcrestCameraInterface amc = new AmcrestCameraInterface(resolution, "admin", "Enohpoxas98*", "10.42.0.143",
				4096000, IP2M_841);

		Camera EVI_D70 = Camera.getCamera("Sony EVI D-70");
		OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(new Dimension(720, 576), EVI_D70);

		// FFMPEGIngestor camIn = new FFMPEGIngestor(new Dimension(800, 600), new
		// File("/dev/video0"));
		OpenCVIngestor camIn = new OpenCVIngestor(new Dimension(720, 576), 0);
		NDI5Ingestor ndiIn = new NDI5Ingestor(resolution, "Ingest1", ndiName);
		NDI5Ingestor opi3In = new NDI5Ingestor(resolution, "Ingest2", ndiName);

		OpenCVFaceDetectorDigestor faces = new OpenCVFaceDetectorDigestor(resolution);
		FaceSetPaintingDigestor facePaint = new FaceSetPaintingDigestor(resolution);
		AutoFramingDigestor autoFrame = new AutoFramingDigestor(resolution);

		MultiviewDigestor mv = new MultiviewDigestor(resolution, 4, 2, 2);

		PTZLocalWindowEgressor egress = new PTZLocalWindowEgressor(resolution, false);
		NDI5Egressor ndi5Out = new NDI5Egressor(resolution, "Multiview Out");

		SerialEgressor serial = new SerialEgressor(SerialPort.getCommPort("/dev/ttyUSB0"));
		VISCAControlInterface visca = new VISCAControlInterface();

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(amc, null, null);
		FlowNode<VideoProcessor> ingressNode2 = new VideoFlowNode(ndiIn, null, null);
		FlowNode<VideoProcessor> ingressNode3 = new VideoFlowNode(camIn, null, null);
		FlowNode<VideoProcessor> ingressNode4 = new VideoFlowNode(opi3In, null, null);

		FlowNode<VideoProcessor> autoTrackNode = new VideoFlowNode(auto, ingressNode3, null);

		FlowNode<VideoProcessor> faceNode = new VideoFlowNode(faces, ingressNode, null);
		FlowNode<VideoProcessor> facePaintNode = new VideoFlowNode(facePaint, faceNode, null);
		FlowNode<VideoProcessor> cropNode = new VideoFlowNode(autoFrame, facePaintNode, null);

		FlowNode<VideoProcessor> digestNode = new MultiplexedVideoFlowNode(mv, null, null);

		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, digestNode, null);
		FlowNode<VideoProcessor> egressNode2 = new VideoFlowNode(ndi5Out, egressNode, null);

		ingressNode.setEgressDestinationNode(faceNode);
		ingressNode2.setEgressDestinationNode(digestNode);
		ingressNode3.setEgressDestinationNode(autoTrackNode);
		ingressNode4.setEgressDestinationNode(digestNode);

		autoTrackNode.setEgressDestinationNode(digestNode);

		faceNode.setEgressDestinationNode(facePaintNode);
		facePaintNode.setEgressDestinationNode(cropNode);
		cropNode.setEgressDestinationNode(digestNode);

		digestNode.setEgressDestinationNode(egressNode);

		egressNode.setEgressDestinationNode(egressNode2);

		FlowController.registerFlowNode(ingressNode);
		FlowController.registerFlowNode(ingressNode2);
		FlowController.registerFlowNode(ingressNode3);
		FlowController.registerFlowNode(ingressNode4);

		amc.open();
		opi3In.open();
		ndiIn.open();
		camIn.open();

		auto.open();
		faces.open();
		facePaint.open();

		mv.open();
		egress.open();
		ndi5Out.open();

		auto.addSubscriber(visca);
		visca.addSubscriber(serial);

		egress.addSubscriber(amc);
		faces.addSubscriber(facePaint);
		faces.addSubscriber(autoFrame);

		FlowController.startFlowControl();
	}
}
