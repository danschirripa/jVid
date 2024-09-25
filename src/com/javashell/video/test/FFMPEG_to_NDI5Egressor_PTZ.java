package com.javashell.video.test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;

import com.fazecast.jSerialComm.SerialPort;
import com.javashell.flow.FlowController;
import com.javashell.flow.FlowNode;
import com.javashell.flow.VideoFlowNode;
import com.javashell.video.VideoProcessor;
import com.javashell.video.camera.VISCAControlInterface;
import com.javashell.video.egressors.NDI5Egressor;
import com.javashell.video.egressors.PTZFullScreenEgressor;
import com.javashell.video.egressors.SerialEgressor;
import com.javashell.video.ingestors.FFMPEGIngestor;

public class FFMPEG_to_NDI5Egressor_PTZ {
	public static void main(String[] args) throws MalformedURLException {
		File streamDevice = new File(args[0]);
		String ndiName = "jVid";

		FFMPEGIngestor ingest;
		int width = 1920, height = 1080;
		double fps = 30.0;
		String codec = "video4linux2", format = "rawvideo";
		if (args.length > 1) {
			width = Integer.parseInt(args[1]);
			height = Integer.parseInt(args[2]);
			if (args.length >= 4) {
				codec = args[3];
				System.out.println("Using " + codec);
			}
			if (args.length >= 5) {
				format = args[4];
				System.out.println("With " + format);
			}
			if (args.length >= 6) {
				fps = Double.parseDouble(args[5]);
				System.out.println("At " + fps + " fps");
			}
		}
		Dimension resolution = new Dimension(width, height);
		ingest = new FFMPEGIngestor(resolution, streamDevice);
		ingest.setFrameRate(fps);
		ingest.setOption("input_format", format);
		ingest.setOption("video_size", width + "x" + height);
		// ingest.setVideoOption("threads", "1");

		NDI5Egressor egress = new NDI5Egressor(resolution, ndiName);
		PTZFullScreenEgressor preview = new PTZFullScreenEgressor(resolution);

		FlowNode<VideoProcessor> ingressNode = new VideoFlowNode(ingest, null, null);
		FlowNode<VideoProcessor> egressNode = new VideoFlowNode(egress, ingressNode, null);
		FlowNode<VideoProcessor> previewNode = new VideoFlowNode(preview, egressNode, null);
		ingressNode.setEgressDestinationNode(egressNode);
		egressNode.setEgressDestinationNode(previewNode);

		FlowController.registerFlowNode(ingressNode);

		ingest.open();
		egress.open();
		preview.open();

		SerialEgressor serialPort = new SerialEgressor(SerialPort.getCommPort("/dev/ttyUSB0"));

		preview.addSubscriber(serialPort);

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
		FlowController.startFlowControl();
	}
}
