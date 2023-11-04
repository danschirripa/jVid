package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.VISCAControlInterface;
import com.javashell.video.digestors.OpenCVAutoTrackerDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.OpenCVIngestor;

public class OpenCV_autotracking_to_local_Egressor {
	public static void main(String[] args) throws MalformedURLException {
		int device = Integer.parseInt(args[0]);
		Dimension resolution = new Dimension(1920, 1080);

		OpenCVIngestor ingest = new OpenCVIngestor(resolution, device);
		OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(resolution, Camera.getCamera("Sony EVI HD7V"));
		LocalWindowEgressor egress = new LocalWindowEgressor(resolution);

		SerialEgressor serial = new SerialEgressor(null);
		VISCAControlInterface visca = new VISCAControlInterface();

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> autoNode = new VideoFlowNode(auto, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, autoNode, null);

		ingressNode.setEgressDestinationNode(autoNode);
		autoNode.setEgressDestinationNode(egressNode);

		FlowController.registerFlowNode(ingressNode);

		auto.addSubscriber(visca);
		visca.addSubscriber(serial);

		ingest.open();
		auto.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
