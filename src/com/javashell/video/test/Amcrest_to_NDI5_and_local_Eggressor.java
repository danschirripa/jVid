package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.extras.AmcrestCameraInterface;
import com.javashell.video.digestors.OpenCVAutoTrackerDigestor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.PTZLocalWindowEgressor;

public class Amcrest_to_NDI5_and_local_Eggressor {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
		Camera IP2M_841 = Camera.getCamera("IP2M-841");

		AmcrestCameraInterface amc = new AmcrestCameraInterface(new Dimension(1920, 1080), "admin", "Enohpoxas98*",
				"10.42.0.143", 4096000, IP2M_841);
		OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(new Dimension(1920, 1080), IP2M_841);

		NDI5Egressor ndi5 = new NDI5Egressor(new Dimension(1920, 1080));
		PTZLocalWindowEgressor egress = new PTZLocalWindowEgressor(new Dimension(1920, 1080), false);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(amc, null, null);
		FlowNode<VideoProcessor> autoNode = new VideoFlowNode(auto, ingressNode, null);
		FlowNode<VideoProcessor> ndiNode = new VideoFlowNode(ndi5, autoNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ndiNode, null);

		ingressNode.setEgressDestinationNode(autoNode);
		autoNode.setEgressDestinationNode(ndiNode);
		ndiNode.setEgressDestinationNode(egressNode);

		auto.addSubscriber(amc);

		FlowController.registerFlowNode(ingressNode);

		amc.open();
		ndi5.open();
		egress.open();
		auto.open();

		egress.addSubscriber(amc);

		FlowController.startFlowControl();

		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
			}

		}));
	}
}
