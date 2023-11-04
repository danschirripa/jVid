package com.javashell.video.camera.extras;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.javashell.video.ControlInterface;
import com.javashell.video.VideoIngestor;
import com.javashell.video.camera.PTZControlInterface;

import jakarta.xml.bind.DatatypeConverter;

public class AmcrestCameraInterface extends VideoIngestor implements PTZControlInterface {
	private String realm, nonce, opaque, cnonce, nc, qop, ha1, host;
	private int ncInt = 0;
	private Thread captureThread;
	private boolean isOpen = false;
	private BufferedImage curFrame, bufFrame, nullFrame;
	private FFmpegFrameGrabber grabber;
	private Java2DFrameConverter conv;
	private long lastFPS;
	private int frameDelay = 16;
	private String user;

	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}

	public AmcrestCameraInterface(Dimension resolution, String user, String pass, String host)
			throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
		super(resolution);
		this.host = host;
		this.user = user;

		String requestUri = "/cgi-bin/ptz.cgi?action=getStatus";
		URL hostUrl = new URL(host + requestUri);
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder().uri(hostUrl.toURI()).headers("User-Agent", "jvid/1.0").GET().build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println(response.statusCode());

		incrementNC();
		generateCnonce();

		var responseHeaders = response.headers();

		var authValues = responseHeaders.allValues("www-authenticate");
		var authParams = authValues.get(0).split(",");
		Map<String, String> authParamMap = new HashMap<String, String>();
		for (String authParam : authParams) {
			final String[] keyValue = authParam.split("=");
			final String key = keyValue[0].replace("\"", "").replace(" ", "");
			final String value = keyValue[1].replace("\"", "");
			authParamMap.put(key, value);
		}

		realm = authParamMap.get("Digestrealm");
		qop = authParamMap.get("qop");
		nonce = authParamMap.get("nonce");
		opaque = authParamMap.get("opaque");

		var responseValue = generateResponseValue("GET", requestUri, user, pass);

		client = HttpClient.newHttpClient();
		Builder requestBuilder = HttpRequest.newBuilder().uri(hostUrl.toURI()).headers("User-Agent", "jvid/1.0").GET();
		var authHeader = createAuthHeader(requestUri, user, responseValue);
		requestBuilder.header("Accept", "*/*");
		requestBuilder.header("Authorization", authHeader);
		request = requestBuilder.build();

		System.out.println(responseValue);
		System.out.println(request.headers().toString());

		response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println(response.statusCode() + " - " + hostUrl.getHost());

		grabber = new FFmpegFrameGrabber(
				"rtsp://" + user + ":" + pass + "@" + hostUrl.getHost() + ":554/cam/realmonitor?channel=1&subtype=0");

		init();
	}

	private char[] chars = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
			's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	private void generateCnonce() {
		int[] bytes = new int[16];
		bytes = new Random().ints(16, 0, chars.length).toArray();
		cnonce = "";
		for (int i = 0; i < bytes.length; i++) {
			cnonce = cnonce + chars[bytes[i]];
		}
	}

	private String createAuthHeader(String requestUri, String user, String response) {
		return "Digest username=\"" + user + "\", realm=\"" + realm + "\", nonce=\"" + nonce + "\", cnonce=\"" + cnonce
				+ "\", qop=" + qop + ", nc=" + nc + ", uri=\"" + requestUri + "\", algorithm=\"MD5\", response=\""
				+ response + "\", opaque=\"" + opaque + "\"";
	}

	private String generateResponseValue(String method, String uri, String user, String pass)
			throws NoSuchAlgorithmException {

		var digest = MessageDigest.getInstance("MD5");
		byte[] hash = digest.digest((user + ":" + realm + ":" + pass).getBytes(StandardCharsets.UTF_8));
		var HA1 = bytesToHex(hash);

		digest = MessageDigest.getInstance("MD5");
		hash = digest.digest((method + ":" + uri).getBytes(StandardCharsets.UTF_8));
		var HA2 = bytesToHex(hash);

		digest = MessageDigest.getInstance("MD5");
		hash = digest.digest(
				(HA1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + HA2).getBytes(StandardCharsets.UTF_8));
		var responseValue = bytesToHex(hash);
		return responseValue;
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	private String generateResponseValue(String method, String uri) throws NoSuchAlgorithmException {
		var digest = MessageDigest.getInstance("MD5");
		byte[] hash = digest.digest((method + ":" + uri).getBytes(StandardCharsets.UTF_8));
		var HA2 = bytesToHex(hash);

		digest = MessageDigest.getInstance("MD5");
		hash = digest.digest(
				(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + HA2).getBytes(StandardCharsets.UTF_8));
		var responseValue = bytesToHex(hash);
		return responseValue;
	}

	private void incrementNC() {
		ncInt++;
		var tempNc = Integer.toHexString(ncInt);
		while (tempNc.length() < 8)
			tempNc = "0" + tempNc;
		nc = tempNc;
	}

	private void resetNC() {
		ncInt = 0;
		var tempNc = Integer.toHexString(ncInt);
		while (tempNc.length() < 8)
			tempNc = "0" + tempNc;
		nc = tempNc;
	}

	@Override
	public boolean addSubscriber(ControlInterface cf) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void processControl(Object obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeControlEgressor(ControlInterface cf) {
		// TODO Auto-generated method stub

	}

	private int panDeltaMax = getResolution().width / 2;
	private int tiltDeltaMax = getResolution().height / 2;

	@Override
	public void PTZ(int tilt, int pan, int zoom) {
		try {

			if (tilt == 0 && pan == 0) {

			}

			if (zoom == 0) {

			}

			String requestUri = "/cgi-bin/ptz.cgi?action=start&code=Up&channel=1&arg1=" + pan + "&arg2=" + tilt
					+ "&arg3=" + zoom + "&arg4=30";

			String authUri = "/cgi-bin/ptz.cgi?action=start&code=Continuously";

			URL hostUrl = new URL(host + requestUri);
			var client = HttpClient.newHttpClient();

			incrementNC();

			var responseValue = generateResponseValue("GET", requestUri);

			Builder requestBuilder = HttpRequest.newBuilder().uri(hostUrl.toURI())
					.headers("User-Agent", "jvid/1.0", "Accept", "*/*").GET();
			requestBuilder.header("Authorization", createAuthHeader(requestUri, user, responseValue));
			var request = requestBuilder.build();
			System.out.println(request.headers().toString());

			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println(response.statusCode() + " - " + hostUrl.getHost());
			System.out.println(response.headers().toString());
			System.out.println(hostUrl.toString());

			if (response.statusCode() == 401) {
				var responseHeaders = response.headers();

				var authValues = responseHeaders.allValues("www-authenticate");
				var authParams = authValues.get(0).split(",");
				Map<String, String> authParamMap = new HashMap<String, String>();
				for (String authParam : authParams) {
					final String[] keyValue = authParam.split("=");
					final String key = keyValue[0].replace("\"", "").replace(" ", "");
					final String value = keyValue[1].replace("\"", "");
					authParamMap.put(key, value);
				}

				realm = authParamMap.get("Digestrealm");
				qop = authParamMap.get("qop");
				nonce = authParamMap.get("nonce");
				opaque = authParamMap.get("opaque");

				resetNC();
				incrementNC();
				generateCnonce();

				responseValue = generateResponseValue("GET", requestUri);

				requestBuilder = HttpRequest.newBuilder().uri(hostUrl.toURI())
						.headers("User-Agent", "jvid/1.0", "Accept", "*/*").GET();
				requestBuilder.header("Authorization", createAuthHeader(requestUri, user, responseValue));
				request = requestBuilder.build();
				System.out.println(request.headers().toString());

				response = client.send(request, HttpResponse.BodyHandlers.ofString());
				System.out.println(response.statusCode() + " - " + hostUrl.getHost());
				System.out.println(response.headers().toString());
				System.out.println(response.body());
				System.out.println(hostUrl.toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void IRIS(int in, int out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void FOCUS(int in, int out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void HOME() {
		// TODO Auto-generated method stub

	}

	@Override
	public void AUTOFOCUS() {
		// TODO Auto-generated method stub

	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		curFrame = bufFrame;
		return curFrame;
	}

	@Override
	public boolean open() {
		try {
			grabber.setVideoBitrate(2048000);
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

	private void init() {
		captureThread = new Thread(new Runnable() {
			public void run() {
				Frame javacvFrame;
				while (isOpen) {
					try {
						long startTime = System.nanoTime();
						javacvFrame = grabber.grabAtFrameRate();
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
