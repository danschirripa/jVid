package com.javashell.audio;

import java.nio.FloatBuffer;

public interface AudioProcessor {

	public void processSamples(FloatBuffer[] samples);

	public int getAudioChannels();

	public void addSubscriber(AudioProcessor p);

	public void removeSubscriber(AudioProcessor p);

}
