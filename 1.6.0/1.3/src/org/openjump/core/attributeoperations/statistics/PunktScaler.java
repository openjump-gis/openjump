/*
 * Created on 18.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: PunktScaler.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.attributeoperations.statistics;

import org.openjump.core.graph.pirolProject.PirolPoint;
import org.openjump.core.graph.pirolProject.ScaleChanger;

/**
 * class to scale the "coordinates" of a punkt object, often needed for statistical calculations.
 * Scales the given "coordinates" to values between 0 and 1.
 *
 * @author Ole Rahn
 * @author FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * Project: PIROL (2005),
 * Subproject: Daten- und Wissensmanagement
 * 
 * @see de.fhOsnabrueck.jump.pirol.utilities.statistics.CorrelationCoefficients
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments
 */
public class PunktScaler implements ScaleChanger {


    protected int dimension = 0;
    protected double[] mins = null;
    protected double[] ranges = null;

    public PunktScaler(int dimension, double[] mins, double[] ranges) {
        super();
        this.dimension = dimension;
        this.mins = mins;
        this.ranges = ranges;
    }
    
    public PunktScaler(PirolPoint[] punktArray) {
        super();
        try {
            this.getValuesFromArray(punktArray);
        } catch (Exception e) {
            //logger.printWarning(e.getMessage());
        }
    }
    
    protected void getValuesFromArray(PirolPoint[] array) throws Exception{
        if (array.length == 0){
            //logger.printError("no points in array - can not scale!");
            return;
        }
        PirolPoint pkt = array[0];
        this.dimension = pkt.getDimension();
        
        this.mins = new double[this.dimension];
        this.ranges = new double[this.dimension];
        double[] maxs = new double[this.dimension];
        
        for (int dim=0; dim<this.dimension; dim++){
            this.mins[dim] = Double.MAX_VALUE;
            this.ranges[dim] = 0;
            maxs[dim] = -1.0 * (Double.MAX_VALUE - 1);
        }
        
        double value;
        
        for (int i=0; i<array.length; i++){
            pkt = array[i];
            pkt.setScaler(this);
            for (int dim=0; dim<this.dimension; dim++){
                value = pkt.getCoordinate(dim);
                
                if (value < this.mins[dim])
                    this.mins[dim] = value;
                if (value > maxs[dim])
                    maxs[dim] = value;
            }
        }
        
        for (int dim=0; dim<this.dimension; dim++){
            this.ranges[dim] = maxs[dim] - this.mins[dim];
        }
    }
    
    public double scale(double value, int dimension) {
		if (dimension < this.dimension){
			return (value - this.mins[dimension])/this.ranges[dimension];
		}
		return Double.NaN;
	}

	public double unScale(double value, int dimension) {
		if (dimension < this.dimension){
			return (value * this.ranges[dimension]) + this.mins[dimension];
		}
		return Double.NaN;
	}

}
