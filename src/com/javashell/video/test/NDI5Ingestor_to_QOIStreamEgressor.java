package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.cl.QOIStreamEgressorC;
import com.javashell.video.ingestors.NDI5Ingestor;

public class NDI5Ingestor_to_QOIStreamEgressor {
	public static void main(String[] args) throws MalformedURLException {
		String ndiName = "";
		for (String arg : args) {
			ndiName += arg + " ";
		}
		ndiName = ndiName.stripTrailing();
		Dimension resolution = new Dimension(1920, 1080);
		NDI5Ingestor ingest = new NDI5Ingestor(resolution, ndiName);
		QOIStreamEgressorC preview = new QOIStreamEgressorC(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, ingressNode, null);
		ingressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
