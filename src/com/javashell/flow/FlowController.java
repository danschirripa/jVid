package com.javashell.flow;

import java.util.HashSet;

import com.javashell.video.VideoProcessor;

public class FlowController {

	private static HashSet<FlowNode<VideoProcessor>> processors = new HashSet<FlowNode<VideoProcessor>>();
	private static Thread frameRateTimer = new Thread(new FrameRateTimerRunnable(), "FlowRateController");
	private static long frameRateInterval = (long) 16.3 * 1000000;
	private static long averageFrameRateInterval = 0;
	private static long[] frameRateIntervals = new long[200];
	private static int frameIndex = 0;
	private static boolean stopTimer = false, pauseFlow = false;
	private static Object stateChangeLock = new Object(), caughtPauseLock = new Object();

	public static void startFlowControl() {
		if (frameRateTimer.isAlive())
			return;
		stopTimer = false;
		frameRateTimer.start();
	}

	public static void stopFlowControl() {
		if (frameRateTimer.isAlive())
			stopTimer = true;
	}

	public static void pauseFlow() {
		if (pauseFlow)
			return;
		synchronized (stateChangeLock) {
			pauseFlow = true;
			System.out.println("Flow paused");
		}
		try {
			synchronized (caughtPauseLock) {
				caughtPauseLock.wait();
			}
			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void resumeFlow() {
		synchronized (stateChangeLock) {
			stateChangeLock.notify();
			System.out.println("Flow resumed");
		}
	}

	public static boolean isFlowing() {
		return frameRateTimer.isAlive();
	}

	public static void registerFlowNode(FlowNode<VideoProcessor> node) {
		if (node.contentIsIngestor())
			processors.add(node);
	}

	public static void registerUncheckedFlowNode(FlowNode<VideoProcessor> node) {
		processors.add(node);
	}

	public static void unregisterFlowNode(FlowNode<VideoProcessor> node) {
		processors.remove(node);
	}

	public static void setAveragingSampleCount(int count) {
		frameRateIntervals = new long[count];
		frameIndex = 0;
	}

	public static long getAverageFrameDelta() {
		return averageFrameRateInterval;
	}

	public static long getAverageInternalFrameRate() {
		if (averageFrameRateInterval == 0)
			return 0;
		return (1000000000 / averageFrameRateInterval);
	}

	private static class FrameRateTimerRunnable implements Runnable {

		@Override
		public void run() {
			long lastTime = System.nanoTime();
			while (!stopTimer) {
				synchronized (stateChangeLock) {
					if (pauseFlow) {
						System.out.println("Caught pause");
						synchronized (caughtPauseLock) {
							caughtPauseLock.notify();
						}
						try {
							stateChangeLock.wait();
						} catch (Exception e) {
							e.printStackTrace();
						}
						pauseFlow = false;
						System.out.println("Caught resume");
					}
				}
				if (System.nanoTime() - lastTime >= frameRateInterval) {
					lastTime = System.nanoTime();
					for (FlowNode<VideoProcessor> fn : processors) {
						long startTime = System.nanoTime();
						fn.triggerFrame(null, null);
						long endTime = System.nanoTime();
						long deltaTime = endTime - startTime;
						if (deltaTime > frameRateInterval) {
							System.err.println("Unable to complete frame render in requested timer interval, took "
									+ deltaTime + "ns, requested was " + frameRateInterval + "ns - "
									+ (deltaTime - frameRateInterval) + " too long");
						}
						frameRateIntervals[frameIndex] = deltaTime;
						frameIndex++;
						if (frameIndex == frameRateIntervals.length) {
							final long[] frameRateIntervalsBuf = frameRateIntervals;
							Thread frameRateCalculationThread = new Thread(new Runnable() {
								public void run() {
									long running = 0;
									for (long delta : frameRateIntervalsBuf) {
										running += delta;
									}
									averageFrameRateInterval = (running / frameRateIntervalsBuf.length);
								}
							});
							frameRateCalculationThread.start();
							frameIndex = 0;
						}
					}
				}
			}
		}

	}

}
