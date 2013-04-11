/*
 * Created on 04.01.2005
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2446 $
 *  $Date: 2006-09-12 14:57:25 +0200 (Di, 12 Sep 2006) $
 *  $Id: CoordinateComparator.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 */
package org.openjump.core.apitools.comparisonandsorting;

import java.util.Comparator;

/**
 * 
 * Comparator class for Sortable objects, sets comparision criteria
 * on the fly -> no need to set them manually before sorting
 *
 * @author orahn
 *
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 *  
 * @see Sortable
 * 
 */

public class CoordinateComparator implements Comparator {
    public static int SORTFOR_X = 0;
	public static int SORTFOR_Y = 1;
	public static int SORTFOR_Z = 2;
	/* added by oster
	 * this is useful to sort a point field for booth x and y
	 */
	public static int SORTFOR_XY = 3;
	
	protected int sortFor = CoordinateComparator.SORTFOR_X;
    
    public CoordinateComparator(int sortFor) {
        this.sortFor = sortFor;
    }
    
    public int getSortFor() {
        return sortFor;
    }
    public void setSortFor(int sortFor) {
        this.sortFor = sortFor;
    }
    
    public int compare(Object arg0, Object arg1) {
        return this.compare((Sortable)arg0, (Sortable)arg1);
    }
    
    public int compare(Sortable one, Sortable two) {
        
        one.setSortFor(this.sortFor);
        two.setSortFor(this.sortFor);
        
        return one.compareTo(two);
    }


}
