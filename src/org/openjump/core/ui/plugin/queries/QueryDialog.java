package org.openjump.core.ui.plugin.queries;

import java.util.*;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import buoy.event.*;
import buoy.widget.*;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.FeatureSelection;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.I18N;



/**
 * QueryDialog
 * @author Michael MICHAUD
 * version 0.1.0 (4 Dec 2004)
 * version 0.1.1 (15 Jan 2005)
 * version 0.2 (16 Oct 2005)
 * version 0.2.1 (10 aug 2007)
 */ 
public class QueryDialog extends BDialog {
    
    public static final int ALL_LAYERS = 0;
    public static final int SELECTION = 1;
    public static final int SELECTED_LAYERS = 2;
    
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
    
    // if mmpatch is used (mmpatch gives more attribute types), mmaptch must
    // be set to true
    // boolean mmpatch = false; 
    
    // selected features initialized in execute query if "select" option is true
    Collection selection; 
    
    BCheckBox charFilter;
    BCheckBox caseSensitive;
    BCheckBox numFilter;
    BCheckBox geoFilter;

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
    * Main method : create a Query Dialog.
    */
    //public static void main(String[] args) {
        //BFrame frame = new BFrame();
        //QueryDialog qd = new QueryDialog(null);
    //}
    
   /**
    * Overloads createComponent in BDialog, in order to link this BDialog
    * to a swing Window (instead of a WindowWidget).
    */
    //protected JDialog createComponent(Window parent, String title, boolean modal) {
        //return super.createComponent(parent, "", modal); 
    //} 
    
   /**
    * Constructor of a QueryDialog
    * @param context the plugin context
    */
    public QueryDialog(PlugInContext context) {
    	component = super.createComponent(context.getWorkbenchFrame(), "", false);
        //component = createComponent(context.getWorkbenchFrame(), "", false);
        //initInternal();
        //BDialog dialog = new BDialog();
        addEventLink(WindowClosingEvent.class, this, "exit");
        //if (AttributeType.allTypes().size()>6) {
        //    mmpatch = true;
        //}
        this.context = context;
        setTitle(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.query-builder"));
        initUI();
    }
    
  /**
   * Perform internal initialization.
   * Copied from BDialog because it is declared as a private method.
   * (the method is called in QueryDialog constructor)
   */
   /*private void initInternal() {
       component.addComponentListener(new ComponentAdapter() {
           public void componentResized(ComponentEvent ev) {
               if (lastSize == null || !lastSize.equals(component.getSize())) {
                   lastSize = null;
                   layoutChildren();
                   QueryDialog.this.dispatchEvent(new WindowResizedEvent(QueryDialog.this));
               }
               else
                   lastSize = null;
           }
       });
       ((JDialog) component).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
   }*/


   /**
    * User Interface Initialization
    */
    protected void initUI() {
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
                    FormContainer attributeFilterPanel = new FormContainer(2,3);
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
                FormContainer queryConstructorPanel = new FormContainer(5,3);
                queryConstructorPanel.setBackground(Color.decode(
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.color1")
                ));
                queryConstructorPanel.setDefaultLayout(centerNone3);
                BOutline queryConstructorPanelB = new BOutline(queryConstructorPanel, border2);
                
                // PROGRESS BAR
                FormContainer progressBarPanel = new FormContainer(5,1);
                
                // SOUTH PANEL (OK/CANCEL BUTTONS)
            FormContainer southPanel = new FormContainer(8,1);
        
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
            
        // SET THE RESULT OPTIONS
        display = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.display-the-table"), false);
            resultPanel.add(display, 0, 0);
        select = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-the-result"), true);
            resultPanel.add(select, 0, 1);
        create = new BCheckBox(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.create-a-new-layer"), false);
            resultPanel.add(create, 0, 2);
        
        // SET THE COMBO BOXES
        BLabel layerLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.layer"));
            queryConstructorPanel.add(layerLabel, 0, 0, centerNone3);
        BLabel attributeLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.attribute"));
            queryConstructorPanel.add(attributeLabel, 1, 0, centerNone3);
        BLabel functionLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.function"));
            queryConstructorPanel.add(functionLabel, 2, 0, centerNone3);
        BLabel operatorLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.operator"));
            queryConstructorPanel.add(operatorLabel, 3, 0, centerNone3);
        BLabel valueLabel = new BLabel(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.value"));
            queryConstructorPanel.add(valueLabel, 4, 0, centerNone3);
            
        layerCB = new BComboBox();
            layerCB.addEventLink(ValueChangedEvent.class, this, "layerChanged");
            queryConstructorPanel.add(layerCB, 0, 1);
        attributeCB = new BComboBox();
            attributeCB.addEventLink(ValueChangedEvent.class, this, "attributeChanged");
            queryConstructorPanel.add(attributeCB, 1, 1);
        functionCB = new BComboBox();
            functionCB.addEventLink(ValueChangedEvent.class, this, "functionChanged");
            queryConstructorPanel.add(functionCB, 2, 1);
        operatorCB = new BComboBox();
            operatorCB.addEventLink(ValueChangedEvent.class, this, "operatorChanged");
            queryConstructorPanel.add(operatorCB, 3, 1);
        valueCB = new BComboBox();
            valueCB.addEventLink(ValueChangedEvent.class, this, "valueChanged");
            queryConstructorPanel.add(valueCB, 4, 1);
            
        comments = new BLabel(" ");
            queryConstructorPanel.add(comments, 0, 2, 5, 1, centerH3);
            
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
        cancelButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.cancel"));
            cancelButton.addEventLink(CommandEvent.class, this, "cancel");
        stopButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.stop"));
            stopButton.addEventLink(CommandEvent.class, this, "stop");
        refreshButton = new BButton(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.refresh"));
            refreshButton.addEventLink(CommandEvent.class, this, "refresh");
        
        southPanel.add(okButton, 2, 0);
        southPanel.add(refreshButton, 3, 0);
        southPanel.add(cancelButton, 4, 0);
        southPanel.add(stopButton, 5, 0);
        
        dialogContainer.add(northPanel, dialogContainer.NORTH);
        dialogContainer.add(centerPanel, dialogContainer.CENTER);
        dialogContainer.add(southPanel, dialogContainer.SOUTH);
        
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
        //comments.setText("");
        progressBarTitle.setText("");
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setProgressText("");
    }
    
    void initComboBoxes() {
        // INIT layerCB and attributeCB
        layerCB.removeAll();
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.all-layers"));
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selection"));
        layerCB.add(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.selected-layers"));
        
        List layers = context.getLayerManager().getLayers();
        for (int i = 0 ; i < layers.size() ; i++) {
            Layer layer = (Layer)layers.get(i);
            layerCB.add(layer.getName());
        }
        
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
            layers.add(context.getLayerManager().getLayer((String)layerCB.getSelectedValue()));
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
        else if (type==AttributeType.DOUBLE && numFilter.getState()) return true;
        else if (type==AttributeType.DATE) return true;
        /* Special MM attributes, to add if mmpatch is added to the core
        else if (mmpatch && type==AttributeType.LONG && numFilter.getState()) return true;
        else if (mmpatch && type==AttributeType.BOOLEAN && numFilter.getState()) return true;
        else if (mmpatch && type instanceof AttributeType.Char && charFilter.getState()) return true;
        else if (mmpatch && type instanceof AttributeType.Decimal && numFilter.getState()) return true;
        else if (mmpatch && type instanceof AttributeType.Enumeration) return true;
        */
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
                    /* add this test if mmpatch is added to OpenJUMP core
                    if(mmpatch && type instanceof AttributeType.Enumeration) {
                        set.add(att + " (ENUM)");
                        enumerations.put(att, ((AttributeType.Enumeration)type).getEnumerationArray());
                    }
                    */
                    //else {
                        set.add(att + " (" + type.toString().split(" ")[0] + ")");
                    //}
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
    
    public void attributeChanged() {
        String att = (String)attributeCB.getSelectedValue();
        attribute = att.substring(0, att.lastIndexOf(' '));
        String attType = att.substring(att.lastIndexOf('(')+1,
                                       att.lastIndexOf(')'));
        char newat = 'S';
        //if (mmpatch && attType.equals("BOOLEAN")) newat = 'B';
        if (attType.equals("INTEGER")) newat = 'N';
        //else if (mmpatch && attType.equals("LONG")) newat = 'N';
        else if (attType.equals("DOUBLE")) newat = 'N';
        //else if (mmpatch && attType.equals("DECIMAL")) newat = 'N';
        else if (attType.equals("STRING")) newat = 'S';
        //else if (mmpatch && attType.equals("CHAR")) newat = 'S';
        //else if (mmpatch && attType.equals("ENUM")) newat = 'E';
        else if (attType.equals("GEOMETRY")) newat = 'G';
        else;
        // No type change
        if (newat==attributeType) {if (newat=='E' || newat=='S') updateValues();}
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
        String ft = functionCB.getSelectedValue().toString();
        try {
            // added on 11/01/2005
            if (((Function)functionCB.getSelectedValue()).type!=function.type) {
                updateOperators();
                operatorChanged();
                updateValues();
            }
            function = (Function)functionCB.getSelectedValue();
        } catch(Exception e) {}
        // SET IF FUNCTION IS EDITABLE OR NOT
        if(function==Function.SUBS) {
            functionCB.setEditable(true);
            String f = functionCB.getSelectedValue().toString();
            String sub = f.substring(f.lastIndexOf('(')+1, f.lastIndexOf(')'));
            String[] ss = sub.split(",");
            Function.SUBS.args = new int[ss.length];
            if (ss.length>0) Function.SUBS.args[0] = Integer.parseInt(ss[0]);
            if (ss.length>1) Function.SUBS.args[1] = Integer.parseInt(ss[1]);
            functionCB.setSelectedValue(Function.SUBS);
        }
        else if(function==Function.BUFF) {
            functionCB.setEditable(true);
            String f = functionCB.getSelectedValue().toString();
            String sub = f.substring(f.lastIndexOf('(')+1, f.lastIndexOf(')'));
            Function.BUFF.arg = Double.parseDouble(sub);
            functionCB.setSelectedValue(Function.BUFF);
        }
        else {functionCB.setEditable(false);}
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
                operatorCB.setContents(Operator.NUMERIC_OP);
                break;
            case 'G' :
                operatorCB.setContents(Operator.GEOMETRIC_OP);
                break;
            default :
        }
    }
    
    public void operatorChanged() {
        String op = operatorCB.getSelectedValue().toString();
        try {
            if(((Operator)operatorCB.getSelectedValue()).type!=operator.type) {
                updateValues();
            }
            operator = (Operator)operatorCB.getSelectedValue();
        }
        catch(Exception e) {System.out.println(e);}
        if (operator==Operator.WDIST) {
            operatorCB.setEditable(true); // added on 2007-07-02 (bug fix)
            String f = operatorCB.getSelectedValue().toString();
            String sub = f.substring(f.lastIndexOf('(')+1, f.lastIndexOf(')'));
            Operator.WDIST.arg = Double.parseDouble(sub);
            operatorCB.setSelectedValue(Operator.WDIST);
        }
    }
    
   /**
    * Update the possible values list (may be editable or not)
    */
    private void updateValues() {
        if(function==Function.EMPT || function==Function.SIMP ||
           function==Function.VALI || function==Function.BNOF) {
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
        else if (attributeType=='S') {
            valueCB.setContents(availableStrings(attribute, 12));
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
                set.add(((Feature)it.next()).getAttribute(attribute));
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
            if (operatorCB.getItem(operatorIndex)==Operator.WDIST) {
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
        Runnable runnable = new Runnable() {
            public void run() {
                // runningQuery is set to true while the query is running
                runningQuery=true;
                cancelQuery=false;
                
                // New condition
                Condition condition = new Condition(queryDialog, context);
                
                comments.setText(
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-from") +
                    " \"" + layerCB.getSelectedValue() + "\" " + 
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-where") + " " +
                    condition + "..."
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
                
                System.out.println(condition);
                
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
                    List okFeatures = new ArrayList();
                    try {
                        for (Iterator it = features.iterator() ; it.hasNext() ; ) {
                            count++;
                            if (count%10==0) {
                                progressBar.setProgressText(""+count+"/"+total);
                                progressBar.setValue(count);
                            }
                            Feature f = (BasicFeature)it.next();
                            if (condition.test(f)) {
                                okFeatures.add(f);
                                featuresfound++;
                                if (cancelQuery) break;
                                Thread.yield();
                            }
                        }
                        progressBar.setProgressText(""+count+"/"+total);
                        progressBar.setValue(count);
                    }
                    catch(Exception e) {}
                    if (cancelQuery) break;
                    
                    if (okFeatures.size()==0) continue;
                    
                    // 
                    if(select.getState()) {
                        selectedFeatures.selectItems(layer, okFeatures);
                    }
                    
                    if(create.getState() || display.getState()) {
                        dataset.addAll(okFeatures);
                    }
                    if(create.getState()) {
                        context.getLayerManager().addLayer(
                            context.getLayerManager().getCategory(layer).getName(),
                            layer.getName()+"_"+value, dataset
                        );
                    }
                    if(display.getState()) {
                        info.getModel().add(layer, okFeatures);
                        //context.getWorkbenchFrame().addInternalFrame(info);
                    }
                    
                }
                if (cancelQuery) {
                    initVariables();
                    comments.setText(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.query-interrupted")); 
                    return;
                }
                progressBarTitle.setText(I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.result-display"));
                progressBar.setIndeterminate(true);
                
                comments.setText(
                		I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.select-from") +
                    " \"" + layerCB.getSelectedValue() + "\" " + 
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-where") + " " +
                    condition + " : " + featuresfound + " " +
                    I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.features-found")
                );
                
                // update the selection attribute
                if(select.getState()) {
                    selection = context.getLayerViewPanel().getSelectionManager().getSelectedItems();
                }
                
                if(display.getState()) {
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
    
    private void cancel() {setVisible(false);}
    
    private void stop() {if (runningQuery) cancelQuery = true;}
    
    private void refresh() {initComboBoxes();}
    
    private void exit() {setVisible(false);}
    
}
