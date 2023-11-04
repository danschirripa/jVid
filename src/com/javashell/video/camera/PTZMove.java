package com.javashell.video.camera;

public class PTZMove {
	private int tilt, pan, zoom, deltaX, deltaY;
	private Camera cam;

	public PTZMove(int tilt, int pan, int zoom) {
		this.tilt = tilt;
		this.pan = pan;
		this.zoom = zoom;
	}

	public PTZMove(int tilt, int pan, int zoom, int deltaX, int deltaY) {
		this.tilt = tilt;
		this.pan = pan;
		this.zoom = zoom;
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}

	public void setCamera(Camera cam) {
		this.cam = cam;
	}

	public Camera getCamera() {
		return cam;
	}

	public int getTilt() {
		return tilt;
	}

	public int getPan() {
		return pan;
	}

	public int getZoom() {
		return zoom;
	}

	public int getDeltaX() {
		return deltaX;
	}

	public int getDeltaY() {
		return deltaY;
	}

}
