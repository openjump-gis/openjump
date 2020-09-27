package com.vividsolutions.jump.datastore.spatialdatabases;

import java.awt.Color;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

public class SpatialDSLayer extends Layer {

  public SpatialDSLayer() {
    super();
  }

  public SpatialDSLayer(String name, Color fillColor, FeatureCollection featureCollection, LayerManager layerManager) {
    super(name, fillColor, featureCollection, layerManager);
  }

  public void dispose() {
    super.dispose();
    ConnectionManager.instance(JUMPWorkbench.getInstance().getContext()).closeConnection(this);
  }
}
