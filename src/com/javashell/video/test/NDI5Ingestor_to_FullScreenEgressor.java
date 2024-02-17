package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.FullScreenEgressor;
import com.javashell.video.ingestors.NDI5Ingestor;

public class NDI5Ingestor_to_FullScreenEgressor {
	public static void main(String[] args) throws MalformedURLException {
		String ndiName = "";
		for (String arg : args) {
			ndiName += arg + " ";
		}
		ndiName = ndiName.stripTrailing();
		Dimension resolution = new Dimension(1920, 1080);
		NDI5Ingestor ingest = new NDI5Ingestor(resolution, ndiName);
		FullScreenEgressor preview = new FullScreenEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, ingressNode, null);
		ingressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
