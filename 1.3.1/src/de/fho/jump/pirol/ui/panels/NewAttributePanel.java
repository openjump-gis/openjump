/*
 * Created on 20.06.2005
 *
 * CVS information:
 *  $Author$
 *  $Date$
 *  $ID$
 *  $Rev$
 *  $Id$
 *  $Log$
 *  Revision 1.2  2007/02/03 14:19:29  mentaer
 *  modified debug output for pirol stuff
 *
 *  Revision 1.1  2006/11/23 18:53:51  mentaer
 *  added EditAttributeByFormula Plugin by Pirol including some parts of the baseclasses - note: plugin needs java 1.5
 *
 *  Revision 1.6  2005/08/23 11:57:17  orahn
 *  ... dezimaltrennzeichen in tabellen
 *
 *  Revision 1.5  2005/08/03 14:33:13  orahn
 *  +i18n
 *  -warnings
 *
 *  Revision 1.4  2005/08/03 09:51:10  orahn
 *  +i18n
 *
 *  Revision 1.3  2005/06/29 16:03:11  orahn
 *  +default Attribut-Name
 *  +Possibility of a Attribute name follows
 *
 *  Revision 1.2  2005/06/28 15:46:17  orahn
 *  adjustable, if a default value for the new attribute to be queried ...
 *
 *  Revision 1.1  2005/06/20 18:18:23  orahn
 *  preparation for the Formula Editor
 *
 */
package de.fho.jump.pirol.ui.panels;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openjump.core.apitools.FeatureCollectionTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;

import de.fho.jump.pirol.utilities.attributes.AttributeInfo;
import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;
//import de.fho.jump.pirol.utilities.i18n.PirolPlugInMessages;

/**
 * 
 * A Panel that contains controls to collect all information needed to create
 * a new attribute out of.
 * The following information will be collected:<br>
 * -name of the attribute<br>
 * -type of the attribute values<br>
 * -Default-value (will initially filled in all features)<br>
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 */
public class NewAttributePanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = -2577345752815728142L;
    
    protected JTextField nameTextField = new JTextField();
    protected JTextField defValueTextField = new JTextField();
    protected JComboBox typeDropDown = new JComboBox();
    protected String drownActionCommand = "selectType";
    protected AttributeType[] onlyTypes = null;
    
    protected PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    
    protected String typeLabelText = I18N.get("pirol.ui.panels.type-of-new-attribute");
    protected String nameLabelText = I18N.get("pirol.ui.panels.name-of-new-attribute");
    protected String defValLabelText = I18N.get("pirol.ui.panels.default-value-for-new-attribute");
    
    protected JLabel nameLabel = new JLabel();
    protected JLabel defValLabel = new JLabel();
    protected JLabel typeLabel = new JLabel();
    
    protected boolean needDefaultValue = true;

    /**
     * @param arg0 see JPanel for information
     * @param needDefaultValue a field for a default value will be shown or not
     */
    public NewAttributePanel(boolean arg0, boolean needDefaultValue) {
        super(arg0);
        
        this.typeDropDown.setActionCommand(this.drownActionCommand);
        this.typeDropDown.addItem(AttributeType.DOUBLE);
        this.typeDropDown.addItem(AttributeType.INTEGER);
        this.typeDropDown.addItem(AttributeType.STRING);
        this.typeDropDown.setSelectedIndex(0);
        this.typeDropDown.addActionListener(this);
        
        this.needDefaultValue = needDefaultValue;
        
        this.setupUI();
    }
    
    /**
     * @param arg0 see JPanel for information
     * @param onlyTypes array of attribute type, that are supposed to be chooseable
     * @param needDefaultValue a field for a default value will be shown or not
     */
    public NewAttributePanel(boolean arg0,AttributeType[] onlyTypes, boolean needDefaultValue) {
        super(arg0);
        
        this.onlyTypes = onlyTypes;
        
        this.typeDropDown.setActionCommand(this.drownActionCommand);
        for (int i=0; i<this.onlyTypes.length; i++){
            this.typeDropDown.addItem(this.onlyTypes[i]);
        }
        this.typeDropDown.setSelectedIndex(0);
        this.typeDropDown.addActionListener(this);
        
        this.needDefaultValue = needDefaultValue;
        
        this.setupUI();
    }
    
    /**
     * Sets up GUI controlls.
     *
     */
    protected void setupUI(){
        this.setLayout(new GridLayout(  (this.needDefaultValue)?3:2 ,2));
        
        this.nameLabel = new JLabel(this.nameLabelText); 
        this.add(this.nameLabel);
        this.add(this.nameTextField);
        
        this.typeLabel = new JLabel(this.typeLabelText);
        this.add(this.typeLabel);
        this.add(this.typeDropDown);
        
        if (this.needDefaultValue){
            this.defValLabel = new JLabel(this.defValLabelText); 
            this.add(this.defValLabel);
            this.add(this.defValueTextField);
        }
    }
    
    
    /**
     * Sets the text that will be displayed in the text field for the new attribute's name.
     *@param attrName  text that will be displayed in the name text field
     */
    public void setAttributeName(String attrName) {
        nameTextField.setText(attrName);
    }
    /**
     * Returns the collected information on the new attribute. Since this method calls
     * getDefaultValue(), it may throw the same Exception...
     * @return the collected information
     */
    public AttributeInfo getAttributeInfo(){
        
        String newAttrName = this.nameTextField.getText();
        
        if (newAttrName == null || newAttrName.length() == 0) newAttrName = "NEW_ATTRIBUTE";
        
        AttributeInfo attrInfo = new AttributeInfo((AttributeType)this.typeDropDown.getSelectedItem(), newAttrName);
        
        if (this.needDefaultValue)
            attrInfo.setNullValue(this.getDefaultValue());
        
        return attrInfo;
    }

    /**
     * checks and fixes the integrity of the values given, when the attribute
     * type is changed.
     * @param event the action event
     */
    public void actionPerformed(ActionEvent event) {
        if (JComboBox.class.isInstance(event.getSource())){
            if ( this.needDefaultValue && FeatureCollectionTools.isAttributeTypeNumeric((AttributeType)this.typeDropDown.getSelectedItem()) ){
                AttributeType at = (AttributeType)this.typeDropDown.getSelectedItem();
                
                if (at.equals(AttributeType.INTEGER)){
                    try {
                        Integer.parseInt(this.defValueTextField.getText());
                    } catch (Exception e){
                        this.defValueTextField.setText("0");
                    }
                } else {
                    try {
                        Double.parseDouble(this.defValueTextField.getText());
                    } catch (Exception e){
                        this.defValueTextField.setText("0.0");
                    }
                }
            }
        }
        
    }
    
    /**
     * This allows you to check, if the data filled in by the user makes sense or not.
     * You may want enable/disable the ok-button, depending on the return value of this
     * function.
     * @return true if values are ok, else false
     */
    public boolean isDataValid(){
        boolean dataValid = true;
        
        if (this.needDefaultValue){
	        try {
	            this.getDefaultValue();
	        } catch (Exception e){
	            dataValid = false;
	        }
        }
        
        if (this.nameTextField.getText() == null || this.nameTextField.getText().length() == 0)
            dataValid = false;
        
        return dataValid;
        
    }
    
    /**
     * Returns the default value for the attribute we want to create. This function may throw
     * a RuntimeException, if the value filled in for the default value can not be parsed!
     * @return default value
     */
    protected Object getDefaultValue(){
        if ( FeatureCollectionTools.isAttributeTypeNumeric((AttributeType)this.typeDropDown.getSelectedItem()) ){
            AttributeType at = (AttributeType)this.typeDropDown.getSelectedItem();
            
            if (at.equals(AttributeType.INTEGER)){
                int i = Integer.parseInt(this.defValueTextField.getText());
                return new Integer(i);
            }
            double d = Double.parseDouble(this.defValueTextField.getText());
            return new Double(d);
        }
        return this.defValueTextField.getText();
    }
    
    
    public void addActionListenerToDefValueTextfield(ActionListener arg0) {
        defValueTextField.addActionListener(arg0);
    }
    
    public void addActionListenerToNameTextfield(ActionListener arg0) {
        this.nameTextField.addActionListener(arg0);
    }
    
    public void addActionListenerToTypeDropDown(ActionListener arg0) {
        this.typeDropDown.addActionListener(arg0);
    }
    
    
    /**
     * Sets the label text for the default value field
     * @param defValLabel The defValLabel to set.
     */
    public void setDefValLabel(String defValLabel) {
        this.defValLabelText = defValLabel;
        this.defValLabel.setText(this.defValLabelText);
    }
    /**
     * Sets the label text for the name field
     * @param nameLabel The nameLabel to set.
     */
    public void setNameLabel(String nameLabel) {
        this.nameLabelText = nameLabel;
        this.nameLabel.setText(this.nameLabelText);
    }
    /**
     * Sets the label text for the type field
     * @param typeLabel The typeLabel to set.
     */
    public void setTypeLabel(String typeLabel) {
        this.typeLabelText = typeLabel;
        this.typeLabel.setText(this.typeLabelText);
    }
}
