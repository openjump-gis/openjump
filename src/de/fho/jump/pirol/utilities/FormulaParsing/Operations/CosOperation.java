package de.fho.jump.pirol.utilities.FormulaParsing.Operations;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;


/**
 * Class to handle additions within a formula.
 *
 *  10 dic 2011 
 *  @author Giuseppe Aruta
 *  
 * Cosine class
 * 
 **/

public class CosOperation extends FormulaValue {
	 protected FormulaValue value = null;
	 
	    public CosOperation(FormulaValue value) {
	        super();
	        this.value = value;
	    }

	   
	    public double getValue(Feature feature)  {
	        return StrictMath.cos(this.value.getValue(feature));
	         }
	    
	    public boolean isFeatureDependent() {
	        return this.value.isFeatureDependent();
	    }
	    
	    public String toString() {
	        return "StrictMath.cos("+ this.value.toString() +")";
	    }

		
	}
