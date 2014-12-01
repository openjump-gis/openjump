/*
 * Created on 11.04.2005 for PIROL
 * 
 * SVN header information:
 * $Author$
 * $Rev$
 * $Date$
 * $Id$
 */
package de.fho.jump.pirol.ui.panels;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.vividsolutions.jump.I18N;

//import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;

/**
 * This class is a JPanel with a "Cancel" and a "OK" button.
 * @author Carsten Schulze
 * @author FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * Project: PIROL (2005),
 * Subproject: Daten- und Wissensmanagement
 */
public class OkCancelButtonPanel extends JPanel {
	
    private static final long serialVersionUID = -4703181650847522122L;
    
    /**The constant ActionCommand String for the ok-button*/
	public static final String OK_BUTTON_ACTION_COMMAND = new String("OK_BUTTON_ACTION_COMMAND");
	/**The constant ActionCommand String for the cancel-button*/
	public static final String CANCEL_BUTTON_ACTION_COMMAND = new String("CANCEL_BUTTON_ACTION_COMMAND");
	private JButton cancelButton;
	private JButton okButton;
	/**
	 * This is the default constructor
	 */
	public OkCancelButtonPanel() {
		super();
		initialize();
	}
	/**
	 * Adds the given ActionListener to both buttons.
	 * @param listener the listener
	 */
	public void addActionListener(ActionListener listener){
		getOkButton().addActionListener(listener);
		getCancelButton().addActionListener(listener);
	}
	/**
	 * This method initializes cancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	public JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(I18N.get("ui.OKCancelPanel.cancel"));
			cancelButton.setActionCommand(CANCEL_BUTTON_ACTION_COMMAND);
			cancelButton.setFocusPainted(false);
		}
		return cancelButton;
	}
	
	/**
	 * This method initializes okButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	public JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(I18N.get("ui.OKCancelPanel.ok"));
			okButton.setActionCommand(OK_BUTTON_ACTION_COMMAND);
			okButton.setFocusPainted(false);
		}
		return okButton;
	}
	/**
	 * Enables/Disables the ok button. May be useful, if the user had to put in values
	 * that may not be correct. You can disable the ok button in this case.
	 * @param enable enables the ok button if true, else disables it
	 */
	public void setOkButtonEnabled(boolean enable){
	    this.getOkButton().setEnabled(enable);
	}
	/**
	 * This method initializes this
	 */
	private  void initialize() {
		this.setSize(300,40);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.add(Box.createHorizontalGlue(),null);
		this.add(getCancelButton(), null);
		this.add(Box.createRigidArea(new Dimension(10,0)));
		this.add(getOkButton(), null);
		
		this.requestFocus();
	}
	
	public void requestFocus() {
		okButton.setFocusable(true);
		okButton.requestFocus();		
	}
	
}
