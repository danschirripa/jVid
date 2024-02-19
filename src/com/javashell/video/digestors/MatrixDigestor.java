package com.javashell.video.digestors;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.javashell.flow.FlowNode;
import com.javashell.video.VideoProcessor;

public class MatrixDigestor extends FlowNode<VideoProcessor> {
	public HashMap<FlowNode<VideoProcessor>, HashSet<FlowNode<VideoProcessor>>> matrix;

	public MatrixDigestor() {
		super(null, null, null);
		matrix = new HashMap<>();
	}

	@Override
	public void setIngestSourceNode(FlowNode<VideoProcessor> source) {
	}

	@Override
	public void setEgressDestinationNode(FlowNode<VideoProcessor> destination) {
	}

	public void createCrossPoint(FlowNode<VideoProcessor> source, FlowNode<VideoProcessor> destination) {
		if (!matrix.containsKey(source)) {
			matrix.put(source, new HashSet<>());
		}
		matrix.get(source).add(destination);
		source.setEgressDestinationNode(this);
	}

	public void breakCrossPoint(FlowNode<VideoProcessor> source, FlowNode<VideoProcessor> destination) {
		if (matrix.containsKey(source)) {
			matrix.get(source).remove(destination);
		}
	}

	@Override
	public void triggerFrame(UUID nodeId, BufferedImage img) {
		Set<FlowNode<VideoProcessor>> keys = matrix.keySet();
		for (FlowNode<VideoProcessor> source : keys) {
			if (source.getUUID() == nodeId) {
				HashSet<FlowNode<VideoProcessor>> outputs = matrix.get(source);
				for (FlowNode<VideoProcessor> vid : outputs) {
					vid.triggerFrame(nodeId, img);
				}
			}
		}
	}

}
