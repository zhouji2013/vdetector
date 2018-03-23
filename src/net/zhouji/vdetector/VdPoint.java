/**
 * 
 */
package net.zhouji.vdetector;

import java.util.Collection;

/**
 * A abstract point in V-detector.
 * 
 *  This is not named 'Point' to avoid confusion with java.awt.Point
 *  
 * @author Zhou
 *
 */
public interface VdPoint<T extends Comparable<T>> {
    public Comparable<T> distance(VdPoint<T> p2);
    public T nearestDistance(Collection< VdPoint<T> > col);
}




