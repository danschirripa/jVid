package com.javashell.video.ingestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.stream.Stream;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.elements.AppSink;
import org.opencv.core.Core;

import com.javashell.video.VideoIngestor;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

public class GStreamerIngestor extends VideoIngestor {
	private AppSink component;
	private BufferedImage bufFrame;
	private Dimension res;
	private Pipeline pipeline;

	public GStreamerIngestor(Dimension resolution, String inputDescriptor) {
		super(resolution);
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		configurePaths();
		Gst.init(Version.BASELINE, "jVid");

		res = resolution;
		component = new AppSink("GStreamerIngest");
		component.set("emit-signals", true);
		AppSinkListener listener = new AppSinkListener();
		component.connect((AppSink.NEW_SAMPLE) listener);
		component.connect((AppSink.NEW_PREROLL) listener);
		StringBuilder caps = new StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,");
		// JNA creates ByteBuffer using native byte order, set masks according to that.
		caps.append("format=ABGR");

		component.setCaps(new Caps(caps.toString()));
		System.out.println();
		Bin bin = Gst.parseBinFromDescription(
				inputDescriptor + " ! videoconvert ! " + "capsfilter caps=video/x-raw,framerate=30/1,width="
						+ (int) res.getWidth() + ",height=" + (int) res.getHeight(),
				true);
		pipeline = new Pipeline();
		pipeline.addMany(bin, component);
		Pipeline.linkMany(bin, component);
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		return bufFrame;
	}

	@Override
	public boolean open() {
		pipeline.play();
		return true;
	}

	@Override
	public boolean close() {
		pipeline.stop();
		return true;
	}

	private class AppSinkListener implements AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {

		@Override
		public FlowReturn newSample(AppSink elem) {
			Sample sample = elem.pullSample();
			Structure capsStruct = sample.getCaps().getStructure(0);
			int w = capsStruct.getInteger("width");
			int h = capsStruct.getInteger("height");
			Buffer buffer = sample.getBuffer();
			ByteBuffer bb = buffer.map(false);
			if (bb != null) {
				rgbFrame(false, w, h, bb);
				buffer.unmap();
			}
			sample.dispose();
			return FlowReturn.OK;
		}

		@Override
		public FlowReturn newPreroll(AppSink elem) {
			Sample sample = elem.pullPreroll();
			Structure capsStruct = sample.getCaps().getStructure(0);
			int w = capsStruct.getInteger("width");
			int h = capsStruct.getInteger("height");
			Buffer buffer = sample.getBuffer();
			ByteBuffer bb = buffer.map(false);
			if (bb != null) {
				rgbFrame(true, w, h, bb);
				buffer.unmap();
			}
			sample.dispose();
			return FlowReturn.OK;
		}

		private void rgbFrame(boolean isPrerollFrame, int width, int height, ByteBuffer rgb) {
			final BufferedImage renderImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			byte[] pixels = ((DataBufferByte) renderImage.getRaster().getDataBuffer()).getData();
			rgb.get(pixels, 0, width * height * 4);
			bufFrame = renderImage;
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

	static String findWindowsLocation() {
		if (Platform.is64Bit()) {
			return Stream
					.of("GSTREAMER_1_0_ROOT_MSVC_X86_64", "GSTREAMER_1_0_ROOT_MINGW_X86_64",
							"GSTREAMER_1_0_ROOT_X86_64")
					.map(System::getenv).filter(p -> p != null).map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\")
					.findFirst().orElse("");
		} else {
			return "";
		}
	}

}
