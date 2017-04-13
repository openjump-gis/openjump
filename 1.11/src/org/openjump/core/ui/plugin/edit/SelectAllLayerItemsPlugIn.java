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
 * created:  		16.05.2005
 * last modified:  	
 * 
 * description:
 *    selects all items of the actual layer
 *    and informs about the number of selected items
 * 
 *****************************************************/

package org.openjump.core.ui.plugin.edit;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

/**
 * Selects all items of the actual layer and informs about the number of
 * selected items
 * 
 * @author sstein
 * 
 */
public class SelectAllLayerItemsPlugIn extends AbstractPlugIn {

  private String name = I18N
      .get("org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn.select-all-items-of-selected-layers");

  public SelectAllLayerItemsPlugIn() {
    super();
    this.setShortcutKeys(KeyEvent.VK_A);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public void initialize(PlugInContext context) throws Exception {

    context
        .getFeatureInstaller()
        .addPopupMenuPlugin(
            context.getWorkbenchContext().getWorkbench().getFrame()
                .getLayerNamePopupMenu(),
            this,
            I18N.get("org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn.select-current-layer-items"),
            false, null, createEnableCheck(context.getWorkbenchContext()));

    context
        .getFeatureInstaller()
        .addMainMenuPlugin(
            this,
            new String[] { MenuNames.EDIT, MenuNames.SELECTION },
            name,
            false, null, createEnableCheck(context.getWorkbenchContext()));
    
  }

  public static MultiEnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(checkFactory
        .createAtLeastNLayersMustBeSelectedCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {

    int count = 0;
    Layer[] selectedLayers = context.getSelectedLayers();
    for (int i = 0; i < selectedLayers.length; i++) {
      Layer actualLayer = selectedLayers[i];
      if (actualLayer.isVisible()) {
        FeatureCollection fc = context.getSelectedLayer(i)
            .getFeatureCollectionWrapper().getWrappee();
        Collection<Feature> features = new ArrayList<>();

        for (Iterator iter = fc.iterator(); iter.hasNext();) {
          Feature element = (Feature) iter.next();
          features.add(element);
          count++;
        }
        context.getLayerViewPanel().getSelectionManager().getFeatureSelection()
            .selectItems(actualLayer, features);
      }
    }
    final Collection myf = context.getLayerViewPanel().getSelectionManager()
        .getFeaturesWithSelectedItems();
    context
        .getWorkbenchFrame()
        .setTimeMessage(
            I18N.get("org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn.layer-items")
                + ": "
                + count
                + ", "
                + I18N
                    .get("org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn.selected-items")
                + ": " + myf.size());
    System.gc();
    return true;
  }

  public String getName() {
    return name;
  }
}
