/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */
/***********************************************
 * created on 		21.Nov.2007
 * last modified: 	08.May.2008
 * 
 * author:			sstein
 * 
 * description:
 * 
 * 
 ***********************************************/
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.openjump.core.apitools.FeatureSchemaTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class CalcVarianceAndMeanPerClassPlugIn extends AbstractPlugIn implements ThreadedPlugIn{
    
    private MultiInputDialog dialog;
    
    private String sidetext = "Calculates mean and variance for a specified attribute " +
            "in accordance with the classes, which need to be given";
    private String OLAYER = "select origin layer";
    private String ATTRIBUTEA = "Select attribute to calculate ratio from";
    private String ATTRIBUTEB = "Select attribute with classification";
    private Layer selLayerO = null; 
    private String selAttributeA = null;
    private String selAttributeB = null;
    
    private String sName = "Calculate Mean and Variance Per Class";
    private String sWrongDataType = "Wrong datatype of chosen attribute";
    private String sSearch = "reading data and search for complement";
    private String sCalcRatios = "calculating ratios";
	
    /**
     * this method is called on the startup by JUMP/OpenJUMP.
     * We set here the menu entry for calling the function.
     */
    public void initialize(PlugInContext context) throws Exception {    	
    	
        sidetext = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.descriptiontext");
        OLAYER = GenericNames.SELECT_LAYER;
        ATTRIBUTEA = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.Select-attribute-to-calculate-statistics-from");
        ATTRIBUTEB = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.Select-attribute-with-classification");
        sName = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.Calculate-Mean-and-Variance-Per-Class");
        sWrongDataType = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");
        sSearch = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.reading-data-and-searching-for-complement");
        sCalcRatios = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.calculating-statistics");	
    	
    	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuItem(
    			this,                               //exe
    			//new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},     //menu path
    			new String[] {MenuNames.TOOLS, MenuNames.STATISTICS},
    			this.sName + "...", //name methode .getName recieved by AbstractPlugIn 
    			false,          //checkbox
    			null,           //icon
    			createEnableCheck(context.getWorkbenchContext())); //enable check   
        	
    }

    /**
     * This method is used to define when the menu entry is activated or
     * disabled. In this example we allow the menu entry to be usable only
     * if one layer exists.
     */
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                        .add(checkFactory.createTaskWindowMustBeActiveCheck());
    }
    
    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI /user-input things
     * In this example we call two additional methods {@link #setDialogValues(MultiInputDialog, PlugInContext)}
     * and {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the buffer radius by the user. 
     */
    public boolean execute(PlugInContext context) throws Exception{
    	    	
        this.reportNothingToUndoYet(context);         
        
            dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), sName, true);
            this.setDialogValues(dialog, context);
            GUIUtil.centreOnWindow(dialog);
            dialog.setVisible(true);
            if (! dialog.wasOKPressed()) { return false; }
            this.getDialogValues(dialog);
            
        return true;
    }
    
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        calcClass(context, monitor, this.selLayerO);
		
	}
    
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.setSideBarDescription(this.sidetext);
        dialog.addLayerComboBox(OLAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        List listO = FeatureSchemaTools.getFieldsFromLayerWithoutGeometry(context.getCandidateLayer(0));
        Object valA = listO.size()>0?listO.iterator().next():null;
        Object valB = listO.size()>0?listO.iterator().next():null;
        final JComboBox jcb_attributeA = dialog.addComboBox(ATTRIBUTEA, valA, listO,ATTRIBUTEA);
        if (listO.size() == 0) jcb_attributeA.setEnabled(false);
        final JComboBox jcb_attributeB = dialog.addComboBox(ATTRIBUTEB, valB, listO,ATTRIBUTEB);
        if (listO.size() == 0) jcb_attributeB.setEnabled(false);              
        
        dialog.getComboBox(OLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = getFieldsFromLayerWithoutGeometryO();
                if (list.size() == 0) {
                    jcb_attributeA.setModel(new DefaultComboBoxModel(new String[0]));
                    jcb_attributeA.setEnabled(false);
                }
                jcb_attributeA.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
            }            
        });

        dialog.getComboBox(OLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = getFieldsFromLayerWithoutGeometryO();
                if (list.size() == 0) {
                    jcb_attributeB.setModel(new DefaultComboBoxModel(new String[0]));
                    jcb_attributeB.setEnabled(false);
                }
                jcb_attributeB.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
            }            
        });
              
      }

    private void getDialogValues(MultiInputDialog dialog) {
        //this.itemlayer = dialog.getLayer(this.CLAYER);
        this.selLayerO = dialog.getLayer(OLAYER);
        this.selAttributeA = dialog.getText(ATTRIBUTEA);
        this.selAttributeB = dialog.getText(ATTRIBUTEB);  
      }
    
    private boolean calcClass(final PlugInContext context, TaskMonitor monitor,Layer orgLayer) throws Exception {
        
        // ======================================================
        // collect attribute values for every object
        // ======================================================
        FeatureCollection fcO = orgLayer.getFeatureCollectionWrapper();
        FeatureSchema fsO = fcO.getFeatureSchema();  
        //assuming that all classes are stored with same type
        AttributeType typeA = null;        
        AttributeType typeB = null;
        if ((fsO.getAttributeType(this.selAttributeA) == AttributeType.DOUBLE) || 
                (fsO.getAttributeType(this.selAttributeA) == AttributeType.INTEGER))
                {
            //move on
            typeA = fsO.getAttributeType(this.selAttributeA);
            typeB = fsO.getAttributeType(this.selAttributeB);
        }
        else{
            //System.out.println("ClassifyAttributesPlugIn: wrong datatype of chosen attribute");
			context.getWorkbenchFrame().warnUser(sWrongDataType);
            return false;
        }
        
        double[] data = new double[fcO.size()];
        int[] classes = new int[fcO.size()]; //store class id        
        int[] exists = new int[fcO.size()]; //to store if value should be included in calculation: 1 = yes, 0 = no
        									//can be used to define a value that needs to be excluded from calculations
        
        int i=0;
        for (Iterator iter = fcO.iterator(); iter.hasNext();) {
            monitor.report(i, fcO.size(), sSearch);
            Feature f = (Feature) iter.next();
            Object valA = f.getAttribute(this.selAttributeA); // get ratio attribute 
            Object valB = f.getAttribute(this.selAttributeB); // get class nr
            if (typeA == AttributeType.DOUBLE){
                data[i] = ((Double)valA).doubleValue();
            }
            else if (typeA == AttributeType.INTEGER){
                data[i] = ((Integer)valA).intValue();
            }
            //-- class nr
            if (typeB == AttributeType.DOUBLE){
                classes[i] = ((Double)valB).intValue();
            }
            else if (typeB == AttributeType.INTEGER){
                classes[i] = ((Integer)valB).intValue();
            }             
            //-- set global for now (maybe refine it later to exclude specific values)
            exists[i] = 1;
            i++;
        } 
        // ======================================================
        // calculate ratio for every class
        // ======================================================
        monitor.report(sCalcRatios);
        double[] var = org.math.array.StatisticSample.fill(fcO.size(), -9999.0);
        double[] mean = org.math.array.StatisticSample.fill(fcO.size(), -9999.0);
        
        int[] diffClasses = this.getDifferentClassValues(classes);
        
        for (int j = 0; j < diffClasses.length; j++) {
            int actualClass = diffClasses[j];            
            int[] idxValsOfSameClass =  getIndexValuesOfSameClass(classes, actualClass);
            int[] existingIdxVals = reduceToExisting(idxValsOfSameClass, exists);
            double[] vals = getValuesToIndex(data,existingIdxVals);
            double meanC = org.math.array.StatisticSample.mean(vals); 
            double varC = org.math.array.StatisticSample.variance(vals);
            for (int k = 0; k < existingIdxVals.length; k++) {
                var[existingIdxVals[k]]= varC;
                mean[existingIdxVals[k]]= meanC;
            }
        }        
        
        
        // ======================================================
        // create layer with new field for results
        // ======================================================
        
        FeatureDataset fd = null;
        ArrayList outData = new ArrayList();
        FeatureSchema targetFSnew = null;
        int count=0;        
        Iterator iterp = fcO.iterator();     
        String attnameB = this.selAttributeA + "_mean";        
        String attname = this.selAttributeA + "_var";
        while(iterp.hasNext()){         
            count=count+1;
//          if(monitor != null){
//              monitor.report("item: " + count + " of " + size);
//          }
            Feature p = (Feature)iterp.next();
            if (count == 1){
                FeatureSchema targetFs = p.getSchema();
                targetFSnew = FeatureSchemaTools.copyFeatureSchema(targetFs);
                if (targetFSnew.hasAttribute(attnameB)){
                    //attribute will be overwriten
                }
                else{
                    //add attribute                    
                    targetFSnew.addAttribute(attnameB, AttributeType.DOUBLE);
                }
                if (targetFSnew.hasAttribute(attname)){
                    //attribute will be overwriten
                }
                else{
                    //add attribute                    
                    targetFSnew.addAttribute(attname, AttributeType.DOUBLE);
                }                
            }
            //-- evaluate value for every polygon           
            Feature fcopy = FeatureSchemaTools.copyFeature(p, targetFSnew);
            //fcopy.setAttribute(this.selClassifier, new Integer(classes[count-1]));
            fcopy.setAttribute(attnameB, new Double(mean[count-1]));
            fcopy.setAttribute(attname, new Double(var[count-1]));
            outData.add(fcopy);
        }
        fd = new FeatureDataset(targetFSnew);  
        fd.addAll(outData); 

        String name = this.selAttributeA + "_meanvar";
        context.addLayer(StandardCategoryNames.WORKING, name, fd);

        return true;
    }

    public double[] getValuesToIndex(double[] data, int[] existingIdxVals) {
        double[] vals = new double[existingIdxVals.length];
        for (int i = 0; i < existingIdxVals.length; i++) {
            vals[i] = data[existingIdxVals[i]];
        }
        return vals;
    }

    /**
     * 
     * @param idxValsOfSameClass
     * @param exists containing values of 0 = not existing, 1= existing 
     * @return
     */
    private int[] reduceToExisting(int[] idxValsOfSameClass, int[] exists) {
        int[] idxVals = null;
        ArrayList<Integer> vals = new ArrayList<Integer>(); 
        for (int i = 0; i < idxValsOfSameClass.length; i++) {
            if (exists[i] == 1){
                vals.add(new Integer(idxValsOfSameClass[i]));
            }
        }        
        idxVals = new int[vals.size()];
        int i = 0; 
        for (Iterator iter = vals.iterator(); iter.hasNext();) {
            Integer idx = (Integer) iter.next();            
            idxVals[i] = idx;
            i++;
        }
        return idxVals;
    }

    /**
     * 
     * @param classes
     * @param actualClass the reference class value to search for
     * @return all position index values for items that belong to the "actualClass"
     */
    public int[] getIndexValuesOfSameClass(int[] classes, int actualClass) {
        int[] idxVals = null;
        ArrayList<Integer> vals = new ArrayList<Integer>(); 
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == actualClass){
                vals.add(new Integer(i));
            }
        }        
        idxVals = new int[vals.size()];
        int i = 0; 
        for (Iterator iter = vals.iterator(); iter.hasNext();) {
            Integer idx = (Integer) iter.next();            
            idxVals[i] = idx;
            i++;
        }
        return idxVals;
    }
   
    /**
     * 
     * @param classes
     * @return all classes that are found in the array
     */
    public int[] getDifferentClassValues(int[] classes) {
        int[] classVals = null;
        ArrayList<Integer> vals = new ArrayList<Integer>(); 
        for (int i = 0; i < classes.length; i++) {
            if (i==0){ 
                //-- add first class directly
                vals.add(new Integer(classes[i]));
            }
            else{
                //-- search if already added
                //   TODO: make while loop to avoid parsing always the full list
                boolean found = false; 
                for (Iterator iter = vals.iterator(); iter.hasNext();) {
                    Integer existingClass = (Integer) iter.next();
                    if(classes[i] == existingClass.intValue()){
                        found = true;
                    }
                }
                if (found == false){
                    vals.add(new Integer(classes[i]));
                }
            }
                
        }        
        classVals = new int[vals.size()];
        int i = 0; 
        for (Iterator iter = vals.iterator(); iter.hasNext();) {
            Integer idx = (Integer) iter.next();            
            classVals[i] = idx;
            i++;
        }
        return classVals;
    }
    
    private List getFieldsFromLayerWithoutGeometryO() {
        return FeatureSchemaTools.getFieldsFromLayerWithoutGeometry(dialog.getLayer(OLAYER));
    }
    

}
