/**
 * 
 */
package net.zhouji.vdetector;

import java.io.Serializable;
import java.util.Collection;

/**
 * Real vector point whose distance is defined as the square of Euclidean distance.
 * 
 * @author Zhou
 *
 */
public class RealVectorPoint implements VdPoint<Double>, Serializable {
	private static final long serialVersionUID = -6046611982201233778L;
	private double[] x;
    
    public int getDim() {
    	return x.length;
    }

	public Double distance(VdPoint<Double> point) {
		RealVectorPoint realVectorPoint = (RealVectorPoint)point;
		double d = 0;
		for(int i=0; i<x.length; i++) {
			double dx = x[i]-realVectorPoint.x[i];
			d += dx*dx;
		}
		return new Double(d);
	}

	/** Constructor to make a random point on [0,1]^dim. */
    public RealVectorPoint(int dim) {
		x = new double[dim];
		for(int i=0; i<dim; i++) x[i] = Math.random();
	}

    public RealVectorPoint(double a[]) {
		int dim = a.length;
		x = new double[dim];
		for(int i=0; i<dim; i++)x[i] = a[i];
	}

    public Double nearestDistance(Collection< VdPoint<Double> > col) {
    	double d = Double.MAX_VALUE;
    	for(VdPoint<Double> p: col) {
    		double dd = distance(p);
    		if(dd<d)
    			d = dd; 
    	}
    	return new Double(d);
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer("(");
    	for(int i=0; i<x.length-1; i++)sb.append(x[i]+", ");
    	sb.append(x[x.length-1]).append(")");
    	return sb.toString();
    }
    
    public double[] getData() { return x; }
}
