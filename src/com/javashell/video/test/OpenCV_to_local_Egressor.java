package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.OpenCVIngestor;

public class OpenCV_to_local_Egressor {
	public static void main(String[] args) throws MalformedURLException {
		int device = Integer.parseInt(args[0]);
		Dimension resolution = new Dimension(1920, 1080);
		OpenCVIngestor ingest = new OpenCVIngestor(resolution, device);
		LocalWindowEgressor egress = new LocalWindowEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		ingressNode.setEgressDestinationNode(egressNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
