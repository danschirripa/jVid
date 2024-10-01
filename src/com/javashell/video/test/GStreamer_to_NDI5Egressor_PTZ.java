package com.javashell.video.test;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.fazecast.jSerialComm.SerialPort;
import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.PTZFullScreenEgressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.GStreamerIngestor;

public class GStreamer_to_NDI5Egressor_PTZ {
	public static void main(String[] args) throws IOException {
		String devPath, width, height, format, framerate, decoder;
		devPath = args[0];
		width = args[1];
		height = args[2];
		format = args[3];
		framerate = args[4];
		decoder = args[5];
		Dimension resolution = new Dimension(Integer.parseInt(width), Integer.parseInt(height));
		GStreamerIngestor ingest = new GStreamerIngestor(resolution, "v4l2src device=\"" + devPath + "\" ! " + format
				+ ", width=" + width + ", height=" + height + ", framerate=" + framerate + "/1 ! " + decoder);
		NDI5Egressor ndi = new NDI5Egressor(resolution, "jVid");
		PTZFullScreenEgressor egress = new PTZFullScreenEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> ndiNode = new VideoFlowNode(ndi, ingressNode, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ndiNode, null);
		ingressNode.setEgressDestinationNode(ndiNode);
		ndiNode.setEgressDestinationNode(egressNode);

		SerialEgressor serialPort = new SerialEgressor(SerialPort.getCommPort("/dev/ttyUSB0"));

		egress.addSubscriber(serialPort);

		Thread viscaOverIPThread = new Thread(new Runnable() {
			public void run() {
				try {
					ServerSocket ss = new ServerSocket(5600);
					while (!ss.isClosed()) {
						Socket s = ss.accept();
						System.out.println("Received VISCA client: " + s.getInetAddress().toString());
						new Thread(new VISCAOverIPClientHandler(s)).start();
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}

			class VISCAOverIPClientHandler implements Runnable {
				private final Socket s;

				public VISCAOverIPClientHandler(Socket s) {
					this.s = s;
				}

				public void run() {
					try {
						InputStream in = s.getInputStream();
						int b;
						while ((b = in.read()) != -1) {
							serialPort.processControl(b);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		viscaOverIPThread.start();
		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		ndi.open();
		egress.open();

		FlowController.startFlowControl();
	}
}
