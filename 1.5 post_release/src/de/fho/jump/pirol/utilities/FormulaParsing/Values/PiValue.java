package de.fho.jump.pirol.utilities.FormulaParsing.Values;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

public class PiValue extends FormulaValue {
    
    protected double value;

    public PiValue(double value) {
        super();
        this.value = value;
    }
        public double getValue(Feature feature) {
            return Math.PI;
        }
        
        /**
         * @inheritDoc
         */
        public boolean isFeatureDependent() {
            return false;
        }
        
        /**
         * @inheritDoc
         */
        public String toString(){
            return "PI";
        }

    }
