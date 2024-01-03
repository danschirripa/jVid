package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.ImageSeriesEgressor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.QOIStreamIngestorC;

public class QOIStream_to_local_Egressor_CL {
	public static void main(String[] args) throws MalformedURLException {
		Dimension resolution = new Dimension(1920, 1080);
		QOIStreamIngestorC ingest = new QOIStreamIngestorC(resolution, args[0], 4500, false, 1);
		ImageSeriesEgressor output = new ImageSeriesEgressor(resolution, new File("/home/dan/tmp"), 10);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> outputNode = new VideoFlowNode(output, ingressNode, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, outputNode, null);
		ingressNode.setEgressDestinationNode(outputNode);
		outputNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		output.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
