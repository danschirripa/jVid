package com.javashell.audio.dsp;

import java.nio.FloatBuffer;
import java.util.Random;

public class BasicReverb implements DigitalSignalProcessor {

	private SampleQueue sampleDoubleBuf = null;
	private int bufSizeInSeconds;
	private Random rand = new Random();
	private float max = Float.MIN_VALUE;

	public BasicReverb(int bufSizeInSeconds) {
		this.bufSizeInSeconds = bufSizeInSeconds;
	}

	@Override
	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float decayFactor, float sampleRate) {
		final float[] samples = sampleBuf.array();
		if (sampleDoubleBuf == null)
			sampleDoubleBuf = new SampleQueue((int) (sampleRate * bufSizeInSeconds) / samples.length, samples.length);

		sampleDoubleBuf.add(samples);

		int numSamples = (int) ((float) delay * (sampleRate / 1000));
		for (int j = 0; j < samples.length; j++) {
			System.out.println(samples[j]);
			final float pSample1 = sampleDoubleBuf.get(j + numSamples);
			final float pSample2 = sampleDoubleBuf.get(j + numSamples - rand.nextInt(50));
			final float pSample3 = sampleDoubleBuf.get(j + numSamples + rand.nextInt(50));
			final float pSample4 = sampleDoubleBuf.get(j + numSamples - rand.nextInt(50));

			samples[j] += (pSample1 * (decayFactor));
			samples[j] += (pSample2 * (decayFactor));
			samples[j] += (pSample3 * (decayFactor));
			samples[j] += (pSample4 * (decayFactor));

			if (Math.abs(samples[j]) > max)
				max = samples[j];

			if (samples[j] > 0.0f) {
				System.out.println("CLIP");
			}
		}

		sampleBuf = null;
		return FloatBuffer.wrap(samples);
	}
}
