package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.feature.*;

import java.util.ArrayList;
import java.util.Collection;

public class LayerView extends Layer {

  private Layer layer;

  /**
   * Called by Java2XML
   */
  public LayerView() {
  }

  // 2018-08-22 : add layerName attribute (original name of the underlying layer)
  // to increase compatibility with java2xml and to make it possible to build the
  // LayerView from the xml project file before the underlying Layer is completely
  // initialized
  String layerName;
  public String getLayerName() {
    return layerName;
  }
  public void setLayerName(String layerName) {
    this.layerName = layerName;
  }

  @Override
  public void setLayerManager(LayerManager layerManager) {
    super.setLayerManager(layerManager);
    this.layer = layerManager.getLayer(getLayerName());
  }

  public LayerView(final String layerName, LayerManager layerManager) {
    super(layerName,
            layerManager.generateLayerFillColor(),
            layerManager.getLayer(layerName).getFeatureCollectionWrapper(),
            layerManager);
    this.layerName = layerName;
    this.layer = layerManager.getLayer(layerName);
    boolean firingEvents = getLayerManager().isFiringEvents();
    getLayerManager().setFiringEvents(false);
    try {
      setName(getName().replaceAll(this.layer.getName(),"").trim());
    } finally {
      getLayerManager().setFiringEvents(firingEvents);
    }
  }


  @Override
  public void setFeatureCollection(FeatureCollection featureCollection) {

    ObservableFeatureCollection observableFeatureCollection = (ObservableFeatureCollection) featureCollection;

    observableFeatureCollection.add(new ObservableFeatureCollection.Listener() {

      public void featuresAdded(Collection features) {
        getLayerManager().fireFeaturesChanged(features, FeatureEventType.ADDED,
                LayerView.this);
      }

      public void featuresRemoved(Collection features) {
        getLayerManager().fireFeaturesChanged(features,
                FeatureEventType.DELETED, LayerView.this);
      }

    });

    super.setFeatureCollectionWrapper(observableFeatureCollection);
  }


  @Override
  public FeatureCollectionWrapper getFeatureCollectionWrapper() {
    // When the LayerView is build from the project xml file, it may be built
    // before underlying Layer is defined...
    if (layer != null) {
      return layer.getFeatureCollectionWrapper();
    } else {
      // ... in this case, try again to load data from the underlying Layer
      // next time the application need to display the data.
      this.layer = getLayerManager().getLayer(layerName);
      if (this.layer != null) {
        return layer.getFeatureCollectionWrapper();
      } else {
        return new ObservableFeatureCollection(
                FeatureDatasetFactory.createFromGeometry(new ArrayList<Feature>())
        );
      }
    }
  }


  public Layer getLayer() {
    return layer;
  }

  public boolean isSelectable() {
    return false;
  }

  public boolean isEditable() {
    return false;
  }

  public void dispose() {}

}
