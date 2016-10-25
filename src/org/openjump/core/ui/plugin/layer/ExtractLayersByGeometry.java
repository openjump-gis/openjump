/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2007 Integrated Systems Analysts, Inc.
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

import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * This plugin split a layer into several layers when its features have
 * different geometry dimensions. It can produce a maximum of 4 layers
 * <ul>
 * <li>Empty geometries</li>
 * <li>Punctual geometries</li>
 * <li>Linear geometries</li>
 * <li>Polygonal geometries</li>
 * </ul>
 * Rules followed to spread features into the 4 layers :
 * <ul>
 * <li>Empty GeometryCollection's are copied into empty-geometries layer</li>
 * <li>Non-empty GeometryCollections are exploded into simple geometry features</li>
 * <li>Empty simple features are copied into the layer corresponding to their
 * dimension</li>
 * <li>Non-empty simple features are copied into the layer corresponding to
 * their dimension</li>
 * </ul>
 */
public class ExtractLayersByGeometry extends AbstractThreadedUiPlugIn {

  private final static String EXTRACT_LAYERS_BY_GEOMETRY_TYPE = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-Layers-by-Geometry-Type");

  private final static String EXTRACT_BY_GEOMETRY_TYPE = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Geometry-Type");
  private final static String EXTRACT_BY_GEOMETRY_TYPE_TT = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Geometry-Type-Tooltip");

  private final static String EXTRACT_BY_SHAPEFILE_TYPE = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Shapefile-Type");
  private final static String EXTRACT_BY_SHAPEFILE_TYPE_TT = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Shapefile-Type-Tooltip");

  private final static String EXTRACT_BY_GEOMETRY_DIMENSION = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Geometry-Dimension");
  private final static String EXTRACT_BY_GEOMETRY_DIMENSION_TT = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Extract-by-Geometry-Dimension-Tooltip");

  private final static String KEEP_EMPTY_GEOMETRIES_APPART = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Keep-Empty-Geometries-Appart");

  private final static String DO_NOT_EXPLODE_GEOMETRY_COLLECTIONS = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Do-Not-Explode-GeometryCollections");
  private final static String EXPLODE_ALL_GEOMETRY_COLLECTIONS = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Explode-All-GeometryCollections");
  private final static String EXPLODE_PURE_GEOMETRY_COLLECTIONS = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Explode-Pure-GeometryCollections");


  private final static String ONLY_ONE_GEOMETRY_TYPE_FOUND = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.Only-one-geometry-type-found");
  private final static String EMPTY = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.empty");
  private final static String POINT = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.point");
  private final static String MULTIPOINT = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.multipoint");
  private final static String ZERODIM = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.point");
  private final static String LINESTRING = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.linestring");
  private final static String MULTILINESTRING = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.multilinestring");
  private final static String POLYLINE = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.polyline");
  private final static String ONEDIM = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.line");
  private final static String POLYGON = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.polygon");
  private final static String MULTIPOLYGON = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.multipolygon");
  private final static String TWODIM = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.area");
  private final static String GEOMETRYCOLLECTION = I18N
          .get("org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry.geometrycollection");

  private final static String LAYER = GenericNames.SELECT_LAYER;

  public Layer layer = null;
  private boolean extractByGeometryType      = true;
  private boolean extractByShapefileType     = false;
  private boolean extractByGeometryDimension = false;

  private boolean doNotExplodeGeometryCollections = true;
  private boolean explodePureGeometryCollections  = false;
  private boolean explodeAllGeometryCollections   = false;

  private boolean keepEmptyGeometryAppart = true;

  public ExtractLayersByGeometry() {

  }

  public void initialize(PlugInContext context) throws Exception {
    FeatureInstaller.addMainMenu(context.getFeatureInstaller(),
        new String[] { MenuNames.EDIT }, MenuNames.EXTRACT, -1);
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[] { MenuNames.EDIT, MenuNames.EXTRACT }, getName(), false,
        ICON, createEnableCheck(context.getWorkbenchContext()));
  }

  public static MultiEnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithSelectionManagerMustBeActiveCheck()).add(
        checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {
    MultiInputDialog dialog =
            new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    layer = context.getCandidateLayer(0);
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
    monitor.allowCancellationRequests();
    if (layer != null) {
      splitLayer(monitor, context, layer);
    }
  }

  public String getName() {
    return EXTRACT_LAYERS_BY_GEOMETRY_TYPE + "...";
  }

  public static final ImageIcon ICON = IconLoader.icon("extract.gif");

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarDescription(EXTRACT_LAYERS_BY_GEOMETRY_TYPE);
    dialog.addLayerComboBox(LAYER, layer, context.getLayerManager());
    dialog.addRadioButton(EXTRACT_BY_GEOMETRY_TYPE, "GROUPBY", extractByGeometryType, EXTRACT_BY_GEOMETRY_TYPE_TT);
    dialog.addRadioButton(EXTRACT_BY_SHAPEFILE_TYPE, "GROUPBY", extractByShapefileType, EXTRACT_BY_SHAPEFILE_TYPE_TT);
    dialog.addRadioButton(EXTRACT_BY_GEOMETRY_DIMENSION, "GROUPBY", extractByGeometryDimension, EXTRACT_BY_GEOMETRY_DIMENSION_TT);
    dialog.addSeparator();
    dialog.addRadioButton(DO_NOT_EXPLODE_GEOMETRY_COLLECTIONS, "GC", doNotExplodeGeometryCollections, DO_NOT_EXPLODE_GEOMETRY_COLLECTIONS);
    dialog.addRadioButton(EXPLODE_PURE_GEOMETRY_COLLECTIONS, "GC", explodePureGeometryCollections, EXPLODE_PURE_GEOMETRY_COLLECTIONS);
    dialog.addRadioButton(EXPLODE_ALL_GEOMETRY_COLLECTIONS, "GC", explodeAllGeometryCollections, EXPLODE_ALL_GEOMETRY_COLLECTIONS);
    dialog.addSeparator();
    dialog.addCheckBox(KEEP_EMPTY_GEOMETRIES_APPART, true, KEEP_EMPTY_GEOMETRIES_APPART);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    this.layer = dialog.getLayer(LAYER);
    extractByGeometryType      = dialog.getBoolean(EXTRACT_BY_GEOMETRY_TYPE);
    extractByShapefileType     = dialog.getBoolean(EXTRACT_BY_SHAPEFILE_TYPE);
    extractByGeometryDimension = dialog.getBoolean(EXTRACT_BY_GEOMETRY_DIMENSION);

    doNotExplodeGeometryCollections = dialog.getBoolean(DO_NOT_EXPLODE_GEOMETRY_COLLECTIONS);
    explodePureGeometryCollections = dialog.getBoolean(EXPLODE_PURE_GEOMETRY_COLLECTIONS);
    explodeAllGeometryCollections = dialog.getBoolean(EXPLODE_ALL_GEOMETRY_COLLECTIONS);

    keepEmptyGeometryAppart = dialog.getBoolean(KEEP_EMPTY_GEOMETRIES_APPART);
  }

  private void splitLayer(TaskMonitor monitor, PlugInContext context, Layer layer) {

    FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();

    FeatureCollection emptyFeatures = new FeatureDataset(schema);
    FeatureCollection pointFeatures = new FeatureDataset(schema);
    FeatureCollection multiPointFeatures = new FeatureDataset(schema);
    FeatureCollection zeroDimFeatures = new FeatureDataset(schema);
    FeatureCollection lineFeatures = new FeatureDataset(schema);
    FeatureCollection multiLineFeatures = new FeatureDataset(schema);
    FeatureCollection polyLineFeatures = new FeatureDataset(schema);
    FeatureCollection oneDimFeatures = new FeatureDataset(schema);
    FeatureCollection polygonFeatures = new FeatureDataset(schema);
    FeatureCollection multiPolyFeatures = new FeatureDataset(schema);
    FeatureCollection twoDimFeatures = new FeatureDataset(schema);
    FeatureCollection geometryCollectionFeatures = new FeatureDataset(schema);

    Map<String,FeatureCollection> map = new HashMap<>();
    map.put(EMPTY, emptyFeatures);
    map.put(POINT, pointFeatures);
    map.put(MULTIPOINT, multiPointFeatures);
    map.put(ZERODIM, zeroDimFeatures);
    map.put(LINESTRING, lineFeatures);
    map.put(MULTILINESTRING, multiLineFeatures);
    map.put(POLYLINE, polyLineFeatures);
    map.put(ONEDIM, oneDimFeatures);
    map.put(POLYGON, polygonFeatures);
    map.put(MULTIPOLYGON, multiPolyFeatures);
    map.put(TWODIM, twoDimFeatures);
    map.put(GEOMETRYCOLLECTION, geometryCollectionFeatures);

    FeatureCollection featureCollection = layer.getFeatureCollectionWrapper();

    for (Feature feature : featureCollection.getFeatures()) {
      if (monitor.isCancelRequested()) {
        break;
      }
      extractByGeomType(feature, map);
    }
    int nbList = 0;
    for (FeatureCollection fc : map.values()) {
      if (fc.size()>0) nbList++;
    }
    if (nbList < 2) {
      context.getWorkbenchFrame().warnUser(ONLY_ONE_GEOMETRY_TYPE_FOUND);
    } else {
      for (Map.Entry<String,FeatureCollection> entry : map.entrySet()) {
        if (entry.getValue().size() == 0) continue;
        Layer lyr = context.addLayer(StandardCategoryNames.RESULT, layer.getName() + " - " + entry.getKey(), entry.getValue());
        lyr.setStyles(layer.cloneStyles());
      }
      context.getLayerViewPanel().repaint();
    }
  }

  private void extractByGeomType(Feature feature, Map<String,FeatureCollection> map) {
    Geometry geometry = feature.getGeometry();
    Feature f = feature.clone(false, true);
    f.setGeometry(geometry);

    if(keepEmptyGeometryAppart && geometry.isEmpty()) {
      map.get(EMPTY).add(f);
    }

    else if (geometry instanceof GeometryCollection &&
            (((explodePureGeometryCollections && geometry.getGeometryType().equals("GeometryCollection")) ||
              explodeAllGeometryCollections))) {
      for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
        Geometry g = geometry.getGeometryN(i);
        Feature f2 = feature.clone(false, true);
        f2.setGeometry(g);
        extractByGeomType(f2, map);
      }
    } else if (geometry.getGeometryType().equals("Point")) {
      if (extractByGeometryType) {
        map.get(POINT).add(f);
      } else if (extractByShapefileType) {
        map.get(POINT).add(f);
      } else if (extractByGeometryDimension) {
        map.get(ZERODIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("MultiPoint")) {
      if (extractByGeometryType) {
        map.get(MULTIPOINT).add(f);
      } else if (extractByShapefileType) {
        map.get(MULTIPOINT).add(f);
      } else if (extractByGeometryDimension) {
        map.get(ZERODIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("LineString")) {
      if (extractByGeometryType) {
        map.get(LINESTRING).add(f);
      } else if (extractByShapefileType) {
        map.get(POLYLINE).add(f);
      } else if (extractByGeometryDimension) {
        map.get(ONEDIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("MultiLineString")) {
      if (extractByGeometryType) {
        map.get(MULTILINESTRING).add(f);
      } else if (extractByShapefileType) {
        map.get(POLYLINE).add(f);
      } else if (extractByGeometryDimension) {
        map.get(ONEDIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("Polygon")) {
      if (extractByGeometryType) {
        map.get(POLYGON).add(f);
      } else if (extractByShapefileType) {
        map.get(POLYGON).add(f);
      } else if (extractByGeometryDimension) {
        map.get(TWODIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("MultiPolygon")) {
      if (extractByGeometryType) {
        map.get(MULTIPOLYGON).add(f);
      } else if (extractByShapefileType) {
        map.get(POLYGON).add(f);
      } else if (extractByGeometryDimension) {
        map.get(TWODIM).add(f);
      }
    } else if (geometry.getGeometryType().equals("GeometryCollection")) {
      map.get(GEOMETRYCOLLECTION).add(f);
    }
  }

}
