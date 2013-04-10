/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class CombineSelectedFeaturesPlugIn extends AbstractPlugIn {
  public static ImageIcon ICON = IconLoader.icon("features_combine.png");

  public CombineSelectedFeaturesPlugIn() {
    super();
    this.setShortcutKeys(KeyEvent.VK_G);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public boolean execute(final PlugInContext context) throws Exception {
    final ArrayList originalFeatures = new ArrayList(context
        .getLayerViewPanel().getSelectionManager()
        .getFeaturesWithSelectedItems());
    final Feature combinedFeature = combine(originalFeatures, context);
    final Layer layer = (Layer) context.getLayerViewPanel()
        .getSelectionManager().getLayersWithSelectedItems().iterator().next();
    execute(new UndoableCommand(getName()) {
      public void execute() {
        layer.getFeatureCollectionWrapper().removeAll(originalFeatures);
        layer.getFeatureCollectionWrapper().add(combinedFeature);
      }

      public void unexecute() {
        layer.getFeatureCollectionWrapper().remove(combinedFeature);
        layer.getFeatureCollectionWrapper().addAll(originalFeatures);
      }
    }, context);
    // Select outside #execute so it's not done on a redo. [Jon Aquino]
    context.getLayerViewPanel().getSelectionManager().getFeatureSelection()
        .selectItems(layer, combinedFeature);
    return true;
  }

  private Feature combine(Collection originalFeatures, PlugInContext context) {
    GeometryFactory factory = new GeometryFactory();
    Feature feature = (Feature) ((Feature) originalFeatures.iterator().next())
        .clone();
    Class narrowestCollectionClass = narrowestCollectionClass(originalFeatures);
    if (narrowestCollectionClass == MultiPoint.class) {
      feature.setGeometry(factory.createMultiPoint((Point[]) FeatureUtil
          .toGeometries(originalFeatures).toArray(
              new Point[originalFeatures.size()])));
    } else if (narrowestCollectionClass == MultiLineString.class) {
      feature.setGeometry(factory
          .createMultiLineString((LineString[]) FeatureUtil.toGeometries(
              originalFeatures)
              .toArray(new LineString[originalFeatures.size()])));
    } else if (narrowestCollectionClass == MultiPolygon.class) {
      // Conbining adjacent polygons may produce invalid MultiPolygon
      // In this case, we prefer returning a GeometryCollection 
      MultiPolygon mp = factory.createMultiPolygon((Polygon[]) FeatureUtil
          .toGeometries(originalFeatures).toArray(
              new Polygon[originalFeatures.size()]));
      if (mp.isValid()) {
          feature.setGeometry(mp);
      }
      else {
          context.getWorkbenchFrame().warnUser(I18N.get("com.vividsolutions.jump.workbench.ui.plugin.CombineSelectedFeaturesPlugIn.invalid-multipolygon"));
          feature.setGeometry(factory.createGeometryCollection((Geometry[]) FeatureUtil
          .toGeometries(originalFeatures).toArray(
              new Geometry[originalFeatures.size()])));
      }
    } else {
      feature
          .setGeometry(factory
              .createGeometryCollection((Geometry[]) FeatureUtil.toGeometries(
                  originalFeatures).toArray(
                  new Geometry[originalFeatures.size()])));
    }
    return feature;
  }

  public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustHaveSelectedItemsCheck(1))
        .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(2))
        .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
  }

  private Class narrowestCollectionClass(Collection features) {
    boolean hasPoints = false;
    boolean hasLineStrings = false;
    boolean hasPolygons = false;
    for (Iterator i = features.iterator(); i.hasNext();) {
      Feature feature = (Feature) i.next();
      if (feature.getGeometry() instanceof Point) {
        hasPoints = true;
      } else if (feature.getGeometry() instanceof LineString) {
        hasLineStrings = true;
      } else if (feature.getGeometry() instanceof Polygon) {
        hasPolygons = true;
      } else {
        return GeometryCollection.class;
      }
    }
    if (hasPoints && !hasLineStrings && !hasPolygons) {
      return MultiPoint.class;
    }
    if (!hasPoints && hasLineStrings && !hasPolygons) {
      return MultiLineString.class;
    }
    if (!hasPoints && !hasLineStrings && hasPolygons) {
      return MultiPolygon.class;
    }
    return GeometryCollection.class;
  }

  public ImageIcon getIcon() {
    return ICON;
  }

}
