package com.javashell.video.camera.extras;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoIngestor;
import com.javashell.video.camera.Camera;
import com.javashell.video.camera.PTZControlInterface;

public class AmcrestCameraInterface extends VideoIngestor implements PTZControlInterface {
	private String host;
	private Thread captureThread;
	private boolean isOpen = false;
	private BufferedImage curFrame, bufFrame, nullFrame;
	private FFmpegFrameGrabber grabber;
	private Java2DFrameConverter conv;
	private long lastFPS;
	private int frameDelay = 16, bitrate = 2048000;
	private CloseableHttpClient client;
	private Camera cam;
	private CredentialsProvider provider;

	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}

	public AmcrestCameraInterface(Dimension resolution, String user, String pass, String ip, int bitrate, Camera cam)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
		super(resolution);
		this.host = ip;
		this.bitrate = bitrate;
		this.cam = cam;

		grabber = new FFmpegFrameGrabber(
				"rtsp://" + user + ":" + pass + "@" + host + ":554/cam/realmonitor?channel=1&subtype=0");

		CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(new AuthScope(ip, 80), new UsernamePasswordCredentials(user, pass));

		CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

		String requestUri = "/cgi-bin/ptz.cgi?action=getStatus&channel=1";
		String url = "http://" + ip + requestUri;

		HttpGet get = new HttpGet(url);
		HttpResponse resp = httpClient.execute(get);

		System.out.println(resp.getStatusLine().getStatusCode());

		client = httpClient;

		init();
		HOME();
		get.releaseConnection();
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		return false;
	}

	@Override
	public void processControl(Object obj) {

	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
	}

	@Override
	public void PTZ(int pan, int tilt, int zoom) {
		System.out.println(pan + " - " + tilt + " - " + zoom);
		try {
			relativePTZ(pan, tilt, zoom);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void relativePTZ(int pan, int tilt, int zoom) throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		String url = "http://" + host + "/cgi-bin/ptz.cgi?action=moveRelatively";
		params.put("channel", "1");
		if (pan == 0 && tilt == 0 && zoom == 0) {
			url = "http://" + host + "/cgi-bin/ptz.cgi?action=stop&code=Up&channel=1&arg1=0&arg2=0&arg3=0";
		} else {
			String tiltValue = "" + ((double) tilt) / getResolution().height + "";
			String panValue = "" + ((double) pan) / getResolution().width + "";
			String zoomValue = "" + ((zoom == 0) ? 0 : 0.01) + "";
			if (zoom < 0) {
				zoomValue = "-0.01";
			}
			System.out.println(tiltValue + " - " + panValue);

			params.put("arg1", tiltValue);
			params.put("arg2", panValue);
			params.put("arg3", zoomValue);

			url += "&" + "channel=1&arg1=" + panValue + "&arg2=" + tiltValue + "&arg3=" + zoomValue;
		}
		System.out.println(url);
		HttpGet get = new HttpGet(url);
		HttpResponse resp = client.execute(get);
		System.out.println(resp.getStatusLine().getStatusCode() + " - " + url);
		System.out.println();
		get.releaseConnection();
	}

	private void indirrectPTZ(int pan, int tilt, int zoom) throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		String url = "http://" + host + "/cgi-bin/ptz.cgi?action=start&code=";
		params.put("channel", "1");
		if (pan == 0 && tilt == 0 && zoom == 0) {
			url = "http://" + host + "/cgi-bin/ptz.cgi?action=stop&code=Up&channel=1&arg1=0&arg2=0&arg3=0";
		} else {
			String code = "";
			String arg1 = "0";
			String arg2 = "0";
			String tiltValue = "" + ((tilt == 0) ? 0 : 1) + "";
			String panValue = "" + ((pan == 0) ? 0 : 1) + "";
			System.out.println(code + " " + tiltValue + " - " + panValue);

			if (tilt > 0 && pan > 0) {
				code = "RightUp";
				arg1 = tiltValue;
				arg2 = panValue;
			}
			if (tilt > 0 && pan < 0) {
				code = "LeftUp";
				arg1 = tiltValue;
				arg2 = panValue;
			}
			if (tilt < 0 && pan > 0) {
				code = "RightDown";
				arg1 = tiltValue;
				arg2 = panValue;
			}
			if (tilt < 0 && pan < 0) {
				code = "LeftDown";
				arg1 = tiltValue;
				arg2 = panValue;
			}
			if (tilt == 0 && pan > 0) {
				code = "Right";
				arg2 = panValue;
			}
			if (tilt == 0 && pan < 0) {
				code = "Left";
				arg2 = panValue;
			}
			if (tilt > 0 && pan == 0) {
				code = "Up";
				arg2 = tiltValue;
			}
			if (tilt < 0 && pan == 0) {
				code = "Down";
				arg2 = tiltValue;
			}
			if (zoom > 0) {
				code = "ZoomTele";
			}
			if (zoom < 0) {
				code = "ZoomWide";
			}
			params.put("arg1", arg1);
			params.put("arg2", arg2);
			params.put("arg3", "0");

			url += code + "&" + getParamsString(params);
		}
		System.out.println(url);
		HttpGet get = new HttpGet(url);
		HttpResponse resp = client.execute(get);
		System.out.println(resp.getStatusLine().getStatusCode() + " - " + url);
		get.releaseConnection();
	}

	@Override
	public void IRIS(int in, int out) {
	}

	@Override
	public void FOCUS(int in, int out) {
	}

	@Override
	public void HOME() {
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("arg3", "0");
			params.put("arg2", "0");
			params.put("arg1", "0");
			params.put("channel", "1");
			String paramLine = getParamsString(params);

			String url = "http://" + host + "/cgi-bin/ptz.cgi?action=stop&code=Up";
			HttpGet get = new HttpGet(url);
			client.execute(get);
			get.releaseConnection();

			url = "http://" + host + "/cgi-bin/ptz.cgi?action=moveAbsolutely&" + paramLine;
			System.out.println(url);

			get = new HttpGet(url);
			HttpResponse resp = client.execute(get);
			System.out.println(resp.getStatusLine().getStatusCode());
			get.releaseConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void AUTOFOCUS() {

	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			grabber.setVideoBitrate(bitrate);
			grabber.setVideoCodecName("h264_vdpau");
			grabber.setVideoOption("threads", "3");
			grabber.start(false);
			frameDelay = (int) (1 / grabber.getFrameRate() * 1000);
			isOpen = true;
			captureThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void generateNullFrame() {
		nullFrame = new BufferedImage(this.getResolution().width, this.getResolution().height,
				BufferedImage.TYPE_INT_RGB);
		nullFrame.getGraphics().setColor(java.awt.Color.CYAN);
		nullFrame.getGraphics().fill3DRect(0, 0, getResolution().width, getResolution().height, true);
	}

	private void getPTZCapability() {
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("channel", "1");
			String paramLine = getParamsString(params);

			String url = "http://" + host + "/cgi-bin/ptz.cgi?action=getCurrentProtocolCaps";
			HttpGet get = new HttpGet(url);
			client.execute(get);
			get.releaseConnection();

			url = "http://" + host + "/cgi-bin/ptz.cgi?action=moveAbsolutely&" + paramLine;
			System.out.println(url);

			get = new HttpGet(url);
			HttpResponse resp = client.execute(get);
			System.out.println(resp.getStatusLine().getStatusCode());
			get.releaseConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init() {
		captureThread = new Thread(new Runnable() {
			public void run() {
				Frame javacvFrame;
				while (isOpen) {
					try {
						long startTime = System.nanoTime();
						javacvFrame = grabber.grab();
						while (javacvFrame.image == null) {
							javacvFrame.close();
							javacvFrame = grabber.grab();
						}
						bufFrame = conv.convert(javacvFrame);
						long endTime = System.nanoTime();
						lastFPS = (1000000000 / (endTime - startTime));
						Thread.sleep(frameDelay);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				curFrame = nullFrame;
			}
		});
		conv = new Java2DFrameConverter();
		generateNullFrame();
	}

	@Override
	public boolean close() {
		try {
			grabber.stop();
			isOpen = false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			result.append("&");
		}

		String resultString = result.toString();
		return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
	}

}
