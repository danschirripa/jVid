package com.javashell.video.test;

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.cl.QOIStreamEgressorC;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_to_QOIStream_Egressor_C {
	public static void main(String[] args) throws MalformedURLException {
		URL streamURL = new URL(args[0]);
		Dimension resolution = new Dimension(640, 360);
		FFMPEGIngestor ingest = new FFMPEGIngestor(resolution, streamURL);
		QOIStreamEgressorC egress = new QOIStreamEgressorC(resolution);
		LocalWindowEgressor preview = new LocalWindowEgressor(resolution);

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
