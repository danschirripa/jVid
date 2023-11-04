package com.javashell.flow;

import com.javashell.video.ControlInterface;

public class ControlFlowNode extends FlowNode<ControlInterface> {

	public ControlFlowNode(ControlInterface content, FlowNode<ControlInterface> ingestSource,
			FlowNode<ControlInterface> egressDestination) {
		super(content, ingestSource, egressDestination);
	}

}
