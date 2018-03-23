/**
 * 
 */
package net.zhouji.vdetector;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;

/**
 * @author Zhou
 *
 */
class VDetectorDemo {
    public static void main(String[] args) throws Exception {
    	Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("parameters.properties"));
        } catch (IOException e) {
        	throw e;
        }
        
        boolean hypothesisTesting = false;
        String ht = properties.getProperty("hypothesis.testing", "no");
        if(ht.equals("yes") || ht.equals("true"))
        	hypothesisTesting = true;
        String[] thr = properties.getProperty("threshold").split("\\s");
        String[] cvr = properties.getProperty("coverage").split("\\s");
        String[] lmt = properties.getProperty("number.limit").split("\\s");
        
        int numberThreshold = thr.length;
        if(numberThreshold!=cvr.length)
        	throw new Exception("number of the thresholds not matching that of coverage");
        if(numberThreshold!=lmt.length)
        	throw new Exception("number of the thresholds not matching that of detector number limit");
        
        double[] thresholds = new double[numberThreshold];
        double[] coverage = new double[numberThreshold];
        int[] limit = new int[numberThreshold];
        for(int i=0; i<numberThreshold; i++){
        	thresholds[i] = Double.parseDouble(thr[i]); 
        	coverage[i] = Double.parseDouble(cvr[i]); 
        	limit[i] = Integer.parseInt(lmt[i]); 
        }
        	
        String[] trainingDataFilename = properties.getProperty("training.datafile").split("\\s");
        String[] testDataFilename = properties.getProperty("test.datafile").split("\\s");
        String[] outputDataFilename = properties.getProperty("output.datafile").split("\\s");
        int numberTraining = trainingDataFilename.length;
        if(numberTraining!=testDataFilename.length)
        	throw new Exception("number of the training data files not matching that of test files");
        if(numberTraining!=outputDataFilename.length)
        	throw new Exception("number of the training data files not matching that of output files");

        String rpt = properties.getProperty("repeat", "1");
        int repeat = Integer.parseInt(rpt);
        
        for(int i=0; i<numberTraining; i++) {
        	for(int j=0; j<numberThreshold; j++) {
        		Setting<Double> setting = null;
                DetectorGenerator<Double> generator = null;
        		if(hypothesisTesting) {
        			setting  = new HypothesisSetting<Double>(
                		coverage[j], // coverage
                		new Double(thresholds[j]), // threshold 
                		trainingDataFilename[i],  // filename
                		limit[j], 	// numberLimit
                		0.9); // alpha
        			generator = new RealVectorDetectorGenerator((HypothesisSetting<Double>)setting);
        		} else {
        			setting  = new NaiveSetting<Double>(
                    		coverage[j], // coverage
                    		new Double(thresholds[j]), // threshold 
                    		trainingDataFilename[i],  // filename
                    		limit[j] 	// number of detectors
                    		);
        			generator = new RealVectorDetectorGenerator((NaiveSetting<Double>)setting);
        		}

        		for(int k=0; k<repeat; k++) {
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
        		} // end of k loop
        		
        	} // end of j loop
        } // end of i loop
    }
   
    VDetectorDemo() {
    }
}
