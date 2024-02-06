package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.MultiplexedVideoFlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.extras.AmcrestCameraInterface;
import com.javashell.video.digestors.MultiviewDigestor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.PTZLocalWindowEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;
import com.javashell.video.ingestors.NDI5Ingestor;

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
		AmcrestCameraInterface amc = new AmcrestCameraInterface(resolution, "admin", "Enohpoxas98*", "10.42.0.144",
				4096000, IP2M_841);

		FFMPEGIngestor camIn = new FFMPEGIngestor(resolution, new File("/dev/video0"));

		NDI5Ingestor ndiIn = new NDI5Ingestor(resolution, ndiName);

		MultiviewDigestor mv = new MultiviewDigestor(resolution, 3, 2, 2);

		PTZLocalWindowEgressor egress = new PTZLocalWindowEgressor(resolution, false);
		NDI5Egressor ndi5Out = new NDI5Egressor(resolution, "Multiview Out");

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(amc, null, null);
		FlowNode<VideoProcessor> ingressNode2 = new VideoFlowNode(ndiIn, null, null);
		FlowNode<VideoProcessor> ingressNode3 = new VideoFlowNode(camIn, null, null);

		FlowNode<VideoProcessor> digestNode = new MultiplexedVideoFlowNode(mv, null, null);

		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, digestNode, null);
		FlowNode<VideoProcessor> egressNode2 = new VideoFlowNode(ndi5Out, egressNode, null);

		ingressNode.setEgressDestinationNode(digestNode);
		ingressNode2.setEgressDestinationNode(digestNode);
		ingressNode3.setEgressDestinationNode(digestNode);

		digestNode.setEgressDestinationNode(egressNode);

		egressNode.setEgressDestinationNode(egressNode2);

		FlowController.registerFlowNode(ingressNode);
		FlowController.registerFlowNode(ingressNode2);
		FlowController.registerFlowNode(ingressNode3);

		amc.open();
		ndiIn.open();
		camIn.open();
		mv.open();
		egress.open();
		ndi5Out.open();

		egress.addSubscriber(amc);

		FlowController.startFlowControl();
	}
}
