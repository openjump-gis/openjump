package de.fho.jump.pirol.utilities.FormulaParsing.Operations;


/**
 * Class to handle additions within a formula.
 *
 *  10 dic 2011 
 *  @author Giuseppe Aruta
 *  
 * Degrees to Radians class
 * 
 **/


import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

public class ToRadiansOperation extends FormulaValue {
	 protected FormulaValue value = null;
	 
	    public ToRadiansOperation(FormulaValue value) {
	        super();
	        this.value = value;
	        	    }

	   
	    public double getValue(Feature feature)  {
	    	
	        //return StrictMath.toRadians(this.value.getValue(feature));
	    	return this.value.getValue(feature) / (57.295779513);
	    }
	    
	    public boolean isFeatureDependent() {
	        return this.value.isFeatureDependent();
	    }
	    
	    public String toString() {
	        return "Degrees to Radians("+ this.value.toString() +")";
	    }

		
	}

