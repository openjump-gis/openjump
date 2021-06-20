package com.vividsolutions.jump.datastore.spatialdatabases;

import java.awt.Color;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

public class SpatialDSLayer extends Layer {
  protected WorkbenchContext workbenchContext;

  // enforce parameters using constructor below
  private SpatialDSLayer() {
    super();
  }

  public SpatialDSLayer(String name, Color fillColor, FeatureCollection featureCollection, LayerManager layerManager, WorkbenchContext workbenchContext) {
    super(name, fillColor, featureCollection, layerManager);
    this.workbenchContext = workbenchContext;
  }

  public void dispose() {
    super.dispose();
    ConnectionManager.instance(workbenchContext).closeConnection(this);
  }
}
