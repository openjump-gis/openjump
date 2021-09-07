/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2008 Integrated Systems Analysts, Inc.
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

package org.openjump.core.ui.plugin.layer;

import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ExtractLayersByAttribute extends AbstractPlugIn {

  private final static String EXTRACT_LAYERS_BY_ATTRIBUTE = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extract-Layer-by-Attribute");
  private static final String LAYER_ATTRIBUTE = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Attribute");
  private static final String DIALOGMSG = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extracts-layers-using-a-common-attribute");
  private static final String LAYER = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.LAYER");
  // private static final String TEXT =
  // I18N.getInstance().get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.TEXT");
  private static final String EXTRACT = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.Extract");
  // NULL has not to be translated
  private static final String NULL = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute._NULL_");
  private static final String EMPTY = I18N.getInstance()
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute._EMPTY_");

  private Layer sourceLayer = null;

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    context.getFeatureInstaller().addMainMenuPlugin(this, new String[] { MenuNames.EDIT, MenuNames.EXTRACT }, getName(),
        false, ICON, createEnableCheck(context.getWorkbenchContext()));
  }

  public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
    return new MultiEnableCheck().add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1)).add(new EnableCheck() {
          // At one point, this EnableCheck should be added to
          // EnableCheckFactory with it's own I18N key string
          public String check(JComponent component) {
            Layer[] lyrs = workbenchContext.getLayerableNamePanel().getSelectedLayers();
            if (lyrs.length == 0) {
              return I18N.getInstance()
                  .get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-be-selected");
            } else if (lyrs[0].getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount() < 2) {
              return I18N.getInstance().get("ui.renderer.style.ColorThemingPanel.layer-must-have-at-least-1-attribute");
            }
            return null;
          }
        });
  }

  public boolean execute(PlugInContext context) throws Exception {
    sourceLayer = context.getSelectedLayer(0);
    MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    setDialogFields(dialog);

    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (dialog.wasOKPressed()) {
      String attrName = dialog.getText(LAYER_ATTRIBUTE);
      extractLayers(context, sourceLayer, attrName);
      return true;
    }
    return false;
  }

  private void setDialogFields(final MultiInputDialog dialog) {
    dialog.setSideBarDescription(DIALOGMSG);
    List<String> attributes = AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(sourceLayer);
    // If an attribute is named LAYER, it is set as the default (useful for DXF)
    String layerName = attributes.get(0);
    for (String attribute : attributes) {
      if (attribute.equalsIgnoreCase(LAYER)) {
        layerName = attribute;
        break;
      }
    }
    dialog.addComboBox(LAYER_ATTRIBUTE, layerName, attributes, null);
  }

  public String getName() {
    return EXTRACT_LAYERS_BY_ATTRIBUTE;
  }

  public static final ImageIcon ICON = IconLoader.icon("extract.gif");

  private void extractLayers(PlugInContext context, Layer layer, String attributeName) {
    FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
    List<Feature> featureList = featureCollection.getFeatures();
    FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
    int attributeIndex = featureSchema.getAttributeIndex(attributeName);

    Map<String, FeatureCollection> newLayersMap = new HashMap<>();
    for (Feature feature : featureList) {
      Object attribute = feature.getAttribute(attributeIndex);
      String attributeString = attribute == null ? NULL : attribute.toString().trim();
      if (attributeString.length() == 0)
        attributeString = EMPTY;
      FeatureCollection fc = newLayersMap.get(attributeString);
      if (fc == null) {
        fc = new FeatureDataset(featureSchema);
        newLayersMap.put(attributeString, fc);
      }
      fc.add(feature);
    }

    for (Map.Entry<String, FeatureCollection> entry : (new TreeMap<>(newLayersMap)).entrySet()) {
      context.addLayer(EXTRACT, entry.getKey(), entry.getValue());
    }
    context.getLayerViewPanel().repaint();
  }

}
