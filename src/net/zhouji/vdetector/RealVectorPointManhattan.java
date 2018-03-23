/**
 * 
 */
package net.zhouji.vdetector;

/**
 * 
 * @author Zhou
 *
 */
public class RealVectorPointManhattan extends RealVectorPoint {
	private static final long serialVersionUID = 4288959477927780967L;

	RealVectorPointManhattan(int dim) {
		super(dim);
	}

	@Override
	public Double distance(VdPoint<Double> p2) {
		RealVectorPointManhattan pp2 = (RealVectorPointManhattan)p2;
		double d = 0;
		double[] x1 = getData();
		double[] x2 = pp2.getData();
		for(int i=0; i<x1.length; i++) 
		{
			d += Math.abs(x1[i]-x2[i]);
		}
		return new Double(d);
	}

}
