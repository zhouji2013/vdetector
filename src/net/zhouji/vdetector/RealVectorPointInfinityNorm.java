/**
 * 
 */
package net.zhouji.vdetector;

/**
 * 
 * @author Zhou
 *
 */
public class RealVectorPointInfinityNorm extends RealVectorPoint {
	private static final long serialVersionUID = 1293308908038904324L;

	RealVectorPointInfinityNorm(int dim) {
		super(dim);
	}

	@Override
	public Double distance(VdPoint<Double> point) {
		RealVectorPointInfinityNorm p2 = (RealVectorPointInfinityNorm)point;
		double d = 0;
		double[] x1 = getData();
		double[] x2 = p2.getData();
		for(int i=0; i<x1.length; i++) {
			double dx = Math.abs(x1[i]-x2[i]);
			if(dx>d)d=dx;
		}
		return new Double(d);
	}

}
