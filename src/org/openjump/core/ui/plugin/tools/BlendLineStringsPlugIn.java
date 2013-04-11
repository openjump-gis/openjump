/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 *
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.tools;

import java.util.Collection;
import java.util.Iterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BlendLineStringsPlugIn extends AbstractPlugIn {
    
	private WorkbenchContext workbenchContext;
    
    private final String THE_BLEND_TOLERANCE_TOOLTIP = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.The-blend-tolerance");
    private final String NEW_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.New");
    private final String TOLERANCE = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Tolerance");
    private final String PLUGIN_NAME = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Blend-LineStrings");

	private final String REMOVE_SOURCE_LINES = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.remove-source-lines");
	private final String REMOVE_SOURCE_LINES_TOOLTIP = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.The-source-lines-will-be-removed");
	private final String TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.transfer-first-attributes-to-all-resulting-linesstrings");
	private final String TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS_TOOLTIP = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.the-attributes-of-the-first-linestring-will-be-transfered");
	private String CREATE_NEW_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Create-a-new-layer-for-the-results");
	private final String CREATE_NEW_LAYER_TOOLTIP = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.A-new-layer-will-be-created-for-the-results");
    
    private double blendTolerance = 0.1;
	private boolean removeSourceLines = false;
	private boolean transferFirstAttributesToAllResultingLineStrings = false;
	private boolean createNewLayer = false;
	
	private JCheckBox removeCheckBox = null;
	private JCheckBox newLayerCheckBox = null;

    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuItem(this, new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY }, PLUGIN_NAME, false, null, this.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
    	
        reportNothingToUndoYet(context);
        
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(); 
        Feature currFeature = ((Feature) selectedFeatures.iterator().next());
        Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
        LayerManager layerManager = context.getLayerManager();
        FeatureDataset newFeatures = new FeatureDataset(currFeature.getSchema());       
        ArrayList<Feature> inputFeatures = new ArrayList(selectedFeatures.size());
		Layer selectedLayer = (Layer) context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems().iterator().next();
        
		// get all LineString Features into the inputFeatures variable
		Iterator<Feature> selectedFeaturesIterator = selectedFeatures.iterator();
		while (selectedFeaturesIterator.hasNext())
        {
            Feature feature = selectedFeaturesIterator.next();            
            if (feature.getGeometry() instanceof LineString)
            {
                inputFeatures.add(feature);
            }
        }

		// loop through all LineStrings
        while (inputFeatures.size() > 0)
        {
            //start a new blended linestring
			boolean blended = false;
			Feature inputFeature = inputFeatures.get(0);
			Feature blendedFeature;
			if (transferFirstAttributesToAllResultingLineStrings) {
				blendedFeature = currFeature.clone(false);
			} else {
				blendedFeature = inputFeature.clone(false);
			}
            CoordinateList blendedCoords = new CoordinateList(inputFeature.getGeometry().getCoordinates());
            Feature startFeature = inputFeatures.remove(0);
            //sequence through remaining input linestrings
            //and find those which can be added to either
            //the beginning or end of the current blended coordinate list
            int currIndex = 0; //index of current linestring in input vector
            while (currIndex < inputFeatures.size())
            {
				inputFeature = inputFeatures.get(currIndex);
                CoordinateList lsCoords = new CoordinateList(inputFeature.getGeometry().getCoordinates());
                if (blended(blendedCoords, lsCoords))
                {
                    inputFeatures.remove(currIndex);
                    currIndex = 0; //start at top since some that were rejected before might add to new string
					blended = true;
					// remove the original LineString if required
					if (removeSourceLines && !createNewLayer) selectedLayer.getFeatureCollectionWrapper().remove(inputFeature);
                }
                else
                {
                    currIndex++;
                }
            }
            
			// only if two or more LineStrings are blended, we delete the starting LineString and add the new blended  LineString to the selected or new Layer
			if (blended) {
				if (removeSourceLines && !createNewLayer) selectedLayer.getFeatureCollectionWrapper().remove(startFeature);
				blendedFeature.setGeometry(new GeometryFactory().createLineString(blendedCoords.toCoordinateArray()));
				if (createNewLayer) {
					newFeatures.add(blendedFeature);
				} else {
					selectedLayer.getFeatureCollectionWrapper().add(blendedFeature);
				}
			}
        }
                                   
		if(createNewLayer) {
			layerManager.addLayer(selectedCategories.isEmpty()
			? StandardCategoryNames.WORKING
			: selectedCategories.iterator().next().toString(),
			layerManager.uniqueLayerName(NEW_LAYER),
			newFeatures);
	        layerManager.getLayer(0).setFeatureCollectionModified(true);
			layerManager.getLayer(0).setEditable(true);
		}
        
        
        return true;
    }
    
    private boolean blended(CoordinateList blendedCoords, CoordinateList lsCoords)
    {
        Coordinate start = blendedCoords.getCoordinate(0);
        Coordinate end = blendedCoords.getCoordinate(blendedCoords.size()-1);
        Coordinate first = lsCoords.getCoordinate(0);
        Coordinate last = lsCoords.getCoordinate(lsCoords.size()-1);
        if (start.distance(first) < blendTolerance)
        {
            for (int i = 1; i < lsCoords.size(); i++)
            {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        }
        else if (start.distance(last) < blendTolerance)
        {
            for (int i = lsCoords.size()-2; i >= 0; i--)
            {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        }
        else if (end.distance(first) < blendTolerance)
        {
            for (int i = 1; i < lsCoords.size(); i++)
            {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }
        }
        else if (end.distance(last) < blendTolerance)
        {
            for (int i = lsCoords.size()-2; i >= 0; i--)
            {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }            
        }
        else
        {
            return false;
        }
        return true;
    }
    
      private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.addDoubleField(TOLERANCE, blendTolerance, 6, THE_BLEND_TOLERANCE_TOOLTIP);
		removeCheckBox = dialog.addCheckBox(REMOVE_SOURCE_LINES, removeSourceLines, REMOVE_SOURCE_LINES_TOOLTIP);
		removeCheckBox.setEnabled(!createNewLayer);
		dialog.addCheckBox(TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS, transferFirstAttributesToAllResultingLineStrings, TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS_TOOLTIP);
		newLayerCheckBox = dialog.addCheckBox(CREATE_NEW_LAYER, createNewLayer, CREATE_NEW_LAYER_TOOLTIP);
		newLayerCheckBox.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				removeCheckBox.setEnabled(!((JCheckBox) e.getSource()).isSelected());
			}
		});
      }

      private void getDialogValues(MultiInputDialog dialog) {
        blendTolerance = dialog.getDouble(TOLERANCE);
		removeSourceLines = dialog.getBoolean(REMOVE_SOURCE_LINES);
		transferFirstAttributesToAllResultingLineStrings = dialog.getBoolean(TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS);
		createNewLayer = dialog.getBoolean(CREATE_NEW_LAYER);
      }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createOnlyOneLayerMayHaveSelectedFeaturesCheck())
            .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(2));
    }    
}
