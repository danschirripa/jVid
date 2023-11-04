package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.VISCAControlInterface;
import com.javashell.video.digestors.OpenCVAutoTrackerDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_autotracking_to_local_Egressor2 {
	public static void main(String[] args) throws MalformedURLException {
		URL streamURL = new URL(args[0]);
		Dimension resolution = new Dimension(1920, 1080);
		FFMPEGIngestor ingest = new FFMPEGIngestor(resolution, streamURL);
		OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(resolution, Camera.getCamera("Sony EVI D-70"));
		LocalWindowEgressor egress = new LocalWindowEgressor(resolution);

		SerialEgressor serial = new SerialEgressor(null);
		VISCAControlInterface visca = new VISCAControlInterface();

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> autoNode = new VideoFlowNode(auto, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, autoNode, null);

		ingressNode.setEgressDestinationNode(autoNode);
		autoNode.setEgressDestinationNode(egressNode);

		auto.addSubscriber(visca);
		visca.addSubscriber(serial);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();
		auto.open();

		FlowController.startFlowControl();
	}
}
