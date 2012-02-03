/*
 * Created on 23.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.FormulaParsing;

import com.vividsolutions.jump.feature.Feature;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * Base class for each sub-formula or value of a formula, since we don't want to parse the formula again and again for each value...
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
public abstract class FormulaValue {
    
    protected PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);

    /**
     * Returns the value (as a double) of this part of the formula.
     * It may be the rsult of a sub-formula, a feature-specific attribute value or just a constant value...
     * Since the value may depend on a feature, we give the feature to the method to get a unified interface...
     *@return value of this part of the formula
     */
    public abstract double getValue(Feature feature);
    
    /**
     * Helps to determine, if the value depends on a feature's attribute value. 
     * @return true, if the value depends on a feature
     */
    public abstract boolean isFeatureDependent();
    
    /**
     * @inheritDoc
     */
    public String toString(){
        return "";
    }

}
