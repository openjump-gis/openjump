/*
 * Created on 23.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FormulaParsing.Operations;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

/**
 * Class to handle additions within a formula.
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
public class AdditionOperation extends GenericOperation {

    /**
     * Sets the value, that will be operated on.
     */
    public AdditionOperation(FormulaValue value1, FormulaValue value2) {
        super(value1, value2);
        this.opString = "+";
    }

    /**
     * Returns the summarized values of the sub-values or sub-operations of this operation
     * @param feature the Feature to get the value from
     * @return summarized values of the sub-values or sub-operations
     */
    public double getValue(Feature feature) {
        return this.value1.getValue(feature) + this.value2.getValue(feature);
    }

}
