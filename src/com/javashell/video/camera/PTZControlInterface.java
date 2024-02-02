package com.javashell.video.camera;

import com.javashell.video.ControlInterface;

public interface PTZControlInterface extends ControlInterface {
	public void PTZ(int pan, int tilt, int zoom);

	public void IRIS(int in, int out);

	public void FOCUS(int in, int out);

	public void HOME();

	public void AUTOFOCUS();
}
