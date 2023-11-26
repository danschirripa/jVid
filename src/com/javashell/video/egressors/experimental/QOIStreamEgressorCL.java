package com.javashell.video.egressors.experimental;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Scanner;

import com.javashell.video.VideoEgress;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLProgram;

public class QOIStreamEgressorCL extends VideoEgress {
	private ServerSocket server;
	private static HashSet<Socket> clients;
	private static byte[] encodedBuffer0, encodedBuffer1;
	private static long lastTime;
	private static boolean isRunning;
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private Thread serverThread, egressThread;
	private static String clKernel = "";

	public QOIStreamEgressorCL(Dimension resolution) {
		super(resolution);
		Scanner sc = new Scanner(QOIStreamEgressorCL.class.getResourceAsStream("/cl_QOIEncoder.cl"));
		while (sc.hasNextLine()) {
			clKernel += sc.nextLine() + "\n";
		}
		System.out.println(clKernel);
	}

	@Override
	public BufferedImage processFrame(final BufferedImage frame) {
		if (frame == null)
			return frame;

		encodedBuffer0 = encodedBuffer1;
		CLContext context = CLContext.create();
		try {
			CLDevice device = context.getMaxFlopsDevice();

			CLCommandQueue queue = device.createCommandQueue();

			CLProgram program = context.createProgram(clKernel);
			program.build();
			CLKernel kernel = program.createCLKernel("qoi_encode_kernel");

			final byte[] inputImageData = convertBufferedImageToByteArray(frame);

			// Create OpenCL memory buffers for input and output data
			ByteBuffer imageBuffer = ByteBuffer.allocateDirect(inputImageData.length);
			imageBuffer.put(inputImageData);
			imageBuffer.rewind();
			CLBuffer<ByteBuffer> imageCLBuffer = context.createByteBuffer(inputImageData.length,
					CLMemory.Mem.READ_ONLY);
			imageCLBuffer.use(imageBuffer);

			// Create an OpenCL buffer for the qoi_rgba_t index
			int numPixels = frame.getWidth() * frame.getHeight();

			// Create an OpenCL buffer for the output data
			ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputImageData.length);
			CLBuffer<ByteBuffer> outputCLBuffer = context.createByteBuffer(inputImageData.length);
			outputCLBuffer.use(outputBuffer);

			// Create an OpenCL buffer for the final size of the encoded data
			ByteBuffer finalSizeCL = ByteBuffer.allocateDirect(4);
			CLBuffer<ByteBuffer> finalSizeCLBuffer = context.createByteBuffer(4);
			finalSizeCLBuffer.use(finalSizeCL);

			// Set up the OpenCL kernel arguments
			int channels = 4; // Assuming the input image has 4 channels (RGBA)
			kernel.setArg(0, imageCLBuffer).setArg(1, outputCLBuffer).setArg(2, frame.getWidth())
					.setArg(3, frame.getHeight()).setArg(4, finalSizeCLBuffer);

			queue.putWriteBuffer(imageCLBuffer, false).put1DRangeKernel(kernel, 0, numPixels, 0)
					.putReadBuffer(outputCLBuffer, true).putReadBuffer(finalSizeCLBuffer, true);

			outputBuffer.rewind();
			int finalSize = finalSizeCLBuffer.getBuffer().getInt() / 4;
			System.out.println(outputBuffer.remaining() + " : " + finalSize);

			encodedBuffer1 = null;
			encodedBuffer1 = new byte[finalSize];
			outputBuffer.get(encodedBuffer1, 0, finalSize);

			FileOutputStream fOut = new FileOutputStream(new File("/home/dan/tmp.qoi"));
			fOut.write(encodedBuffer1);
			System.exit(0);

			imageCLBuffer.release();
			finalSizeCLBuffer.release();
			outputCLBuffer.release();
			kernel.release();
			program.release();
			context.release();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return frame;
	}

	@Override
	public boolean open() {
		try {
			isRunning = true;
			clients = new HashSet<Socket>();
			server = new ServerSocket(4500);
			serverThread = new Thread(new Runnable() {
				public void run() {
					while (!server.isClosed()) {
						try {
							final Socket sock = server.accept();
							clients.add(sock);
							System.out.println("New client " + sock.getInetAddress().getCanonicalHostName());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			egressThread = new Thread(new EgressRunnable());
			serverThread.start();
			egressThread.start();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static byte[] convertBufferedImageToByteArray(BufferedImage image) {
		if (image == null) {
			return null;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int numComponents = 4; // Assuming 4 components (RGBA) per pixel

		// Create a byte array to hold the image data
		byte[] imageData = new byte[width * height * numComponents];

		// Get the image's raster data
		int[] pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);

		int index = 0;
		for (int pixel : pixels) {
			// Extract the individual color components (RGBA) from the pixel
			int alpha = (pixel >> 24) & 0xFF;
			int red = (pixel >> 16) & 0xFF;
			int green = (pixel >> 8) & 0xFF;
			int blue = pixel & 0xFF;

			// Store the components in the byte array (assuming RGBA order)
			imageData[index++] = (byte) red;
			imageData[index++] = (byte) green;
			imageData[index++] = (byte) blue;
			imageData[index++] = (byte) alpha;
		}

		return imageData;
	}

	@Override
	public boolean close() {
		isRunning = false;
		return true;
	}

	private byte[] intToBytes(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	private class EgressRunnable implements Runnable {
		public void run() {
			lastTime = System.nanoTime();
			while (isRunning) {
				if (System.nanoTime() - lastTime >= frameRateInterval) {
					try {
						if (encodedBuffer0 == null) {
							// System.out.println("null");
							continue;
						}
						final byte[] qoiBytes = encodedBuffer0;
						final byte[] size = intToBytes(qoiBytes.length);
						for (Socket client : clients) {
							try {
								client.getOutputStream().write(size);
								client.getOutputStream().write(qoiBytes);
							} catch (Exception e) {
								clients.remove(client);
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
						;
					}
				}
			}
		}
	}

}
