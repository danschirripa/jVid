package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bytedeco.javacv.CanvasFrame;

import com.javashell.video.VideoEgress;

import me.saharnooby.qoi.QOIUtilAWT;

public class LocalAcceleratedWindowEgressor extends VideoEgress {
	private static CanvasFrame egressFrame;
	private static BufferedImage curFrame;
	private static boolean isOpen;
	private static long frameRateInterval = (long) 16.3 * 1000000 / 2;

	public LocalAcceleratedWindowEgressor(Dimension resolution) {
		super(resolution);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = frame;
		return null;
	}

	@Override
	public boolean open() {
		Dimension resolution = this.getResolution();
		Thread renderThread = new Thread(new Runnable() {
			public void run() {
				egressFrame = new CanvasFrame("Local Accelerated Window");
				egressFrame.setSize(resolution);
				egressFrame.setCanvasSize(resolution.width, resolution.height);

				egressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				egressFrame.setVisible(true);

				isOpen = true;
				while (isOpen) {
					long lastTime = System.nanoTime();
					while (true) {
						if (System.nanoTime() - lastTime >= frameRateInterval) {
							lastTime = System.nanoTime();
							egressFrame.showImage(curFrame);
						}
					}
				}
			}
		});
		renderThread.start();
		return true;
	}

	@Override
	public boolean close() {
		isOpen = false;
		egressFrame.setVisible(false);
		return true;
	}
}
