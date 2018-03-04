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

package org.openjump.core.ui.plugin.layer;

import java.awt.Color;
import java.awt.Dimension;
import java.util.*;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopySelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

public class CombineSelectedLayersPlugIn extends AbstractPlugIn {

  private static final String LAYER = I18N
      .get("org.openjump.core.ui.plugin.layer.ExtractLayersByAttribute.LAYER");
  private static final String COMBINE_SELECTED_LAYERS = I18N
      .get("org.openjump.core.ui.plugin.layer.CombineSelectedLayers");

  private String uniqueName = LAYER;

  @Override
  public String getName() {
    return COMBINE_SELECTED_LAYERS;
  }

//  public void initialize(PlugInContext context) throws Exception {
//    context.getFeatureInstaller().addMainMenuItem(this,
//        new String[] { MenuNames.LAYER }, COMBINE_SELECTED_LAYERS, false, null,
//        createEnableCheck(context.getWorkbenchContext()));
//  }

  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    new CopySelectedItemsPlugIn().execute(context);

    Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();

    Set<String> allAttributeNames = getAllAttributeNames(selectedLayers);
    setUniqueAttributeName(allAttributeNames);

    FeatureSchema featureSchema = new FeatureSchema();
    featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    featureSchema.addAttribute(uniqueName, AttributeType.STRING);
    // add all attributes from selected layers
    for (Layer layer : selectedLayers) {
      FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
      for (int j = 0; j < schema.getAttributeCount(); j++) {
        String name = schema.getAttributeName(j);
        if (AttributeType.GEOMETRY == schema.getAttributeType(name)) {
          continue;
        }
        if (!featureSchema.hasAttribute(name)) {
          featureSchema.addAttribute(name, schema.getAttributeType(name));
        } else if (schema.getAttributeType(name) != featureSchema
            .getAttributeType(name)) {
          featureSchema.addAttribute(
              name + getAttributeTypeChar(schema.getAttributeType(name)),
              schema.getAttributeType(name));
        }
      }
    }

    FeatureDataset featureDataset = new FeatureDataset(featureSchema);

    Collection selectedCategories = context.getLayerNamePanel()
        .getSelectedCategories();
    Layer newLayer = context.addLayer(
        selectedCategories.isEmpty() ? StandardCategoryNames.RESULT
            : selectedCategories.iterator().next().toString(), "Combined",
        featureDataset);

    newLayer.setFeatureCollectionModified(true).setEditable(true);
    Map<Object,BasicStyle> attributeToStyleMap = new HashMap<>();
    ColorScheme colorScheme = ColorScheme.create("Set 3 (ColorBrewer)");
    for (Layer layer : selectedLayers) {
      Collection<Feature> features = layer.getFeatureCollectionWrapper().getFeatures();
      newLayer.getFeatureCollectionWrapper().addAll(
          conform(features, featureSchema, layer.getName()));
      attributeToStyleMap.put(layer.getName(),
          new BasicStyle(colorScheme.next()));
    }

    newLayer.getBasicStyle().setEnabled(false);
    ColorThemingStyle themeStyle = new ColorThemingStyle(LAYER,
        attributeToStyleMap, new BasicStyle(Color.gray));
    themeStyle.setEnabled(true);
    newLayer.addStyle(themeStyle);
    ColorThemingStyle.get(newLayer).setEnabled(true);
    newLayer.removeStyle(ColorThemingStyle.get(newLayer));
    ColorThemingStyle.get(newLayer).setEnabled(true);
    newLayer.getBasicStyle().setEnabled(false);

    return true;
  }

  private static String getAttributeTypeChar(AttributeType type) {
    return type.toString().substring(0, 1);
  }

  private Set<String> getAllAttributeNames(Layer[] layers) {
    Set<String> set = new HashSet<>();
    for (Layer layer : layers) {
      FeatureCollection fc = layer.getFeatureCollectionWrapper();
      for (int i = 0 ; i < fc.getFeatureSchema().getAttributeCount() ; i++) {
        set.add(fc.getFeatureSchema().getAttributeName(i));
      }
    }
    return set;
  }

  private void setUniqueAttributeName(Set<String> set) {
    if (!set.contains(LAYER)) uniqueName = LAYER;
    else {
      int i = 0;
      while (set.contains(uniqueName)) uniqueName = LAYER + "_" + ++i;
    }
  }

  public Collection<Feature> conform(Collection<Feature> features,
      FeatureSchema targetFeatureSchema, String layerName) {
    final ArrayList<Feature> featureCopies = new ArrayList<>();

    for (Feature feature : features) {
      featureCopies.add(conform(feature, targetFeatureSchema, layerName));
    }

    return featureCopies;
  }

  private Feature conform(Feature original,
      FeatureSchema targetFeatureSchema, String layerName) {
    // Transfer as many attributes as possible, matching on name. [Jon Aquino]
    Feature copy = new BasicFeature(targetFeatureSchema);
    copy.setGeometry((Geometry) original.getGeometry().clone());

    for (int i = 0; i < original.getSchema().getAttributeCount(); i++) {
      if (i == original.getSchema().getGeometryIndex()) {
        continue;
      }

      String attributeName = original.getSchema().getAttributeName(i);
      String newAttributeName = original.getSchema().getAttributeName(i);
      if (!copy.getSchema().hasAttribute(attributeName)) {
        continue;
      }

      if (copy.getSchema().getAttributeType(attributeName) != original
          .getSchema().getAttributeType(attributeName)) {
        newAttributeName += getAttributeTypeChar(original.getSchema()
            .getAttributeType(attributeName));
        if (copy.getSchema().getAttributeType(newAttributeName) != original
            .getSchema().getAttributeType(attributeName)) {
          continue;
        }
      }

      copy.setAttribute(newAttributeName, original.getAttribute(attributeName));
    }
    copy.setAttribute(uniqueName, layerName);

    return copy;
  }

  public MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithSelectionManagerMustBeActiveCheck()).add(
        checkFactory.createAtLeastNLayersMustBeSelectedCheck(2));
  }

  @Override
  public Icon getIcon(Dimension dim) {
    return IconLoader.icon("famfam/page_white_stack.png");
  }

  
}
