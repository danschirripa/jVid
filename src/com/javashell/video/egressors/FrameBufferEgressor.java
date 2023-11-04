package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.javashell.video.VideoEgress;

import uk.co.bithatch.linuxio.FrameBuffer;

public class FrameBufferEgressor extends VideoEgress {
	private static FrameBuffer fb;

	public FrameBufferEgressor(Dimension resolution, int fbNum) throws IOException {
		super(resolution);
		fb = FrameBuffer.getFrameBuffer("/dev/fb" + fbNum);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		if (frame == null) {
			return frame;
		}
		try {
			fb.getBuffer().rewind();
			final int[] argb = frame.getRGB(0, 0, frame.getWidth(), frame.getHeight(), null, 0, frame.getWidth());
			final byte[] bgr = new byte[argb.length * 4];

			for (int i = 0; i < argb.length; i++) {
				int red = (argb[i] >> 16) & 0xFF;
				int green = (argb[i] >> 8) & 0xFF;
				int blue = (argb[i] >> 0) & 0xFF;
				bgr[i] = (byte) ((byte) (blue << 16) | (green << 8) | (red << 0));
			}
			fb.getBuffer().put(bgr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public boolean close() {
		return true;
	}

}
