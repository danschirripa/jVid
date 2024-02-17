package com.javashell.video.camera;

import java.util.HashSet;

import org.opencv.core.Rect;

public class FaceSet extends HashSet<Rect> {
	private Rect primary;

	private static final long serialVersionUID = -7839280829720181630L;

	public FaceSet(Rect[] faces, Rect primary) {
		appendArray(faces);
		this.primary = primary;
	}

	public void appendArray(Rect[] faces) {
		for (Rect r : faces) {
			add(r);
		}
	}

	public Rect getPrimary() {
		return primary;
	}

}
