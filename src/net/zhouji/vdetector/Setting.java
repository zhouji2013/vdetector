/**
 * 
 */
package net.zhouji.vdetector;

/**
 * Basic parameter setting.
 * 
 * It is extended as either HypothesisSetting or Naive setting. This class describes the common part between the two. 
 * @author Zhou
 *
 */
public abstract class Setting<T> {
   	private T threshold;
   	private String trainingDataFileName;
   	private double coverage;

   	Setting(double coverage, T threshold, String filename, boolean isHypothesisTesting) {
   		this.coverage = coverage;
   		this.threshold = threshold;
   		trainingDataFileName = filename;
   	}
   	
   	double getCoverage() { return coverage; }
   	T getThreshold() {return threshold; }
   	String getTrainingDataFileName() {return trainingDataFileName; }
}
