package net.zhouji.vdetector;

public class NaiveSetting<T> extends Setting<T> {
	int numDetector;

	public NaiveSetting(double coverage, T threshold, String filename,
			int  numDetector) {
		super(coverage, threshold, filename, false);
		
		this.numDetector = numDetector;
	}

	int getNumDetector() {  return numDetector;}
}
