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
import javax.swing.text.JTextComponent;

/**
 * Action add the text of a given text field to a text area on action performed.
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
public class AddTextFieldTextToTextAreaOnClick_Action extends AbstractAction {

    private static final long serialVersionUID = -6699586829803392059L;

    protected JTextComponent textFrom = null, textTo = null;
    

    public AddTextFieldTextToTextAreaOnClick_Action(JTextComponent textFrom,
            JTextComponent textTo, String actionName) {
        super();
        this.textFrom = textFrom;
        this.textTo = textTo;
        this.putValue(AbstractAction.NAME, actionName);
    }
    
    /**
     *@inheritDoc
     */
    public void actionPerformed(ActionEvent arg0) {
        String number = textFrom.getText();
        number = number.replaceAll(",", ".");
        if (textTo.getText().length() != 0)
            number = " " + number;
        textTo.setText(textTo.getText() + number);
        textFrom.setText("");
    }

}
