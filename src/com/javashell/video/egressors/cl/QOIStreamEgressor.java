package com.javashell.video.egressors.cl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Scanner;

import com.javashell.video.VideoEgress;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;

import me.saharnooby.qoi.QOIImage;
import me.saharnooby.qoi.QOIUtil;
import me.saharnooby.qoi.QOIUtilAWT;

public class QOIStreamEgressor extends VideoEgress {
	private ServerSocket server;
	private static HashSet<Socket> clients;
	private static BufferedImage bufFrame0, bufFrame1;
	private int lastFrameThread = 0;
	private static QOIImage curFrame;
	private static long lastTime;
	private static boolean isRunning;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private Thread serverThread, egressThread, frameConverterThread0, frameConverterThread1;

	public QOIStreamEgressor(Dimension resolution) {
		super(resolution);
		CLContext context = CLContext.create();
		try {
			CLDevice device = context.getMaxFlopsDevice();

			CLCommandQueue queue = device.createCommandQueue();

			Scanner sc = new Scanner(
					ClassLoader.getSystemResourceAsStream("com/javashell/video/egressors/cl/cl_QOIEncoder.cl"));
			String clKernel = "";
			while (sc.hasNextLine()) {
				clKernel += sc.nextLine();
			}
			System.out.println(clKernel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		context.release();
	}

	@Override
	public BufferedImage processFrame(final BufferedImage frame) {
		if (frame == null)
			return frame;

		if (lastFrameThread == 0) {
			lastFrameThread = 1;
			bufFrame1 = frame;
		} else {
			lastFrameThread = 0;
			bufFrame0 = frame;
		}

		return frame;
	}

	@Override
	public boolean open() {
		return true;
	}

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	@Override
	public boolean close() {
		isRunning = false;
		return true;
	}

}
