/*
 * Created on 20.06.2005
 *
 * CVS information:
 *  $Author: javamap $
 *  $Date: 2007-06-18 22:15:27 -0600 (Mo, 18 Jun 2007) $
 *  $ID$
 *  $Rev: 856 $
 *  $Id: OKCancelListener.java 856 2007-06-19 04:15:27Z javamap $
 *  $Log$
 *  Revision 1.2  2007/02/03 14:19:29  mentaer
 *  modified debug output for pirol stuff
 *
 *  Revision 1.1  2006/11/23 18:53:51  mentaer
 *  added EditAttributeByFormula Plugin by Pirol including some parts of the baseclasses - note: plugin needs java 1.5
 *
 *  Revision 1.4  2006/01/09 12:55:24  orahn
 *  korrektere Fehlerausgabe
 *
 *  Revision 1.3  2005/07/06 13:02:46  orahn
 *  Code clean up, improve performance (?)
 *
 *  Revision 1.2  2005/06/30 10:43:30  orahn
 *  Standard ok/cancel Listener added the possibility of the OK button to function except when the input value is not valid...
 *
 *  Revision 1.1  2005/06/20 18:18:23  orahn
 *  Preparation for the Formula Editor
 *
 */
package org.openjump.core.ui.swing.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.openjump.core.ui.swing.OkCancelButtonPanel;
import org.openjump.core.ui.swing.ValueChecker;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * 
 * Class that implements a default Action Listener behavior for
 * an OKCancelButtonPanel. It remembers if ok was clicked and closes 
 * a given dialog.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 856 $
 */
public class OKCancelListener implements ActionListener {
    
    protected boolean okWasClicked = false;
    
    protected JDialog dialog = null;
    
    protected PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    
    protected ArrayList valueCheckers = new ArrayList();

    

    /**
     * @param dialog dialog to be closed after ok or cancel was clicked.
     */
    public OKCancelListener(JDialog dialog) {
        super();
        this.dialog = dialog;
    }
    
    /**
     * The first invokation of this method enables value checking (enables/disables funtionality of the ok button) 
     *@param valChecker object that checks if the given value are ok or not
     */
    public void addValueChecker( ValueChecker valChecker ){
        this.valueCheckers.add(valChecker);
    }
    
    /**
     * @inheritDoc
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent arg0) {
        try {
            JButton button = (JButton)arg0.getSource();
            
            if (button.getActionCommand().equals(OkCancelButtonPanel.OK_BUTTON_ACTION_COMMAND)){
                
                // disable ok button, if value are not ok!
                if (!this.valuesOk()) return;
                
                this.okWasClicked = true;
            }
            
            if (this.dialog != null){
                this.dialog.setVisible(false);
                this.dialog.dispose();
            }
            
        } catch (ClassCastException e){
            this.logger.printError(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * asks the existent value checkers (if any), if the values are ok
     *@return true if values are ok, else false
     */
    protected boolean valuesOk(){
        if (this.valueCheckers.isEmpty()) return true;
        
        boolean valsOk = true;
        ValueChecker[] valueCheckerArray = (ValueChecker[])this.valueCheckers.toArray(new ValueChecker[0]);
        
        for (int i=0; i<valueCheckerArray.length; i++){
            valsOk = valsOk && ( valueCheckerArray[i].areValuesOk() );
        }
        
        return valsOk;
    }

    /**
     * Tells you, if ok was clicked to close the dialog
     * @return Returns the okWasClicked.
     */
    public boolean wasOkClicked() {
        return okWasClicked;
    }
}
