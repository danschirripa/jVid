package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.ingestors.GStreamerIngestor;

public class GStreamer_to_local_Egressor {
	public static void main(String[] args) throws IOException {
		String devPath, width, height, format, framerate, decoder;
		devPath = args[0];
		width = args[1];
		height = args[2];
		format = args[3];
		framerate = args[4];
		decoder = args[5];
		Dimension resolution = new Dimension(Integer.parseInt(width), Integer.parseInt(height));
		GStreamerIngestor ingest = new GStreamerIngestor(resolution, "v4l2src device=\"" + devPath + "\" ! " + format
				+ ", width=" + width + ", height=" + height + ", framerate=" + framerate + "/1 ! " + decoder);
		LocalWindowEgressor egress = new LocalWindowEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		ingressNode.setEgressDestinationNode(egressNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
