package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.javashell.video.VideoIngestor;
import com.javashell.video.egressors.NDI5Egressor;

public class NDI5Ingestor extends VideoIngestor {
	private String ndiName = "jVid";
	private final String sourceName;

	static {
		try {
			String arch = System.getProperty("os.arch");
			String prefix = "amd64";
			System.out.println(arch);
			if (arch.equals("aarch64")) {
				prefix = "aarch64";
			}
			System.out.println("Prefix: " + prefix);

			InputStream libNDIDecoderStream = NDI5Egressor.class.getResourceAsStream("/" + prefix + "/libndi.so");
			File libNDIDecoderFile = File.createTempFile("libndi", ".so");
			FileOutputStream libNDIDecoderOutputStream = new FileOutputStream(libNDIDecoderFile);
			libNDIDecoderOutputStream.write(libNDIDecoderStream.readAllBytes());
			libNDIDecoderOutputStream.flush();
			libNDIDecoderOutputStream.close();
			libNDIDecoderStream.close();
			System.load(libNDIDecoderFile.getAbsolutePath());

			libNDIDecoderStream = NDI5Egressor.class.getResourceAsStream("/" + prefix + "/libNDIDecoder.so");
			libNDIDecoderFile = File.createTempFile("libNDIDecoder", ".so");
			libNDIDecoderOutputStream = new FileOutputStream(libNDIDecoderFile);
			libNDIDecoderOutputStream.write(libNDIDecoderStream.readAllBytes());
			libNDIDecoderOutputStream.flush();
			libNDIDecoderOutputStream.close();
			libNDIDecoderStream.close();
			System.load(libNDIDecoderFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public NDI5Ingestor(Dimension resolution, String sourceName) {
		this(resolution, "jVid", sourceName);
	}

	public NDI5Ingestor(Dimension resolution, String ndiName, String sourceName) {
		super(resolution);
		this.sourceName = sourceName;
		this.ndiName = ndiName;
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		BufferedImage img = new BufferedImage(getResolution().width, getResolution().height,
				BufferedImage.TYPE_4BYTE_ABGR);
		byte[] frameBytes = grabFrame();
		if (frameBytes == null) {
			System.out.println("NULL");
			return null;
		}
		System.arraycopy(frameBytes, 0, ((DataBufferByte) img.getRaster().getDataBuffer()).getData(), 0,
				frameBytes.length);
		return img;
	}

	@Override
	public boolean open() {
		System.out.println("Initializing NDI with " + ndiName + ":" + sourceName);
		initializeNDI(ndiName, sourceName);
		return true;
	}

	@Override
	public boolean close() {
		return false;
	}

	private native byte[] grabFrame();

	private native void initializeNDI(String receiverName, String sourceName);

	private void setResolution(int width, int height) {
		this.getResolution().setSize(width, height);
		System.out.println("Reset frame dimensions " + width + "x" + height);
	}

}
