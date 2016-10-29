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

package org.openjump.core.ui.plugin.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
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

import static com.vividsolutions.jump.workbench.ui.AttributeTypeFilter.*;

/**
 * Table join - attaches attributes from one layer to another layer
 *
 * created: 2012-10-01
 * refactored : 2016-10-29
 * @author sstein
 * @author mmichaud
 *
 */
public class JoinTableFromExistingLayerPlugIn extends AbstractThreadedUiPlugIn{

	private final static String sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.description");
	private final static String BASE_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Base-layer-that-should-be-extended");
	private final static String JOIN_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Layer-with-attributes-to-join");
	private final static String BASE_LAYER_ID = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Base-layer-attribute-with-unique-feature-IDs");
	private final static String JOIN_LAYER_ID = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.Attribute-with-unique-IDs");
	private final static String sDisplayUnmatched = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.display-unmatched-items-from-base-layer");
	private final static String sAllMatched = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.All-items-matched-no-layer-with-unmatched-features");
	private final static String sItemsProcessed = I18N.get("org.openjump.core.ui.plugin.tools.generate.PointLayerFromAttributeTablePlugIn.items-processed");

	//-- for output of layers
	private final static String sJoinResult =  I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.join-result");
	private final static String sUnmatchedItems = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.unmatched-items");
	private final static String sTooManyItems = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.multiple-matches");
	private final static String sMultiMatchesMsg = I18N.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn.multiple-matches-for-feature-FID");

	private Layer baseLayer = null;
	private Layer joinLayer = null;
	private String baseLayerIdAttribute = "";
	private String joinLayerJoinAttribute = "";
	private boolean displayUnmatched = true;
	
	private MultiInputDialog dialog;

	public String getName() {
		return I18N
				.get("org.openjump.core.ui.plugin.tools.JoinTableFromExistingLayerPlugIn") + "...";
	}

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuPlugin(
				this,
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
				this.getName(), false, null,
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

		monitor.allowCancellationRequests();

		FeatureCollection baseFC = baseLayer.getFeatureCollectionWrapper();
		FeatureCollection joinFC = joinLayer.getFeatureCollectionWrapper();
		int baseLayerAttributeIndex = baseFC.getFeatureSchema().getAttributeIndex(baseLayerIdAttribute);
		int joinLayerAttributeIndex = joinFC.getFeatureSchema().getAttributeIndex(joinLayerJoinAttribute);
		
		//List<Feature> joinFeatures = joinFC.getFeatures();
		//int joinLayerSize = joinFeatures.size();
		
		//-- prep the attribute transfer
    	AttributeMapping mapping;
    	mapping = new AttributeMapping(baseFC.getFeatureSchema(), joinFC.getFeatureSchema());

		FeatureCollection featuresFound = new FeatureDataset(mapping.createSchema("Geometry"));
		FeatureCollection featuresMissing = new FeatureDataset(baseFC.getFeatureSchema());
		FeatureCollection featuresWithManyMatches = new FeatureDataset(mapping.createSchema("Geometry"));
		
		//-- loop over all base features (as they are our reference)
		int i = 0;
		int baseLayerSize = baseFC.size();
		for (Feature baseFeature : baseFC.getFeatures()) {
			monitor.report(i++, baseLayerSize, sItemsProcessed);
			Object baseId = baseFeature.getAttribute(baseLayerAttributeIndex);

			//-- find the base-features corresponding entry
			//   we could optimize this procedure by removing found
			//   table items from the list
			//boolean notFound = true;
			//boolean foundFirst = false;
			//boolean foundSecond = false;
			Feature firstJoinFeature = null;
			List<Feature> nextJoinFeatures = new ArrayList<>();
			int countMatches = 0;
			//int j = 0;
			for (Feature joinCandidateFeature : joinFC.getFeatures()) {
			//while(notFound){
				//Feature tmpTableItem = joinFeatures.get(j);
				//int tableItemId = tmpTableItem.getInteger(joinLayerAttributeIndex);
				Object joinId = joinCandidateFeature.getAttribute(joinLayerAttributeIndex);
				if((baseId == null && joinId == null) ||
						baseId != null && joinId != null && baseId.toString().equals(joinId.toString())) {
					if (countMatches == 0) {
						firstJoinFeature = joinCandidateFeature;

					} else {
						nextJoinFeatures.add(joinCandidateFeature);
						context.getWorkbenchFrame().warnUser(sMultiMatchesMsg + " : " + baseFeature.getID());
						/*
						if (!foundSecond) {
							//we got i>=2 matches
							//write the original
							Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
							mapping.transferAttributes(baseFeature, null, newFeature);
							newFeature.setGeometry((Geometry) baseFeature.getGeometry().clone());
							featuresFound.add(newFeature);
							//and inform the user
							context.getWorkbenchFrame().warnUser(sMultiMatchesMsg + " : " + baseFeature.getID());
							//add the first and the second to the multiple list
							//add first
							Feature newFeature1 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, firstJoinFeature, newFeature1);
							newFeature1.setGeometry((Geometry) baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature1);
							//add second
							Feature newFeature2 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, tmpTableItem, newFeature2);
							newFeature2.setGeometry((Geometry) baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature2);

							foundSecond = true;
						} else {//this should be the third match
							//just add it to the list
							Feature newFeature3 = new BasicFeature(featuresWithManyMatches.getFeatureSchema());
							mapping.transferAttributes(baseFeature, tmpTableItem, newFeature3);
							newFeature3.setGeometry((Geometry) baseFeature.getGeometry().clone());
							featuresWithManyMatches.add(newFeature3);
						}
						*/
					}
					countMatches++;
				}
			} //end while loop over stop-list
			// Unique join
			if (countMatches > 0) {
				Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
				mapping.transferAttributes(baseFeature, firstJoinFeature, newFeature);
				newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
				if (countMatches == 1) {
					featuresFound.add(newFeature);
				} else {
					featuresWithManyMatches.add(newFeature);
					for (Feature match : nextJoinFeatures) {
						newFeature = new BasicFeature(featuresFound.getFeatureSchema());
						mapping.transferAttributes(baseFeature, match, newFeature);
						newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
						featuresWithManyMatches.add(newFeature);
					}
				}

			} else {
				featuresMissing.add(baseFeature.clone(true));
			}
			//-- save if not saved yet
			/*
			if(!foundFirst){
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
				if(!foundSecond){
					//we have only one match
					Feature newFeature = new BasicFeature(featuresFound.getFeatureSchema());
					mapping.transferAttributes(baseFeature, firstJoinFeature, newFeature);
					newFeature.setGeometry((Geometry)baseFeature.getGeometry().clone());
					featuresFound.add(newFeature);
				}
			}
			*/
		}

		// show results
		if(featuresFound.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, this.baseLayer.getName() + " - " + sJoinResult , featuresFound);
		}
		if(featuresWithManyMatches.size() > 0){
			context.addLayer(StandardCategoryNames.RESULT, this.baseLayer.getName() + " - " + sTooManyItems , featuresWithManyMatches);
		}
		if((this.displayUnmatched) && (featuresMissing.size() > 0)){
			context.addLayer(StandardCategoryNames.RESULT, this.baseLayer.getName() + " - " + sUnmatchedItems, featuresMissing);
		}
		else{
			context.getWorkbenchFrame().warnUser(sAllMatched);
		}
	}

	private void initDialog(PlugInContext context) {

		dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
		dialog.setSideBarDescription(sSidebar);

		dialog.addLayerComboBox(BASE_LAYER, context.getCandidateLayer(0), null, context.getLayerManager());

		List<String> baseLayerAttributeList = NUMSTRING_FILTER.filter(context.getCandidateLayer(0));
		String valBaseAttribute = baseLayerAttributeList.size()>0?baseLayerAttributeList.get(0):null;
		final JComboBox<String> baseAttributeBox = dialog.addComboBox(BASE_LAYER_ID, valBaseAttribute, baseLayerAttributeList, BASE_LAYER_ID);
		if (baseLayerAttributeList.size() == 0) baseAttributeBox.setEnabled(false);

		dialog.addSeparator(); //----

		dialog.addLayerComboBox(JOIN_LAYER, context.getCandidateLayer(0), null, context.getLayerManager());

		List<String> joinLayerAttributeList = NUMSTRING_FILTER.filter(context.getCandidateLayer(0));
		String valJoinAttribute = joinLayerAttributeList.size()>0?joinLayerAttributeList.get(0):null;
		final JComboBox<String> tableAttributeBox = dialog.addComboBox(JOIN_LAYER_ID, valJoinAttribute, joinLayerAttributeList, JOIN_LAYER_ID);
		if (joinLayerAttributeList.size() == 0) tableAttributeBox.setEnabled(false);

		dialog.addSeparator(); //----
		
		dialog.addCheckBox(sDisplayUnmatched, displayUnmatched);
		
		// do listener stuff
		dialog.getComboBox(BASE_LAYER).addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<String> list = NUMSTRING_FILTER.filter(dialog.getLayer(BASE_LAYER));
				if (list.size() == 0) {
					baseAttributeBox.setModel(new DefaultComboBoxModel<>(new String[0]));
					baseAttributeBox.setEnabled(false);
				}
				else {
					baseAttributeBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
					baseAttributeBox.setEnabled(true);
				}
			}
		});        

		dialog.getComboBox(JOIN_LAYER).addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<String> list = NUMSTRING_FILTER.filter(dialog.getLayer(JOIN_LAYER));
				if (list.size() == 0) {
					tableAttributeBox.setModel(new DefaultComboBoxModel<>(new String[0]));
					tableAttributeBox.setEnabled(false);
				}
				else {
					tableAttributeBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
					tableAttributeBox.setEnabled(true);
				}
			}
		});  

		GUIUtil.centreOnWindow(dialog);
	}

	private void getDialogValues(MultiInputDialog dialog) {
		this.baseLayer =  dialog.getLayer(BASE_LAYER);
		this.joinLayer = dialog.getLayer(JOIN_LAYER);
		this.baseLayerIdAttribute = dialog.getText(BASE_LAYER_ID);
		this.joinLayerJoinAttribute = dialog.getText(JOIN_LAYER_ID);
		this.displayUnmatched = dialog.getBoolean(sDisplayUnmatched);
	}

}