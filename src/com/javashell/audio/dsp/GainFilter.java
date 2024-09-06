package com.javashell.audio.dsp;

import java.nio.FloatBuffer;

public class GainFilter implements DigitalSignalProcessor {
	private float gainPercentage;

	public void setGain(float gainPercentage) {
		this.gainPercentage = gainPercentage;
	}

	@Override
	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float delayFactor, float sampleRate) {
		float[] samples = sampleBuf.array();
		for (int i = 0; i < samples.length; i++)
			samples[i] = samples[i] * gainPercentage;
		return FloatBuffer.wrap(samples);
	}

}
