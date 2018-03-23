package net.zhouji.vdetector;

public class HypothesisSetting<T> extends Setting<T> {
	int numberLimit;
	double alpha;

	public HypothesisSetting(double coverage, T threshold, String filename,
			int  numberLimit, double alpha) {
		super(coverage, threshold, filename, true);
		
		this.numberLimit = numberLimit;
		this.alpha = alpha;
	}

	int getNumberLimit() {  return numberLimit;}
	double getAlpha() { return alpha; }
}
