package de.fho.jump.pirol.utilities.FormulaParsing.Operations;

import com.vividsolutions.jump.feature.Feature;


import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

/**
 * Class to handle additions within a formula.
 *
 *  10 dic 2011 
 *  @author Giuseppe Aruta
 *  
 * Arccosine class
 *  
 **/

public class AcosOperation extends FormulaValue {
	 protected FormulaValue value = null;
	 
	    public AcosOperation(FormulaValue value) {
	        super();
	        this.value = value;
	    }

	   
	    public double getValue(Feature feature)  {
	        return StrictMath.acos(this.value.getValue(feature));
	    }
	    
	    public boolean isFeatureDependent() {
	        return this.value.isFeatureDependent();
	    }
	    
	    public String toString() {
	        return "StrictMath.acos("+ this.value.toString() +")";
	    }

		
	}
