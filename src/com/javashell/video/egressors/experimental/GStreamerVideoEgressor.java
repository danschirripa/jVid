package com.javashell.video.egressors.experimental;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.IntBuffer;

import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeInfo;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.PadProbeType;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Version;

import com.javashell.video.VideoEgress;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

public class GStreamerVideoEgressor extends VideoEgress {
	private static BufferedImage curFrame;
	private static Pipeline pipeline;

	public GStreamerVideoEgressor(Dimension resolution) {
		super(resolution);
		configurePaths();
		Gst.init(Version.BASELINE, "jVid");
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = frame;
		return frame;
	}

	@Override
	public boolean open() {
		String caps = "video/x-raw, width=" + getResolution().width + ", height=" + getResolution().height
				+ ", pixel-aspect-ratio=1/1, framerate=" + 60 + "/1";

		String fullPipelineString = "videotestsrc pattern=white ! " + " videoscale ! " + caps
				+ " ! identity name=identity ! videoconvert ! " + "x265enc ! video/x-h265 ! "
				+ "tcpserversink host=10.42.0.1 port=7896 name=sink";

		System.out.println(fullPipelineString);

		pipeline = (Pipeline) Gst.parseLaunch(fullPipelineString);
		Element identity = pipeline.getElementByName("identity");
		identity.getStaticPad("sink").addProbe(PadProbeType.BUFFER, new Render());

		pipeline.getBus().connect((Bus.ERROR) ((source, code, message) -> {
			System.out.println(message);
			Gst.quit();
		}));
		pipeline.getBus().connect((Bus.EOS) (source) -> Gst.quit());
		pipeline.play();
		return true;
	}

	@Override
	public boolean close() {
		pipeline.stop();
		return true;
	}

	private class Render implements Pad.PROBE {
		private final BufferedImage image;
		private final int[] data;

		public Render() {
			image = new BufferedImage(getResolution().width, getResolution().height, BufferedImage.TYPE_INT_ARGB);
			data = ((DataBufferInt) (image.getRaster().getDataBuffer())).getData();
		}

		@Override
		public PadProbeReturn probeCallback(Pad pad, PadProbeInfo info) {
			if (curFrame == null) {
				return PadProbeReturn.OK;
			}
			Buffer buf = info.getBuffer();
			if (buf.isWritable()) {
				IntBuffer ib = buf.map(true).asIntBuffer();
				ib.get(data);
				image.getGraphics().drawImage(curFrame, 0, 0, null);
				ib.rewind();
				ib.put(data);
				buf.unmap();
			}
			return PadProbeReturn.OK;
		}

	}

	static void configurePaths() {
		if (Platform.isWindows()) {
			String gstPath = System.getProperty("gstreamer.path", findWindowsLocation());
			if (!gstPath.isEmpty()) {
				String systemPath = System.getenv("PATH");
				if (systemPath == null || systemPath.trim().isEmpty()) {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath);
				} else {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath + File.pathSeparator + systemPath);
				}
			}
		} else if (Platform.isMac()) {
			String gstPath = System.getProperty("gstreamer.path", "/Library/Frameworks/GStreamer.framework/Libraries/");
			if (!gstPath.isEmpty()) {
				String jnaPath = System.getProperty("jna.library.path", "").trim();
				if (jnaPath.isEmpty()) {
					System.setProperty("jna.library.path", gstPath);
				} else {
					System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
				}
			}

		}
	}

	/**
	 * Query over a stream of possible environment variables for GStreamer location,
	 * filtering on the first non-null result, and adding \bin\ to the value.
	 *
	 * @return location or empty string
	 */
	static String findWindowsLocation() {
		if (Platform.is64Bit()) {
			return java.util.stream.Stream
					.of("GSTREAMER_1_0_ROOT_MSVC_X86_64", "GSTREAMER_1_0_ROOT_MINGW_X86_64",
							"GSTREAMER_1_0_ROOT_X86_64")
					.map(System::getenv).filter(p -> p != null).map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\")
					.findFirst().orElse("");
		} else {
			return "";
		}
	}

}
