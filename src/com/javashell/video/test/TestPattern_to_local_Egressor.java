package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.digestors.ScalingDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.TestPatternIngestor;
import com.javashell.video.ingestors.TestPatternIngestor.TestPattern;

public class TestPattern_to_local_Egressor {
	public static void main(String[] args) throws IOException {
		Dimension resolution = new Dimension(1920, 1080);
		TestPatternIngestor ingest = new TestPatternIngestor(resolution, TestPattern.HD_TEST);
		ScalingDigestor scaler = new ScalingDigestor(resolution, new Dimension(1920, 1080));
		LocalWindowEgressor egress = new LocalWindowEgressor(resolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> scalerNode = new VideoFlowNode(scaler, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, scalerNode, null);
		ingressNode.setEgressDestinationNode(scalerNode);
		scalerNode.setEgressDestinationNode(egressNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		scaler.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
