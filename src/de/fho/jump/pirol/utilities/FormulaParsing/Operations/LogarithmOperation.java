package de.fho.jump.pirol.utilities.FormulaParsing.Operations;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

/**
 * Class to handle Math.log() like operations. The result is a value that equals
 * <code>Math.log(value1)</code>.
 *
 * @author Giuseppe Aruta
 * <br>
 * <br>GeoArbores - http://sourceforge.net/projects/opensit/
 * <br>Project: PIROL (2005),
 * 
 * @version $Rev: 001 $
 * 
 */
public class LogarithmOperation extends FormulaValue {
	 protected FormulaValue value = null;
	 
    public LogarithmOperation(FormulaValue value) {
        super();
        this.value = value;
    }

   
    public double getValue(Feature feature)  {
        return Math.log(this.value.getValue(feature));
    }
    
    public boolean isFeatureDependent() {
        return this.value.isFeatureDependent();
    }
    
    public String toString() {
        return "Math.log("+ this.value.toString() +")";
    }

	
}
