package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.digestors.ScalingDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEGStream_to_local_Egressor {
	public static void main(String[] args) throws MalformedURLException {
		URL streamURL = new URL(args[0]);
		Dimension resolution = new Dimension(640, 360);
		Dimension outResolution = new Dimension(1920, 1080);
		FFMPEGIngestor ingest = new FFMPEGIngestor(resolution, streamURL);
		ScalingDigestor scaler = new ScalingDigestor(resolution, outResolution);
		LocalWindowEgressor egress = new LocalWindowEgressor(outResolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> scalingNode = new VideoFlowNode(scaler, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, scalingNode, null);
		ingressNode.setEgressDestinationNode(scalingNode);
		scalingNode.setEgressDestinationNode(egressNode);

		FlowController.registerFlowNode(ingressNode); 

		ingest.open();
		scaler.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
