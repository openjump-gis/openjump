/*
 * Created on 04.01.2005
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2446 $
 *  $Date: 2006-09-12 14:57:25 +0200 (Di, 12 Sep 2006) $
 *  $Id: Sortable.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 */
package org.openjump.core.apitools.comparisonandsorting;

/**
 * 
 * Abstract base class for sortable objects like punkt object.
 * Adds a natural ordering to those objects and allows to change what to sort for.
 * 
 * @author orahn
 *
 * FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * Project PIROL 2005
 * Daten- und Wissensmanagement
 * 
 * @see PirolPoint
 */
public abstract class Sortable implements Comparable {
	protected int sortFor = CoordinateComparator.SORTFOR_X;
    
    public abstract int getSortFor();

    public abstract void setSortFor(int sortFor);

    public abstract int compareTo(Object arg0);
}