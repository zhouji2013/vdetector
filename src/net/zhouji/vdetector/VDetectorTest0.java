package net.zhouji.vdetector;

import java.util.List;

import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;
import junit.framework.TestCase;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;


public class VDetectorTest0 extends TestCase {

	public void testDetector() {
		VdPoint<Double> p = new RealVectorPoint(2);
		Detector<Double> d = new Detector<Double>(p, 0.5);
		System.out.println("A detector "+d);
		if(d==null)
			fail("d is null");
	}
	
	public void testHypothesis() {
        String trainingDataFilename = "Pentagram-mid_train.txt";
        HypothesisSetting<Double> setting  = new HypothesisSetting<Double>(
        		0.9, // coverage
        		new Double(0.1), // threshold 
        		trainingDataFilename,  // filename
        		1000, 	// numberLimit
        		0.9); // alpha
        DetectorGenerator<Double> generator = null;
		try {
			generator = new RealVectorDetectorGenerator(setting);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        List<Detector<Double>> detectorSet = null;
		try {
			detectorSet = generator.createDetectorSet(true);
		} catch (VDetectorParameterException e) {
			e.printStackTrace();
		}
        if(detectorSet==null) {
        	System.out.println("null detectorSet");
        	System.exit(0);
        }
        VdPoint<Double> point = new RealVectorPoint(2);
        System.out.println("normal?"+ !Detector.match(detectorSet, point));
    }
	
	public void testNaive() {
        String trainingDataFilename = "Pentagram-mid_train.txt";
        NaiveSetting<Double> setting  = new NaiveSetting<Double>(
        		0.9, // coverage
        		new Double(0.1), // threshold 
        		trainingDataFilename,  // filename
        		1000 	// numberDetector
        	);
        DetectorGenerator<Double> generator = null;
		try {
			generator = new RealVectorDetectorGenerator(setting);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        List<Detector<Double>> detectorSet = null;
		try {
			detectorSet = generator.createDetectorSet(false);
		} catch (VDetectorParameterException e) {
			e.printStackTrace();
		}
        if(detectorSet==null) {
        	System.out.println("null detectorSet");
        	System.exit(0);
        }
        VdPoint<Double> point = new RealVectorPoint(2);
        System.out.println("normal?"+ !Detector.match(detectorSet, point));
    }
	
	public void testInvCdf() {
		NormalDistribution normalDistribution = new NormalDistributionImpl();
		for(double x = 0; x<=1.; x+=0.05)
			try {
				System.out.println(x+" "+ normalDistribution.inverseCumulativeProbability(x));
			} catch (MathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	}
}
