package org.openjump.core.ui.plugin.queries;

import buoy.event.CommandEvent;
import buoy.event.MouseEnteredEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.FeatureSelection;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.TaskFrame;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * QueryDialog
 * @author Michael MICHAUD
 * version 0.1.0 (4 Dec 2004)
 * version 0.1.1 (15 Jan 2005)
 * version 0.2 (16 Oct 2005)
 * version 0.2.1 (10 aug 2007)
 * version 0.3.0 (04 sept 2010) complete rewrite of functionChenged and operatorChanged methods
 * version 0.4.0 (28 june 2013) add relate method based on the DE-9IM matrix
 * version 0.5.0 (06 avril 2015) clone features in result and improve boolean attribute type
 */ 
public class QueryDialog extends BDialog {
    
    public static final int ALL_LAYERS = 0;
    public static final int SELECTION = 1;
    public static final int SELECTED_LAYERS = 2;
    
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat();
    
    private PlugInContext context;
    
    // List of layers to search
    List layers = new ArrayList();
    // List of available attributes
    Set attributes = new TreeSet();
    // Selected attribute name
    String attribute;
    // Selected attribute type
    char attributeType;
    // Selected function
    Function function;
    // Selected operator
    Operator operator;
    // Selected value
    String value;
    // Map attributes with lists of available values read in the fc
    Map enumerations = new HashMap();
    // Flag indicating a query is running
    static boolean runningQuery = false;
    static boolean cancelQuery = false;
    
    // selected features initialized in execute query if "select" option is true
    Collection selection; 
    
    BCheckBox charFilter;
    BCheckBox caseSensitive;
    BCheckBox numFilter;
    BCheckBox geoFilter;
    BCheckBox booFilter;

    BCheckBox display;
    BCheckBox select;
    BCheckBox create;
            
    BComboBox layerCB;
    BComboBox attributeCB;
    BComboBox functionCB;
    BComboBox operatorCB;
    BComboBox valueCB;

    BLabel comments;
    BLabel progressBarTitle;
    BProgressBar progressBar;

    BButton okButton;
    BButton refreshButton;
    BButton cancelButton;
    BButton stopButton;

    
   /**
    * Constructor of a QueryDialog
    * @param context the plugin context
    */
    public QueryDialog(PlugInContext context) {
    	component = super.createComponent(context.getWorkbenchFrame(), "", false);
        addEventLink(WindowClosingEvent.class, this, "exit");
        this.context = context;
        setTitle(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.query-builder"));
        initUI(context);
    }


   /**
    * User Interface Initialization
    */
    protected void initUI(PlugInContext context) {
        // LAYOUT DEFINITIONS
        LayoutInfo centerNone6 =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                        new java.awt.Insets(6,6,6,6), new java.awt.Dimension());
                        
        LayoutInfo centerBoth3 =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
                        
        LayoutInfo centerBoth1 =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH,
                        new java.awt.Insets(1,1,1,1), new java.awt.Dimension());
        
        LayoutInfo nwBoth1 =
            new LayoutInfo(LayoutInfo.NORTHWEST, LayoutInfo.BOTH,
                        new java.awt.Insets(1,1,1,1), new java.awt.Dimension());
        
        LayoutInfo centerNone3 =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
                        
        LayoutInfo centerH3 =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
        LayoutInfo centerBoth =
            new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
            
        LayoutInfo rightAlign =
            new LayoutInfo(LayoutInfo.EAST, LayoutInfo.HORIZONTAL,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
        LayoutInfo leftAlign =
            new LayoutInfo(LayoutInfo.WEST, LayoutInfo.HORIZONTAL,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
        LayoutInfo leftAlignShort =
            new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
                        new java.awt.Insets(3,3,3,3), new java.awt.Dimension());
                        
        Border border = BorderFactory.createLineBorder(java.awt.Color.BLACK);
        Border border2 = BorderFactory.createLineBorder(java.awt.Color.BLACK, 2);

        // MAIN GUI CONTAINERS
        BorderContainer dialogContainer = new BorderContainer();
            // NORTH
            BorderContainer northPanel = new BorderContainer();
            BorderContainer titleContainer =  new BorderContainer();
            titleContainer.setDefaultLayout(centerNone6);
            // CENTER
            FormContainer centerPanel = new FormContainer(1,3);
                //OPTIONS
                FormContainer optionPanel = new FormContainer(4,1);
                optionPanel.setDefaultLayout(centerBoth1);
                BOutline optionPanelB = new BOutline(optionPanel, border);
                    // MANAGER
                    FormContainer managerPanel = new FormContainer(3,3);
                    managerPanel.setDefaultLayout(nwBoth1);
                    BOutline managerPanelB = new BOutline(managerPanel, 
                                  BorderFactory.createTitledBorder(border,
                                		  I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.query-manager")));
                    // ATTRIBUTE FILTER
                    FormContainer attributeFilterPanel = new FormContainer(2,4);
                    attributeFilterPanel.setDefaultLayout(nwBoth1);
                    BOutline attributeFilterPanelB = new BOutline(attributeFilterPanel,
                                  BorderFactory.createTitledBorder(border,
                                		  I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.filter-on-attribute-type")));
                    // RESULT OPTIONS
                    FormContainer resultPanel = new FormContainer(1,3);
                    resultPanel.setDefaultLayout(nwBoth1);
                    BOutline resultPanelB = new BOutline(resultPanel,
                                  BorderFactory.createTitledBorder(border,
                                		  I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.results")));
                // QUERY CONSTRUCTOR
                //FormContainer queryConstructorPanel = new FormContainer(5,3);
                FormContainer queryConstructorPanel = new FormContainer(2,7);
                queryConstructorPanel.setBackground(Color.decode(
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.color1")
                ));
                queryConstructorPanel.setDefaultLayout(centerNone3);
                BOutline queryConstructorPanelB = new BOutline(queryConstructorPanel, border2);
                
                // PROGRESS BAR
                FormContainer progressBarPanel = new FormContainer(5,1);
                
                // SOUTH PANEL (OK/CANCEL BUTTONS)
            FormContainer southPanel = new FormContainer(7,1);
        
        // SET THE MANAGER BUTTONS
        BButton openButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.open"));
            openButton.addEventLink(CommandEvent.class, this, "open");
            managerPanel.add(openButton, 1, 0, centerH3);
        BButton saveasButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.save-as"));
            saveasButton.addEventLink(CommandEvent.class, this, "saveas");
            managerPanel.add(saveasButton, 1, 2, centerH3);
                
        // SET THE ATTRIBUTE FILTER CHECKBOXES
        charFilter = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.string"), true);
            charFilter.addEventLink(ValueChangedEvent.class, this, "charFilterChanged");
            attributeFilterPanel.add(charFilter, 0, 0);
        caseSensitive = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.case-sensitive"), false);
            //caseSensitive.addEventLink(ValueChangedEvent.class, this, "caseSensitiveChanged");
            attributeFilterPanel.add(caseSensitive, 1, 0, centerNone3);
        numFilter = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.numeric"), true);
            numFilter.addEventLink(ValueChangedEvent.class, this, "numFilterChanged");
            attributeFilterPanel.add(numFilter, 0, 1);
        geoFilter = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.geometric"), true);
            geoFilter.addEventLink(ValueChangedEvent.class, this, "geoFilterChanged");
            attributeFilterPanel.add(geoFilter, 0, 2);
        booFilter = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.boolean"), true);
            booFilter.addEventLink(ValueChangedEvent.class, this, "booFilterChanged");
            attributeFilterPanel.add(booFilter, 0, 3);
            
        // SET THE RESULT OPTIONS
        display = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.display-the-table"), false);
            resultPanel.add(display, 0, 0);
        select = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-the-result"), true);
            resultPanel.add(select, 0, 1);
        create = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.create-a-new-layer"), false);
            resultPanel.add(create, 0, 2);
        
        // SET THE COMBO BOXES
        BLabel layerLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.layer"), BLabel.EAST);
            queryConstructorPanel.add(layerLabel, 0, 0, rightAlign);
        BLabel attributeLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.attribute"), BLabel.EAST);
            queryConstructorPanel.add(attributeLabel, 0, 1, rightAlign);
        BLabel functionLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.function"), BLabel.EAST);
            queryConstructorPanel.add(functionLabel, 0, 2, rightAlign);
        BLabel operatorLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.operator"), BLabel.EAST);
            queryConstructorPanel.add(operatorLabel, 0, 3, rightAlign);
        BLabel valueLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.value"), BLabel.EAST);
            queryConstructorPanel.add(valueLabel, 0, 4, rightAlign);
            
        layerCB = new BComboBox();
            layerCB.addEventLink(ValueChangedEvent.class, this, "layerChanged");
            queryConstructorPanel.add(layerCB, 1, 0, leftAlign);
            // mmichaud 2008-11-13 limit the width to about 40 chars
            ((javax.swing.JComboBox)layerCB.getComponent())
                .setPrototypeDisplayValue("012345678901234567890123456789O1");
        attributeCB = new BComboBox();
            attributeCB.addEventLink(ValueChangedEvent.class, this, "attributeChanged");
            queryConstructorPanel.add(attributeCB, 1, 1, leftAlign);
            // mmichaud 2008-11-13 limit the width to about 40 chars
            ((javax.swing.JComboBox)attributeCB.getComponent())
                .setPrototypeDisplayValue("01234567890123456789012345678901");
        functionCB = new BComboBox();
            functionCB.addEventLink(ValueChangedEvent.class, this, "functionChanged");
            queryConstructorPanel.add(functionCB, 1, 2, leftAlignShort);
        
        operatorCB = new BComboBox();
            operatorCB.addEventLink(ValueChangedEvent.class, this, "operatorChanged");
            queryConstructorPanel.add(operatorCB, 1, 3, leftAlignShort);
        valueCB = new BComboBox();
            valueCB.addEventLink(ValueChangedEvent.class, this, "valueChanged");
            queryConstructorPanel.add(valueCB, 1, 4, leftAlign);
            // mmichaud 2008-11-13 limit the width to about 40 chars
            ((javax.swing.JComboBox)valueCB.getComponent())
                .setPrototypeDisplayValue("012345678901234567890123456789012345678901234567");
            
        comments = new BLabel("<html>&nbsp;<br>&nbsp;</html>");
            queryConstructorPanel.add(comments, 0, 5, 2, 2, centerBoth);
            
        // PROGRESS BAR PANEL
        progressBarTitle = new BLabel(" ");
            progressBarPanel.add(progressBarTitle, 0, 0, 1, 1, centerH3);
        progressBar = new BProgressBar();
            progressBarPanel.add(progressBar, 1, 0, 4, 1, centerH3);
        
        // CENTER PANEL LAYOUT
        optionPanel.add(managerPanelB, 0, 0);
        optionPanel.add(attributeFilterPanelB, 1, 0, 2, 1);
        optionPanel.add(resultPanelB, 3, 0);
        
        centerPanel.setDefaultLayout(centerBoth3);
        centerPanel.add(optionPanel, 0, 0);
        centerPanel.add(queryConstructorPanelB, 0, 1);
        centerPanel.add(progressBarPanel, 0, 2);
        
        // SET THE OK/CANCEL BUTTONS
        okButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.ok"));
            okButton.addEventLink(CommandEvent.class, this, "ok");
        //cancelButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.cancel"));
        //    cancelButton.addEventLink(CommandEvent.class, this, "cancel");
        stopButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.stop"));
            stopButton.addEventLink(CommandEvent.class, this, "stop");
        refreshButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.refresh"));
            refreshButton.addEventLink(CommandEvent.class, this, "refresh");
        
        southPanel.add(okButton, 2, 0);
        southPanel.add(refreshButton, 3, 0);
        southPanel.add(stopButton, 4, 0);
        
        dialogContainer.add(northPanel, dialogContainer.NORTH);
        dialogContainer.add(centerPanel, dialogContainer.CENTER);
        dialogContainer.add(southPanel, dialogContainer.SOUTH);
        
        // added on 2007-08-22 to synchronize the UI with layerNamePanel changes
        context.getLayerManager().addLayerListener(new LayerListener() {
            public void categoryChanged(CategoryEvent e) {}
            public void featuresChanged(FeatureEvent e) {}
            public void layerChanged(LayerEvent e) {if (!runningQuery) refresh();}
        }); 
        
        initComboBoxes();
        setContent(dialogContainer);
        addEventLink(MouseEnteredEvent.class, this, "toFront");
        pack();
        setVisible(true);
        toFront();
    }
    
    // To add an eventLink in the QueryPlugIn 
    BButton getOkButton() {return okButton;}
    BButton getCancelButton() {return cancelButton;} 
    
    
    void initVariables() {
        runningQuery = false;
        cancelQuery = false;
        progressBarTitle.setText("");
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setProgressText("");
        refreshButton.setEnabled(true);
    }
    
    void initComboBoxes() {
        // INIT layerCB and attributeCB
        LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();
        layerListCellRenderer.setCheckBoxVisible(false);
        layerListCellRenderer.setProgressIconLabelVisible(false);
        
        layerCB.removeAll();
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.all-layers"));
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selection"));
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selected-layers"));
        
        List layers = context.getLayerManager().getLayers();
        for (int i = 0 ; i < layers.size() ; i++) {
            Layer layer = (Layer)layers.get(i);
            layerCB.add(layer);
        }
        ((javax.swing.JComboBox)layerCB.getComponent()).setRenderer(layerListCellRenderer);
        this.layers = layers;
        this.attributes = authorizedAttributes(layers);
        attributeType = 'G';
        attributeCB.setContents(attributes);
        
        // INIT functionCB
        functionCB.setContents(Function.GEOMETRIC_FUNCTIONS);
        function = (Function)functionCB.getSelectedValue();
        
        // INIT operatorCB
        operatorCB.setContents(Operator.GEOMETRIC_OP);
        operator = (Operator)operatorCB.getSelectedValue();
        
        // INIT valueCB
        valueCB.setContents(availableTargets());
        valueChanged();
        pack();
        
        initVariables();
    }
    
    public void layerChanged() {
        layers.clear();
        attributes.clear();
        // index 0 ==> all the layers
        if (layerCB.getSelectedIndex() == ALL_LAYERS) {
            layers.addAll(context.getLayerManager().getLayers());
        }
        // index 1 ==> all the selected features
        else if (layerCB.getSelectedIndex() == SELECTION) {
            layers.addAll(context.getLayerViewPanel()
                                 .getSelectionManager()
                                 .getLayersWithSelectedItems());
        }
        // index 2 ==> all the selected layers
        else if (layerCB.getSelectedIndex() == SELECTED_LAYERS) {
            Layer[] ll = context.getLayerNamePanel().getSelectedLayers();
            layers.addAll(Arrays.asList(ll));
        }
        // selected layer
        else {
            //layers.add(context.getLayerManager().getLayer((String)layerCB.getSelectedValue()));
            layers.add(layerCB.getSelectedValue());
        }
        attributes.addAll(authorizedAttributes(layers));
        attributeCB.setContents(attributes);
        attributeChanged();
    }
    
    private boolean isAttributeAuthorized(FeatureSchema fs, String attributeName) {
        AttributeType type = fs.getAttributeType(attributeName);
        if (type==AttributeType.GEOMETRY && geoFilter.getState()) return true;
        else if (type==AttributeType.STRING && charFilter.getState()) return true;
        else if (type==AttributeType.INTEGER && numFilter.getState()) return true;
        else if (type==AttributeType.LONG && numFilter.getState()) return true;
        else if (type==AttributeType.DOUBLE && numFilter.getState()) return true;
        else if (type==AttributeType.DATE && numFilter.getState()) return true;
        else if (type==AttributeType.BOOLEAN && booFilter.getState()) return true;
        else return false;
    }
    
    private Set authorizedAttributes(List layers) {
        // set of authorized Attributes
        Set set = new TreeSet();
        // map of enumerations
        enumerations = new HashMap();
        // Geometry is processed separetely in order to always have it first
        if (geoFilter.getState()) set.add(" (GEOMETRY)");
        attribute = "";
        for (int i = 0 ; i < layers.size() ; i++) {
            Layer layer = (Layer)layers.get(i);
            FeatureSchema fs = layer.getFeatureCollectionWrapper().getFeatureSchema();
            for (int j = 0 ; j < fs.getAttributeCount() ; j++) {
                String att = fs.getAttributeName(j);
                AttributeType type = fs.getAttributeType(j);
                if (type!=AttributeType.GEOMETRY && isAttributeAuthorized(fs, att) ) {
                    set.add(att + " (" + type.toString().split(" ")[0] + ")");
                }
            }
        }
        return set;
    }
    
    private void charFilterChanged() {
        if (charFilter.getState()) caseSensitive.setVisible(true);
        else caseSensitive.setVisible(false);
        layerChanged();
    }
    
    private void numFilterChanged() {
        layerChanged();
    }
    
    private void geoFilterChanged() {
        layerChanged();
    }

    private void booFilterChanged() {
        layerChanged();
    }
    
    public void attributeChanged() {
        String att = (String)attributeCB.getSelectedValue();
        attribute = att.substring(0, att.lastIndexOf(' '));
        String attType = att.substring(att.lastIndexOf('(')+1,
                                       att.lastIndexOf(')'));
        char newat = 'S';
        if (attType.equals("INTEGER")) newat = 'N';
        else if (attType.equals("LONG")) newat = 'N';
        else if (attType.equals("DATE")) newat = 'D';
        else if (attType.equals("DOUBLE")) newat = 'N';
        else if (attType.equals("STRING")) newat = 'S';
        else if (attType.equals("GEOMETRY")) newat = 'G';
        else if (attType.equals("BOOLEAN")) newat = 'B';
        else;
        // No type change
        if (newat==attributeType) {
            if (newat=='S') updateValues();
        }
        else {
            attributeType = newat;
            updateFunctions();
            functionChanged();
            updateValues();
        }
    }
    
    private void updateFunctions() {
        switch (attributeType) {
            case 'B' :
                functionCB.setContents(Function.BOOLEAN_FUNCTIONS);
                break;
            case 'N' :
                functionCB.setContents(Function.NUMERIC_FUNCTIONS);
                break;
            case 'D' :
                functionCB.setContents(Function.DATE_FUNCTIONS);
                break;
            case 'S' :
                functionCB.setContents(Function.STRING_FUNCTIONS);
                break;
            case 'E' :
                functionCB.setContents(Function.STRING_FUNCTIONS);
                break;
            case 'G' :
                functionCB.setContents(Function.GEOMETRIC_FUNCTIONS);
                break;
            default :
        }
    }
    
    public void functionChanged() {
        // if function is edited to change the parameter value by hand (buffer),
        // functionCB.getSelectedValue() class changes from Function to String
        String ft = functionCB.getSelectedValue().toString();
        try {
            if (functionCB.getSelectedValue() instanceof Function) {
                Function newfunction = (Function)functionCB.getSelectedValue();
                if (newfunction.type!=function.type) {
                    updateOperators();
                    operatorChanged();
                    updateValues();
                }
                function = (Function)functionCB.getSelectedValue();
                if (function == Function.SUBS || function == Function.BUFF) {
                    functionCB.setEditable(true);
                }
                else functionCB.setEditable(false);
            }
            else if (functionCB.getSelectedValue() instanceof String) {
                // SET IF FUNCTION IS EDITABLE OR NOT
                if(function==Function.SUBS) {
                    //functionCB.setEditable(true);
                    String f = functionCB.getSelectedValue().toString();
                    String sub = f.substring(f.lastIndexOf('(')+1, f.lastIndexOf(')'));
                    String[] ss = sub.split(",");
                    Function.SUBS.args = new int[ss.length];
                    if (ss.length>0) Function.SUBS.args[0] = Integer.parseInt(ss[0]);
                    if (ss.length>1) Function.SUBS.args[1] = Integer.parseInt(ss[1]);
                    functionCB.setSelectedValue(Function.SUBS);
                }
                else if(function==Function.BUFF) {
                    //functionCB.setEditable(true);
                    String f = functionCB.getSelectedValue().toString();
                    String sub = f.substring(f.lastIndexOf('(')+1, f.lastIndexOf(')'));
                    Function.BUFF.arg = Double.parseDouble(sub);
                    functionCB.setSelectedValue(Function.BUFF);
                }
                else {
                    functionCB.setEditable(false);
                    context.getWorkbenchFrame().warnUser("Cannot modify this function name");
                }
            }
        } catch(Exception e) {
            context.getWorkbenchFrame().toMessage(e);
        }
    }
    
    private void updateOperators() {
        function = (Function)functionCB.getSelectedValue();
        switch(function.type) {
            case 'S' :
            case 'E' :
                operatorCB.setContents(Operator.STRING_OP);
                break;
            case 'B' :
                operatorCB.setContents(Operator.BOOLEAN_OP);
                break;
            case 'N' :
            case 'D' :
                operatorCB.setContents(Operator.NUMERIC_OP);
                break;
            case 'G' :
                operatorCB.setContents(Operator.GEOMETRIC_OP);
                break;
            default :
        }
    }
    
    public boolean operatorChanged() {
        //Operator newop = (Operator)operatorCB.getSelectedValue();
        String newopstring = operatorCB.getSelectedValue().toString();
        try {
            if (operatorCB.getSelectedValue() instanceof Operator) {
                Operator newop = (Operator)operatorCB.getSelectedValue();
                if(newop.type!=operator.type) {
                    updateValues();
                }
                if (operator!=Operator.MATC && operator!=Operator.FIND &&
                    (newop==Operator.MATC || newop==Operator.FIND)) {
                    updateValues();
                }
                if ((operator==Operator.MATC || operator==Operator.FIND) &&
                    (newop!=Operator.MATC && newop!=Operator.FIND)) {
                    updateValues();
                }
                operator = newop;
                if (operator == Operator.WDIST || operator == Operator.RELAT) {
                    operatorCB.setEditable(true);
                }
                else {
                    operatorCB.setEditable(false);
                }
            }
            if (operatorCB.getSelectedValue() instanceof String) {
                if (operator==Operator.WDIST) {
                    //operatorCB.setEditable(true); // added on 2007-07-02 (bug fix)
                    String f = operatorCB.getSelectedValue().toString();
                    Pattern regex = Pattern.compile(".*\\(([0-9]+(\\.[0-9]+)?)\\)");
                    Matcher matcher = regex.matcher(f);
                    if (matcher.matches()) {
                        Operator.WDIST.arg = Double.parseDouble(matcher.group(1));
                        operatorCB.setSelectedValue(Operator.WDIST);
                    }
                    else {
                        context.getWorkbenchFrame().warnUser(I18N.getMessage("org.openjump.core.ui.plugin.queries.SimpleQuery.illegal-argument-for", operator));
                        return false;
                    }
                }
                // added on 2013-06-28
                else if (operator==Operator.RELAT) {
                    String f = operatorCB.getSelectedValue().toString();
                    Pattern regex = Pattern.compile(".*\\(([012TFtf\\*]{9})\\)");
                    Matcher matcher = regex.matcher(f);
                    if (matcher.matches()) {
                        Operator.RELAT.arg = matcher.group(1);
                        operatorCB.setSelectedValue(Operator.RELAT);
                    }
                    else {
                        context.getWorkbenchFrame().warnUser(I18N.getMessage("org.openjump.core.ui.plugin.queries.SimpleQuery.illegal-argument-for", operator));
                        return false;
                    }
                }
                else {
                    operatorCB.setEditable(false);
                    context.getWorkbenchFrame().warnUser("Cannot modify this function name");
                }
            }
        }
        catch(Exception e) {
            context.getWorkbenchFrame().toMessage(e);
            return false;
        }
        return true;
        
    }
    
   /**
    * Update the possible values list (may be editable or not)
    */
    private void updateValues() {
        if(function.type == 'B') {
               valueCB.setContents(new String[]{
            		   I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.true"),
            		   I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.false")
               });
               valueCB.setEditable(false);
        }
        else if (operator.type=='G') {
            valueCB.setContents(availableTargets());
            valueCB.setEditable(true);
        }
        else if (attributeType=='E') {
            if (operator.type=='S') {
                valueCB.setContents((Object[])enumerations.get(attribute));
            }
            else if (operator.type=='N') {
                valueCB.setContents(new Object[]{"0"});
            }
            valueCB.setEditable(true);
        }
        else if (attributeType=='D') {
            valueCB.setContents(new Object[]{DATE_FORMATTER.format(new Date())});
        }
        else if (attributeType=='S') {
            valueCB.setContents(availableStrings(attribute, 256));
            if (operator==Operator.MATC || operator==Operator.FIND) {
                valueCB.setContents(new String[]{
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.regular-expression")
                });
            }
            valueCB.setEditable(true);
        }
        else {
            valueCB.setContents(new String[]{""});
            valueCB.setEditable(true);
        }
    }
    
    private void valueChanged() {
        value = (String)valueCB.getSelectedValue();
    }
    
    private List availableTargets() {
        List list = new ArrayList();
        list.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.all-layers"));
        list.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selection"));
        list.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selected-layers"));
        List layers = context.getLayerManager().getLayers();
        for (int i = 0 ; i < layers.size() ; i++) {
            list.add(((Layer)layers.get(i)).getName());
        }
        return list;
    }
    
    private Set availableStrings(String attribute, int maxsize) {
        Set set = new TreeSet();
        set.add("");
        for (int i = 0 ; i < layers.size() ; i++) {
            FeatureCollection fc = ((Layer)layers.get(i)).getFeatureCollectionWrapper();
            if (!fc.getFeatureSchema().hasAttribute(attribute)) continue;
            Iterator it = fc.iterator();
            while (it.hasNext() && set.size()<maxsize) {
                Feature f = (Feature)it.next();
                Object val = f.getAttribute(attribute);
                if (val != null) set.add(val);
            }
        }
        return set;
    }
    
    private void open() {
        BFileChooser bfc = new BFileChooser(BFileChooser.OPEN_FILE,
        		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.open"));
        Properties prop = new Properties();
        if (bfc.showDialog(this)) {
            try {
                File f = bfc.getSelectedFile();
                prop.load(new FileInputStream(f));
            }
            catch(IOException e){
                context.getWorkbenchFrame().warnUser(e.getMessage());
            }
        }
        else {return;}
        charFilter.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.string")).booleanValue());
        caseSensitive.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.case-sensitive")).booleanValue());
        numFilter.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.numeric")).booleanValue());
        geoFilter.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.geometric")).booleanValue());

        display.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.display-the-table")).booleanValue());
        select.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.select-the-result")).booleanValue());
        create.setState(new Boolean(prop.getProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.create-a-new-layer")).booleanValue());
        
        initComboBoxes();
        
        int layerIndex = Integer.parseInt(prop.getProperty("layer_index"));
        String layerName = prop.getProperty("layer_name");
        if (layerIndex<3) layerCB.setSelectedIndex(layerIndex);
        else layerCB.setSelectedValue(layerName);
        if(!layerName.equals(layerCB.getSelectedValue().toString())) {
            context.getWorkbenchFrame().warnUser(layerName + " " +
            		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.does-not-exist"));
            return;
        }
        layerChanged();
        
        int attributeIndex = Integer.parseInt(prop.getProperty("attribute_index"));
        String attributeName = prop.getProperty("attribute_name");
        attributeCB.setSelectedValue(attributeName);
        if(!attributeName.equals(attributeCB.getSelectedValue().toString())) {
            context.getWorkbenchFrame().warnUser(attributeName + " " +
            		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.does-not-exist"));
            return;
        }
        attributeChanged();
        
        int functionIndex = Integer.parseInt(prop.getProperty("function_index"));
        String functionName = prop.getProperty("function_name");
        functionCB.setSelectedIndex(functionIndex%functionCB.getItemCount());
        if(!functionName.equals(functionCB.getSelectedValue().toString())) {
            if (functionCB.getItem(functionIndex)==Function.SUBS) {
                functionCB.setEditable(true);
                functionCB.setSelectedValue(functionName);
            }
            else if (functionCB.getItem(functionIndex)==Function.BUFF) {
                functionCB.setEditable(true);
                functionCB.setSelectedValue(functionName);
            }
            else {
                context.getWorkbenchFrame().warnUser(functionName + " " +
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.does-not-exist"));
                return;
            }
        }
        functionChanged();
        
        int operatorIndex = Integer.parseInt(prop.getProperty("operator_index"));
        String operatorName = prop.getProperty("operator_name");
        operatorCB.setSelectedIndex(operatorIndex%operatorCB.getItemCount());
        if(!operatorName.equals(operatorCB.getSelectedValue().toString())) {
            if (operatorCB.getItem(operatorIndex)==Operator.WDIST || operatorCB.getItem(operatorIndex)==Operator.RELAT) {
                operatorCB.setEditable(true);
                operatorCB.setSelectedValue(operatorName);
            }
            else {
                context.getWorkbenchFrame().warnUser(operatorName + " " +
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.does-not-exist"));
                return;
            }
        }
        operatorChanged();
        
        String value =  prop.getProperty("value");
        valueCB.setSelectedValue(value);
        
    }
    
    private void saveas() {
        Properties prop = new Properties();
        
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.string", ""+charFilter.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.case-sensitive", ""+caseSensitive.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.numeric", ""+numFilter.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.geometric", ""+geoFilter.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.display-the-table", ""+display.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.select-the-result", ""+select.getState());
        prop.setProperty("org.openjump.core.ui.plugin.queries.SimpleQuery.create-a-new-layer", ""+create.getState());
        
        prop.setProperty("layer_index", ""+layerCB.getSelectedIndex());
        prop.setProperty("layer_name", ""+layerCB.getSelectedValue());
        prop.setProperty("attribute_index", ""+attributeCB.getSelectedIndex());
        prop.setProperty("attribute_name", ""+attributeCB.getSelectedValue());
        prop.setProperty("function_index", ""+functionCB.getSelectedIndex());
        prop.setProperty("function_name", ""+functionCB.getSelectedValue());
        prop.setProperty("operator_index", ""+operatorCB.getSelectedIndex());
        prop.setProperty("operator_name", ""+operatorCB.getSelectedValue());
        prop.setProperty("value", ""+valueCB.getSelectedValue());
        
        BFileChooser bfc = new BFileChooser(BFileChooser.SAVE_FILE,
        		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.save-as"));
        if (bfc.showDialog(this)) {
            try {
                File f = bfc.getSelectedFile();
                prop.store(new FileOutputStream(f), "Query file for Sqi4jump");
            }
            catch(FileNotFoundException e) {
                context.getWorkbenchFrame().warnUser(e.getMessage());
            }
            catch(IOException e) {
                context.getWorkbenchFrame().warnUser(e.getMessage());
            }
        }
        
    }
    
    
    void executeQuery() {
        final QueryDialog queryDialog = this;
        // dirty patch to avoid executing query if the operator combobox is in an invalid state
        // some operators are editable. If the user enter an invalid operator parameter, and then
        // click execute, the new user edited operator is evaluated before execution
        // @TODO improve semantic of operatorChange retrun value
        if (!operatorChanged()) return;
        Runnable runnable = new Runnable() {
            public void run() {
                // runningQuery is set to true while the query is running
                runningQuery=true;
                cancelQuery=false;
                refreshButton.setEnabled(false);
                
                // New condition
                Condition condition = new Condition(queryDialog, context);
                
                comments.setText("<html>" +
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-from") +
                    " \"" + layerCB.getSelectedValue() + "\" " + 
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-where") + " " +
                    condition + "...</html>"
                );
                
                // The FeatureSelection before the query
                FeatureSelection selectedFeatures = context.getLayerViewPanel()
                                                           .getSelectionManager()
                                                           .getFeatureSelection();
                                                           
                // srcFeaturesMap keys are layers to query
                // srcFeaturesMap values are collection of features to query
                CollectionMap srcFeaturesMap = new CollectionMap();
        
                int total = 0; // total number of objects to scan
                int featuresfound = 0;
                if (layerCB.getSelectedIndex() == SELECTION) {
                    for (Iterator it = selectedFeatures.getLayersWithSelectedItems().iterator() ; it.hasNext() ; ) {
                        Layer layer = (Layer)it.next();
                        srcFeaturesMap.put(layer, selectedFeatures.getFeaturesWithSelectedItems(layer));
                    }
                    total = srcFeaturesMap.size();
                }
                else {
                    for (int i = 0 ; i < layers.size() ; i++) {
                        total += ((Layer)layers.get(i)).getFeatureCollectionWrapper().size();
                    }
                }
                
                // Set the selection used as target for geometric operations
                // Bug fixed on 2007-08-10 : selection has index 1 (SELECTION), not 0
                if (operator.type=='G' && valueCB.getSelectedIndex() == SELECTION) {
                    selection = context.getLayerViewPanel().getSelectionManager().getSelectedItems();
                }
                
                // initialize the selection if the select option is true
                if(select.getState()) {selectedFeatures.unselectItems();}
                
                // initialization for infoframe
                InfoFrame info = null;
                if(display.getState()) {
                    info = new InfoFrame(context.getWorkbenchContext(),
                            (LayerManagerProxy)context,
                            (TaskFrame)context.getWorkbenchFrame().getActiveInternalFrame());
                }
                
                // Loop on the requested layers
                int count = 0;
                for (int i = 0 ; i < layers.size() ; i++) {
                    Layer layer = (Layer)layers.get(i);
                    FeatureCollection fc = layer.getFeatureCollectionWrapper();
                    
                    // When the user choose all layers, some attributes are not
                    // available for all attributes
                    if(attributeType!='G' && !fc.getFeatureSchema().hasAttribute(attribute)) continue;
                    
                    //monitor.report(layer.getName());
                    Collection features = null;
                    // case 1 : query only selected features
                    if (layerCB.getSelectedIndex()==1) {
                        features = (Collection)srcFeaturesMap.get(layer);
                    }
                    // other cases : query the whole layer
                    else {
                        features = fc.getFeatures();
                    }
                    // initialize a new dataset
                    progressBarTitle.setText(layer.getName());
                    progressBarTitle.getParent().layoutChildren();
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(total);
                    progressBar.setValue(0);
                    progressBar.setProgressText("0/"+total);
                    progressBar.setShowProgressText(true);
                    
                    FeatureCollection dataset = new FeatureDataset(fc.getFeatureSchema());
                    
                    // initialize a new list for the new selection
                    List<Feature> okFeatures = new ArrayList<Feature>();
                    int mod = 1;
                    if (total > 1000) mod = 10;
                    if (total > 33000) mod = 100;
                    if (total > 1000000) mod = 1000;
                    try {
                        for (Iterator it = features.iterator() ; it.hasNext() ; ) {
                            count++;
                            if (count%mod==0) {
                                progressBar.setProgressText(""+count+"/"+total);
                                progressBar.setValue(count);
                            }
                            Feature f = (BasicFeature)it.next();
                            if (condition.test(f)) {
                                okFeatures.add(f);
                                featuresfound++;
                            }
                            Thread.yield();
                            if (cancelQuery) break;
                        }
                        progressBar.setProgressText(""+count+"/"+total);
                        progressBar.setValue(count);
                    }
                    catch(Exception e) {e.printStackTrace();}
                    if (cancelQuery) break;
                    
                    if (okFeatures.size()==0) continue;
                    
                    // 
                    if(select.getState()) {
                        selectedFeatures.selectItems(layer, okFeatures);
                    }

                    if(create.getState()) {
                        for (Feature f : okFeatures) {
                            dataset.add((Feature)f.clone());
                        }
                        String outputLayerName = layer.getName() + "_";
                        if (attributeType != 'G') {
                            outputLayerName += (attribute + "_");
                        } 
                        outputLayerName += (operator + "_" + value);
                        context.getLayerManager().addLayer(
                            StandardCategoryNames.RESULT, // modified on 2007-08-22
                            outputLayerName, dataset
                        );
                    }
                    if(display.getState()) {
                        info.getModel().add(layer, okFeatures);
                    }
                    
                }
                if (cancelQuery) {
                    initVariables();
                    comments.setText(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.query-interrupted")); 
                    return;
                }
                progressBarTitle.setText(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.result-display"));
                progressBar.setIndeterminate(true);
                
                comments.setText("<html>" +
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-from") +
                    " \"" + layerCB.getSelectedValue() + "\" " + 
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-where") + " " +
                    condition + " : " + featuresfound + " " +
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-found") + "</html>"
                );
                
                // update the selection attribute
                if(select.getState()) {
                    selection = context.getLayerViewPanel().getSelectionManager().getSelectedItems();
                }
                
                if(display.getState()) {
                    info.pack();
                    context.getWorkbenchFrame().addInternalFrame(info);
                }
                
                // init ComboBoxes to add new layers
                if (create.getState()) { initComboBoxes(); }  
                
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());

                //comments.setText("Select from " + layerCB.getSelectedValue() + " where " + condition + " : " + total + " features found");
                initVariables();
            }
        };
        Thread t = new Thread(runnable);
        // Set a low priority to the thread to keep a responsive interface
        // (progresBar progression and interruption command)
        t.setPriority(Thread.currentThread().getPriority()-1);
        // start the thread
        t.start();
    }
    
    private void ok() {executeQuery();}

    private void stop() {if (runningQuery) cancelQuery = true;}
    
    private void refresh() {initComboBoxes();}
    
    private void exit() {
        if (runningQuery) cancelQuery = true;
        setVisible(false);
    }
    
}
