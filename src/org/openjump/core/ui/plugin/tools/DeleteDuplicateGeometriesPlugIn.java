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
package org.openjump.core.ui.plugin.tools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
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

/**
 * Eliminates features that have exactly the same geometry.
 * 
 * <p><b>Input:</b> A read-only source layer.<br>
 * <b>Output:</b> A result layer with deleted duplicate geometries. 
 * 
 * <p><b>Features:</b>
 * <ul>
 * <li>Implements DialogPlugIn with MultiInputDialog.
 * <li>Implements ThreadedPlugIn with TaskMonitor.
 * <li>Implements InstallablePlugIn with FeatureInstaller (menu items).
 * </ul>
 * 
 * @author Stefan Steiniger <i>(original author)</i>
 * @author Michaël Michaud <i>(algorithm reworked to take advantage of indexes)</i>
 * @author Benjamin Gudehus <i>(refactored class in order to improve readabilty)</i>
 */
public final class DeleteDuplicateGeometriesPlugIn extends AbstractPlugIn implements
        ThreadedPlugIn {
    
    //-----------------------------------------------------------------------------------
    // FIELDS.
    //-----------------------------------------------------------------------------------

    // Configuration parameters.
    private Layer confSourceLayer = null;
    private boolean confDeleteOnlySameAttributes = false;
    
    // Language strings.
    private String langName = "Delete Duplicate Geometries";
    private String langDescription = "deletes features with similar geometry";
    private String langSourceLayer = "select layer";
    private String langDeleteOnlySameAttributes = "delete only if attributes are the same";
    private String langMonitorCheckedFeatures = "checked";
    private String langResultNameCleaned = "cleaned";

    //-----------------------------------------------------------------------------------
    // PUBLIC METHODS.
    //-----------------------------------------------------------------------------------
    
    public String getName() {
        return langName;
    }
    
    public void initialize(PlugInContext context) throws Exception {
        initializeLanguageStrings();
        initializeMenuItem(context);
    }

    public boolean execute(PlugInContext context) throws Exception {
        this.reportNothingToUndoYet(context);
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), 
                getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        String resultLayerName = confSourceLayer.getName() + "-" + langResultNameCleaned;
        FeatureCollection resultDataset = this.deleteDuplicateGeometries(monitor);
        context.addLayer(StandardCategoryNames.RESULT, resultLayerName, resultDataset);
        System.gc();
    }
    
    //-----------------------------------------------------------------------------------
    // PRIVATE METHODS.
    //-----------------------------------------------------------------------------------
    
    private void initializeLanguageStrings() {
        String langPrefix = getClass().getCanonicalName();
        langSourceLayer = GenericNames.SELECT_LAYER;
        langName = I18N.get(langPrefix + ".Delete-Duplicate-Geometries");
        langDescription = I18N.get(langPrefix + 
                ".deletes-features-with-similar-geometry");
        langDeleteOnlySameAttributes = I18N.get(langPrefix + 
                ".delete-only-if-attributes-are-the-same");
        langMonitorCheckedFeatures = I18N.get(langPrefix + ".checked");
        langResultNameCleaned = I18N.get(langPrefix + ".cleaned");
    }
    
    private void initializeMenuItem(PlugInContext context) {
        FeatureInstaller installer = new FeatureInstaller(context.getWorkbenchContext());
        String[] menuPath = new String[] { MenuNames.TOOLS, MenuNames.TOOLS_QA };
        installer.addMainMenuItem(this, menuPath, new JMenuItem(getName() + "..."),
                createEnableCheck(context.getWorkbenchContext()));
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory
                .createAtLeastNLayersMustExistCheck(1));
    }
    
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarDescription(langDescription);
        dialog.addLayerComboBox(langSourceLayer, context.getCandidateLayer(0), null, 
                context.getLayerManager());
        dialog.addCheckBox(langDeleteOnlySameAttributes, confDeleteOnlySameAttributes);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        confSourceLayer = dialog.getLayer(langSourceLayer);
        confDeleteOnlySameAttributes = dialog.getBoolean(langDeleteOnlySameAttributes);
    }

    private FeatureCollection deleteDuplicateGeometries(TaskMonitor monitor) {
        // Method is completely reworked to take advantage of indexes [mmichaud
        // 2012-01-15].
        FeatureCollection sourceDataset = confSourceLayer.getFeatureCollectionWrapper();
        FeatureSchema sourceSchema = sourceDataset.getFeatureSchema();
        
        // Input data is indexed.
        FeatureCollection indexedDataset = new IndexedFeatureCollection(sourceDataset);
        Set<Integer> duplicateIDs = new HashSet<Integer>();
        
        @SuppressWarnings("unchecked")
        List<Feature> sourceFeatures = sourceDataset.getFeatures();
        int checkCount = 0;
        int checkSize = sourceDataset.size();
        
        for (Feature feature : sourceFeatures) {
            monitor.report(checkCount, checkSize, langMonitorCheckedFeatures);
            
            // For each feature, only candidate features are compared.
            Envelope envelope = feature.getGeometry().getEnvelopeInternal();
            @SuppressWarnings("unchecked")
            List<Feature> candidates = indexedDataset.query(envelope);
            
            for (Feature candidate : candidates) {
                // For equal features, the one with the greater ID is removed.
                if (candidate.getID() > feature.getID()
                        && feature.getGeometry().equalsNorm(candidate.getGeometry())) {
                    if (confDeleteOnlySameAttributes) {
                        // If geometry and attributes are equals, add ID to
                        // duplicates.
                        if (areAttributesEqual(feature, candidate, sourceSchema)) {
                            duplicateIDs.add(candidate.getID());
                        }
                    }
                    // If geometry are equals, add ID to duplicates.
                    else {
                        duplicateIDs.add(candidate.getID());
                    }
                }
            }
            checkCount += 1;
        }
        
        // Create a feature collection with features which ID is not in duplicates.
        FeatureCollection resultDataset = new FeatureDataset(sourceSchema);
        for (Feature feature : sourceFeatures) {
            if (!duplicateIDs.contains(feature.getID())) {
                resultDataset.add(feature.clone(true));
            }
        }
        return resultDataset;
    }
    
    private boolean areAttributesEqual(Feature feature, Feature candidate, 
            FeatureSchema schema) {
        boolean attributesEqual = true;
        int geometryIndex = schema.getGeometryIndex();
        for (int index = 0; index < schema.getAttributeCount(); index++) {
            if (index != geometryIndex) {
                Object attr1 = feature.getAttribute(index);
                Object attr2 = candidate.getAttribute(index);
                if (attr1 == null && attr2 == null) {
                    continue;
                } 
                else if (attr1 == null || attr2 == null || !attr1.equals(attr2)) {
                    attributesEqual = false;
                    break;
                }
            }
        }
        return attributesEqual;
    }

}
