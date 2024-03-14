package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_to_NDI5Egressor {
	public static void main(String[] args) throws MalformedURLException {
		File streamDevice = new File(args[0]);
		String ndiName = "jVid";

		int width = 1920, height = 1080;
		if (args.length > 1) {
			width = Integer.parseInt(args[1]);
			height = Integer.parseInt(args[2]);
		}

		Dimension resolution = new Dimension(width, height);
		FFMPEGIngestor ingest = new FFMPEGIngestor(resolution, streamDevice);
		NDI5Egressor egress = new NDI5Egressor(resolution, ndiName);
		// LocalWindowEgressor preview = new LocalWindowEgressor(resolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		// FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode,
		// null);
		ingressNode.setEgressDestinationNode(egressNode);
		// egressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();
		// preview.open();

		FlowController.startFlowControl();
	}
}
