/**
 * 
 */
package net.zhouji.vdetector;

/**
 * 
 * @author Zhou
 *
 */
public class RealVectorPointMinkowski extends RealVectorPoint {
	private static final long serialVersionUID = -1173280534202268955L;

	RealVectorPointMinkowski(int dim) {
		super(dim);
	}
	
	static private int norm = 2;
	
	public static void setNorm(int n) {norm = n;}

	@Override
	public Double distance(VdPoint<Double> point) {
		RealVectorPointMinkowski p2 = (RealVectorPointMinkowski)point;
		double d = 0;
		double[] x1 = getData();
		double[] x2 = p2.getData();
		for(int i=0; i<x1.length; i++) {
			double dx = Math.abs(x1[i]-x2[i]);
			d += Math.pow(dx, norm);
		}
		return new Double(Math.pow(d, 1./norm));
	}

}
