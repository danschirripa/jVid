package com.javashell.audio.dsp;

import java.nio.FloatBuffer;

import com.javashell.audio.dsp.CombFilter.CombFilterType;

public class SchroederReverb implements DigitalSignalProcessor {
	private CombFilter cf;
	private AllPassFilter apf;

	public SchroederReverb() {
		cf = new CombFilter(CombFilterType.FEED_BACK);
		apf = new AllPassFilter();
	}

	@Override
	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float delayFactor, float sampleRate) {
		float[] cfBuf1 = cf.processSamples(sampleBuf, delay, delayFactor, sampleRate).array();
		float[] cfBuf2 = cf.processSamples(sampleBuf, delay - 11.73f, delayFactor - 0.1313f, sampleRate).array();
		float[] cfBuf3 = cf.processSamples(sampleBuf, delay + 19.31f, delayFactor - 0.2743f, sampleRate).array();
		float[] cfBuf4 = cf.processSamples(sampleBuf, delay - 7.7f, delayFactor - 0.31f, sampleRate).array();

		float[] outputComb = new float[cfBuf1.length];
		for (int i = 0; i < outputComb.length; i++) {
			outputComb[i] = ((cfBuf1[i] + cfBuf2[i] + cfBuf3[i] + cfBuf4[i]));
		}

		sampleBuf = FloatBuffer.wrap(outputComb);

		cfBuf1 = null;
		cfBuf2 = null;
		cfBuf3 = null;
		cfBuf4 = null;

		sampleBuf = apf.processSamples(sampleBuf, delay, delayFactor, sampleRate);
		sampleBuf = apf.processSamples(sampleBuf, delay, delayFactor, sampleRate);

		return sampleBuf;
	}

}
