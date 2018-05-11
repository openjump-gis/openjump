package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

import java.util.Collection;
import java.util.List;

public interface VectorLayerable extends GeoreferencedLayerable {

  List<Style> getStyles();

  void addStyle(Style style);

  void removeStyle(Style style);

  void setStyles(Collection<Style> newStyles);

  void setFeatureCollection(final FeatureCollection featureCollection);

  FeatureCollectionWrapper getFeatureCollectionWrapper();

}
