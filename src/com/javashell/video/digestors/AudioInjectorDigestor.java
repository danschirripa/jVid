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

public class AudioInjectorDigestor extends VideoDigestor implements AudioProcessor, ControlInterface {
	private final FloatBuffer[] samples;
	private int channels;
	private JackAudioDigestor audioClient;

	private HashSet<AudioProcessor> audioProcessors;

	public AudioInjectorDigestor(String clientName, String[] inputs, Dimension resolution) {
		super(resolution);
		channels = inputs.length;
		samples = new FloatBuffer[channels];
		audioClient = new AudioInjector(clientName, inputs, null);
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
		for (AudioProcessor ap : audioProcessors)
			ap.processSamples(this.samples);
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
			HashMap<String, JackPort> inputs = this.getInputs();
			Iterator<String> iter = inputs.keySet().iterator();

			int i = 0;
			while (iter.hasNext()) {
				JackPort port = inputs.get(iter.next());
				final float[] floatSamples = new float[port.getFloatBuffer().capacity()];
				port.getFloatBuffer().get(floatSamples);
				samples[i] = FloatBuffer.wrap(floatSamples);
				i++;
			}
			processSamples(null);
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
