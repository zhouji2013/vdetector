/**
 * 
 */
package net.zhouji.vdetector.apps;

import java.util.List;

import net.zhouji.vdetector.Detector;
import net.zhouji.vdetector.DetectorGenerator;
import net.zhouji.vdetector.NaiveSetting;
import net.zhouji.vdetector.VdPoint;
import net.zhouji.vdetector.RealVectorDetectorGenerator;
import net.zhouji.vdetector.RealVectorPoint;
import net.zhouji.vdetector.Setting;
import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;

/**
 * This is V-detector's 'hello world' - the simplest case how V-detector is
 * used. Anything we can do with V-detector will at least include the stuff
 * demonstrated by this program.
 * 
 * @author Zhou
 * 
 */
public class HelloWorld {
	public static void main(String[] args) throws Exception {
		// example parameters
		double coverage = 0.99;
		double thresholds = 0.1;
		String trainingDataFilename = "Pentagram-mid_train.txt";
		int limit = 1000;

		Setting<Double> setting = new NaiveSetting<Double>(coverage, // coverage
				new Double(thresholds), // threshold
				trainingDataFilename, // filename
				limit // number of detectors
		);
		DetectorGenerator<Double> generator = new RealVectorDetectorGenerator(
				(NaiveSetting<Double>) setting);

		try {
			// naive version instead of hypothesis testing
			List<Detector<Double>> detectorSet = generator.createDetectorSet();
			VdPoint<Double> point = new RealVectorPoint(2);

			System.out.println("A random point " + point.toString()
					+ " is tested.");
			if (Detector.match(detectorSet, point))
				System.out.println("It is not normal.");
			else
				System.out.println("It is normal.");

		} catch (VDetectorParameterException e) {
			e.printStackTrace();
		}

	}
}
