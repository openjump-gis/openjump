/*
 * Created on 28.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.plugins.EditAttributeByFormula;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;

import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * Action to add a button's text to a given JTextArea, when the button is pressed.
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
public class AddFormulaPartToTextArea_Action extends AbstractAction {

    private static final long serialVersionUID = 3157240050072519462L;

    protected JTextArea textArea = null;
    protected String[] mathSigns = null;
    protected String formulaPart = null;
    
    protected boolean isMathSign = false;
    
    protected FeatureSchema featureSchema = null;
    
    
    /**
     * 
     *@param textArea text area to add the button text to
     */
    public AddFormulaPartToTextArea_Action(String formulaPart, JTextArea textArea, String[] mathSigns, FeatureSchema featureSchema) {
        super();
        this.textArea = textArea;
        this.mathSigns = mathSigns;
        this.formulaPart = formulaPart;
        
        this.isMathSign = this.isOperator(this.formulaPart);
        
        this.featureSchema = featureSchema;
        
        this.putValue(AbstractAction.NAME, this.formulaPart);
        this.putValue(AbstractAction.SHORT_DESCRIPTION, this.formulaPart);
    }
    
    protected boolean isOperator(String op){
        for (int i=0; i<this.mathSigns.length; i++){
            if (op.equals(this.mathSigns[i])) return true;
        }
        return false;
    }
    
    /**
     *@inheritDoc
     */
    public void actionPerformed(ActionEvent event) {
        String buttonText = this.formulaPart;
        
        if (this.featureSchema.hasAttribute(buttonText) && buttonText.indexOf(" ")>-1){
            buttonText = "\"" + buttonText + "\"";
        }
        
        if (textArea.getText().length() != 0 && buttonText.length() != 0)
            buttonText = " " + buttonText;
        
        if (buttonText.length() != 0){
            boolean formulaOk = true;
            
            // TODO: prüfen ob zwei operatoren oder zwei operanden hintereinander benutzt wurden...
            
            if (formulaOk){
                //this.textArea.append(buttonText);
                this.textArea.setText(this.textArea.getText() + buttonText);
            }
        }
    }

}
