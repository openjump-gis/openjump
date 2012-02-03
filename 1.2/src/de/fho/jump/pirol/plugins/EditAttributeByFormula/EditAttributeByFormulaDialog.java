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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;

import de.fho.jump.pirol.ui.eventHandling.OKCancelListener;
import de.fho.jump.pirol.ui.panels.NewAttributePanel;
import de.fho.jump.pirol.ui.panels.OkCancelButtonPanel;
import de.fho.jump.pirol.ui.tools.DialogTools;
import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;
import de.fho.jump.pirol.utilities.Properties.PropertiesHandler;
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
        JPanel content = new JPanel();
        GridBagLayout gridbagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        
        int gridy = 1;
        c.gridx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 20;
        content.setLayout(gridbagLayout);
        
        c.gridy = gridy++;
        content.add(DialogTools.getPanelWithLabels(this.text, 50), c);
        
        this.newAttrPanel = new NewAttributePanel(true, new AttributeType[]{AttributeType.DOUBLE}, false);
        c.gridy = gridy++;
        content.add(this.newAttrPanel, c);
        
        //[sstein 24.March 2007] we dont have stored formulas
        if (this.storedFormulas != null){
        	this.formPanel = new FormulaEditingPanel(this.featureSchema, this.storedFormulas, this.newAttrPanel);
        }
        else{
        	this.formPanel = new FormulaEditingPanel(this.featureSchema, this.newAttrPanel);
        }
        this.okCancelListener.addValueChecker(this.formPanel);
        c.gridy = gridy++;
        content.add(this.formPanel, c);
        
        this.okCancelPanel = new OkCancelButtonPanel();
        this.okCancelPanel.addActionListener(this.okCancelListener);
        c.gridy = gridy++;
        content.add(this.okCancelPanel, c);
        
        content.doLayout();
        
        this.getContentPane().add(content);
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
