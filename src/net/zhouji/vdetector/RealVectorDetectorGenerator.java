/**
 * 
 */
package net.zhouji.vdetector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhou
 *
 */
public class RealVectorDetectorGenerator extends DetectorGenerator<Double> {
	private int dim = 0;
	
    protected VdPoint<Double> randomPoint() {
    	return new RealVectorPoint(dim);
    }
    
    public RealVectorDetectorGenerator(HypothesisSetting<Double> setting) throws Exception {
    	super(setting);
    	// readTraingData is called from super, so dim is lost once back here 
    	dim = ((RealVectorPoint)trainingData.get(0)).getDim();
    }

    public RealVectorDetectorGenerator(NaiveSetting<Double> setting) throws Exception {
    	super(setting);
    	// readTraingData is called from super, so dim is lost once back here 
    	dim = ((RealVectorPoint)trainingData.get(0)).getDim();
    }

	protected List< VdPoint<Double> > readTrainingDataFile() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(setting.getTrainingDataFileName()));
		// loop until file is finished
		String line = br.readLine();
		if(line==null) {
			System.out.println("It failed to read the header line of the training data file: "+setting.getTrainingDataFileName());
			throw new IOException("failed to read the header line of the training data file");
		}
		String[] token = line.split("\\s");
		dim = Integer.parseInt(token[0]);
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
	} // end of readTraningDataFile
	
	
}
