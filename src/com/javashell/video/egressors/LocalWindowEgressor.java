package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.javashell.video.VideoEgress;

public class LocalWindowEgressor extends VideoEgress {
	private JFrame egressFrame;
	private BufferedImage curFrame;
	private JPanel egressPanel;

	public LocalWindowEgressor(Dimension resolution) {
		this(resolution, true);
	}

	public LocalWindowEgressor(Dimension resolution, boolean doScale) {
		super(resolution);

		egressFrame = new JFrame("Local Window Egressor") {
			@Override
			public void update(Graphics g) {
				paint(g);
			}
		};
		egressFrame.setSize(resolution);
		egressFrame.setTitle("Local Window Egressor");

		curFrame = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_ARGB);

		egressPanel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				int width = resolution.width, height = resolution.height;
				if (doScale) {
					int ratio = egressFrame.getWidth() / width;
					width = egressFrame.getWidth();
					height = height * ratio;
				}
				g.drawImage(curFrame, 0, 0, width, height, egressFrame);
			}
		};
		egressPanel.setSize(resolution);
		egressPanel.setPreferredSize(resolution);

		egressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		egressFrame.add(egressPanel);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame.getGraphics().drawImage(frame, 0, 0, egressFrame);
		if (egressFrame.isVisible()) {
			try {
				egressFrame.repaint();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return frame;
	}

	@Override
	public boolean open() {
		egressFrame.setVisible(true);
		return true;
	}

	@Override
	public boolean close() {
		egressFrame.setVisible(false);
		return true;
	}

}
