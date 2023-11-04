package com.javashell.video.camera;

import java.util.HashSet;

import com.javashell.control.protocols.VISCA;
import com.javashell.video.ControlInterface;

public class VISCAControlInterface implements PTZControlInterface {
	private HashSet<ControlInterface> interfaces;

	public VISCAControlInterface() {
		interfaces = new HashSet<>();
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		interfaces.add(cf);
		cf.processControl(VISCA.ADDRESS_SET);
		cf.processControl(VISCA.HOME);
		cf.processControl(VISCA.directZoomCommand((byte) 0));
		System.out.println("Added subscriber");
		return true;
	}

	@Override
	public void processControl(Object obj) {
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
		interfaces.remove(cf);
	}

	@Override
	public void PTZ(int pan, int tilt, int zoom) {
		byte[] ptCommand = VISCA.relativePtCommand(pan, tilt, (byte) 0x18);
		byte[] zCommand = VISCA.zoomCommand(((zoom > 0) ? VISCA.PTZ_IN : VISCA.PTZ_OUT),
				(zoom == 0 ? (byte) 0x0 : (byte) zoom));
		for (ControlInterface cf : interfaces) {
			cf.processControl(VISCA.Z_STOP);
			cf.processControl(ptCommand);
			cf.processControl(zCommand);
		}
	}

	@Override
	public void IRIS(int in, int out) {
	}

	@Override
	public void FOCUS(int in, int out) {
	}

	public void HOME() {
		for (ControlInterface cf : interfaces) {
			cf.processControl(VISCA.HOME);
			cf.processControl(VISCA.directZoomCommand((byte) 0));
		}
	}

	@Override
	public void AUTOFOCUS() {
		for (ControlInterface cf : interfaces) {
			cf.processControl(VISCA.AF_ONE_PRESS);
		}
	}

}
