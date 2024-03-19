package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;

import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;

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
		ingest = new FFMPEGIngestor(resolution, streamDevice);
		ingest.setFrameRate(fps);
		ingest.setOption("input_format", format);
		ingest.setOption("video_size", width + "x" + height);

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
