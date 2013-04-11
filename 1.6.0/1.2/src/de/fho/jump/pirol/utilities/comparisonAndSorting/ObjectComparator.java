/*
 * Created on 08.12.2004
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.comparisonAndSorting;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 */
public class ObjectComparator {
    public static PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    
	public static int compare( Object o1, Object o2 ){
		
		Double value1, value2;
		
		value1 = new Double(ObjectComparator.getDoubleValue(o1));
		value2 = new Double(ObjectComparator.getDoubleValue(o2));
		
		if (value1.doubleValue() ==Double.NaN || value2.doubleValue()==Double.NaN)
		    logger.printError("got NAN");
		
		return value1.compareTo(value2);		
	}

    /**
     * Method to generate a <code>double</code> value out of different number objects.
     *@param o
     *@return a double value representing to given object or <code>Double.NAN</code> if it can't be parsed
     */
	public static double getDoubleValue(Object o){
		double value = Double.NaN;
		
		if (o==null){
		    logger.printMinorError("got NULL value");
		} else {
			if (Integer.class.isInstance(o)){
				value = ((Integer)o).doubleValue();
			} else if (Double.class.isInstance(o)){
				value = ((Double)o).doubleValue();
            } else if (Float.class.isInstance(o)){
                value = ((Float)o).doubleValue();
			} else if (BigDecimal.class.isInstance(o)){
                value = ((BigDecimal)o).doubleValue();
            } else if (BigInteger.class.isInstance(o)){
                value = ((BigInteger)o).doubleValue();
            } else if (Long.class.isInstance(o)){
                value = ((Long)o).doubleValue();
            } else if (Short.class.isInstance(o)){
                value = ((Short)o).doubleValue();
            } else if (Byte.class.isInstance(o)){
                value = ((Byte)o).doubleValue();
            } else if (String.class.isInstance(o)){
                value = Double.parseDouble(o.toString());
            } else {
			    logger.printError(" can't get double value... - " + o.getClass().getName());
			}
		}
		return value;
	}
}
