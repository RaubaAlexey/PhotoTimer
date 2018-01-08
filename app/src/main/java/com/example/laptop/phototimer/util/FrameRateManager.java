package com.example.laptop.phototimer.util;
import java.util.LinkedList;

public class FrameRateManager {
	
	double[] targetFrameRates;
	double[] minimumFrameRates;
	int currentRateIndex = 0;
	long currentNanosPerFrame;
	
	double targetFrameRateFudgeFactor = 1.015;
	double[] unfudgedTargetFrameRates;
	
	LinkedList<Long> previousFrameTimestamps = new LinkedList<Long>();
	
	int frameHistorySize = 10;
	boolean allowReducingFrameRate = true;
	boolean allowLockingFrameRate = true;
	
	boolean frameRateLocked = false;
	
	int maxGoodFrames = 500;
	int maxSlowFrames = 150;
	
	double currentFPS = -1;
	int goodFrames = 0;
	int slowFrames = 0;

	long totalFrames = 0;
	
	final static long BILLION = 1000000000L;
	final static long MILLION = 1000000L;


	public FrameRateManager(double[] targetRates, double[] minRates) {
		if (targetRates==null || minRates==null || minRates.length < targetRates.length-1) {
			throw new IllegalArgumentException("Must specify as many minimum rates as target rates minus one");
		}
		
		this.unfudgedTargetFrameRates = targetRates;
		this.minimumFrameRates = minRates;
		
		this.targetFrameRates = new double[targetRates.length];
		for(int i=0; i<targetRates.length; i++) {
			this.targetFrameRates[i] = targetFrameRateFudgeFactor * targetRates[i];
		}
		
		setCurrentRateIndex(0);
	}
	

	public FrameRateManager(double frameRate) {
		this(new double[] {frameRate}, new double[0]);
	}
	

	public void clearTimestamps() {
		previousFrameTimestamps.clear();
		goodFrames = 0;
		slowFrames = 0;
		currentFPS = -1;
	}
	
	void setCurrentRateIndex(int index) {
		currentRateIndex = index;
		currentNanosPerFrame = (long)(BILLION / targetFrameRates[currentRateIndex]);
	}
	
	void reduceFPS() {
		setCurrentRateIndex(currentRateIndex + 1);
		goodFrames = 0;
		slowFrames = 0;
		frameRateLocked = false;
	}
	
	public void resetFrameRate() {
		clearTimestamps();
		setCurrentRateIndex(0);
		frameRateLocked = false;
	}
	
	
	public void frameStarted(long time) {
		++totalFrames;
		previousFrameTimestamps.add(time);
		if (previousFrameTimestamps.size() > frameHistorySize) {
			long firstTime = previousFrameTimestamps.removeFirst();
			double seconds = (time - firstTime) / (double)BILLION;
			currentFPS = frameHistorySize / seconds;
			
			if (!frameRateLocked && currentRateIndex < minimumFrameRates.length) {
				if (currentFPS < minimumFrameRates[currentRateIndex]) {
					++slowFrames;
					if (slowFrames >= maxSlowFrames) {
						reduceFPS();
					}
				}
				else {
					++goodFrames;
					if (maxGoodFrames > 0 && goodFrames >= maxGoodFrames) {
						if (allowLockingFrameRate) {
							frameRateLocked = true;
						}
						slowFrames = 0;
						goodFrames = 0;
					}
				}
			}
		}
	}
	
	public void frameStarted() {
		frameStarted(System.nanoTime());
	}
	
	public double currentFramesPerSecond() {
		return currentFPS;
	}
	
	public double targetFramesPerSecond() {
		return unfudgedTargetFrameRates[currentRateIndex];
	}
	
	public String formattedCurrentFramesPerSecond() {
		return String.format("%.1f", currentFPS);
	}
	
	public String fpsDebugInfo() {
		return String.format("FPS: %.1f target: %.1f %s", currentFPS, targetFramesPerSecond(), (frameRateLocked) ? "(locked)" : "");
	}
	
	public long lastFrameStartTime() {
		return previousFrameTimestamps.getLast();
	}

	public long nanosToWaitUntilNextFrame(long time) {
		long lastStartTime = previousFrameTimestamps.getLast();
		long singleFrameGoalTime = lastStartTime + currentNanosPerFrame;
		long waitTime = singleFrameGoalTime - time;

		if (previousFrameTimestamps.size()==frameHistorySize) {
			long multiFrameGoalTime = previousFrameTimestamps.getFirst() + frameHistorySize*currentNanosPerFrame;
			long behind = singleFrameGoalTime - multiFrameGoalTime;

			if (behind > 0) waitTime -= behind;
		}
		
		if (waitTime < MILLION) waitTime = MILLION;
		return waitTime;
	}
	
	public long nanosToWaitUntilNextFrame() {
		return nanosToWaitUntilNextFrame(System.nanoTime());
	}
	
	public long sleepUntilNextFrame() {
		long nanos = nanosToWaitUntilNextFrame(System.nanoTime());
		try {
			Thread.sleep(nanos/MILLION, (int)(nanos%MILLION));
		}
		catch(InterruptedException ignored) {}
		return nanos;
	}

	public boolean allowReducingFrameRate() {
		return allowReducingFrameRate;
	}
	
	public void setAllowReducingFrameRate(boolean value) {
		allowReducingFrameRate = value;
	}

	public boolean allowLockingFrameRate() {
		return allowLockingFrameRate;
	}
	public void setAllowLockingFrameRate(boolean value) {
		allowLockingFrameRate = value;
	}

	public long getTotalFrames() {
		return totalFrames;
	}

}
