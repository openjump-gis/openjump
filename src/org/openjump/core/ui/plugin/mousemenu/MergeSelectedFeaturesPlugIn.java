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

package org.openjump.core.ui.plugin.mousemenu;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * A PlugIn to merge selected features. Polygon are unioned while linestring are
 * merged.
 * 
 * @author Micha&euml;l MICHAUD
 **/
public class MergeSelectedFeaturesPlugIn extends AbstractPlugIn {

  private static final ImageIcon ICON = IconLoader.icon("features_merge.png");

  public MergeSelectedFeaturesPlugIn() {
    super();
    this.setShortcutKeys(KeyEvent.VK_M);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public ImageIcon getIcon() {
    return ICON;
  }

  public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustHaveSelectedItemsCheck(1))
        .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(2))
        .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
  }

  public boolean execute(PlugInContext context) throws Exception {

    final Collection features = context.getWorkbenchContext()
        .getLayerViewPanel().getSelectionManager()
        .getFeaturesWithSelectedItems();
    final Layer layer = (Layer) context.getWorkbenchContext()
        .getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems()
        .iterator().next();

    Collection points = new ArrayList();
    Collection linestrings = new ArrayList();
    Collection polygons = new ArrayList();
    for (Iterator it = features.iterator(); it.hasNext();) {
        Feature feature = (Feature) it.next();
        distribute(feature.getGeometry(), points, linestrings, polygons);
    }

    // Merge linear features
    LineMerger merger = new LineMerger();
    merger.add(linestrings);
    linestrings = merger.getMergedLineStrings();

    // Union all features
    Collection geometries = new ArrayList(polygons);
    geometries.addAll(linestrings);
    geometries.addAll(points);
    Geometry geometry = UnaryUnionOp.union(geometries);

    Iterator iterator = features.iterator();
    final Feature mergedFeature = (Feature) ((Feature) iterator.next()).clone();
    mergedFeature.setGeometry(geometry);

    execute(new UndoableCommand(getName(), layer) {
      
      public void execute() {
        getLayer().getFeatureCollectionWrapper().removeAll(features);
        getLayer().getFeatureCollectionWrapper().add(mergedFeature);
      }

      public void unexecute() {
        getLayer().getFeatureCollectionWrapper().remove(mergedFeature);
        getLayer().getFeatureCollectionWrapper().addAll(features);
      }
    }, context);

    context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
    // select the result so that it is easy to add a explode feature step
    context.getWorkbenchContext().getLayerViewPanel().getSelectionManager()
        .getFeatureSelection().selectItems(layer, mergedFeature);
    return true;
  }

  private void distribute(Geometry geometry, Collection points,
      Collection linestrings, Collection polygons) {
      if (geometry instanceof Point)
          points.add(geometry);
      else if (geometry instanceof LineString)
          linestrings.add(geometry);
      else if (geometry instanceof Polygon)
          polygons.add(geometry);
      else if (geometry instanceof GeometryCollection) {
          for (int i = 0; i < geometry.getNumGeometries(); i++) {
              distribute(geometry.getGeometryN(i), points, linestrings, polygons);
          }
      }
  }

}
