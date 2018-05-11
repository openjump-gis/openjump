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
import java.util.*;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.ui.*;
import org.apache.commons.lang3.ArrayUtils;
import org.math.array.StatisticSample;
import org.openjump.core.apitools.FeatureSchemaTools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class CalcVarianceAndMeanPerClassPlugIn extends AbstractPlugIn implements ThreadedPlugIn{
    
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
        sName = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn");
        sWrongDataType = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");
        sCalcRatios = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CalcVarianceAndMeanPerClassPlugIn.calculating-statistics");
    	
    	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuPlugin(
    			this,
    			new String[] {MenuNames.TOOLS, MenuNames.STATISTICS},
    			this.sName + "...",
    			false,              //checkbox
    			null,               //icon
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
                .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI /user-input things
     * In this example we call two additional methods {@link #setDialogValues(MultiInputDialog, PlugInContext)}
     * and {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the buffer radius by the user. 
     */
    public boolean execute(PlugInContext context) throws Exception{
    	    	
        this.reportNothingToUndoYet(context);

            MultiInputDialog dialog = new MultiInputDialog(
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
    
    private void setDialogValues(final MultiInputDialog dialog, final PlugInContext context) {

        dialog.setSideBarDescription(this.sidetext);
        dialog.addLayerComboBox(OLAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        final List<String> numAttributes = AttributeTypeFilter.NUMERIC_FILTER.filter(context.getCandidateLayer(0));
        String valA = numAttributes.size()>0?numAttributes.get(0):null;
        final JComboBox<String> jcb_attributeA = dialog.addComboBox(ATTRIBUTEA, valA, numAttributes, ATTRIBUTEA);
        if (numAttributes.size() == 0) jcb_attributeA.setEnabled(false);

        final List<String> classAttributes = AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(context.getCandidateLayer(0));
        String valB = classAttributes.size()>0?classAttributes.get(0):null;
        final JComboBox<String> jcb_attributeB = dialog.addComboBox(ATTRIBUTEB, valB, classAttributes, ATTRIBUTEB);
        if (classAttributes.size() == 0) jcb_attributeB.setEnabled(false);
        
        dialog.getComboBox(OLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List<String> list = AttributeTypeFilter.NUMERIC_FILTER.filter(dialog.getLayer(OLAYER));
                if (list.size() == 0) {
                    jcb_attributeA.setModel(new DefaultComboBoxModel<>(new String[0]));
                    jcb_attributeA.setEnabled(false);
                }
                jcb_attributeA.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
            }            
        });

        dialog.getComboBox(OLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List<String> list = AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(dialog.getLayer(OLAYER));
                if (list.size() == 0) {
                    jcb_attributeB.setModel(new DefaultComboBoxModel<>(new String[0]));
                    jcb_attributeB.setEnabled(false);
                }
                jcb_attributeB.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
            }            
        });
              
      }

    private void getDialogValues(MultiInputDialog dialog) {
        this.selLayerO = dialog.getLayer(OLAYER);
        this.selAttributeA = dialog.getText(ATTRIBUTEA);
        this.selAttributeB = dialog.getText(ATTRIBUTEB);  
    }
    
    private boolean calcClass(final PlugInContext context, TaskMonitor monitor, Layer orgLayer) throws Exception {

        FeatureCollection fcO = orgLayer.getFeatureCollectionWrapper();
        FeatureSchema fsO = fcO.getFeatureSchema();

        // ======================================================
        // collect attribute values for every object
        // ======================================================

        AttributeType typeA = fsO.getAttributeType(this.selAttributeA);
        if ((typeA != AttributeType.DOUBLE && typeA != AttributeType.INTEGER && typeA != AttributeType.LONG)) {
            context.getWorkbenchFrame().warnUser(sWrongDataType);
            return false;
        }

        Map<Object,List<Double>> classes = new HashMap<>();
        Map<Object,double[]> mean_var = new HashMap<>();
        monitor.report(sCalcRatios);
        for (Feature feature : fcO.getFeatures()) {
            Object key = feature.getAttribute(selAttributeB);
            Object val = feature.getAttribute(selAttributeA);
            List<Double> vals = classes.get(key);
            if (vals == null) {
                vals = new ArrayList<>();
                classes.put(key, vals);
            }
            if (val != null) {
                vals.add(((Number)val).doubleValue());
            }
        }

        // ======================================================
        // compute statistics
        // ======================================================
        for (Map.Entry<Object,List<Double>> entry : classes.entrySet()) {
            double[] vals = ArrayUtils.toPrimitive(entry.getValue().toArray(new Double[0]));
            mean_var.put(entry.getKey(), new double[]{
                    (vals==null||vals.length==0)?Double.NaN:StatisticSample.mean(vals),
                    (vals==null||vals.length==0)?Double.NaN:StatisticSample.variance(vals),
            });
        }

        // ======================================================
        // create layer with new field for results
        // ======================================================

        FeatureSchema newFeatureSchema = fsO.clone();
        if (!newFeatureSchema.hasAttribute(selAttributeA + "_mean")) {
            newFeatureSchema.addAttribute(selAttributeA + "_mean", AttributeType.DOUBLE);
        }
        if (!newFeatureSchema.hasAttribute(selAttributeA + "_var")) {
            newFeatureSchema.addAttribute(selAttributeA + "_var", AttributeType.DOUBLE);
        }
        FeatureDataset fd = new FeatureDataset(newFeatureSchema);
        for (Feature feature : fcO.getFeatures()) {
            Feature newFeature = FeatureSchemaTools.copyFeature(feature, newFeatureSchema);
            newFeature.setAttribute(selAttributeA + "_mean", mean_var.get(feature.getAttribute(selAttributeB))[0]);
            newFeature.setAttribute(selAttributeA + "_var", mean_var.get(feature.getAttribute(selAttributeB))[1]);
            fd.add(newFeature);
        }
        String name = this.selAttributeA + "_mean_var";
        context.addLayer(StandardCategoryNames.WORKING, name, fd);

        return true;
    }

}
