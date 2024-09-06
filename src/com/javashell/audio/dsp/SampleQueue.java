package com.javashell.audio.dsp;

import java.util.LinkedList;

public class SampleQueue {

	private int capacity;
	private float[][] samples;
	private int offset, bufsize;

	public SampleQueue(int capacity, int bufsize) {
		this.capacity = capacity;
		this.bufsize = bufsize;
		System.out.println("Cap: " + capacity);
		samples = new float[capacity][];
		for (int i = 0; i < capacity; i++)
			samples[i] = new float[bufsize];
	}

	public void add(float[] o) {
		samples[offset] = o;
		offset++;
		if (offset == samples.length)
			offset = 0;
	}

	public float get(int index) {
		int remainder = index % bufsize;
		int cycles = index / bufsize;

		int bucket = offset + cycles;

		if (bucket > samples.length)
			bucket = bucket - samples.length;

		// System.out.println(bucket + " : " + remainder);
		return samples[bucket][remainder];
	}

}
