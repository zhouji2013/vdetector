/**
 * 
 */
package net.zhouji.vdetector.apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Properties;

import net.zhouji.vdetector.Detector;
import net.zhouji.vdetector.DetectorGenerator;
import net.zhouji.vdetector.HypothesisSetting;
import net.zhouji.vdetector.NaiveSetting;
import net.zhouji.vdetector.RealVectorDetectorGenerator;
import net.zhouji.vdetector.Setting;
import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;

/**
 * Only generate a detector set and save it as disk file.
 * This is real value version.
 * 
 * @author Zhou
 * 
 */
public class GenerateDetectorSetOnly {
	private boolean hypothesisTesting;
	private double coverage;
	private double thresholds;
	private int limit;
	private String trainingFileNames;

	public GenerateDetectorSetOnly(File propertiesFile) throws Exception {
    	Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));
        
        hypothesisTesting = false;
        String ht = properties.getProperty("hypothesis.testing", "no");
        if(ht.equals("yes") || ht.equals("true"))
        	hypothesisTesting = true;
        String[] thr = properties.getProperty("threshold").split("\\s");
        String[] cvr = properties.getProperty("coverage").split("\\s");
        String[] lmt = properties.getProperty("number.limit").split("\\s");
		
        	thresholds = Double.parseDouble(thr[0]); 
        	coverage = Double.parseDouble(cvr[0]); 
        	limit = Integer.parseInt(lmt[0]); 
        	
        trainingFileNames = properties.getProperty("training.datafile").split("\\s")[0];
	}

	public void generateDetectorSet() throws Exception {
				Setting<Double> setting = null;
				DetectorGenerator<Double> generator = null;
				thresholds *= thresholds; // if using RealVectorPoint, Euclidean square distance is used
				
				if (hypothesisTesting) {
					setting = new HypothesisSetting<Double>(coverage, // coverage
							new Double(thresholds), // threshold
							trainingFileNames, // filename
							limit, // numberLimit
							0.9); // alpha
					generator = new RealVectorDetectorGenerator(
							(HypothesisSetting<Double>) setting);
				} else {
					setting = new NaiveSetting<Double>(coverage, // coverage
							new Double(thresholds), // threshold
							trainingFileNames, // filename
							limit // number of detectors
					);
					generator = new RealVectorDetectorGenerator(
							(NaiveSetting<Double>) setting);
				}

					List<Detector<Double>> detectorSet = null;
					try {
						detectorSet = generator.createDetectorSet(true);
						ObjectOutputStream out = null;
						try {
							out = new ObjectOutputStream(new FileOutputStream("detectorSet.ser"));
							out.writeObject(detectorSet);
							out.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						} finally {
							if(out!=null)
								try {
									out.close();
								} catch (IOException e) {
								}
						}
						
					} catch (VDetectorParameterException e) {
						e.printStackTrace();
					}
	}
	


	public static void main(String[] args) throws Exception {
		GenerateDetectorSetOnly app = new GenerateDetectorSetOnly(new File("parameters.properties"));
		app.generateDetectorSet();
		
		System.out.println("done");
	}
}
