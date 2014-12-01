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

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

import de.fho.jump.pirol.utilities.FormulaParsing.Operations.AdditionOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Operations.DivisionOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Operations.MultiplicationOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Operations.PowerOfOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Operations.SquareRootOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Operations.SubtractionOperation;
import de.fho.jump.pirol.utilities.FormulaParsing.Values.AttributeValue;
import de.fho.jump.pirol.utilities.FormulaParsing.Values.ConstantValue;
import de.fho.jump.pirol.utilities.attributes.AttributeInfo;
import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;
import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;

/**
 * This class is a utility to parse formulas, that describe how an additional attribute value is to be calculated on a by feature basis. 
 * Formulas thereby can contain constant values as well as attribute values, that need to be extracted for each feature.<br><br>
 * 
 * Formulas are exspected to be space-separated: Each attribute name, constant value, bracket or operator has to be surrounded by empty spaces.<br><br>
 * A valid formulas (for a FeatureSchema that has the attributes "yield" and "grain mois") would look like this: <br><code>( 4 + 6 ) * yield</code><br> or<br><code>grain mois / 2</code>.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 * @see de.fhOsnabrueck.jump.pirol.utilities.FeatureCollectionTools#applyFormulaToFeatureCollection(FeatureCollection, AttributeInfo, FormulaValue, boolean)
 * 
 */
public class FormulaParser {
    
    protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    public static final String KEY_SQRT = "sqrt:";
    public static final String KEY_POW = "power:";
    
    /**
     * Recursively parses a given (sub-) formula into a FormulaValue, which can be an operation with
     * sub-FormularValues or a value. 
     *@param formula
     *@param featSchema The feature schema to check attribute names, if neccessary
     *@return the given formula parsed into a FormulaValue or null if the given String did not contain formula information
     */
    public static FormulaValue getValue(String formula, FeatureSchema featSchema){
        logger.printDebug("parsing: " + formula);
        
        // kick out leading or trailing whitespaces
        formula = formula.trim();
        
        FormulaValue value1 = null, value2 = null, theValue = null;
        
        // kick out brackets that surround the whole formula
        if (formula.startsWith("(") && formula.endsWith(")"))
            formula = FormulaParser.kickOutSurroundingBracket(formula);
        
        // if there is nothing to parse, return null
        if (formula.length() == 0)
            return null;
        
        
        // result should be { [formula/value], [operator], [formula/value]  }
        String[] operation = FormulaParser.splitToFirstLevelOperation(formula);
        
        if (operation[1] == null){
            // "formula" is really just a value!
            try {
                double value = Double.parseDouble(operation[0]);
                logger.printDebug("got value: " + value);
                theValue = new ConstantValue(value);
            } catch (Exception e) {
                // value could not be parsed -> is it an attribute??
                if (featSchema.hasAttribute(operation[0]) || (operation[0].startsWith("\"") && operation[0].endsWith("\"")) ){
                    // yes! it's an attribute!
                    String attrName = operation[0];
                    if ( attrName.startsWith("\"") && attrName.endsWith("\"") ) {
                        attrName = attrName.substring(1, attrName.length() - 1);
                    }
                    
                    if (featSchema.hasAttribute(attrName)) {
                        theValue = new AttributeValue(attrName);
                    } else {
                        logger.printError("could not parse: " + attrName);
                        throw new IllegalArgumentException( PirolPlugInMessages.getString("do-not-know-how-to-parse") + ": >" + attrName + "<");
                    }
                        
                } else if (operation[0].trim().startsWith(FormulaParser.KEY_SQRT)){
                    theValue = new SquareRootOperation(FormulaParser.getValue(operation[0].substring(FormulaParser.KEY_SQRT.length()+1).trim(), featSchema));
                } else if (operation[0].trim().startsWith(FormulaParser.KEY_POW)){
                    String theTwoValuesStr = operation[0].trim().substring(FormulaParser.KEY_POW.length()+1).trim();
                    
                    if (theTwoValuesStr.indexOf(",") < 0){
                        logger.printError("damaged power of operation, can not determine exponent: >" + operation[0] + "<");
                        throw new IllegalArgumentException("damaged power of operation, can not determine exponent: >" + operation[0] + "<");
                    }
                    
                    String value1Str = theTwoValuesStr.substring(0,theTwoValuesStr.indexOf(",")).trim();
                    String value2Str = theTwoValuesStr.substring(theTwoValuesStr.indexOf(",")+1).trim();
                    
                    theValue = new PowerOfOperation(FormulaParser.getValue(value1Str, featSchema), FormulaParser.getValue(value2Str, featSchema));
                } else {
                    logger.printError("could not parse: " + operation[0]);
                    throw new IllegalArgumentException( PirolPlugInMessages.getString("do-not-know-how-to-parse") + ": >" + operation[0] + "<");
                }
            }
        } else {
            value1 = FormulaParser.getValue(operation[0], featSchema);
            value2 = FormulaParser.getValue(operation[2], featSchema);
            
            if (operation[1].length()!=1)
                logger.printWarning("corrupted operator (?): " + operation[1]);
            
            char op = operation[1].charAt(0);
            
            switch (op){
            	case '*':
            	    theValue = new MultiplicationOperation(value1, value2);
            	    break;
            	case '/':
            	    theValue = new DivisionOperation(value1, value2);
            	    break;
            	case '+':
            	    theValue = new AdditionOperation(value1, value2);
            	    break;
            	case '-':
            	    theValue = new SubtractionOperation(value1, value2);
            	    break;
            	default:
            	    logger.printError("unknown operator found: " + op);
            		throw new IllegalArgumentException("unknown operator found: " + op);
            
            }
            
            if (!theValue.isFeatureDependent()){
                // identify sub-formulas that consist of constant values and turn them
                // into ConstantValue object, to speed up processing
                logger.printDebug("found constant parts: " + theValue.toString());
                theValue = new ConstantValue(theValue.getValue(null));
            }
        }
        
        return theValue;
        
    }
    
    protected static boolean isOperator( String op ){
        return (op.equals("*") || op.equals("/") || op.equals("+") || op.equals("-"));
    }
    
    protected static boolean isBracket( String brack ){
        return (brack.equals("(") || brack.equals(")"));
    }
    
    protected static int findFirstOccuranceOutsideABracket(String toBeFound, String formula, int fromIndex){
        char[] characters = formula.toCharArray();
        char char2bFound = toBeFound.charAt(0);
        
        if (toBeFound.length() != 1)
            logger.printWarning("string does not seem to be an operator");
        
        int bracketOpen = 0, bracketClose = 0;
        int numQuote = 0;
        
        for (int i=Math.max(0, fromIndex); i<characters.length; i++){
            
            if (characters[i] == '(') bracketOpen++;
            else if (characters[i] == ')') bracketClose++;
            else if (characters[i] == '\"') numQuote++;

            else if (characters[i]==char2bFound && bracketOpen==bracketClose && numQuote%2==0) return i;
        }
        return -1;
    }
    
    protected static int findFirstAddSubOperatorOutsideABracket(String formula, int fromIndex){
        int firstAddOperator = FormulaParser.findFirstOccuranceOutsideABracket("+", formula, fromIndex);
        int firstSubOperator = FormulaParser.findFirstOccuranceOutsideABracket("-", formula, fromIndex);
        
        return ( firstAddOperator > -1 && firstSubOperator > -1 )?Math.min(firstAddOperator,firstSubOperator):Math.max(firstAddOperator,firstSubOperator);
    }
    
    protected static int findFirstMultiDivOperatorOutsideABracket(String formula, int fromIndex){
        int firstMultiOperator = FormulaParser.findFirstOccuranceOutsideABracket("*", formula, fromIndex);
        int firstDivOperator = FormulaParser.findFirstOccuranceOutsideABracket("/", formula, fromIndex);
        
        return ( firstMultiOperator > -1 && firstDivOperator > -1 )?Math.min(firstMultiOperator,firstDivOperator):Math.max(firstMultiOperator,firstDivOperator);
    }
    
    protected static String[] splitToFirstLevelOperation(String formula){
        String[] firstLevelOperation = new String[]{null, null, null};
        int firstMultiOrDivIndex = -1;
        int firstAddOrSubIndex = -1;
        int operatorIndex = -1;

        // are there multiplication/divsions??
        firstMultiOrDivIndex =  FormulaParser.findFirstMultiDivOperatorOutsideABracket(formula, -1); 
        firstAddOrSubIndex   =  FormulaParser.findFirstAddSubOperatorOutsideABracket(formula, -1);
        
        if ( firstMultiOrDivIndex < 0 && firstAddOrSubIndex < 0 ){
            // no operations - just a simple value!
            firstLevelOperation[0] = formula;
        } else { 
            
            if ((firstMultiOrDivIndex < 0 || firstAddOrSubIndex < 0)) {
                // just like operations, no priorities
                if (firstAddOrSubIndex > -1)
                    operatorIndex = firstAddOrSubIndex;
                else {
                    int firstMultiOperator = FormulaParser.findFirstOccuranceOutsideABracket("*", formula, -1);
                    int firstDivOperator = FormulaParser.findFirstOccuranceOutsideABracket("/", formula, -1);
                    
                    if (firstMultiOperator < 0){
                        operatorIndex = firstDivOperator;
                    } else {
                        operatorIndex = firstMultiOperator;
                    }
                }
            } else if (firstMultiOrDivIndex > -1 && firstAddOrSubIndex > -1) {
                // mixed operations, multiplications/divisions have priority! do not divide the formula there!
                operatorIndex = firstAddOrSubIndex;
            }

            firstLevelOperation[0] = formula.substring(0, operatorIndex).trim();
            firstLevelOperation[1] = formula.substring(operatorIndex, Math.min(operatorIndex + 2, formula.length())).trim();
            firstLevelOperation[2] = formula.substring(Math.min(operatorIndex + 2, formula.length())).trim();
            
            logger.printDebug("----");
            logger.printDebug(firstLevelOperation[0] +"; " + firstLevelOperation[1] + "; " + firstLevelOperation[2]);
            
        }
        
        return firstLevelOperation;
    }
    
    protected static String getFirstCompleteBracketString(String formula, int fromIndex){
        formula = formula.trim();
        
        char[] characters = formula.toCharArray();
        int bracketOpen = 0, bracketClose = 0, firstOpenPos = -1;
        
        for (int i=Math.max(0, fromIndex); i<characters.length; i++){
            if (characters[i] == '('){
                if (bracketOpen==0)
                    firstOpenPos = i;
                bracketOpen++;
            }
            else if (characters[i] == ')') bracketClose++;
            
            if ( (bracketOpen != 0 && bracketClose!=0) && i<(characters.length-1) && bracketOpen==bracketClose ){
                return formula.substring(firstOpenPos, i+1).trim();
            } 
        }
        
        if (bracketOpen!=bracketClose && fromIndex>-1){
            logger.printMinorError("damaged bracket found in: " + formula);
            throw new IllegalArgumentException("damaged bracket found in: " + formula);
        }
        
        return formula;
    }
    
    /**
     * deletes a bracket that surrounds the whole formula from the formula.
     *@param formula formula String
     *@return formula String without surrounding bracket
     */
    protected static String kickOutSurroundingBracket(String formula){
        formula = formula.trim();
        
        // first check if one bracket surrounds the whole formula
        char[] characters = formula.toCharArray();
        int bracketOpen = 0, bracketClose = 0;
        
        for (int i=0; i<characters.length; i++){
            if (characters[i] == '(') bracketOpen++;
            else if (characters[i] == ')') bracketClose++;
            
            if ( (bracketOpen != 0 && bracketClose!=0) && i<(characters.length-1) && bracketOpen==bracketClose ){
                // nope, the bracket does not surround the whole formula!
                return formula;
            } 
        }
        
        if (bracketOpen!=bracketClose){
            logger.printMinorError("damaged bracket found in: " + formula);
            throw new IllegalArgumentException("damaged bracket found in: " + formula);
        }
        
        // yes, seems like the bracket indeed surrounds the whole formula!
        return formula.substring(formula.indexOf("(")+1,formula.lastIndexOf(")")).trim();
    }

}
