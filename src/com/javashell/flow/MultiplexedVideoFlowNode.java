package com.javashell.flow;

import java.util.HashSet;

import com.javashell.video.MultiplexedVideoProcessor;
import com.javashell.video.VideoProcessor;

public class MultiplexedVideoFlowNode extends VideoFlowNode {
	private HashSet<FlowNode<VideoProcessor>> ingestSources;

	public MultiplexedVideoFlowNode(MultiplexedVideoProcessor content, FlowNode<VideoProcessor> ingestSource,
			FlowNode<VideoProcessor> egressDestination) {
		super(content, ingestSource, egressDestination);
	}

	@Override
	public void setIngestSourceNode(FlowNode<VideoProcessor> source) {
		if (source == null)
			return;
		ingestSources.add(source);
		source.setEgressDestinationNode(this);
	}

	public void removeIngestSourceNode(FlowNode<VideoProcessor> source) {
		ingestSources.remove(source);
		source.setEgressDestinationNode(null);
	}

}
