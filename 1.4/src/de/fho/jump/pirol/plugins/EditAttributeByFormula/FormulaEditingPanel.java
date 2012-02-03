/*
 * Created on 23.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.plugins.EditAttributeByFormula;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.openjump.core.apitools.FeatureSchemaTools;
import org.openjump.core.ui.swing.ValueChecker;
import org.openjump.io.PropertiesHandler;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.ui.GenericNames;

import de.fho.jump.pirol.ui.documents.NumberInputDocument;
import de.fho.jump.pirol.ui.panels.NewAttributePanel;
import de.fho.jump.pirol.utilities.FormulaParsing.FormulaParser;
import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;
import de.fho.jump.pirol.utilities.attributes.AttributeInfo;
import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 * Panel to help the user to create a formula that describes how the value of a new attribute will be calculated. 
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
public class FormulaEditingPanel extends JPanel implements ActionListener, ValueChecker {
    
    protected PersonalLogger logger = new PersonalLogger(DebugUserIds.ALL);
    
    private static final long serialVersionUID = -7709755834905111906L;
    
    protected JTextArea formulaField = new JTextArea(4,20), errorMessages = new JTextArea(3,20); 
    protected FeatureSchema featureSchema = null;
    protected AttributeInfo[] attributeInfos = null;
    
    protected JTextField numberInputField = null;
    protected JComboBox storedFormulasDropDown = new JComboBox(); 
    
    protected static final String[] mathSigns = new String[]{"+", "-", "*", "/", "(", ")", FormulaParser.KEY_SQRT, FormulaParser.KEY_POW};
    private PropertiesHandler storedFormulas;
    private NewAttributePanel newAttributePanel;
    
    protected FormulaValue parsedFormula = null;

    public FormulaEditingPanel(FeatureSchema featureSchema, PropertiesHandler storedFormulas, NewAttributePanel newAttributePanel) {
        super();
        this.featureSchema = featureSchema;
        this.attributeInfos = FeatureSchemaTools.getAttributesWithTypes(this.featureSchema, new AttributeType[]{AttributeType.DOUBLE, AttributeType.INTEGER});
        
        this.formulaField.setWrapStyleWord(true);
        this.storedFormulas = storedFormulas;
        this.newAttributePanel = newAttributePanel;
        
        this.setupUI();
    }
    
    //-- [sstein] new constructor since we do not assume property-files
    public FormulaEditingPanel(FeatureSchema featureSchema, NewAttributePanel newAttributePanel) {
        super();
        this.featureSchema = featureSchema;
        this.attributeInfos = FeatureSchemaTools.getAttributesWithTypes(this.featureSchema, new AttributeType[]{AttributeType.DOUBLE, AttributeType.INTEGER});
        
        this.formulaField.setWrapStyleWord(true);
        //this.storedFormulas = storedFormulas;
        this.newAttributePanel = newAttributePanel;
        
        this.setupUI();
    }
    
    protected void setupUI(){
        this.setLayout(new BorderLayout());
        JPanel formulaAndOperators = new JPanel();
        BorderLayout moreGenerousLayout = new BorderLayout();
        moreGenerousLayout.setVgap(15);
        formulaAndOperators.setLayout(moreGenerousLayout);
        
        //-- [sstein 23.March.2007] -- disable since we do not assume an existing file
        JPanel loadedForms = null;
        if (this.storedFormulas != null){
        	loadedForms = new JPanel();
	        loadedForms.setLayout(new BorderLayout());
	        
	        loadedForms.add(new JLabel(I18N.get("pirol.plugIns.FormulaEditingPanel.load-formula")+" : "), BorderLayout.WEST); //$NON-NLS-1$
	        
	        String[] formulaNames = (String[])this.storedFormulas.keySet().toArray(new String[0]);
	        
	        for (int i=0; i<formulaNames.length; i++){
	            this.storedFormulasDropDown.addItem(formulaNames[i]);
	        }
	        this.storedFormulasDropDown.setSelectedItem(null);
	        this.storedFormulasDropDown.addActionListener(this);
	        
	        loadedForms.add(this.storedFormulasDropDown, BorderLayout.CENTER);
	        formulaAndOperators.add(loadedForms, BorderLayout.NORTH);
        }
        formulaAndOperators.add(this.formulaField, BorderLayout.CENTER);
        
        JPanel mathSignsButtonPanel = new JPanel();
        int gridColumns = FormulaEditingPanel.mathSigns.length/2;
        mathSignsButtonPanel.setLayout(new GridLayout((FormulaEditingPanel.mathSigns.length/gridColumns), gridColumns));
        JButton button;
        
        for (int i=0; i<FormulaEditingPanel.mathSigns.length; i++){
            button = new JButton();
            button.setAction(new AddFormulaPartToTextArea_Action(FormulaEditingPanel.mathSigns[i], this.formulaField, FormulaEditingPanel.mathSigns, this.featureSchema));
            mathSignsButtonPanel.add(button);
        }
        
        formulaAndOperators.add(mathSignsButtonPanel, BorderLayout.SOUTH);
        
        this.add(formulaAndOperators, BorderLayout.NORTH);
        
        Box vbox = Box.createVerticalBox();
        
        Box hbox = Box.createHorizontalBox();
        int sumOfWidthes = 0;
        //-- [sstein] new
        int wantedWidth = 0; 
        if (loadedForms != null){
        	wantedWidth = loadedForms.getPreferredSize().width;
        }
        for (int i=0; i<attributeInfos.length; i++){
            button = new JButton();
            button.setAction(new AddFormulaPartToTextArea_Action(attributeInfos[i].getUniqueAttributeName(), this.formulaField, FormulaEditingPanel.mathSigns, this.featureSchema));
            
            hbox.add(button);

            sumOfWidthes += button.getPreferredSize().width;
            
            logger.printDebug("sumOfWidthes: " + sumOfWidthes + ", wantedWidth: " + wantedWidth);
            
            if (sumOfWidthes >= wantedWidth || i == attributeInfos.length-1){
                hbox.add(Box.createGlue());
                vbox.add(hbox);
                vbox.add(Box.createVerticalStrut(5));
                hbox = Box.createHorizontalBox();
                sumOfWidthes = 0;
            }
        }
        
        vbox.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(vbox);
        scrollPane.setSize(new Dimension(wantedWidth, 200));
        scrollPane.setMinimumSize(scrollPane.getSize());
        scrollPane.setPreferredSize(scrollPane.getSize());
        this.setPreferredSize(new Dimension(wantedWidth, 320));
        this.setSize(new Dimension(wantedWidth, 320));
        this.add(scrollPane, BorderLayout.CENTER);
        
        JPanel numberInputAndErrorPanel = new JPanel(new BorderLayout());
        JPanel numberInputPanel = new JPanel(new BorderLayout());
        this.numberInputField = new JTextField();
        this.numberInputField.setDocument(new NumberInputDocument());
        
        numberInputPanel.add(this.numberInputField, BorderLayout.CENTER);
        numberInputPanel.add( new JButton(new AddTextFieldTextToTextAreaOnClick_Action(this.numberInputField, this.formulaField, I18N.get("pirol.plugIns.FormulaEditingPanel.copy-value-to-formula"))), BorderLayout.EAST ); //$NON-NLS-1$
        
        numberInputAndErrorPanel.add(numberInputPanel, BorderLayout.NORTH);
        numberInputAndErrorPanel.add(this.errorMessages, BorderLayout.SOUTH);
        this.errorMessages.setEditable(false);
        this.errorMessages.setWrapStyleWord(true);
        this.errorMessages.setLineWrap(true);
        this.errorMessages.setFont(this.errorMessages.getFont().deriveFont(Font.BOLD));
        this.errorMessages.setForeground(Color.red);
        this.errorMessages.setBackground(this.getBackground());
        
        this.add(numberInputAndErrorPanel, BorderLayout.SOUTH);
        
    }
    
    /**
     *@return formula as String
     */
    public String getFormula(){
        return this.formulaField.getText();
    }
    
    /**
     *@return parsed formula as FormulaValue object
     */
    public FormulaValue getParsedFormula(){
        return this.parsedFormula;
    }

    /**
     * ... to react on a selction in the stored formula drop down menu ...
     *@param event
     */
    public void actionPerformed(ActionEvent event) {
        if (this.storedFormulas != null){
            String selectedFormName = this.storedFormulasDropDown.getSelectedItem().toString();
            this.formulaField.setText(this.storedFormulas.getProperty(selectedFormName));
            this.newAttributePanel.setAttributeName(selectedFormName);
        }
    }

    /**
     *@inheritDoc
     */
    public boolean areValuesOk() {
        try {
            this.parsedFormula = FormulaParser.getValue(this.getFormula(), this.featureSchema);
        } catch (RuntimeException e) {
            this.parsedFormula = null;
            this.errorMessages.setText("\n" + GenericNames.ERROR + ": " + e.getMessage()); //$NON-NLS-1$
            return false;
        }
        return true;
    }


}
