package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.cl.QOIStreamEgressorC;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_to_QOIStream_Egressor_C {
	public static void main(String[] args) throws MalformedURLException {
		// URL streamURL = new URL(args[0]);
		File streamDevice = new File(args[0]);

		int width = 1920, height = 1080;
		if (args.length == 3) {
			width = Integer.parseInt(args[1]);
			height = Integer.parseInt(args[2]);
		}

		Dimension resolution = new Dimension(width, height);
		FFMPEGIngestor ingest = new FFMPEGIngestor(resolution, streamDevice);
		QOIStreamEgressorC egress = new QOIStreamEgressorC(resolution);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode, null);
		ingressNode.setEgressDestinationNode(egressNode);
		egressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();
		preview.open();

		FlowController.startFlowControl();
	}
}
