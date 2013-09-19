package com.vividsolutions.jump.workbench.imagery;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.model.Disposable;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

// this is just a dummy to make layers created with referenced images 
// distinguishable from plain vector layers
public class ReferencedImagesLayer extends Layer implements Disposable {

  public ReferencedImagesLayer() {
    super();
  }

  public ReferencedImagesLayer(String name, Color fillColor,
      FeatureCollection featureCollection, LayerManager layerManager) {
    super(name, fillColor, featureCollection, layerManager);
  }

  /**
   * cleanup - references in ImageryLayerdataset - ReferencedImage open file
   * streams and such
   */
  public void dispose() {
    ReferencedImageStyle irs = (ReferencedImageStyle) getStyle(ReferencedImageStyle.class);
    ImageryLayerDataset ils = irs.getImageryLayerDataset();

    Collection features = getFeatureCollectionWrapper().getFeatures();
    for (Iterator iter = features.iterator(); iter.hasNext();) {
      Feature feature = (Feature) iter.next();
      try {
        ReferencedImage img = ils.referencedImage(feature);
        if (img instanceof Disposable)
          ((Disposable) img).dispose();

      } catch (Exception e) {
      }
    }

    ils.dispose();
  }

}
