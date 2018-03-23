/**
 * 
 */
package net.zhouji.vdetector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

/**
 * @author Zhou
 *
 */
public abstract class DetectorGenerator<T extends Comparable<T>> {
	private Log log = LogFactory.getLog(DetectorGenerator.class);
	
	protected Setting<T> setting;
	protected List< VdPoint<T> > trainingData;

	private int n = 0; // required number of samples to do hypothesis testing
	private double zalpha = 0;
	private double term1 = 0;
	private double term2 = 0;
	private double z = 0;
	
	private static NormalDistribution normalDistribution = new NormalDistributionImpl();
//	// necessary to be extended
//	DetectorGenerator() {
//	}
   
	/**
	 * Hypothesis testing version, only initialization without generating detectors
	 * @param setting
	 * @throws Exception
	 */
    public DetectorGenerator(HypothesisSetting<T> setting) throws Exception {
		this.setting = setting;

		double maxCoverage = 1. - 5./setting.getNumberLimit();
		if(setting.getCoverage()>maxCoverage) {
			throw new VDetectorParameterException(
					"Exception: too large coverage expected.\n"
					+"coverage="+setting.getCoverage()+", maxCoverage="+maxCoverage);
		}

		double p = setting.getCoverage();
		double q = 1-p;
		n = (int)Math.ceil(Math.max(5./p, 5./q));

		// I used to have copied to source code of method getInvCDF(double, boolean).
		// zalpha = StatUtil.getInvCDF(1.-setting.getAlpha(), true);
		// The current way of using Apache Commons is believed to be more reliable.
		zalpha = normalDistribution.inverseCumulativeProbability(1.-setting.getAlpha());
		term1 = 1./Math.sqrt(n*p*q);
		term2 = -Math.sqrt(n*p/q);
		z = term2;
		
		trainingData = readTrainingDataFile();
	}
    
    /**
     * naive estimate version, 
     * @param setting
     */
    public DetectorGenerator(NaiveSetting<T> setting) throws VDetectorParameterException, IOException {
		this.setting = setting;
		
		trainingData = readTrainingDataFile();
    }
	
    /**
     * The core algorithm of V-detector - default version:
     * the entire process of generating enough detectors - using naive estimate.
     * 
     */
    public List< Detector<T> > createDetectorSet () throws VDetectorParameterException {
    	int maxNumDetector = 0;
    	if(setting instanceof NaiveSetting){
    		maxNumDetector = ((NaiveSetting<T>)setting).getNumDetector();
    	} else {
    		throw new VDetectorParameterException("Default createDetectorSet called for hypothesis testing");
    	}
    	
    	int num = 0; // number of detectors
		
		int covered = 0;
		int maxTries = (int)Math.ceil(1./(1. - setting.getCoverage()));
		
		List<Detector<T>> detectors = new ArrayList<Detector<T>>();
		int totalSampling = 0;
		do {
			Detector<T> newCandidate = findCandidate();

			if(!Detector.match(detectors, newCandidate.getCenter())) {
				num++;
				detectors.add( newCandidate );
				covered = 0;
			} else {
				covered++;
			}
		
			totalSampling++;

			if(covered>=maxTries)break; // satisfied!
		} while (num<maxNumDetector && totalSampling<MAX_SAMPLING);

		if(totalSampling==MAX_SAMPLING)
			log.info("I'm tired of this. I sampled "+MAX_SAMPLING+" times!");
		
		log.info("Total number of detectors is "+detectors.size());
		return detectors;
    }

    /**
     * Second version of the core algorithm of V-detector - allowing variations
     */
    public List< Detector<T> > createDetectorSet(
    		boolean hypothesisTesting) throws VDetectorParameterException {
        if(!hypothesisTesting)return createDetectorSet();
		return generateEnoughDetectors();
    }

	abstract protected List< VdPoint<T> > readTrainingDataFile() throws IOException;
	abstract protected VdPoint<T> randomPoint();
	
	// to be properly efficient, this function has to have a side-effect
	// (1) find a valid (meaning not matching training data) new point - direct purpose
	// (2) keep the "nearest distance" is a valid point is obtained - side effect
	private static int MAX_TRIES = 1000;
	private Detector<T> findCandidate() throws VDetectorParameterException {
		int c = 0;
		
		while(c<MAX_TRIES) {
			VdPoint<T> newPoint = randomPoint();
			T nearestDistance = newPoint.nearestDistance(trainingData);
			if(nearestDistance.compareTo( setting.getThreshold())>=0) 
				return new Detector<T>(newPoint, nearestDistance);
			c++;
		}
		throw new VDetectorParameterException("Exception: too much self region. Threshold="+setting.getThreshold()+"; times of trials="+c);
	}

	private int N = 0;
	private int x = 0;

	private List<Detector<T>> generateEnoughDetectors() throws VDetectorParameterException {
		if(! (setting instanceof HypothesisSetting))
			throw new VDetectorParameterException("Not parameter settings for Hypothesis Testing ");
			
		Collection<Detector<T>> candidates = new ArrayList<Detector<T>>();
		List<Detector<T>> detectors = new ArrayList<Detector<T>>();

		N = 0;
		x = 0;
		
		int totalSampling = 0;
		do {
			Detector<T> newCandidate = findCandidate();
			
			N++;

			if( Detector.match(detectors, newCandidate.getCenter())) {
				x++;
				z += term1;
				if(z>zalpha)break;
			} else {
				candidates.add( newCandidate );
			}

			if (N==n) {
				detectors.addAll(candidates);
				log.debug("A group of "+candidates.size()+" detectors are generated.");
				log.debug("Sampled points N="+N+", covered points x="+x);

				candidates.clear();
				N = 0;
				x = 0;
				z = term2; // by using z this way, x is not necessary in the algorithm except to display
			}

			totalSampling++;
		} while (totalSampling<MAX_SAMPLING);

		if(totalSampling==MAX_SAMPLING)
			log.info("I'm tired of this. I sampled "+MAX_SAMPLING+" times!");
		
		log.info("Total number of detectors is "+detectors.size());
		return detectors;
	} // end of generateEnoughDetectors
	
	private static final int  MAX_SAMPLING = 1000000; 
	
	/* this method needs to be called before a demonstration session with a loop of invocations of tryToGenerateOneDetector */
	/* if called righr after generator's constructor, it in fact doesn't do anything */
	public void resetDemo() throws VDetectorParameterException {
		N = 0;
		x = 0;
	}

	// needed by the movie program
	// otherwise it can be local in the method generateOneDetector()
	protected Detector<T> lastCandidate = null;
	
	/**
	 * try to generate one detector
	 * three possible results: 1: add a new detector; 2: too much self region, quit the entire process and; 3: enough, stop satisfactorily
	 * 
	 * @return true - enough
	 * @return false - a new detector is added, not enough yet
	 * @throws VDetectorParameterException - too much self region to work on
	 * it also must have the side effect of increasing the variable totalSampling
	 */
	public boolean tryToGenerateOneDetector(Collection<Detector<T>> detectors) throws VDetectorParameterException {
		Detector<T> newCandidate = findCandidate();
	
		N++;
	
		if(Detector.match(detectors, newCandidate.getCenter())) {
			log.debug( "This point is already covered: " + newCandidate.getCenter() );
			x++;
			z += term1;
			if(z>zalpha) return true; // enough coverage according to hypothesis testing
		} else {
			lastCandidate = newCandidate;
		}

		// not enough coverage according to hypothesis testing
		// add one detector	- instead of add all the candidates as before
		if (N==n) {
			detectors.add(lastCandidate); // simple use the last valid candidate
			log.debug("One detector is added after hypotheis testing: "+lastCandidate);
			N = 0;
			x = 0;
			z = term2; // by using z this way, x is not necessary in the algorithm except to display
		} else {
			log.debug("Detector is not added yet - wait until finishing hypothesis testing");
		}
	
		return false;
	} // end of tryToGenerateOneDetector
	
	@SuppressWarnings("serial")
	public
	static class VDetectorParameterException extends Exception {
		VDetectorParameterException(String s) {
			super(s);
		}
	}

	public List<VdPoint<T>> getTrainingData() {
		return trainingData;
	}

}
