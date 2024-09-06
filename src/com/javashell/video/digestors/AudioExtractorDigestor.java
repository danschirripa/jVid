package com.javashell.video.digestors;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackPort;

import com.javashell.audio.AudioProcessor;
import com.javashell.audio.JackAudioDigestor;
import com.javashell.video.ControlInterface;
import com.javashell.video.VideoDigestor;

public class AudioExtractorDigestor extends VideoDigestor implements AudioProcessor, ControlInterface {
	private FloatBuffer[] samples;
	private JackAudioDigestor audioClient;
	private int channels;
	private HashSet<AudioProcessor> audioProcessors;

	public AudioExtractorDigestor(String clientName, String[] outputs, Dimension resolution) {
		super(resolution);
		channels = outputs.length;
		samples = new FloatBuffer[channels];
		audioClient = new AudioInjector(clientName, null, outputs);
		audioProcessors = new HashSet<AudioProcessor>();
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
	public void processSamples(FloatBuffer[] samples) {
		this.samples = samples;
	}

	@Override
	public BufferedImage processFrame(BufferedImage frame) {
		return frame;
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public boolean close() {
		audioClient.getRawJackClient().close();
		return true;
	}

	private class AudioInjector extends JackAudioDigestor {

		public AudioInjector(String clientName, String[] inputs, String[] outputs) {
			super(clientName, inputs, outputs);
		}

		@Override
		public boolean process(JackClient client, int nframes) {
			HashMap<String, JackPort> outputs = this.getOutputs();
			Iterator<String> iter = outputs.keySet().iterator();

			int i = 0;
			if (samples != null)
				while (iter.hasNext()) {
					JackPort port = outputs.get(iter.next());
					if (port.getFloatBuffer() != null)
						port.getFloatBuffer().put(samples[i]);
					i++;
				}
			return true;
		}

	}

	@Override
	public int getAudioChannels() {
		return channels;
	}

	@Override
	public void addSubscriber(AudioProcessor p) {
		audioProcessors.add(p);
	}

	@Override
	public void removeSubscriber(AudioProcessor p) {
		audioProcessors.remove(p);
	}

}
