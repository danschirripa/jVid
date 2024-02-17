package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.javashell.video.VideoEgress;

public class FullScreenEgressor extends VideoEgress {
	private JFrame egressFrame;
	private BufferedImage curFrame;
	private JPanel egressPanel;

	public FullScreenEgressor(Dimension resolution) {
		super(resolution);

		egressFrame = new JFrame("Local Window Egressor") {
			@Override
			public void update(Graphics g) {
				paint(g);
			}
		};
		egressFrame.setUndecorated(true);
		Rectangle monitorSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		egressFrame.setSize(monitorSize.width, monitorSize.height);
		egressFrame.setTitle("Local Window Egressor");

		curFrame = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_ARGB);

		egressPanel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				g.drawImage(curFrame, 0, 0, monitorSize.width, monitorSize.height, egressFrame);
			}
		};
		egressPanel.setSize(monitorSize.width, monitorSize.height);
		egressPanel.setPreferredSize(new Dimension(monitorSize.width, monitorSize.height));
		egressFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

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
