/*
 * Created on 20.06.2005
 *
 * CVS information:
 *  $Author$
 *  $Date$
 *  $ID$
 *  $Rev$
 *  $Id$
 *
 */
package de.fho.jump.pirol.plugins.EditAttributeByFormula;

import java.awt.Frame;
import java.awt.BorderLayout;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.openjump.core.ui.swing.DialogTools;
import org.openjump.core.ui.swing.OkCancelButtonPanel;
import org.openjump.core.ui.swing.listener.OKCancelListener;
import org.openjump.io.PropertiesHandler;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;

import de.fho.jump.pirol.ui.panels.NewAttributePanel;
import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;
import de.fho.jump.pirol.utilities.attributes.AttributeInfo;

/**
 * 
 * Dialog to ask the user for information on the new attribute and for a formular,
 * that represents a blue print to generate attribute values with.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 */
public class EditAttributeByFormulaDialog extends JDialog {
    
    private static final long serialVersionUID = 3581710389701646491L;
    
    protected NewAttributePanel newAttrPanel = null;
    protected OkCancelButtonPanel okCancelPanel = null;
    protected FormulaEditingPanel formPanel = null;
    
    protected OKCancelListener okCancelListener = null;
    
    protected String text = null;
    
    protected FeatureSchema featureSchema = null;
    
    protected PropertiesHandler storedFormulas = null;

    /**
     * @param parentFrame
     * @param title
     * @param modal
     * @throws java.awt.HeadlessException
     */
    public EditAttributeByFormulaDialog(Frame parentFrame, String title, boolean modal, String text, FeatureSchema featureSchema, PropertiesHandler storedFormulas )
            throws HeadlessException {
        super(parentFrame, title, modal);
        
        this.text = text;
        this.featureSchema = featureSchema;
        this.storedFormulas = storedFormulas;
        
        this.okCancelListener = new OKCancelListener(this);
        
        this.setupUI();
    }
    
    //[sstein 24.March 2007] new - since we dont have stored formulas
    public EditAttributeByFormulaDialog(Frame parentFrame, String title, boolean modal, String text, FeatureSchema featureSchema)
    	throws HeadlessException {
    	super(parentFrame, title, modal);

    	this.text = text;
    	this.featureSchema = featureSchema;

    	this.okCancelListener = new OKCancelListener(this);

    	this.setupUI();
    }
    
    protected void setupUI(){
        // [mmichaud 2012-10-08] change from gridbaglayout to borderlayout
        this.getContentPane().setLayout(new java.awt.BorderLayout(5,5));
        
        JPanel content = new JPanel(new java.awt.BorderLayout(5,5));
        content.add(DialogTools.getPanelWithLabels(this.text, 50), java.awt.BorderLayout.NORTH);
        
        this.newAttrPanel = new NewAttributePanel(true, new AttributeType[]{AttributeType.DOUBLE}, false);
        content.add(this.newAttrPanel, java.awt.BorderLayout.SOUTH);
        
        //[sstein 24.March 2007] we dont have stored formulas
        if (this.storedFormulas != null){
        	this.formPanel = new FormulaEditingPanel(this.featureSchema, this.storedFormulas, this.newAttrPanel);
        }
        else{
        	this.formPanel = new FormulaEditingPanel(this.featureSchema, this.newAttrPanel);
        }
        this.okCancelListener.addValueChecker(this.formPanel);
        
        this.okCancelPanel = new OkCancelButtonPanel();
        this.okCancelPanel.addActionListener(this.okCancelListener);
        
        this.add(content, java.awt.BorderLayout.NORTH);
        this.add(formPanel, java.awt.BorderLayout.CENTER);
        this.add(okCancelPanel, java.awt.BorderLayout.SOUTH);
        
        this.pack();
        
    }
    
    

    public FormulaValue getParsedFormula() {
        return formPanel.getParsedFormula();
    }
    public String getFormula() {
        return formPanel.getFormula();
    }
    /**
     * @see NewAttributePanel#getAttributeInfo()
     * @return Info on the new attribute
     */
    public AttributeInfo getAttributeInfo() {
        return newAttrPanel.getAttributeInfo();
    }
    /**
     * @see OKCancelListener#wasOkClicked()
     */
    public boolean wasOkClicked() {
        return okCancelListener.wasOkClicked();
    }
}
