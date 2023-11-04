package com.javashell.flow;

import com.javashell.video.VideoProcessor;

public class VideoFlowNode extends FlowNode<VideoProcessor> {

	public VideoFlowNode(VideoProcessor content, FlowNode<VideoProcessor> ingestSource,
			FlowNode<VideoProcessor> egressDestination) {
		super(content, ingestSource, egressDestination);
	}

}
