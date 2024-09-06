package com.javashell.audio.dsp;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class CombFilter implements DigitalSignalProcessor {
	public enum CombFilterType {
		FEED_FORWARD, FEED_BACK
	}

	private final CombFilterType type;

	public CombFilter(CombFilterType type) {
		this.type = type;
	}

	@Override
	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float delayFactor, float sampleRate) {
		float[] samples = sampleBuf.array();
		if (type == CombFilterType.FEED_FORWARD)
			samples = processSamplesFeedforward(samples, samples.length, delay, delayFactor, sampleRate);
		else
			samples = processSamplesFeedback(samples, samples.length, delay, delayFactor, sampleRate);
		return FloatBuffer.wrap(samples);
	}

	private float[] processSamplesFeedforward(float[] samples, int length, float delayMs, float decayFactor,
			float sampleRate) {
		int numSamples = (int) ((float) delayMs * (sampleRate / 1000));
		float[] filterSamples = Arrays.copyOf(samples, length);
		for (int i = 0; i < length - numSamples; i++) {
			filterSamples[i + numSamples] += ((float) filterSamples[i] * decayFactor);
		}
		return filterSamples;
	}

	private float[] processSamplesFeedback(float[] samples, int length, float delayMs, float decayFactor,
			float sampleRate) {
		int numSamples = (int) ((float) delayMs * (sampleRate / 1000));
		float[] filterSamples = Arrays.copyOf(samples, length);
		for (int i = 0; i < length - numSamples; i++) {
			filterSamples[i] += ((float) filterSamples[i + numSamples] * decayFactor);
		}
		return filterSamples;
	}

}
