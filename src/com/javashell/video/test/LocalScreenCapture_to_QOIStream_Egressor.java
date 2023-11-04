package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.LocalWindowEgressor;
import com.javashell.video.egressors.QOIStreamEgressor;
import com.javashell.video.ingestors.LocalScreenIngestor;

public class LocalScreenCapture_to_QOIStream_Egressor {
	public static void main(String[] args)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {

		LocalScreenIngestor ingress = new LocalScreenIngestor(new Dimension(1920, 1080));
		QOIStreamEgressor egress = new QOIStreamEgressor(new Dimension(1920, 1080));
		//LocalWindowEgressor preview = new LocalWindowEgressor(new Dimension(1920, 1080));

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingress, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		//FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode, null);

		ingressNode.setEgressDestinationNode(egressNode);
		//egressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingress.open();
		egress.open();
		//preview.open();

		FlowController.startFlowControl();

	}
}
