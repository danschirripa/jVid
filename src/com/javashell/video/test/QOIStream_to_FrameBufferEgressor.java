package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.FrameBufferEgressor;
import com.javashell.video.ingestors.QOIStreamIngestor;

public class QOIStream_to_FrameBufferEgressor {
	public static void main(String[] args) throws IOException {
		Dimension resolution = new Dimension(1920, 1080);
		QOIStreamIngestor ingest = new QOIStreamIngestor(resolution, args[0], 4500, false);
		FrameBufferEgressor preview = new FrameBufferEgressor(resolution, 0);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, ingressNode, null);
		ingressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
