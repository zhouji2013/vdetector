package net.zhouji.vdetector;

import java.io.Serializable;
import java.util.Collection;

/*
 * this is a key class. some properties may seems to be the detector set's group behavior,
 * but in fact detector based.
 */
public class Detector<T extends Comparable<T> > implements Serializable {
	private static final long serialVersionUID = -5891299383441663987L;

	public String toString() {
		return center+":"+radius;
	}
	
    private VdPoint<T> center;
    private T radius;
    
    // let us prevent a detector without specifying center and radius
    @SuppressWarnings("unused")
	private Detector(){}
   
    Detector(VdPoint<T> c, T r) { center = c; radius = r; }
    public boolean match(VdPoint<T> p2) {
		if( center.distance(p2).compareTo(radius)<=0 ) {
    		return true; 
    	} else { 
    		return false; 
    	}
    }

    static public <D extends Comparable<D> > boolean match (Collection<Detector<D>> set, VdPoint<D> point) {
    	if(set.size()==0)return false;
    	
    	for(Detector<D> detector: set) {
    		if(detector.match(point))return true;
    	}
    	return false;
    }
    
    public VdPoint<T> getCenter() {return center;}
    public T getRadius() {return radius ; }

}
