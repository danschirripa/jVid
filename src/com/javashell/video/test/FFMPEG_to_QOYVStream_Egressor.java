package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.QOYStreamEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_to_QOYVStream_Egressor {
	public static void main(String[] args) throws MalformedURLException {
		FFMPEGIngestor ingest;
		int width = 1920, height = 1080;
		double fps = 30.0;
		String codec = "video4linux2", format = "rawvideo";
		if (args.length > 1) {
			width = Integer.parseInt(args[1]);
			height = Integer.parseInt(args[2]);
			if (args.length >= 4) {
				codec = args[3];
				System.out.println("Using " + codec);
			}
			if (args.length >= 5) {
				format = args[4];
				System.out.println("With " + format);
			}
			if (args.length >= 6) {
				fps = Double.parseDouble(args[5]);
				System.out.println("At " + fps + " fps");
			}
		}
		Dimension resolution = new Dimension(width, height);
		ingest = new FFMPEGIngestor(resolution, args[0]);
		ingest.setFrameRate(fps);
		ingest.setOption("input_format", format);
		ingest.setOption("video_size", width + "x" + height);

		QOYStreamEgressor egress = new QOYStreamEgressor(resolution);
		//LocalWindowEgressor preview = new LocalWindowEgressor(resolution, false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		//FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode, null);
		ingressNode.setEgressDestinationNode(egressNode);
		//egressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();
		//preview.open();

		FlowController.startFlowControl();
	}
}
