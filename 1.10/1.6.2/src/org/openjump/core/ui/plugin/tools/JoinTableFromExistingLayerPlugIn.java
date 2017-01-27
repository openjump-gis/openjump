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

/*****************************************************
 * created:  		1.Oct.2012
 * last modified:			
 * 					
 * 
 * @author sstein
 * 
 * description: Plugin that performs a table join, i.e. attaches attributes
 * 				from an one layer to another layer
 * 	
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;

import org.openjump.core.apitools.FeatureSchemaTools;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * @description: table join - attaches attributes from an one layer to another layer 
 *	
 * @author sstein
 *
 **/
public class JoinTableFromExistingLayerPlugIn extends AbstractThreadedUiPlugIn{

	private String sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.description");   
	private final String sLAYERBase = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Base-layer-that-should-be-extended");
	private final String sLAYERwAttributes = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Layer-with-attributes-to-join");
	private final String sBaseLayerID = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Base-layer-attribute-with-unique-feature-IDs");
	private final String sTableLayerAttributeID = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Attribute-with-unique-IDs");
	private final String sDisplayUnmatched = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.display-unmatched-items-from-base-layer");
	private final String sAllMatched = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.All-items-matched-no-layer-with-unmatched-features");
	//-- for output of layers
	private final String sJoinResult =  I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.join-result");
	private final String sUnmatchedItems = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.unmatched-items");
	private final String sTooManyItems = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.multiple-matches");
	private final String sMultiMatchesMsg = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.multiple-matches-for-feature-FID");
	
	private FeatureCollection base = null;
	private FeatureCollection tabletojoin = null;
	private Layer inputBaseLayer = null;
	private Layer inputTableLayer = null;
	private String selBaseLayerAttribute = "";
	private String selTableLayerJoinIDAttribute = "";
	private boolean displayUnmatched = true;
	
	private MultiInputDialog dialog;
	private PlugInContext context = null;

	public String getName() {
		return I18N
				.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn") + "...";
	}

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuItem(
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES}, 	//menu path
				this,
				new JMenuItem( this.getName(), null),
				createEnableCheck(context.getWorkbenchContext()), -1);
	}

	public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
		return new MultiEnableCheck()
		.add(checkFactory.createTaskWindowMustBeActiveCheck())
		.add(checkFactory.createAtLeastNLayersMustExistCheck(2));
	}

	public boolean execute(PlugInContext context) throws Exception{
		//Unlike ValidatePlugIn, here we always call #initDialog because we want
		//to update the layer comboboxes.
		initDialog(context);
		dialog.setVisible(true);
		if (!dialog.wasOKPressed()) {
			return false;
		}
		else{
			this.getDialogValues(dialog); 
		}
		return true;	    
	}

	public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
		System.gc(); //flush garbage collector
		this.context = context;
		monitor.allowCancellationRequests();
		int dFromID = base.getFeatureSchema().getAttributeIndex(this.selBaseLayerAttribute);
		int dToID = tabletojoin.getFeatureSchema().getAttributeIndex(this.selTableLayerJoinIDAttribute);
		
		List allTableFeatures = this.tabletojoin.getFeatures();
		int numTableFeatures = allTableFeatures.size();
		
		//-- prep the attribute transfer
    	AttributeMapping mapping = null;
    	mapping = new AttributeMapping(base.getFeatureSchema(), tabletojoin.getFeatureSchema());

		FeatureCollection featuresFound = new FeatureDataset(mapping.createSchema("Geometry"));
		FeatureCollection featuresMissing = new FeatureDataset(base.getFeatureSchema());
		FeatureCollection featuresWithManyMatches = new FeatureDataset(mapping.createSchema("Geometry"));
		
		//-- loop over all base features (as they are our reference)
		int i = 0;
		int numFeatures = this.base.size();
		for (Iterator iterator = base.iterator(); iterator.hasNext();) {
			Feature baseFeature = (Feature) iterator.next();
			monitor.report(i, numFeatures, I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.items-processed"));	
			int baseFeatureId = baseFeature.getInteger(dFromID);

			//-- find the base-features corresponding entry
			//   we could optimize this procedure by removing found
			//   table items from the list
			boolean notFound = true;
			boolean foundFirst = false;
			boolean foundSecond = false;
			Feature firstFeatureToJoin = null;
			int j = 0;
			while(notFound){
				Feature tmpTableItem = (Feature)allTableFeatures.get(j);
				int tableItemId = tmpTableItem.getInteger(dToID);
				if(tableItemId == baseFeatureId){
					if (foundFirst == false){
						firstFeatureToJoin = tmpTableItem;
						foundFirst = true;
						notFound = true; //keep searching
					}
					else{
						if(foundSecond == false){
							//we got i>=2 matches
							//write the original
							Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
							mapping.transferAttributes(baseFeature, null, newFeature);
							newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
							featuresFound.add(newFeature);
							//and inform the user
							context.getWorkbenchFrame().warnUser( sMultiMatchesMsg + " : " + baseFeature.getID());
							//add the first and the second to the multiple list
							//add first
							Feature newFeature1 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, firstFeatureToJoin, newFeature1);
							newFeature1.setGeometry((Geometry)baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature1);
							//add second
							Feature newFeature2 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, tmpTableItem, newFeature2);
							newFeature2.setGeometry((Geometry)baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature2);
							
							foundSecond = true;
						}
						else{//this should be the third match
							 //just add it to the list 
							Feature newFeature3 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, tmpTableItem, newFeature3);
							newFeature3.setGeometry((Geometry)baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature3);
						}
						
					}
				}//end: we had a match
				j++;
				if(j < numTableFeatures){
					//we have not searched all yet
				}
				else{
					//we have searched all
					notFound = false;
				}
			} //end while loop over stop-list
			//-- save if not saved yet
			if(foundFirst == false){
				//so we have no matching
				//transfer it anyway
				Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
				mapping.transferAttributes(baseFeature, null, newFeature);
				newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
				featuresFound.add(newFeature);
				//and also put into this list
				featuresMissing.add(baseFeature.clone(true));
				//stop the loop
			}
			else{//foundFirst == true
				if(foundSecond == false){
					//we have only one match
					Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
					mapping.transferAttributes(baseFeature, firstFeatureToJoin, newFeature);
					newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
					featuresFound.add(newFeature);
				}
			}
			i++; //count feature processing (next transfer)
		}

		// show results
		if(featuresFound.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, this.inputBaseLayer.getName() + " - " + sJoinResult , featuresFound);
		}
		if(featuresWithManyMatches.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, this.inputBaseLayer.getName() + " - " + sTooManyItems , featuresWithManyMatches);
		}
		if((this.displayUnmatched) && (featuresMissing.size() > 0)){
			context.addLayer(StandardCategoryNames.RESULT, this.inputBaseLayer.getName() + " - " + sUnmatchedItems, featuresMissing);
		}
		else{
			context.getWorkbenchFrame().warnUser(sAllMatched);
		}
		
		//--
		System.gc();    	   		
	}

	private void initDialog(PlugInContext context) {
		JComboBox layerComboBoxBase = null;
		JComboBox layerComboBoxTable = null;

		dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
		dialog.setSideBarDescription(sSidebar);

		layerComboBoxBase = dialog.addLayerComboBox(this.sLAYERBase, context.getCandidateLayer(0), null, context.getLayerManager());

		List listNumAttributesBase = FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(context.getCandidateLayer(0));
		Object valAttribute = listNumAttributesBase.size()>0?listNumAttributesBase.iterator().next():null;
		final JComboBox baseAttributeBox = dialog.addComboBox(this.sBaseLayerID, valAttribute, listNumAttributesBase, this.sBaseLayerID);
		if (listNumAttributesBase.size() == 0) baseAttributeBox.setEnabled(false);

		dialog.addSeparator(); //----

		layerComboBoxTable = dialog.addLayerComboBox(this.sLAYERwAttributes, context.getCandidateLayer(0), null, context.getLayerManager());

		List listNumAttributesTable = FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(context.getCandidateLayer(0));
		Object valAttributeStops = listNumAttributesTable.size()>0?listNumAttributesTable.iterator().next():null;
		final JComboBox tableAttributeBox = dialog.addComboBox(this.sTableLayerAttributeID, valAttributeStops, listNumAttributesTable, this.sTableLayerAttributeID);
		if (listNumAttributesTable.size() == 0) tableAttributeBox.setEnabled(false);

		dialog.addSeparator(); //----
		
		dialog.addCheckBox(sDisplayUnmatched, displayUnmatched);
		
		// do listener stuff
		dialog.getComboBox(this.sLAYERBase).addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List list = getFieldsFromLayerWithoutGeometryAndStringTransfer();
				if (list.size() == 0) {
					baseAttributeBox.setModel(new DefaultComboBoxModel(new String[0]));
					baseAttributeBox.setEnabled(false);
				}
				else {
					baseAttributeBox.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
					baseAttributeBox.setEnabled(true);
				}
			}
		});        

		dialog.getComboBox(this.sLAYERwAttributes).addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List list = getFieldsFromLayerWithoutGeometryAndStringStops();
				if (list.size() == 0) {
					tableAttributeBox.setModel(new DefaultComboBoxModel(new String[0]));
					tableAttributeBox.setEnabled(false);
				}
				else {
					tableAttributeBox.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
					tableAttributeBox.setEnabled(true);
				}
			}
		});  

		GUIUtil.centreOnWindow(dialog);
	}

	private void getDialogValues(MultiInputDialog dialog) {
		this.inputBaseLayer =  dialog.getLayer(this.sLAYERBase);
		this.inputTableLayer = dialog.getLayer(this.sLAYERwAttributes);
		this.selBaseLayerAttribute = dialog.getText(this.sBaseLayerID);
		this.selTableLayerJoinIDAttribute = dialog.getText(this.sTableLayerAttributeID);
		this.displayUnmatched = dialog.getBoolean(sDisplayUnmatched);

		this.base= this.inputBaseLayer.getFeatureCollectionWrapper(); 
		this.tabletojoin = this.inputTableLayer.getFeatureCollectionWrapper();
	}

	private List getFieldsFromLayerWithoutGeometryAndStringTransfer() {
		return FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(dialog.getLayer(this.sLAYERBase));
	}

	private List getFieldsFromLayerWithoutGeometryAndStringStops() {
		return FeatureSchemaTools.getFieldsFromLayerWithoutGeometryAndString(dialog.getLayer(this.sLAYERwAttributes));
	}

}