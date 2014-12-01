package com.vividsolutions.jump.workbench.model;

import java.awt.Color;

import com.vividsolutions.jump.feature.FeatureCollection;

// this is just a dummy to make layers created with referenced images 
// distinguishable from plain vector layers
public class ReferencedImageLayer extends Layer {

  public ReferencedImageLayer() {
    super();
    // TODO Auto-generated constructor stub
  }

  public ReferencedImageLayer(String name, Color fillColor,
      FeatureCollection featureCollection, LayerManager layerManager) {
    super(name, fillColor, featureCollection, layerManager);
    // TODO Auto-generated constructor stub
  }
}
