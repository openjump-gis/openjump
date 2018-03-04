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
 * created on 		19.10.2007
 * last modified: 	
 * 
 * author:			sstein
 ***********************************************/
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;

import org.openjump.core.apitools.FeatureSchemaTools;
import org.openjump.core.attributeoperations.Classifier1D;
import org.openjump.core.ui.plot.Plot2DPanelOJ;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
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

//TODO: remove for other classification plugins the "save" option (taken from histogram plugin)
public class ClassifyAttributesPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private MultiInputDialog dialog;
    
    private String sideBarText = "Classifies attribute data with the chosen method.\n" +
    							 "The result is added as new field to the attribute table.";
    private String CLASSIFIER = "select classification method";
    private String T2 = "number of classes";
    private String CLAYER = "select layer";
    private String ATTRIBUTE = "select attribute";
    private String OPTIMIZEWITHKMEANS = "optimize with k-means" ;
    private String PROCESSNULLASZERO = "process null as zero";
    
    private String sClassbreaks = "class breaks";
    private String sDatapoints = "data points";
    private String sCount = "count";
    private String sHistogram = "Histogram";
    private String sCalculateBreaks = "Calculate breaks";
    private String sDisplayBreaks = "Display Breaks";
    private String sClassifying = "classifying";
    private String sAddingField = "adding field";
    
    private Layer selLayer = null;
    private int ranges = 7; 
    private FeatureCollection fc = null;
    private String selAttribute = null;
    private String selClassifier = null;
    private Boolean useKmeans = false;
    private boolean nullAsZero = false;
    
    private String sName = "Classify Attributes";
	private String sWarning = "problems appeared";
	private String sNotEnoughValuesWarning = "valid values is not enough";
    private String sWrongDataType = "Wrong datatype of chosen attribute";
    private String sNoAttributeChoosen = "No attribute choosen";
    
    private Plot2DPanelOJ plot;
	
    /**
     * this method is called on the startup by JUMP/OpenJUMP.
     * We set here the menu entry for calling the function.
     */
    public void initialize(PlugInContext context) throws Exception {

        sideBarText = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.descriptiontext");
        CLASSIFIER = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Select-classification-method");
        T2 = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Number-of-classes");
        CLAYER = GenericNames.SELECT_LAYER;
        ATTRIBUTE = GenericNames.SELECT_ATTRIBUTE;
        OPTIMIZEWITHKMEANS = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Optimize-with-k-means");
        PROCESSNULLASZERO = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Process-null-as-zero");
        sClassbreaks = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.class-breaks");
        sDatapoints = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.data-points");
        sCount = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.count");
        sHistogram = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateHistogramPlugIn.Histogram-Plot");
        sCalculateBreaks = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Calculating-Breaks");
        sDisplayBreaks = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Displaying-Breaks");
        sClassifying = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.classifying");
        sAddingField = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.create-output-field");
        sName = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Classify-Attribute");
        sWarning = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Error-during-classification");
        sNotEnoughValuesWarning = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Not-enough-values");
        sWrongDataType = I18N.get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");
        sNoAttributeChoosen = I18N.get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.No-attribute-choosen");
        
    	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuPlugin(
    			this,
    			new String[] {MenuNames.TOOLS, MenuNames.STATISTICS},
    			this.sName + "...", //name methode .getName recieved by AbstractPlugIn 
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
		//-- get the LM because when the Histogram will be shown, the app. focus 
		//   will change and context.addLayer will not work (null pointer exc.)
		// [mmichaud 2012-04-09] to completely resolve this problem, the new
		// JInternalFrame is added after addLayer method has been called
		LayerManager currentLM = context.getLayerManager();
    	monitor.allowCancellationRequests();
        if (this.selAttribute == null) {
		    context.getWorkbenchFrame().warnUser(I18N.get(sNoAttributeChoosen));
		    return;
		}
		javax.swing.JInternalFrame internalFrame = context.getWorkbenchFrame().getActiveInternalFrame();
		FeatureDataset result = classifyAndCreatePlot(monitor, context);
		context.getWorkbenchFrame().activateFrame(internalFrame);
		if (result == null) {
		    context.getWorkbenchFrame().warnUser(I18N.get(sNotEnoughValuesWarning));			
		}
		else if(result.size() > 0){
			String name = this.selAttribute + "_" + this.selClassifier;
			currentLM.addLayer(StandardCategoryNames.WORKING, name, result);
			JInternalFrame frame = new JInternalFrame(this.sHistogram);
            frame.setLayout(new BorderLayout());
            frame.add(plot, BorderLayout.CENTER);
            frame.setClosable(true);
            frame.setResizable(true);
            frame.setMaximizable(true);
            frame.setSize(450, 450);
            context.getWorkbenchFrame().addInternalFrame(frame);
            plot = null;
		}
		else{
			context.getWorkbenchFrame().warnUser(sWarning);			
		}		
	}
    
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    	
		dialog.setSideBarDescription(sideBarText);
    	
        dialog.addLayerComboBox(CLAYER, context.getCandidateLayer(0), context.getLayerManager());
        
        List<String> listNumAttributes = FeatureSchemaTools
                .getFieldsFromLayerWithoutGeometryAndString(context.getCandidateLayer(0));
        Object valAttribute = listNumAttributes.size()>0?listNumAttributes.iterator().next():null;
        final JComboBox<String> jcb_attribute = dialog
                .addComboBox(this.ATTRIBUTE, valAttribute, listNumAttributes, this.ATTRIBUTE);
        if (listNumAttributes.size() == 0) jcb_attribute.setEnabled(false);
        
        List listClassifiers = Classifier1D.getAvailableClassificationMethods();
        Object valClassifier = listNumAttributes.size()>0?listNumAttributes.iterator().next():null;
        dialog.addComboBox(this.CLASSIFIER, valClassifier, listClassifiers, this.CLASSIFIER);
        
        dialog.addIntegerField(T2, this.ranges, 6, T2);
        
        dialog.addCheckBox(this.OPTIMIZEWITHKMEANS, false);
        
        dialog.addCheckBox(this.PROCESSNULLASZERO, false);
        
        dialog.getComboBox(CLAYER).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List<String> list = getFieldsFromLayerWithoutGeometryAndString();
                if (list.size() == 0) {
                    jcb_attribute.setModel(new DefaultComboBoxModel<>(new String[0]));
                    jcb_attribute.setEnabled(false);
                }
                else {
                    jcb_attribute.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
                    jcb_attribute.setEnabled(true);
                }
            }
        });        
      }

    private void getDialogValues(MultiInputDialog dialog) {
        this.ranges = dialog.getInteger(T2);
        this.selLayer = dialog.getLayer(CLAYER);
        this.fc = this.selLayer.getFeatureCollectionWrapper();
        this.selAttribute = dialog.getText(ATTRIBUTE);
        this.selClassifier = dialog.getText(this.CLASSIFIER);
        this.useKmeans = dialog.getBoolean(this.OPTIMIZEWITHKMEANS);
        this.nullAsZero = dialog.getBoolean(this.PROCESSNULLASZERO);
    }
    
    private FeatureDataset classifyAndCreatePlot(TaskMonitor monitor, final PlugInContext context) throws Exception {
        
    	monitor.report(this.sCalculateBreaks);
        //=============== get DATA and prepare ==============/ 
        FeatureSchema fs = this.fc.getFeatureSchema();
        AttributeType type;
        if ((fs.getAttributeType(this.selAttribute) == AttributeType.DOUBLE) || 
                (fs.getAttributeType(this.selAttribute) == AttributeType.INTEGER)){
            //-- move on
            type = fs.getAttributeType(this.selAttribute);
        }
        else{
            //System.out.println("ClassifyAttributesPlugIn: wrong datatype of chosen attribute");
			context.getWorkbenchFrame().warnUser(sWrongDataType);
            return null;
        }
        
        int size = getFeatureCollectionSize(this.fc, this.selAttribute, this.nullAsZero);
        if (size < 3) {
            return null;
        }
        this.ranges = Math.min(this.ranges, size);
        
        
        double[] data = new double[size];
        double[][] plotdata = new double[2][size]; //for drawing 1-D scatter plot
        int[] fID = new int[size];
        int i = 0;
        for (Iterator iter = fc.iterator(); iter.hasNext();) {
            Feature f = (Feature) iter.next();
            if (f.getAttribute(this.selAttribute)==null && !nullAsZero) continue;
            fID[i] = f.getID();
            plotdata[1][i] = 1;
            Object val = f.getAttribute(this.selAttribute);
            if (type == AttributeType.DOUBLE){
                if (val == null) data[i] = 0.0;
                else data[i] = (Double)val;
            }
            else if (type == AttributeType.INTEGER){
                if (val == null) data[i] = 0;
                else data[i] = (Integer)val;
            }               
            plotdata[0][i] = data[i];
            i++;
        } 
        /*
        //-- some testdata
        double[][] plotdata2 = new double[2][8]; 
        double[] data2 = { -2, 4, 6, 5, 0, 10, 7, 1 };
        double[] axis2 =  {  1, 1, 1, 1, 1, 1, 1, 1 };
        plotdata2[0] = data2;
        plotdata2[1] = axis2;
        */
        
        if(monitor.isCancelRequested()){
        	return null;
        }
        
        //=============== find breaks according to chosen method ==============/ 
        double[] limits = null;
        
        if (this.useKmeans == false){
	        if (this.selClassifier == Classifier1D.EQUAL_NUMBER){
	        	limits = Classifier1D.classifyEqualNumber(data, this.ranges);
	        }
	        else if(this.selClassifier == Classifier1D.EQUAL_RANGE){
	        	limits = Classifier1D.classifyEqualRange(data, this.ranges);
	        }
	        else if(this.selClassifier == Classifier1D.MEAN_STDEV){
	        	limits = Classifier1D.classifyMeanStandardDeviation(data, this.ranges);
	        }
	        else if(this.selClassifier == Classifier1D.MAX_BREAKS){
	        	limits = Classifier1D.classifyMaxBreaks(data, this.ranges);
	        }
	        else if(this.selClassifier == Classifier1D.JENKS_BREAKS){
	        	limits = Classifier1D.classifyNaturalBreaks(data, this.ranges);
	        }
        }
        else{
	        if (this.selClassifier == Classifier1D.EQUAL_NUMBER){
	        	limits = Classifier1D.classifyKMeansOnExistingBreaks(data, this.ranges, 3);
	        }
	        else if(this.selClassifier == Classifier1D.EQUAL_RANGE){
	        	limits = Classifier1D.classifyKMeansOnExistingBreaks(data, this.ranges, 2 );
	        }
	        else if(this.selClassifier == Classifier1D.MEAN_STDEV){
	        	limits = Classifier1D.classifyKMeansOnExistingBreaks(data, this.ranges, 4 );
	        }
	        else if(this.selClassifier == Classifier1D.MAX_BREAKS){
	        	limits = Classifier1D.classifyKMeansOnExistingBreaks(data, this.ranges, 1 );
	        }
	        else if(this.selClassifier == Classifier1D.JENKS_BREAKS){
	        	limits = Classifier1D.classifyKMeansOnExistingBreaks(data, this.ranges, 5 );
	        }
        }
                
        if(monitor.isCancelRequested()){
        	return null;
        }
        
    	monitor.report(this.sDisplayBreaks);
        //=============== plot data and class breaks ==============/ 
      	//-- do display here - in case we later want to allow interactive editing of the limits
    	
        //-- reformat limits        
        double[][] limits2show = new double[2][limits.length];
        //-- due to bug in jmathplot add limits twice if only three classes = 2breaks are sought
        if (limits.length == 2){
        	limits2show = new double[2][limits.length*2];
        }
        for (int j = 0; j < limits.length; j++) {
            limits2show[0][j]= limits[j];   //x-axis
            limits2show[1][j]= Math.floor(i/(4.0*this.ranges));  //y-axis, estimate height of "bar" from number of items
            //limits2show[1][j]= 1;
            //-- due to bug in jmathplot add limits twice if only three classes are sought
            if (limits.length == 2){
            	limits2show[0][limits.length+j]= limits[j];
            	limits2show[1][limits.length+j]= Math.floor(i/(4.0*this.ranges));
            }
        }
        
        //=============== plot data and class breaks ==============/
        //-- create plots
        /*final Plot2DPanelOJ*/ plot = new Plot2DPanelOJ();        
        plot.addHistogramPlotOJ(this.selAttribute, data, this.ranges*3, context, selLayer, this.selAttribute);
        plot.addScatterPlotOJ(this.sDatapoints, plotdata, fID, context, this.selLayer);
        plot.addBarPlot(this.sClassbreaks, limits2show);
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, this.selAttribute);
        plot.setAxisLabel(1, this.sCount);
        plot.addLegend("SOUTH");
        
        // [mmichaud 2012-04-09] Moved in run method after the addLayer method
        // to avoid the problem of the focus change
        
        //JInternalFrame frame = new JInternalFrame(this.sHistogram);
        //frame.setLayout(new BorderLayout());
        //frame.add(plot, BorderLayout.CENTER);
        //frame.setClosable(true);
        //frame.setResizable(true);
        //frame.setMaximizable(true);
        //frame.setSize(450, 450);
        //frame.setVisible(true);
        
        //context.getWorkbenchFrame().addInternalFrame(frame);
        
        //=============== classify data ==============/
        if(monitor.isCancelRequested()){
        	return null;
        }
    	monitor.report(this.sClassifying); 
    	int[] classes = Classifier1D.classifyData(data, limits);
    	//double[] classes = org.math.array.StatisticSample.one(data.length);
    	//context.getWorkbenchFrame().warnUser("classification not yet implemented");
    	
        //=============== add field ==============/
        if(monitor.isCancelRequested()){
        	return null;
        }
    	monitor.report(sAddingField); 
	    
    	FeatureDataset fd;
    	ArrayList<Feature> outData = new ArrayList<>();
    	FeatureSchema targetFSnew = null;
	    int count=0;	    
	    Iterator iterp = fc.iterator();	    
	    String attname = this.selAttribute + "_" + this.selClassifier;
	    while(iterp.hasNext()){	    	
	    	//count=count+1;
//	    	if(monitor != null){
//	    	    monitor.report("item: " + count + " of " + size);
//	    	}
	    	Feature p = (Feature)iterp.next();
	    	Object val = p.getAttribute(this.selAttribute);
	    	if (val == null && !this.nullAsZero) continue;
	    	else count++;
	    	if (count == 1){
	    	    FeatureSchema targetFs = p.getSchema();
	    	    targetFSnew = FeatureSchemaTools.copyFeatureSchema(targetFs);
	    	    if (targetFSnew.hasAttribute(attname)){
	    	        //attribute will be overwriten
	    	    }
	    	    else{
	    	        //add attribute
	    	        targetFSnew.addAttribute(attname, AttributeType.INTEGER);
	    	    }
	    	}
	    	//-- evaluate value for every polygon 	    	
	    	Feature fcopy = FeatureSchemaTools.copyFeature(p, targetFSnew);
	    	//fcopy.setAttribute(this.selClassifier, new Integer(classes[count-1]));
	    	fcopy.setAttribute(attname, classes[count-1]);
	    	outData.add(fcopy);
	    }
    	fd = new FeatureDataset(targetFSnew);  
    	fd.addAll(outData);	
        return fd;
    }
    
    private int getFeatureCollectionSize(FeatureCollection fc, String attribute, boolean nullAsZero) {
        int size = 0;
        for (Iterator it = fc.iterator(); it.hasNext();) {
            Feature f = (Feature) it.next();
            if (nullAsZero || f.getAttribute(attribute)!=null) size++;
        }
        return size;
    }
    
    private List<String> getFieldsFromLayerWithoutGeometryAndString() {
        return FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(dialog.getLayer(CLAYER));
    }
    
}
