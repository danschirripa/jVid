package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.NDI5Ingestor;

public class NDI5Ingestor_to_local_Egressor {
	public static void main(String[] args) throws MalformedURLException {
		String ndiName = "";
		for (String arg : args) {
			ndiName += arg + " ";
		}
		ndiName = ndiName.stripTrailing();
		Dimension resolution = new Dimension(1280, 720);
		NDI5Ingestor ingest = new NDI5Ingestor(resolution, ndiName);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, ingressNode, null);
		ingressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
