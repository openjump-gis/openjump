/*
 * Created on 23.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FormulaParsing.Values;

import org.openjump.core.apitools.comparisonandsorting.ObjectComparator;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

/**
 * Class to extract integer or double values (as double) out of the given Feature...
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 */
public class AttributeValue extends FormulaValue {
    
    protected String attributeName = "";
    protected int attributeIndex = -1;

    public AttributeValue(String attributeName) {
        super();
        this.attributeName = attributeName;
    }
    
    /**
     * Gets the value (as a double) of the specified attribute out of the given feature.
     *@param feature the Feature we want to get the attribute value from
     *@return value of the specified attribute
     */
    public double getValue(Feature feature) {
        if (this.attributeIndex < 0){
            this.attributeIndex = feature.getSchema().getAttributeIndex(this.attributeName);
        }
        return ObjectComparator.getDoubleValue(feature.getAttribute(this.attributeIndex));
    }

    /**
     * @inheritDoc
     */
    public boolean isFeatureDependent() {
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public String toString(){
        return this.attributeName;
    }

}
