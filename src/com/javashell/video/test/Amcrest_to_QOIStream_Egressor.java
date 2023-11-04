package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.extras.AmcrestCameraInterface;
import com.javashell.video.digestors.OpenCVAutoTrackerDigestor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.QOIStreamEgressor;

public class Amcrest_to_QOIStream_Egressor {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
		Camera IP2M_841 = Camera.getCamera("IP2M-841");

		AmcrestCameraInterface amc = new AmcrestCameraInterface(new Dimension(1920, 1080), "admin", "Enohpoxas98*",
				args[0], 4096000, IP2M_841);
		OpenCVAutoTrackerDigestor auto = new OpenCVAutoTrackerDigestor(new Dimension(1920, 1080), IP2M_841);

		QOIStreamEgressor egress = new QOIStreamEgressor(new Dimension(1920, 1080));
		LocalWindowEgressor preview = new LocalWindowEgressor(new Dimension(1920, 1080));

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(amc, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode, null);

		ingressNode.setEgressDestinationNode(egressNode);
		egressNode.setEgressDestinationNode(previewNode);

		auto.addSubscriber(amc);

		FlowController.registerFlowNode(ingressNode);

		amc.open();
		egress.open();
		preview.open();

		amc.HOME();

		FlowController.startFlowControl();

	}
}
