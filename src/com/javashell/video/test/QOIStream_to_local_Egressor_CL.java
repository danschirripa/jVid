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
		int width = 1920, height = 1080;
		if (args.length == 3) {
			width = Integer.parseInt(args[1]);
			height = Integer.parseInt(args[2]);
		}
		Dimension resolution = new Dimension(width, height);
		QOIStreamIngestorC ingest = new QOIStreamIngestorC(resolution, args[0], 4500, false, 1);
		ImageSeriesEgressor output = new ImageSeriesEgressor(resolution, new File("."), 10);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution, true);

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
