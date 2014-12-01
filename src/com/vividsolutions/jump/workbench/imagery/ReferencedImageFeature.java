package com.vividsolutions.jump.workbench.imagery;

import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Disposable;

public class ReferencedImageFeature extends BasicFeature implements Disposable {

  private static final long serialVersionUID = -8666111579126825117L;

  public ReferencedImageFeature(FeatureSchema featureSchema) {
    super(featureSchema);
  }

  public void dispose() {
    // at the moment disposal is done in ReferencedImagesLayer
  }

}
