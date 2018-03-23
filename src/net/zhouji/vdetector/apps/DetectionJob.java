/**
 * 
 */
package net.zhouji.vdetector.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.zhouji.vdetector.Detector;
import net.zhouji.vdetector.DetectorGenerator;
import net.zhouji.vdetector.HypothesisSetting;
import net.zhouji.vdetector.NaiveSetting;
import net.zhouji.vdetector.RealVectorDetectorGenerator;
import net.zhouji.vdetector.RealVectorPoint;
import net.zhouji.vdetector.Setting;
import net.zhouji.vdetector.VdPoint;
import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

/**
 * Batch job of detection anomaly.
 * This is real value version.
 * 
 * @author Zhou
 * 
 */
public class DetectionJob {
	private boolean hypothesisTesting;
	private double[] coverage;
	private double[] thresholds;
	private int[] limit;
	private String[] trainingFileNames;
	private String[] testFileNames;
	private String[] outputFileNames;
	int repeat;

	private int numberDataSets = 0;
	private int numberParameterSettings = 0;
	
	private StringBuilder reportStringBuilder = null;


	/**
	 * This must be called to get the correct threshold if RealVectorPoint is used.
	 * 
	 * @param threshold
	 * @return squared threshold - the square of Euclidean distance 
	 */
	private double squaredThreshold(double threshold) {
		return threshold*threshold;
	}
	
	public DetectionJob(File propertiesFile) throws Exception {
    	Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));
        
        hypothesisTesting = false;
        String ht = properties.getProperty("hypothesis.testing", "no");
        if(ht.equals("yes") || ht.equals("true"))
        	hypothesisTesting = true;
        String[] thr = properties.getProperty("threshold").split("\\s");
        String[] cvr = properties.getProperty("coverage").split("\\s");
        String[] lmt = properties.getProperty("number.limit").split("\\s");
		
        numberParameterSettings = thr.length;
        if(numberParameterSettings!=cvr.length)
        	throw new Exception("number of the thresholds not matching that of coverage");
        if(numberParameterSettings!=lmt.length)
        	throw new Exception("number of the thresholds not matching that of detector number limit");
        
        thresholds = new double[numberParameterSettings];
        coverage = new double[numberParameterSettings];
        limit = new int[numberParameterSettings];
        for(int i=0; i<numberParameterSettings; i++){
        	
        	/********************************/
        	thresholds[i] = squaredThreshold( Double.parseDouble(thr[i]) );
        	/********************************/

        	coverage[i] = Double.parseDouble(cvr[i]); 
        	limit[i] = Integer.parseInt(lmt[i]); 
        }
        	
        trainingFileNames = properties.getProperty("training.datafile").split("\\s");
        testFileNames = properties.getProperty("test.datafile").split("\\s");
        outputFileNames = properties.getProperty("output.datafile").split("\\s");
        numberDataSets = trainingFileNames.length;
        if(numberDataSets!=testFileNames.length)
        	throw new Exception("number of the training data files not matching that of test files");
        if(numberDataSets!=outputFileNames.length)
        	throw new Exception("number of the training data files not matching that of output files");

        String rpt = properties.getProperty("repeat", "1");
        repeat = Integer.parseInt(rpt);
        
    	reportStringBuilder = new StringBuilder(separator1+"V-Detector Batch Job Report\n"+separator1);
	}

	private final static String separator1 = "*****************************\n";
	private final static String separator2 = "=============================\n";
	private final static String separator3 = "-----------------------------\n";
	
	private List<EvaluationResult> allEvaluationResults = null;
	private List<DetectionResult> allDetectionResults = null;
	
	public void evaluate() throws Exception {
		allEvaluationResults = new ArrayList<EvaluationResult>();
		
		for (int i = 0; i < numberDataSets; i++) {
			EvaluationResult result = new EvaluationResult(trainingFileNames[i], testFileNames[i], outputFileNames[i]);
			Map<VdPoint<Double>, Boolean> testData = readLabeledData(new File(testFileNames[i]));
			for (int j = 0; j < numberParameterSettings; j++) {
				Setting<Double> setting = null;
				DetectorGenerator<Double> generator = null;
				if (hypothesisTesting) {
					setting = new HypothesisSetting<Double>(coverage[j], // coverage
							new Double(thresholds[j]), // threshold
							trainingFileNames[i], // filename
							limit[j], // numberLimit
							0.9); // alpha
					generator = new RealVectorDetectorGenerator(
							(HypothesisSetting<Double>) setting);
				} else {
					setting = new NaiveSetting<Double>(coverage[j], // coverage
							new Double(thresholds[j]), // threshold
							trainingFileNames[i], // filename
							limit[j] // number of detectors
					);
					generator = new RealVectorDetectorGenerator(
							(NaiveSetting<Double>) setting);
				}

				SummaryStatistics statsDr = new SummaryStatistics();
				SummaryStatistics statsFa = new SummaryStatistics();

				for (int k = 0; k < repeat; k++) {
					List<Detector<Double>> detectorSet = null;
					try {
						detectorSet = generator.createDetectorSet(true);
					} catch (VDetectorParameterException e) {
						e.printStackTrace();
					}
					if (detectorSet == null) {
						System.out.println("null detectorSet");
						System.exit(0);
					}
					int countAbnormal = 0;
					int tp = 0, tn = 0, fp = 0, fn = 0;
					for(VdPoint<Double> point: testData.keySet()) {
						boolean detectedAbnormal = Detector.match(detectorSet, point);
						boolean actualAbnormal = testData.get(point);
						if(detectedAbnormal)
							countAbnormal++;
						if( detectedAbnormal ) {
							if(actualAbnormal) tp++;
							else fp++;
						} else {
							if(actualAbnormal) fn++;
							else tn++;
						}
					}
					double dr = (double)tp/(tp+fn);
					double fa = (double)fp/(fp+tn);
					statsDr.addValue(dr);
					statsFa.addValue(fa);
				} // end of k loop
				OneEvaluationResult oneResult = new OneEvaluationResult(coverage[j], thresholds[j], limit[j], statsDr, statsFa);
				result.oneResult.add(oneResult);
			} // end of j loop
			allEvaluationResults.add(result);
		} // end of i loop
	}
	
	public void detect() throws Exception {
		allDetectionResults = new ArrayList<DetectionResult>();

		for (int i = 0; i < numberDataSets; i++) {
			DetectionResult result = new DetectionResult(trainingFileNames[i], testFileNames[i], outputFileNames[i]);
			List<VdPoint<Double>> testData = readUnlabeledData(new File(testFileNames[i]));
			for (int j = 0; j < numberParameterSettings; j++) {
				reportStringBuilder.append("target coverage "+ coverage[j]+"; threshold "+thresholds[j]+"; detector number limit "+limit[j]).append('\n')
				.append(separator3);
				Setting<Double> setting = null;
				DetectorGenerator<Double> generator = null;
				if (hypothesisTesting) {
					setting = new HypothesisSetting<Double>(coverage[j], // coverage
							new Double(thresholds[j]), // threshold
							trainingFileNames[i], // filename
							limit[j], // numberLimit
							0.9); // alpha
					generator = new RealVectorDetectorGenerator(
							(HypothesisSetting<Double>) setting);
				} else {
					setting = new NaiveSetting<Double>(coverage[j], // coverage
							new Double(thresholds[j]), // threshold
							trainingFileNames[i], // filename
							limit[j] // number of detectors
					);
					generator = new RealVectorDetectorGenerator(
							(NaiveSetting<Double>) setting);
				}

				SummaryStatistics stats = new SummaryStatistics();
				OneDetectionResult oneResult = new OneDetectionResult(coverage[j], thresholds[j], limit[j], stats, testData.size());

				/* in detection, it is pointless to repeat because we are not going to verify the result */
				/*
				for (int k = 0; k < repeat; k++) {
				*/
				List<Detector<Double>> detectorSet = null;
				try {
					detectorSet = generator.createDetectorSet(true);
				} catch (VDetectorParameterException e) {
					e.printStackTrace();
				}
				if (detectorSet == null) {
					System.out.println("null detectorSet");
					System.exit(0);
				}
				int countAbnormal = 0;
				for(VdPoint<Double> point: testData) {
					boolean detectedAbnormal = Detector.match(detectorSet, point);
					if(detectedAbnormal) {
						countAbnormal++;
						oneResult.addAbnormals((RealVectorPoint)point);
					}
				}
				stats.addValue(countAbnormal);
					
				reportStringBuilder.append("Detected abnormals ").append(stats.toString());
				result.oneResult.add(oneResult);
			} // end of j loop
			allDetectionResults.add(result);
		} // end of i loop
	}

	private List<VdPoint<Double>> readUnlabeledData(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		if(line==null) {
			System.out.println("It failed to read the header line of the test data file: "+file.getName());
			throw new IOException("failed to read the header line of the test data file");
		}
		String[] token = line.split("\\s");
		int dim = Integer.parseInt(token[0]);
		int setSize = Integer.parseInt(token[1]);
		
		double[] rawData = new double[dim];
		
		List<VdPoint<Double>> points = new ArrayList<VdPoint<Double>>();
		for(int i=0; i<setSize; i++) {
			line = br.readLine();
			token = line.split("\\s");
		
			for(int j=0; j<dim; j++) {
				rawData[j] = Double.parseDouble(token[j]);
			}
			points.add( new RealVectorPoint(rawData));
		}
		return points;
	}

	private Map<VdPoint<Double>, Boolean> readLabeledData(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		if(line==null) {
			System.out.println("It failed to read the header line of the test data file: "+file.getName());
			throw new IOException("failed to read the header line of the test data file");
		}
		String[] token = line.split("\\s");
		int dim = Integer.parseInt(token[0]);
		int setSize = Integer.parseInt(token[1]);
		
		double[] rawData = new double[dim];
		
		Map<VdPoint<Double>, Boolean> points = new HashMap<VdPoint<Double>, Boolean>();
		for(int i=0; i<setSize; i++) {
			line = br.readLine();
			token = line.split("\\s");
		
			for(int j=0; j<dim; j++) {
				rawData[j] = Double.parseDouble(token[j]);
			}
			Boolean abnormal = true;
			if(Integer.parseInt(token[dim])!=0)abnormal = false;
			points.put( new RealVectorPoint(rawData), abnormal);
		}
		return points;
	}

	public static class OneEvaluationResult {
		double coverage;
		double threshold;
		int limit;

		SummaryStatistics statsDr = null;
		SummaryStatistics statsFa = null;

		OneEvaluationResult(double coverage, double threshold, int limit, SummaryStatistics statsDr, SummaryStatistics statsFa) {
			this.coverage = coverage;
			this.threshold = threshold;
			this.limit = limit;
			this.statsDr = statsDr;
			this.statsFa = statsFa;
		}

		public SummaryStatistics getStatsDr() {
			return statsDr;
		}

		public SummaryStatistics getStatsFa() {
			return statsFa;
		}

		public double getCoverage() {
			return coverage;
		}

		public double getThreshold() {
			return threshold;
		}
	}

	private static class OneDetectionResult {
		double coverage;
		double threshold;
		int limit;

		SummaryStatistics stats = null;
		List<RealVectorPoint> abnormals = null;
		
		int totalCount;

		OneDetectionResult(double coverage, double threshold, int limit, SummaryStatistics stats, int totalCount) {
			this.coverage = coverage;
			this.threshold = threshold;
			this.limit = limit;
			this.stats = stats;
			this.totalCount = totalCount;
			abnormals = new ArrayList<RealVectorPoint>();
		}
		
		void addAbnormals(RealVectorPoint p) {
			abnormals.add(p);
		}
	}
	
	public static class EvaluationResult {
		String trainingDataFile;
		String testDataFile;
		String outputFile;
		List<OneEvaluationResult> oneResult;
		
		EvaluationResult(String trainingDataFile, String testDataFile, String outputFile) {
			this.trainingDataFile = trainingDataFile;
			this.testDataFile = testDataFile;
			this.outputFile = outputFile;
			oneResult = new ArrayList<OneEvaluationResult>();
		}

		public List<OneEvaluationResult> getOneResult() {
			return oneResult;
		}

		public String getTrainingDataFile() {
			return trainingDataFile;
		}

		public String getTestDataFile() {
			return testDataFile;
		}
	}
	private static class DetectionResult {
		String trainingDataFile;
		String testDataFile;
		String outputFile;
		List<OneDetectionResult> oneResult;
		
		DetectionResult(String trainingDataFile, String testDataFile, String outputFile) {
			this.trainingDataFile = trainingDataFile;
			this.testDataFile = testDataFile;
			this.outputFile = outputFile;
			oneResult = new ArrayList<OneDetectionResult>();
		}
	}
	
	public String report() {
		return reportStringBuilder.toString();
	}

	public static void main(String[] args) throws Exception {
		DetectionJob job = new DetectionJob(new File("parameters.properties"));
		if(args.length>=1 && args[0].equalsIgnoreCase("e")) {
			job.evaluate();
			System.out.println(job.formatEvaluationResults());
			job.saveEvaluationResults();
		} else {
			job.detect();
			System.out.println(job.formatDetectionResults());
			job.saveDetectionResults();
		}
		
	}
	
	private String formatEvaluationResults() {
		StringBuilder sb = new StringBuilder(separator1+"V-Detector Batch Job Report\n"+separator1);
		for(EvaluationResult result: allEvaluationResults) {
			sb.append(separator2+"Training Data "+result.trainingDataFile+" | Test Data "+result.testDataFile+"\n"+separator2);
			for(OneEvaluationResult oneResult: result.oneResult) {
				sb.append(separator3+"Target Coverage "+oneResult.coverage+" | Distance Threshold "+
						String.format("%7.4f", oneResult.threshold)+" | Detector Limit "+oneResult.limit+"\n"+separator3);
				sb.append("Detection Rate "+oneResult.statsDr);
				sb.append("False Alarm Rate "+oneResult.statsFa);
			}
		}
		return sb.toString();
	}
	
	public String htmlEvaluationResults() {
		return "<pre>"+formatEvaluationResults()+"</pre>";
	}
	
	public void saveEvaluationResults() throws IOException {
		for(EvaluationResult result: allEvaluationResults) {
			PrintWriter pw = new PrintWriter(new FileWriter(result.outputFile));
			pw.println("dr_min\tdr_max\tdr_mean\tdr_sd\tfa_min\tfa_max\tfa_mean\tfa_sd");
			for(OneEvaluationResult r: result.oneResult) {
				SummaryStatistics statsDr = r.statsDr;
				SummaryStatistics statsFa = r.statsFa;
				pw.println(statsDr.getMin()+"\t"+statsDr.getMax()+"\t"+statsDr.getMean()+"\t"+statsDr.getStandardDeviation()+"\t"+
						statsFa.getMin()+"\t"+statsFa.getMax()+"\t"+statsFa.getMean()+"\t"+statsFa.getStandardDeviation());
				pw.close();
			}
		}
		
	}

	public String formatDetectionResults() {
		StringBuilder sb = new StringBuilder(separator1+"V-Detector Batch Job Report\n"+separator1);
		for(DetectionResult result: allDetectionResults) {
			sb.append(separator2+"Training Data "+result.trainingDataFile+" | Test Data "+result.testDataFile+"\n"+separator2);
			for(OneDetectionResult oneResult: result.oneResult) {
				sb.append(separator3+"Target Coverage "+oneResult.coverage+" | Distance Threshold "+
						String.format("%7.4f", oneResult.threshold)+" | Detector Limit "+oneResult.limit+"\n"+separator3);
				sb.append("Total points "+oneResult.totalCount+"\n");
				sb.append("Abnormals "+oneResult.stats);
//				sb.append("abnormals:\n");
//				for(RealVectorPoint p: oneResult.abnormals) {
//					sb.append(p);
//				}
			}
		}
		return sb.toString();
	}

	public void saveDetectionResults() throws IOException {
		for(DetectionResult result: allDetectionResults) {
			PrintWriter pw = new PrintWriter(new FileWriter(result.outputFile));
			for(OneDetectionResult r: result.oneResult) {
				pw.println(r.abnormals.size()+" abnormals out of "+r.totalCount);
				for(RealVectorPoint p: r.abnormals) {
					pw.println(p);
				}
				pw.close();
			}
		}
	}

	public int getNumberParameterSettings() {
		return numberParameterSettings;
	}

	public List<EvaluationResult> getAllEvaluationResults() {
		return allEvaluationResults;
	}

	public boolean isHypothesisTesting() {
		return hypothesisTesting;
	}

	public double[] getCoverage() {
		return coverage;
	}

	public double[] getThresholds() {
		return thresholds;
	}

	public String[] getTrainingFileNames() {
		return trainingFileNames;
	}

	public int[] getLimit() {
		return limit;
	}
}
