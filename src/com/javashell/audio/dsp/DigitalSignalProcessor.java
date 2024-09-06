package com.javashell.audio.dsp;

import java.nio.FloatBuffer;

public interface DigitalSignalProcessor {

	public FloatBuffer processSamples(FloatBuffer sampleBuf, float delay, float delayFactor, float sampleRate);

}
