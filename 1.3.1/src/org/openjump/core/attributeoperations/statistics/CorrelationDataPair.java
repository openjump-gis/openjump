/*
 * Created on 18.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: CorrelationDataPair.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.attributeoperations.statistics;

import org.openjump.core.graph.pirolProject.PirolPoint;
import org.openjump.core.graph.pirolProject.ScaleChanger;

/**
 * 
 *
 * @author Ole Rahn
 * @author FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * Project: PIROL (2005),
 * Subproject: Daten- und Wissensmanagement
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments
 */
public class CorrelationDataPair extends PirolPoint {

    //protected PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);

    /**
     *@param coords
     */
    public CorrelationDataPair(double[] coords) {
        super(coords);
    }

    /**
     *@param coords
     *@param index
     */
    public CorrelationDataPair(double[] coords, int index) {
        super(coords, index);
    }

    /**
     *@param coords
     *@param index
     *@param scaler
     *@param prescaled
     */
    public CorrelationDataPair(double[] coords, int index, ScaleChanger scaler,
            boolean prescaled) {
        super(coords, index, scaler, prescaled);
    }

    /**
     *@param coords
     *@param index
     *@param scaler
     */
    public CorrelationDataPair(double[] coords, int index, ScaleChanger scaler) {
        super(coords, index, scaler);
    }
    
    /**
     * function to compare value of a (scaled) data pair
     *@param valIndex1 index of first value to compare
     *@param valIndex2 index of first value to compare
     *@return 1 if first value > second value, 0 if equal, else -1  
     * @throws Exception
     */
    public int compareValues(int valIndex1, int valIndex2) throws Exception{
        double value1, value2;

        value1 = this.getCoordinate(valIndex1);
        value2 = this.getCoordinate(valIndex2);

        if (value1 > value2) return 1;
        else if (value1==value2) return 0;
        else return -1;
    }

}
