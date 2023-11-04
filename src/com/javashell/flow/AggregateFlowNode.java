package com.javashell.flow;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoProcessor;

public class AggregateFlowNode<T extends Object & VideoProcessor & ControlInterface> extends FlowNode<T> {

	public AggregateFlowNode(T content, FlowNode<T> ingestSource, FlowNode<T> egressDestination) {
		super(content, ingestSource, egressDestination);
	}

}
