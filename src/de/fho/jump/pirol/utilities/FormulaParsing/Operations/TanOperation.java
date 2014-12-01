package de.fho.jump.pirol.utilities.FormulaParsing.Operations;


/**
 * Class to handle additions within a formula.
 *
 *  10 dic 2011 
 *  @author Giuseppe Aruta
 *  
 * Tangent class
 * 
 **/

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

public class TanOperation extends FormulaValue {
	 protected FormulaValue value = null;
	 
	    public TanOperation(FormulaValue value) {
	        super();
	        this.value = value;
	    }

	   
	    public double getValue(Feature feature)  {
	    	
	    	
	    	return StrictMath.tan(this.value.getValue(feature));
	    }
	    
	    public boolean isFeatureDependent() {
	        return this.value.isFeatureDependent();
	    }
	    
	    public String toString() {
	        return "StrictMath.tan("+ this.value.toString() +")";
	    }

		
	}

