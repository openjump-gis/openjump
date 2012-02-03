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

package org.openjump.core.ui.plugin.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.SelectionManager;

/**
 * Selects all Features with the modified flag set
 * @author beckerl
 *
 */
public class InvertSelectionPlugIn extends AbstractPlugIn {
	
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
            new String[]
                {MenuNames.EDIT},
                I18N.get("org.openjump.core.ui.plugin.edit.InvertSelectionPlugIn.invert-selection")+"{pos:6}",
                false,
                null,
                createEnableCheck(context.getWorkbenchContext())); //enable check
    }

    public boolean execute(final PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        
        Collection oldSelectedFeatures = new ArrayList();
        Collection newSelectedFeatures = new ArrayList();
        LayerViewPanel layerViewPanel = context.getWorkbenchContext().getLayerViewPanel();
        SelectionManager selectionManager = layerViewPanel.getSelectionManager();
        
        // Layers process
        Collection layers = selectionManager.getLayersWithSelectedItems();
        for (Iterator layersIterator = layers.iterator() ; layersIterator.hasNext() ;) {
            // Invisible layers are just cleared
            Layer layer = (Layer)layersIterator.next();
            newSelectedFeatures.clear();
            oldSelectedFeatures = selectionManager.getFeaturesWithSelectedItems(layer);
            selectionManager.getFeatureSelection().unselectItems(layer);
            if (layer.isVisible()) {
                // Get an ordered set of old selected identifiers
                SortedSet ids = new TreeSet();
                for (Iterator it = oldSelectedFeatures.iterator() ; it.hasNext() ; ) {
                    ids.add(new Integer(((Feature)it.next()).getID()));
                }
                FeatureCollection featureCollection = layer.getFeatureCollectionWrapper();
                for (Iterator i = featureCollection.iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    if (!ids.contains(new Integer(feature.getID()))) {
                        newSelectedFeatures.add(feature);
                    }
                }
            }
            selectionManager.getFeatureSelection().selectItems(layer, newSelectedFeatures);
        }
        return true;
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
    }
}

