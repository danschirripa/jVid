package com.javashell.video.camera;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Scanner;

import com.javashell.control.protocols.VISCA;
import com.javashell.video.ControlInterface;

/**
 * Stores and compute necessary PTZ adjustments based on provided camera
 * specifications
 * 
 * @author dan
 *
 */
public class Camera {
	private int maximumX, maximumY, imgCenterX, imgCenterY, fovDegrees, deltaXBound, deltaYBound;
	private int horizontalAngularChangeMaximum, verticalAngularChangeMaximum;
	private static Hashtable<String, Camera> cameraTypes = new Hashtable<String, Camera>();
	private static File configurationFile = new File(System.getProperty("user.home") + "/.autotrack_camera_types");
	private Point imageCenter;

	static {
		loadCameraTypes();
	}

	/**
	 * Create a "Camera" object to represent a physical camera
	 * 
	 * @param imgWidth   Camera's image resolution width
	 * @param imgHeight  Camera's image resolution height
	 * @param fovDegrees The FOV in degrees possible
	 */
	public Camera(int imgWidth, int imgHeight, int fovDegrees) {
		this.maximumX = imgWidth;
		this.maximumY = imgHeight;
		this.fovDegrees = fovDegrees;
		this.imgCenterX = this.maximumX / 2;
		this.imgCenterY = this.maximumY / 2;
		this.imageCenter = new Point(imgCenterX, imgCenterY);
		this.deltaXBound = imgWidth / 30;
		this.deltaYBound = imgHeight / 30;

		this.horizontalAngularChangeMaximum = fovDegrees / 2;

		this.verticalAngularChangeMaximum = (imgCenterY * this.horizontalAngularChangeMaximum) / imgCenterX;

		System.out.println("FOV " + fovDegrees);
		System.out.println("ImgCenterX " + imgCenterX);
		System.out.println("ImgCenterY " + imgCenterY);
		System.out.println("HorizontalAngleMax " + horizontalAngularChangeMaximum);
		System.out.println("VerticalAngleMax " + verticalAngularChangeMaximum);
	}

	public int getHorizontalAngularChangeMax() {
		return horizontalAngularChangeMaximum;
	}

	public int getVerticalAngularChangeMax() {
		return verticalAngularChangeMaximum;
	}

	public Point getImageCenter() {
		return imageCenter;
	}

	public int getWidth() {
		return maximumX;
	}

	public int getHeight() {
		return maximumY;
	}

	/**
	 * Determine PTZ adjustment to move towards the specified point. If the point is
	 * already close to the images center point, halt camera motion to prevent
	 * camera jiggling when movement is not necessary
	 * 
	 * @param centerPoint New point to move towards as center
	 * @return VISCA command translation of point->point translation
	 */
	public byte[] calculateVISCAAdjustment(Point centerPoint) {
		Point imageCenter = getImageCenter();

		int x = centerPoint.x;
		int y = centerPoint.y;
		int deltaX = x - imageCenter.x;
		int deltaY = (-1) * (y - imageCenter.y);

		if ((deltaX < 20 && deltaX > -20) && (deltaY < 20 && deltaY > -20)) {
			return VISCA.PT_STOP;
		}

		int changeX, changeY;
		changeX = (deltaX * getHorizontalAngularChangeMax()) / imageCenter.x;
		changeY = (deltaY * getVerticalAngularChangeMax()) / imageCenter.y;

		System.out.println(deltaX + " : " + deltaY);
		System.out.println();
		System.out.println(changeX + " : " + changeY);

		return VISCA.relativePtCommand(changeX, changeY, (byte) 0x17);
	}

	public int[] calculatePTZAdjustment(Point centerPoint) {
		Point imageCenter = getImageCenter();

		int x = centerPoint.x;
		int y = centerPoint.y;
		int deltaX = x - imageCenter.x;
		int deltaY = (-1) * (y - imageCenter.y);

		if ((deltaX < deltaXBound && deltaX > -(deltaXBound)) && (deltaY < deltaYBound && deltaY > -(deltaYBound))) {
			return new int[] { 0, 0, 0, 0 };
		}

		int changeX, changeY;
		changeX = (deltaX * getHorizontalAngularChangeMax()) / imageCenter.x;
		changeY = (deltaY * getVerticalAngularChangeMax()) / imageCenter.y;

		return new int[] { deltaX, deltaY, changeX, changeY };
	}

	public static Camera getCamera(String type) {
		return cameraTypes.get(type);
	}

	private static void loadCameraTypes() {
		try {
			if (!configurationFile.exists()) {
				configurationFile.createNewFile();

				PrintStream out = new PrintStream(new FileOutputStream(configurationFile));
				out.println("Sony EVI D-70:800:600:48");
				out.flush();
				out.close();
			}
			Scanner sc = new Scanner(new FileInputStream(configurationFile));
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				if (nextLine.startsWith("#"))
					continue;
				String[] values = nextLine.split(":");

				String cameraType = values[0];
				int imgWidth = Integer.parseInt(values[1]);
				int imgHeight = Integer.parseInt(values[2]);
				int fov = Integer.parseInt(values[3]);

				Camera cam = new Camera(imgWidth, imgHeight, fov);

				System.out.println("Added camera \"" + cameraType + "\" with resolution of " + imgWidth + "x"
						+ imgHeight + " and FOV of " + fov);

				cameraTypes.put(cameraType, cam);
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unable to load/create main configuration file...");
			System.exit(-1);
		}
	}

}
