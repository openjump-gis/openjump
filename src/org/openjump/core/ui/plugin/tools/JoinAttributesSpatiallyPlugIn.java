/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
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
/*****************************************************
 * created: 22.06.2006
 * last modified:  
 * 
 * @author sstein
 * 
 * Merges attributes according to some spatial and statistical criteria
 * from one dataset to another 
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.openjump.core.attributeoperations.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


/**
 * Merges attributes according to some spatial and statistical criteria
 * from one dataset to another 
 * 
 * @author sstein
 *
 **/
public class JoinAttributesSpatiallyPlugIn extends ThreadedBasePlugIn{
	

    private String sidebartext = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.Joins-attributes-of-source-layer-according-to-a-spatial-and-a-statistic-criterion");
	private String SRC_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.source-layer");
	private String TGT_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.target-layer");	
	private String SRC_ATTRIB = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-attribute");
	private String ATTRIB_OP = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-attribute-operation");	
	private String SPATIAL_OP = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-spatial-operation");	
	private String joinresult = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.join-result");
	private String notimplemented= I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.not-implemented");
	private String BUFFER_RADIUS = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.buffer-radius");
	//-- vars
	private Layer srcLayer = null;
	private Layer targetLayer = null;
	private String attrName = "";
	int attributeOperation = 0;
	int spatialOperation = 0;	
	private double bradius = 0.0; 
	    
	private MultiInputDialog dialog;	
	private PlugInContext pc = null;
	
	ArrayList attrOpList = new ArrayList();
	ArrayList spatialOpList = new ArrayList();
	
	public void initialize(PlugInContext context) throws Exception {
	    
		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
		featureInstaller.addMainMenuItem(
				this,								//exe				
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS, MenuNames.TWOLAYERS}, 	//menu path
				this.getName()+"{pos:5}", //name methode .getName recieved by AbstractPlugIn 
				false,			//checkbox
				null,			//icon
				createEnableCheck(context.getWorkbenchContext())); //enable check        
	}
	
	public String getName(){
		return I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.Join-Attributes-Spatially") + "...";
	}
	
	public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
		
		return new MultiEnableCheck()
		.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
		.add(checkFactory.createAtLeastNLayersMustExistCheck(2))
		/*.add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(2)*)*/;
	}
	
	/* 
	 * do some dialog things first - processing is done in #run()
	 */
	public boolean execute(PlugInContext context) throws Exception {
	    
			sidebartext = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.Joins-attributes-of-source-layer-according-to-a-spatial-and-a-statistic-criterion");
			SRC_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.source-layer");
			TGT_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.target-layer");	
			SRC_ATTRIB = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-attribute");
			ATTRIB_OP = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-attribute-operation");	
			SPATIAL_OP = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.select-spatial-operation");	
			joinresult = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.join-result");
			notimplemented= I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.not-implemented");
			BUFFER_RADIUS = I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.buffer-radius");

	    	this.generateOpLists();
	    	
			this.dialog = new MultiInputDialog(
					context.getWorkbenchFrame(), getName(), true);
			setDialogValues(dialog, context);
			GUIUtil.centreOnWindow(dialog);
			dialog.setVisible(true);
			if (! dialog.wasOKPressed()) { return false; }
			getdialogValues(dialog);		
		return true;
	}
	
	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {  
		
		monitor.allowCancellationRequests();
		this.pc = context;
		
		List srcFeatures = this.srcLayer.getFeatureCollectionWrapper().getFeatures();
		List targetFeatures = this.targetLayer.getFeatureCollectionWrapper().getFeatures();	
				
		FeatureDataset results = JoinAttributes.joinAttributes(srcFeatures, targetFeatures,
		        						this.attrName, this.attributeOperation, 
		        						this.spatialOperation, this.bradius, monitor);
		if(results.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, joinresult, results);
		}
		else{
			context.getWorkbenchFrame().warnUser(notimplemented);			
		}
		//context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
	}	
	
	
	//============================================================
	// dialog things 
	//============================================================
	
	private JComboBox layerboxA;	
	private JComboBox layerboxB;
	private JComboBox attribbox;
	private JComboBox attribOpbox;
	private JComboBox spatialOpbox;
	
	private Object attrValue = null;
	private Object attrOpValue = "";
	private Object spatialOpValue = "";		
	private ArrayList attColl = new ArrayList();
	private ArrayList attOpColl = new ArrayList();
	private ArrayList SpatialOpColl = new ArrayList();
	
	/**
	 * @param selectTypeDialog2
	 * @param context
	 */
	private void setDialogValues(MultiInputDialog selectTypeDialog2, PlugInContext context) {
		this.dialog.setSideBarDescription(sidebartext);
		//-- target layer
		if (targetLayer == null) targetLayer = context.getCandidateLayer(0);
		layerboxA = this.dialog.addLayerComboBox(TGT_LAYER, targetLayer,"", context.getLayerManager());
		//-- source layer		
		if (srcLayer == null) srcLayer = context.getCandidateLayer(0);		
		layerboxB = this.dialog.addLayerComboBox(SRC_LAYER, srcLayer,"", context.getLayerManager());		
		layerboxB.addItemListener(new MethodItemListener());

		//-- attribute		
		attribbox = this.dialog.addComboBox(SRC_ATTRIB,attrValue,attColl,"");
		updateUIForAttributes();
		
		//-- attributeOp
		attribOpbox = this.dialog.addComboBox(ATTRIB_OP,attrOpValue,attOpColl,"");
        DefaultComboBoxModel modelA = new DefaultComboBoxModel();
        for(int i=0; i < this.attrOpList.size(); i++){
            modelA.addElement(this.attrOpList.get(i));    
        }        
        attribOpbox.setModel(modelA); 
		
		//-- spatial Relation
        spatialOpbox = this.dialog.addComboBox(SPATIAL_OP,attrValue,attOpColl,"");
        DefaultComboBoxModel modelS = new DefaultComboBoxModel();
        for(int i=0; i < this.spatialOpList.size(); i++){
            modelS.addElement(this.spatialOpList.get(i));    
        }        
        spatialOpbox.setModel(modelS);
        
        //-- add buffer 
        dialog.addDoubleField(BUFFER_RADIUS, this.bradius, 7);
	}
	
	private void updateUIForAttributes(){	
		this.srcLayer = dialog.getLayer(SRC_LAYER);
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 0; i < srcLayer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++){
            if (i == srcLayer.getFeatureCollectionWrapper().getFeatureSchema().getGeometryIndex()) {
                continue;
            }
            model.addElement(srcLayer.getFeatureCollectionWrapper()
                                  .getFeatureSchema().getAttributeName(i));
        }
        attribbox.setModel(model);
                
        if (model.getSize() == 0) {
            //Can get here if the only attribute is the geometry. [Jon Aquino]         
        }                
		this.dialog.validate();
		
	}
	
	private void getdialogValues(MultiInputDialog dialog) {
		this.srcLayer = dialog.getLayer(SRC_LAYER);
		this.targetLayer = dialog.getLayer(TGT_LAYER);
		this.attrName = (String) attribbox.getSelectedItem();
		this.attributeOperation = attribOpbox.getSelectedIndex();
		this.spatialOperation = spatialOpbox.getSelectedIndex();
		this.bradius = dialog.getDouble(BUFFER_RADIUS);
	}
	
	private void generateOpLists(){
	    //-- note the order and position is important 
	    //   since it will be used to obtain directly the values
	    
	    // the available operations are defined in AttributeOp.java
	    /** copy from AttributeOp
	    public final static int MAJORITY = 0;
	    public final static int MINORITY = 1;
	    public final static int MEAN = 2;
	    public final static int MEDIAN = 3;
	    public final static int MIN = 4;
	    public final static int MAX = 5;
	    public final static int STD = 6;
	    public final static int SUM = 7;
	    public final static int COUNT = 8;
	    **/
	    this.attrOpList.clear(); //because function may be called several times
	    this.attrOpList.add(AttributeOp.MAJORITY,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.majority"));
	    this.attrOpList.add(AttributeOp.MINORITY,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minority"));
	    this.attrOpList.add(AttributeOp.MEAN,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.mean"));
	    this.attrOpList.add(AttributeOp.MEDIAN,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.median"));
	    this.attrOpList.add(AttributeOp.MIN,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum"));
	    this.attrOpList.add(AttributeOp.MAX,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum"));
	    this.attrOpList.add(AttributeOp.STD,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev"));
	    this.attrOpList.add(AttributeOp.SUM,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum"));
	    this.attrOpList.add(AttributeOp.COUNT,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.count"));
	    
	    // the available operations are defined in SpatialRelationOp.java
	    /** copy from SpatialRelationOp
	    public final static int CONTAINS = 0;
	    public final static int INTERSECTS = 1;
	    **/
	    this.spatialOpList.clear();
	    this.spatialOpList.add(SpatialRelationOp.CONTAINS,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.source-features-contained-in-a-target-feature"));
	    this.spatialOpList.add(SpatialRelationOp.INTERSECTS,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.source-features-intersecting-a-target-feature"));
	    this.spatialOpList.add(SpatialRelationOp.COVEREDBY,I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.target-feature-covered-by-source-features"));	    
	}
	
	//============================================================
	// dialog listeners
	//============================================================
		
	private class MethodItemListener implements ItemListener{
		
		public void itemStateChanged(ItemEvent e) {
			updateUIForAttributes();
		}
	}

}