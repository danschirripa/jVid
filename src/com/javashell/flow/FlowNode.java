package com.javashell.flow;

import java.awt.image.BufferedImage;
import java.util.UUID;

import com.javashell.video.MultiplexedVideoProcessor;
import com.javashell.video.VideoIngestor;
import com.javashell.video.VideoProcessor;

public abstract class FlowNode<T> {
	private final T content;
	private FlowNode<T> ingestSource, egressDestination;
	private boolean isIngest = false, isVideo = false;
	private final UUID nodeId;

	public FlowNode(T content, FlowNode<T> ingestSource, FlowNode<T> egressDestination) {
		this.content = content;
		this.ingestSource = ingestSource;
		this.egressDestination = egressDestination;
		this.nodeId = UUID.randomUUID();
		if (content instanceof VideoIngestor) {
			isIngest = true;
		}
		if (content instanceof VideoProcessor) {
			isVideo = true;
		}
	}

	public T retrieveNodeContents() {
		return content;
	}

	public FlowNode<T> retrieveIngestSourceNode() {
		return ingestSource;
	}

	public FlowNode<T> retrieveEgressDestinationNode() {
		return egressDestination;
	}

	public void setIngestSourceNode(FlowNode<T> source) {
		if (ingestSource == null) {
			ingestSource = source;
			return;
		}
		ingestSource.setEgressDestinationNode(null);
		ingestSource = source;
		source.setEgressDestinationNode(this);
	}

	public void setEgressDestinationNode(FlowNode<T> destination) {
		if (egressDestination == null) {
			egressDestination = destination;
			return;
		}
		egressDestination.setIngestSourceNode(null);
		egressDestination = destination;
		destination.setIngestSourceNode(this);
	}

	public FlowNode<T> retrieveOrigin() {
		if (ingestSource != null) {
			return ingestSource.retrieveOrigin();
		}
		return this;
	}

	public FlowNode<T> retrieveEgress() {
		if (egressDestination != null) {
			return egressDestination.retrieveEgress();
		}
		return this;
	}

	public int getIndex() {
		if (ingestSource != null) {
			return ingestSource.getIndex() + 1;
		}
		return 0;
	}

	public boolean contentIsIngestor() {
		return isIngest;
	}

	public void triggerFrame(UUID nodeId, BufferedImage img) {
		if (!isVideo)
			return;
		if (isIngest) {
			VideoIngestor vi = (VideoIngestor) content;
			BufferedImage frame = vi.processFrame(null);
			egressDestination.triggerFrame(this.nodeId, frame);
		} else {
			VideoProcessor vp = (VideoProcessor) content;
			if (vp instanceof MultiplexedVideoProcessor) {
				MultiplexedVideoProcessor mvp = (MultiplexedVideoProcessor) vp;
				mvp.ingestFrame(nodeId, img);
			}
			BufferedImage frame = vp.processFrame(img);
			if (egressDestination != null)
				egressDestination.triggerFrame(this.nodeId, frame);
		}
	}
}
