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
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Sort categories by number of features
 * 
 * @author clark4444
 * 
 */
public class SortCategoryByFeaturesPlugIn extends SortCategoryAbstractPlugIn {

  private static final ImageIcon ICON = null;

  private String subMenuLabel = "By Features";
  private String mostLabel = "By Least Number of Features";
  private String leastLabel = "By Most Number of Features";

  @Override
  protected String getSubMenuLabel() {
    return subMenuLabel;
  }

  @Override
  protected void addMenuOptions(PlugInContext context) {

    this.subMenuLabel = I18N
        .get("org.openjump.core.ui.plugin.layer.SortCategoryByFeaturesPlugIn.By-Featues");
    this.mostLabel = I18N
        .get("org.openjump.core.ui.plugin.layer.SortCategoryByFeaturesPlugIn.By-Least-Number-of-Features");
    this.leastLabel = I18N
        .get("org.openjump.core.ui.plugin.layer.SortCategoryByFeaturesPlugIn.By-Most-Number-of-Features");

    FeatureInstaller.addMainMenu(context.getFeatureInstaller(),
        new String[] { MenuNames.LAYER }, menuLabelOnLayer, 7);

    context.getFeatureInstaller().addMainMenuItem(this,
        new String[] { MenuNames.LAYER, menuLabelOnLayer, subMenuLabel },
        leastLabel, false, ICON,
        createEnableCheck(context.getWorkbenchContext()));

    context.getFeatureInstaller().addMainMenuItem(this,
        new String[] { MenuNames.LAYER, menuLabelOnLayer, subMenuLabel },
        mostLabel, false, ICON,
        createEnableCheck(context.getWorkbenchContext()));
  }

  @Override
  ArrayList<Layerable> getOrderedLayersInCategory(Category category,
      String sortLabel) {
    ArrayList<Layerable> layers = getCategoryArrayList(category);

    if (sortLabel.equals(mostLabel)) {
      Collections.sort(layers, new LayerableFeatureSort());
    } else if (sortLabel.equals(leastLabel)) {
      Collections.sort(layers,
          Collections.reverseOrder(new LayerableFeatureSort()));
    } else
      throw new IllegalStateException();

    return layers;
  }

  class LayerableFeatureSort implements Comparator<Layerable> {
    public int compare(Layerable layerable1, Layerable layerable2) {
      boolean layer1Feature = false;
      boolean layer2Feature = false;

      if (layerable1 instanceof Layer) {
        layer1Feature = true;
      }
      if (layerable2 instanceof Layer) {
        layer2Feature = true;
      }

      // Both have features
      if (layer1Feature && layer2Feature) {
        Layer layer1 = (Layer) layerable1;
        Layer layer2 = (Layer) layerable2;
        return layer2.getFeatureCollectionWrapper().size()
            - layer1.getFeatureCollectionWrapper().size();
        // not having features
      } else if (!layer1Feature && !layer2Feature) {
        return 0;
      } else if (layer1Feature) {
        return 1;
      } else
        return -1;

    }
  }
}
