package com.javashell.video.egressors;

import java.util.HashSet;

import com.fazecast.jSerialComm.SerialPort;
import com.javashell.video.ControlInterface;

public class SerialEgressor implements ControlInterface {
	private HashSet<ControlInterface> interfaces;
	private int baudRate = 9600, dataBits = 8, stopBits = 1, parity = 0;
	private SerialPort tty;

	public SerialEgressor(SerialPort tty, int baudRate, int dataBits, int stopBits, int parity) {
		this.tty = tty;
		interfaces = new HashSet<>();
		open();
	}

	public SerialEgressor(SerialPort tty) {
		this.tty = tty;
		interfaces = new HashSet<>();
		open();
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		interfaces.add(cf);
		return true;
	}

	@Override
	public void processControl(Object obj) {
		System.out.println("PROCESS: " + obj.getClass());
		if (obj instanceof byte[]) {
			byte[] out = (byte[]) obj;
			tty.writeBytes(out, out.length);
		}
		if (obj.getClass().isAssignableFrom(byte.class) || obj.getClass().isAssignableFrom(int.class)) {
			int bytes = tty.writeBytes(new byte[] { (byte) obj }, 1);
			//System.out.println("WROTE " + bytes);
		}
		if (obj instanceof Integer) {
			byte b = ((Integer) obj).byteValue();
			int bytes = tty.writeBytes(new byte[] { (byte) b }, 1);
			//System.out.println("WROTE " + bytes);
		}
		if (obj instanceof String) {
			String out = (String) obj;
			byte[] outBytes = out.getBytes();
			tty.writeBytes(outBytes, outBytes.length);
		}
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
		interfaces.remove(cf);
	}

	public void open() {
		if (tty == null)
			tty = SerialPort.getCommPorts()[0];
		tty.setComPortParameters(baudRate, dataBits, stopBits, parity);
		tty.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		tty.openPort();
	}

}
