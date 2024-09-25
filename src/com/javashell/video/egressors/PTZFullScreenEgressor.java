package com.javashell.video.egressors;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.javashell.control.protocols.VISCA;
import com.javashell.video.ControlInterface;
import com.javashell.video.VideoEgress;
import com.javashell.video.camera.PTZControlInterface;

public class PTZFullScreenEgressor extends VideoEgress implements ControlInterface {
	private JFrame egressFrame;
	private BufferedImage curFrame;
	private JPanel egressPanel, controlPane;
	private ControlInterface selectedInterface;

	public PTZFullScreenEgressor(Dimension resolution) {
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
				g.setFont(getFont().deriveFont(14));
				g.drawImage(curFrame, 0, 0, monitorSize.width, monitorSize.height, egressFrame);
				g.drawString("Free Mem: " + Runtime.getRuntime().freeMemory() / 1000 + "mb", 0, 0);
			}
		};
		egressPanel.setSize(monitorSize.width, monitorSize.height);
		egressPanel.setPreferredSize(new Dimension(monitorSize.width, monitorSize.height));
		egressFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		egressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		createControlFrame();
		egressFrame.setContentPane(egressPanel);
		egressFrame.add(controlPane);
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

	private void createControlFrame() {
		controlPane = new JPanel();
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
				selectedInterface.processControl(VISCA.ptCommand(VISCA.PTZ_UP, (byte) 0x08));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.PT_STOP);
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
				selectedInterface.processControl(VISCA.ptCommand(VISCA.PTZ_LEFT, (byte) 0x08));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.PT_STOP);
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
				selectedInterface.processControl(VISCA.ptCommand(VISCA.PTZ_RIGHT, (byte) 0x08));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.PT_STOP);
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
				selectedInterface.processControl(VISCA.ptCommand(VISCA.PTZ_DOWN, (byte) 0x08));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.PT_STOP);
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
				selectedInterface.processControl(VISCA.zoomCommand(VISCA.PTZ_IN, (byte) 0x04));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.Z_STOP);
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
				selectedInterface.processControl(VISCA.zoomCommand(VISCA.PTZ_OUT, (byte) 0x04));
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				selectedInterface.processControl(VISCA.Z_STOP);
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
				selectedInterface.processControl(VISCA.HOME);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		controlPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;

		c.gridx = 0;
		c.gridy = 1;

		controlPane.add(left, c);

		c.gridx = 1;
		c.gridy = 0;

		controlPane.add(up, c);

		c.gridx = 2;
		c.gridy = 1;

		controlPane.add(right, c);

		c.gridx = 1;
		c.gridy = 2;

		controlPane.add(down, c);

		c.gridx = 4;

		controlPane.add(Box.createHorizontalStrut(10), c);

		c.gridx = 6;

		controlPane.add(Box.createHorizontalStrut(10), c);

		c.gridx = 7;
		c.gridy = 2;

		controlPane.add(home, c);

		c.gridx = 5;
		c.gridy = 0;

		controlPane.add(in, c);

		c.gridy = 2;

		controlPane.add(out, c);

		controlPane.setSize(150, 150);
		controlPane.setOpaque(false);
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		selectedInterface = cf;
		return true;
	}

	@Override
	public void processControl(Object obj) {
	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
	}

}
