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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Blend lines which (nearly) touch each others.
 */
public class BlendLineStringsPlugIn extends AbstractPlugIn {


    private final String THE_BLEND_TOLERANCE_TOOLTIP = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.The-blend-tolerance");
    private final String NEW_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.New");
    private final String TOLERANCE = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Tolerance");
    private final String PLUGIN_NAME = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn");

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


    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY },
                PLUGIN_NAME, false, null, this.createEnableCheck(context.getWorkbenchContext()));
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
    	
        reportNothingToUndoYet(context);
        
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }

        getDialogValues(dialog);
        Layer selectedLayer = context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems().iterator().next();
        Collection<Feature> selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
        Feature currFeature = selectedFeatures.iterator().next();

		// inputLines list will be processed until there is no more feature in the list
        List<Feature> inputLines = new ArrayList<>();
        // removeFeatures keep track of original features to be removed from the layer
        ArrayList<Feature> removeFeatures = new ArrayList<>(selectedFeatures.size());
        // quadtree is used to accelerate search. It is updated after each blending operation
		Quadtree quadtree = new Quadtree();
		for (Feature f : selectedFeatures) {
            if (f.getGeometry() instanceof LineString)
            {
                inputLines.add(f);
                removeFeatures.add(f);
                quadtree.insert(f.getGeometry().getEnvelopeInternal(), f);
            }
        }

        while (inputLines.size()>0) {
		    Feature currentFeature = inputLines.get(0);
            CoordinateList blendedCoords = new CoordinateList(currentFeature.getGeometry().getCoordinates());
            // Temporary list used to update inputLines after each blending operation
            // (after currentLine blending, we remove all features blended to the currentFeature from inputLines list)
            List<Feature> remove = new ArrayList<>();

            // currentFeature is processed until no more candidate is found
            boolean blended = true;
            while(blended) {
                Envelope search_env = currentFeature.getGeometry().getEnvelopeInternal();
                search_env.expandBy(blendTolerance);
                // Find candidate lines for blending
                List<Feature> candidates = quadtree.query(search_env);
                candidates.remove(currentFeature);
                // Try to blend the neares line first
                Feature bestCandidate = nearest(
                        blendedCoords.getCoordinate(0),
                        blendedCoords.getCoordinate(blendedCoords.size()-1),
                        candidates);
                if (bestCandidate != null) {
                    Feature blendedFeature;
                    if (transferFirstAttributesToAllResultingLineStrings) {
                        blendedFeature = currFeature.clone(false);
                    } else {
                        blendedFeature = currentFeature.clone(false);
                    }
                    CoordinateList lsCoords = new CoordinateList(bestCandidate.getGeometry().getCoordinates());
                    if (blended(blendedCoords, lsCoords)) {
                        quadtree.remove(currentFeature.getGeometry().getEnvelopeInternal(), currentFeature);
                        quadtree.remove(bestCandidate.getGeometry().getEnvelopeInternal(), bestCandidate);
                        remove.add(currentFeature);
                        remove.add(bestCandidate);
                        blendedFeature.setGeometry(new GeometryFactory().createLineString(blendedCoords.toCoordinateArray()));
                        quadtree.insert(blendedFeature.getGeometry().getEnvelopeInternal(), blendedFeature);
                        currentFeature = blendedFeature;
                    }
                } else {
                    blended = false;
                }
            }
            inputLines.remove(0);
            inputLines.removeAll(remove);
        }


		if(createNewLayer) {
            LayerManager layerManager = context.getLayerManager();
            Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
            FeatureDataset newFeatures = new FeatureDataset(currFeature.getSchema());
            newFeatures.addAll(quadtree.queryAll());
            layerManager.addLayer(selectedCategories.isEmpty()
                            ? StandardCategoryNames.WORKING
                            : selectedCategories.iterator().next().toString(),
                    layerManager.uniqueLayerName(NEW_LAYER),
                    newFeatures);
        } else {
		    updateLayer(selectedLayer, removeFeatures, quadtree.queryAll());
        }
        return true;
    }

    private void updateLayer(final Layer layer, final List<Feature> remove, final List<Feature> newFeatures) {
        layer.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                    new UndoableCommand(I18N.get(AutoAssignAttributePlugIn.class.getName())) {
                        public void execute() {
                            if (removeSourceLines) {
                                layer.getFeatureCollectionWrapper().removeAll(remove);
                                layer.getLayerManager().fireFeaturesChanged(remove, FeatureEventType.DELETED,layer);
                            }
                            layer.getFeatureCollectionWrapper().addAll(newFeatures);
                            layer.getLayerManager().fireFeaturesChanged(newFeatures, FeatureEventType.ADDED,layer);
                        }
                        public void unexecute() {
                            layer.getFeatureCollectionWrapper().removeAll(newFeatures);
                            layer.getLayerManager().fireFeaturesChanged(newFeatures, FeatureEventType.DELETED,layer);
                            if (removeSourceLines) {
                                layer.getFeatureCollectionWrapper().addAll(remove);
                                layer.getLayerManager().fireFeaturesChanged(remove, FeatureEventType.ADDED,layer);
                            }
                        }
                    };
            command.execute();
            layer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
        } finally {
            layer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
    }

    private Feature nearest(Coordinate start0, Coordinate end0, List<Feature> candidates) {
        Feature best = null;
        double max = blendTolerance;
        for (Feature c : candidates) {
            Coordinate[] cc = c.getGeometry().getCoordinates();
            if (cc.length==0) continue;
            Coordinate start1 = cc[0];
            Coordinate end1 = cc[cc.length-1];
            double d00 = start0.distance(start1);
            double d01 = start0.distance(end1);
            double d10 = end0.distance(start1);
            double d11 = end0.distance(end1);
            if (d00==0||d01==0||d10==0||d11==0) return c;
            if (d00 <= max) {max = d00; best = c;}
            if (d01 <= max) {max = d01; best = c;}
            if (d10 <= max) {max = d10; best = c;}
            if (d11 <= max) {max = d11; best = c;}
        }
        return best;
    }
    
    private boolean blended(CoordinateList blendedCoords, CoordinateList lsCoords) {
        Coordinate start = blendedCoords.getCoordinate(0);
        Coordinate end = blendedCoords.getCoordinate(blendedCoords.size()-1);
        Coordinate first = lsCoords.getCoordinate(0);
        Coordinate last = lsCoords.getCoordinate(lsCoords.size()-1);
        double d00 = start.distance(first);
        double d01 = start.distance(last);
        double d10 = end.distance(first);
        double d11 = end.distance(last);
        double max = blendTolerance;
        int result = -1;
        if (d00 <= max) {
            max = d00;
            result = 0;
        }
        if (d01 <= max) {
            max = d01;
            result = 1;
        }
        if (d10 <= max) {
            max = d10;
            result = 2;
        }
        if (d11 <= max) {
            //max = d11;
            result = 3;
        }
        if (result == 0) {
            for (int i = 1; i < lsCoords.size(); i++) {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        } else if (result == 1) {
            for (int i = lsCoords.size()-2; i >= 0; i--) {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        } else if (result == 2) {
            for (int i = 1; i < lsCoords.size(); i++) {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }
        } else if (result == 3) {
            for (int i = lsCoords.size()-2; i >= 0; i--) {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }
        }
        return result > -1;
    }
    
    private void setDialogValues(MultiInputDialog dialog) {
        dialog.addDoubleField(TOLERANCE, blendTolerance, 6, THE_BLEND_TOLERANCE_TOOLTIP);
        final JCheckBox removeCheckBox = dialog.addCheckBox(REMOVE_SOURCE_LINES, removeSourceLines, REMOVE_SOURCE_LINES_TOOLTIP);
		removeCheckBox.setEnabled(!createNewLayer);
		dialog.addCheckBox(TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS, transferFirstAttributesToAllResultingLineStrings, TRANSFER_FIRST_ATTRIBUTES_TO_ALL_RESULTING_LINE_STRINGS_TOOLTIP);
        final JCheckBox newLayerCheckBox = dialog.addCheckBox(CREATE_NEW_LAYER, createNewLayer, CREATE_NEW_LAYER_TOOLTIP);
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
