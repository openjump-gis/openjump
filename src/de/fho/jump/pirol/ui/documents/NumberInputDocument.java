/*
 * Created on 13.01.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.ui.documents;

import java.text.DecimalFormatSymbols;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * 
 * TODO: comment class
 *
 * @author Ole Rahn, Stefan Ostermann
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 *
 */
public class NumberInputDocument extends PlainDocument {

    private static final long serialVersionUID = 8158679380473471643L;
    
    protected String actionCommand = "";
    
    public String getActionCommand() {
        return actionCommand;
    }
    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }
	
	public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    	char decimalSeparator = dfs.getDecimalSeparator();
		String clearedStr = str.substring(0);

        if (this.getText(0, this.getLength()).indexOf('.') > -1){
            clearedStr = clearedStr.replaceAll("[^0-9-]","");
        } else if ( decimalSeparator!='.' && this.getText(0, this.getLength()).indexOf(decimalSeparator) > -1) {
        	clearedStr = clearedStr.replaceAll("[^0-9-]","");
        } else {
            clearedStr = clearedStr.replaceAll("[^0-9-."+decimalSeparator+"]","");
        }
        
        if (clearedStr.length() > 0){
            super.insertString(offs, clearedStr, a);
        }
    }
}
