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
 * created: 14.01.2014
 * last modified:  
 * 
 * @author sstein
 * 
 * Calculates distances between the geometries in 2 different datasets
 *****************************************************/

package org.openjump.core.ui.plugin.tools.analysis.twolayers;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.vividsolutions.jts.algorithm.distance.DiscreteHausdorffDistance;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
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
 * Calculates distances between the geometries in 2 different datasets 
 * 
 * @author sstein
 *
 **/
public class CalculateDistancesPlugIn extends ThreadedBasePlugIn{
	

    private String sidebartext = 			I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.Calculates-distances-description");
	private String sSRC_LAYER = 			I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.layer-with-origins");
	private String sSRC_UniqueIdAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.select-unique-attribute-for-origin-identification");
	private String sTGT_LAYER = 			I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.layer-with destinations");	
	private String sTGT_UniqueIdAttrib = 	I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.select-unique-attribute-for-destination-identification");
	private String sDIST_OP = 				I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.select-distance-operation-for-non-point-geometries");	
	private String distresult = 			I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.distances-result");
	private String notimplemented = 		I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.not-implemented");
		
	//-- vars
	private Layer orgLayer = null;
	private Layer destLayer = null;
	private String orgAttrName = "";
	private String destAttrName = "";
	public boolean calcCentroidDistance = true;
	public boolean calcHausdorffDistance = true;
	public boolean displayHausdorffDistanceGeometry = true;
	    
	private MultiInputDialog dialog;	
	private PlugInContext pcontext = null;
		
	public void initialize(PlugInContext context) throws Exception {
		this.sidebartext = "Calculates distance between the geometries in 2 different datasets. It-returns (i) the shortest disstance (sd), the shortest distance between centroids (sds), and the Hausdorff distance (sdh).";

		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
		featureInstaller.addMainMenuPlugin(
				this,				
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
				this.getName() + "...",
				false,			//checkbox
				null,			//icon
				createEnableCheck(context.getWorkbenchContext()));     
	}
	
	public String getName(){
		return I18N.get("org.openjump.core.ui.plugin.tools.CalculateDistancesPlugIn.Calculate-Distances");
	}
	
	public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
		
		return new MultiEnableCheck()
		.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
		.add(checkFactory.createAtLeastNLayersMustExistCheck(2));
	}
	
	/* 
	 * do some dialog things first - processing is done in #run()
	 */
	public boolean execute(PlugInContext context) throws Exception {
	    	
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
		this.pcontext = context;
		
		FeatureCollection originFeatures = this.orgLayer.getFeatureCollectionWrapper();
		FeatureCollection destinationFeatures = this.destLayer.getFeatureCollectionWrapper();	
				
		FeatureDataset results = calcDistances(originFeatures, destinationFeatures, this.orgAttrName,
		        						this.destAttrName, this.calcCentroidDistance, monitor);
		if(results.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, distresult, results);
		}
		else{
			context.getWorkbenchFrame().warnUser(notimplemented);			
		}
		//context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
	}	
	
	//============================================================
	// logic 
	//============================================================
	
	private FeatureDataset calcDistances(FeatureCollection originFeatures,
			FeatureCollection destinationFeatures, String orgAttrName, String destAttrName,
			boolean calcCentroidDistance, TaskMonitor monitor) {
		
		//-- evaluate the number of destinations so we can generate the new schema
		int numDest = destinationFeatures.size();
		FeatureSchema newFs = new FeatureSchema();
		newFs.addAttribute("geometry", AttributeType.GEOMETRY);
		newFs.addAttribute(orgAttrName, originFeatures.getFeatureSchema().getAttributeType(orgAttrName));
		for (Iterator iterator = destinationFeatures.iterator(); iterator.hasNext();) {
			Feature destF = (Feature) iterator.next();
			Object destFid = destF.getAttribute(destAttrName);
			newFs.addAttribute(destFid.toString() + "_sd", AttributeType.DOUBLE);
			if(this.calcCentroidDistance == true){
				newFs.addAttribute(destFid.toString() + "_sdc", AttributeType.DOUBLE);
			}
			if(this.calcHausdorffDistance ==  true){
				newFs.addAttribute(destFid.toString() + "_sdh", AttributeType.DOUBLE);
			}
		}
		
		FeatureDataset resultDistFeatures = new FeatureDataset(newFs);
		GeometryFactory gf = new GeometryFactory();
		
		//-- now calculate the distances
		for (Iterator iterator = originFeatures.iterator(); iterator.hasNext();) {
			Feature orgF = (Feature) iterator.next();
			
			Feature newFeature = new BasicFeature(newFs);
			// set identifier in first row 
			newFeature.setAttribute(orgAttrName, orgF.getAttribute(orgAttrName));
					
			int counter = 1;
			for (Iterator iterator2 = destinationFeatures.iterator(); iterator2.hasNext();) {
				Feature destF = (Feature) iterator2.next();
				Object destfid = destF.getAttribute(destAttrName);
				//-- calculate object distance
				double objectDist = 0;
				Geometry dline = gf.createGeometryCollection(null); 
				try{
					DistanceOp dops = new DistanceOp(orgF.getGeometry(), destF.getGeometry());
					objectDist = dops.distance();
					// get geometry of connection - but only for first destination
					if(counter == 1){
						Coordinate[] coords = dops.nearestPoints();
						dline = gf.createLineString(coords);
						newFeature.setGeometry(dline);
					}
				}
				catch(Exception e){
					objectDist = Double.NaN;
				}
				newFeature.setAttribute(destfid.toString() + "_sd", objectDist);
				
				//-- calculate centroid distance
				if(this.calcCentroidDistance == true){
					double centroidDist = 0;
					try{
						DistanceOp dopc = new DistanceOp(orgF.getGeometry().getCentroid(), destF.getGeometry().getCentroid());
						centroidDist = dopc.distance();
					}
					catch(Exception e){
						centroidDist = Double.NaN;
					}
					newFeature.setAttribute(destfid.toString() + "_sdc", centroidDist);
				}
				
				//-- calculate hausdorff distance
				if(this.calcHausdorffDistance == true){
					double hausdDist = 0;
					try{						
						DiscreteHausdorffDistance doph = new DiscreteHausdorffDistance(orgF.getGeometry(), destF.getGeometry());
						hausdDist = doph.distance();
						if(this.displayHausdorffDistanceGeometry == true){
							if(counter == 1){
								Coordinate[] coords = doph.getCoordinates();
								dline = gf.createLineString(coords);
								newFeature.setGeometry(dline);
							}
						}
					}
					catch(Exception e){
						hausdDist = Double.NaN;
					}
					newFeature.setAttribute(destfid.toString() + "_sdh", hausdDist);
				}
				
				counter = counter + 1;
			}
			resultDistFeatures.add(newFeature);
		}
		
		return resultDistFeatures;
	}
	
	//============================================================
	// dialog things 
	//============================================================

	private JComboBox layerboxAOrg;	
	private JComboBox layerboxBDest;
	private JComboBox attribboxAOrg;
	private JComboBox attribboxBDest;
	
	private Object attrValueOrg = null;
	private Object attrValueDest = null;
	private ArrayList attCollOrg = new ArrayList();
	private ArrayList attCollDest = new ArrayList();
	
	/**
	 * @param selectTypeDialog2
	 * @param context
	 */
	private void setDialogValues(MultiInputDialog selectTypeDialog2, PlugInContext context) {
		this.dialog.setSideBarDescription(sidebartext);
		//-- origin layer
		if (orgLayer == null) orgLayer = context.getCandidateLayer(0);
		layerboxAOrg = this.dialog.addLayerComboBox(sSRC_LAYER, orgLayer,"", context.getLayerManager());
		layerboxAOrg.addItemListener(new MethodItemListenerOrg());
		//-- attribute		
		attribboxAOrg = this.dialog.addComboBox(sSRC_UniqueIdAttrib,attrValueOrg,attCollOrg,"");
		updateUIForAttributesOrg();
		
		//-- destination layer		
		if (destLayer == null) destLayer = context.getCandidateLayer(0);		
		layerboxBDest = this.dialog.addLayerComboBox(sTGT_LAYER, destLayer,"", context.getLayerManager());		
		layerboxBDest.addItemListener(new MethodItemListenerDest());
		//-- attribute		
		attribboxBDest = this.dialog.addComboBox(sTGT_UniqueIdAttrib,attrValueDest,attCollDest,"");
		updateUIForAttributesDest();
		
	}
	
	private void updateUIForAttributesOrg(){	
		this.orgLayer = dialog.getLayer(sSRC_LAYER);
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 0; i < orgLayer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++){
            if (i == orgLayer.getFeatureCollectionWrapper().getFeatureSchema().getGeometryIndex()) {
                continue;
            }
            model.addElement(orgLayer.getFeatureCollectionWrapper()
                                  .getFeatureSchema().getAttributeName(i));
        }
        attribboxAOrg.setModel(model);
                
        if (model.getSize() == 0) {
            //Can get here if the only attribute is the geometry. [Jon Aquino]         
        }                
		this.dialog.validate();
		
	}
	
	private void updateUIForAttributesDest(){	
		this.destLayer = dialog.getLayer(sTGT_LAYER);
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 0; i < destLayer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount(); i++){
            if (i == destLayer.getFeatureCollectionWrapper().getFeatureSchema().getGeometryIndex()) {
                continue;
            }
            model.addElement(destLayer.getFeatureCollectionWrapper()
                                  .getFeatureSchema().getAttributeName(i));
        }
        attribboxBDest.setModel(model);
                
        if (model.getSize() == 0) {
            //Can get here if the only attribute is the geometry. [Jon Aquino]         
        }                
		this.dialog.validate();
		
	}
	
	private void getdialogValues(MultiInputDialog dialog) {
		this.orgLayer = dialog.getLayer(sSRC_LAYER);
		this.destLayer = dialog.getLayer(sTGT_LAYER);
		this.orgAttrName = (String) attribboxAOrg.getSelectedItem();
		this.destAttrName = (String) attribboxBDest.getSelectedItem();
	}
	
	
	//============================================================
	// dialog listeners
	//============================================================
		
	private class MethodItemListenerOrg implements ItemListener{
		
		public void itemStateChanged(ItemEvent e) {
			updateUIForAttributesOrg();
		}
	}
	
	private class MethodItemListenerDest implements ItemListener{
		
		public void itemStateChanged(ItemEvent e) {
			updateUIForAttributesDest();
		}
	}

}