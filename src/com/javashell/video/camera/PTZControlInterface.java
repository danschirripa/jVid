package com.javashell.video.camera;

import com.javashell.video.ControlInterface;

public interface PTZControlInterface extends ControlInterface {
	public void PTZ(int tilt, int pan, int zoom);

	public void IRIS(int in, int out);

	public void FOCUS(int in, int out);

	public void HOME();

	public void AUTOFOCUS();
}
