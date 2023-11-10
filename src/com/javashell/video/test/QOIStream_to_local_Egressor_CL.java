package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.QOIStreamIngestorC;

public class QOIStream_to_local_Egressor_CL {
	public static void main(String[] args) throws MalformedURLException {
		Dimension resolution = new Dimension(1920, 1080);
		QOIStreamIngestorC ingest = new QOIStreamIngestorC(resolution, args[0], 4500, false);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, ingressNode, null);
		ingressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
