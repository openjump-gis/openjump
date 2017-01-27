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

package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool.ModifierKeySpec;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectTool;
import com.vividsolutions.jump.workbench.ui.cursortool.ShortcutsDescriptor;
import com.vividsolutions.jump.workbench.ui.renderer.FeatureSelectionRenderer;

public class SelectOneItemTool extends SelectTool implements ShortcutsDescriptor{
	
    final static String sSelectOneItem =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectOneItemTool.Select-One-Item");
    
    private LayerViewPanel layerViewPanel;
    private int maxFID = 2147483647;
    private int highFID = 0;
    Layer botLayer = null;
    Feature botFeature = null;
    boolean featureSelected = false;
    
    public SelectOneItemTool() {
        super(FeatureSelectionRenderer.CONTENT_ID);
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("SelectOne.gif"));
    }

    public String getName() {
        return sSelectOneItem;
    }
    
    private void reset()
    {
        maxFID = 2147483647;
        highFID = 0;
        botLayer = null;
        botFeature = null;
        featureSelected = false;
    }
    
    public void activate(LayerViewPanel layerViewPanel) {
        this.layerViewPanel = layerViewPanel;
        super.activate(layerViewPanel);
        selection = layerViewPanel.getSelectionManager().getFeatureSelection();
        reset();
    }

    protected void gestureFinished() throws NoninvertibleTransformException
    {
        super.gestureFinished();
        List layerList = layerViewPanel.getLayerManager().getVisibleLayers(false);
        Layer topLayer = null;
        Feature topFeature = null;
        featureSelected = false;
        
        for (Iterator i = layerList.iterator(); i.hasNext();)
        {
            Layer layer = (Layer) i.next();
            Collection selectedFeatures = layerViewPanel.getSelectionManager().getFeaturesWithSelectedItems(layer);
            
            for (Iterator j = selectedFeatures.iterator(); j.hasNext();)
            {
                featureSelected = true;
                Feature feature = (Feature) j.next();
                int fID = feature.getID();
                layerViewPanel.getSelectionManager().getFeatureSelection().unselectItems(layer, feature);
                if ((fID > highFID) && (fID < maxFID))
                {
                    topLayer = layer;
                    topFeature = feature;
                    highFID = fID;
                }
            }
        }
        
        if (!featureSelected) reset();
        
        if ((topLayer != null) && (topFeature != null))
        {
            if (highFID > 0) maxFID = highFID;
            highFID = 0;
            botLayer = topLayer;
            botFeature = topFeature;
            layerViewPanel.getSelectionManager().getFeatureSelection().selectItems(topLayer, topFeature);
        }
        else if ((botLayer != null) && (botFeature != null))
        {
            layerViewPanel.getSelectionManager().getFeatureSelection().selectItems(botLayer, botFeature);
        }
    }
    
    // override SelectTool shortcut, not supported
    public Map<ModifierKeySpec, String> describeShortcuts() {
      return null;
    }
}
