package com.javashell.video.egressors;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoEgress;
import com.javashell.video.camera.PTZControlInterface;

public class PTZLocalWindowEgressor extends VideoEgress implements ControlInterface {
	private JFrame egressFrame, controlFrame;
	private BufferedImage curFrame;
	private JPanel egressPanel;
	private PTZControlInterface selectedInterface = null;

	public PTZLocalWindowEgressor(Dimension resolution) {
		this(resolution, true);
	}

	public PTZLocalWindowEgressor(Dimension resolution, boolean doScale) {
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
		createControlFrame();
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
		return null;
	}

	@Override
	public boolean open() {
		egressFrame.setVisible(true);
		controlFrame.setVisible(true);
		return true;
	}

	@Override
	public boolean close() {
		egressFrame.setVisible(false);
		controlFrame.setVisible(false);
		return true;
	}

	private void createControlFrame() {
		controlFrame = new JFrame("PTZ Control");
		JButton up = new JButton("Up");
		up.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(0, 4, 0);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton left = new JButton("Left");
		left.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(-4, 0, 0);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton right = new JButton("Right");
		right.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(4, 0, 0);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton down = new JButton("Down");
		down.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(0, -4, 0);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton in = new JButton("In");
		in.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 2);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton out = new JButton("Out");
		out.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {

			}

			@Override
			public void mouseExited(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, -2);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.PTZ(0, 0, 0);
			}

		});
		JButton home = new JButton("Home");
		home.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				selectedInterface.HOME();
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		controlFrame.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;

		c.gridx = 0;
		c.gridy = 1;

		controlFrame.add(left, c);

		c.gridx = 1;
		c.gridy = 0;

		controlFrame.add(up, c);

		c.gridx = 2;
		c.gridy = 1;

		controlFrame.add(right, c);

		c.gridx = 1;
		c.gridy = 2;

		controlFrame.add(down, c);

		c.gridx = 4;

		controlFrame.add(Box.createHorizontalStrut(10), c);

		c.gridx = 6;

		controlFrame.add(Box.createHorizontalStrut(10), c);

		c.gridx = 7;
		c.gridy = 2;

		controlFrame.add(home, c);

		c.gridx = 5;
		c.gridy = 0;

		controlFrame.add(in, c);

		c.gridy = 2;

		controlFrame.add(out, c);

		controlFrame.setSize(300, 300);
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		if (cf instanceof PTZControlInterface) {
			selectedInterface = (PTZControlInterface) cf;
			return true;
		}
		return false;
	}

	@Override
	public void processControl(Object obj) {
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
	}

}
