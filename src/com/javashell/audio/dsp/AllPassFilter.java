package com.javashell.audio.dsp;

import java.nio.FloatBuffer;

public class AllPassFilter implements DigitalSignalProcessor {

	@Override
	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float delayFactor, float sampleRate) {
		float[] samples = sampleBuf.array();
		int samplesLength = samples.length;
		int delaySamples = (int) ((float) 89.27f * (sampleRate / 1000)); // Number of delay samples. Calculated from
																			// number of samples per millisecond
		float[] allPassFilterSamples = new float[samplesLength];
		float decayFactor = 0.131f;

		// Applying algorithm for All Pass Filter
		for (int i = 0; i < samplesLength; i++) {
			allPassFilterSamples[i] = samples[i];

			if (i - delaySamples >= 0)
				allPassFilterSamples[i] += -decayFactor * allPassFilterSamples[i - delaySamples];

			if (i - delaySamples >= 1)
				allPassFilterSamples[i] += decayFactor * allPassFilterSamples[i + 20 - delaySamples];
		}

		// This is for smoothing out the samples and normalizing the audio. Without
		// implementing this, the samples overflow causing clipping of audio
		float value = allPassFilterSamples[0];
		float max = 0.0f;

		for (int i = 0; i < samplesLength; i++) {
			if (Math.abs(allPassFilterSamples[i]) > max)
				max = Math.abs(allPassFilterSamples[i]);
		}

		for (int i = 0; i < allPassFilterSamples.length; i++) {
			float currentValue = allPassFilterSamples[i];
			value = ((value + (currentValue - value)) / max);

			allPassFilterSamples[i] = value;
		}
		return FloatBuffer.wrap(allPassFilterSamples);
	}

}
