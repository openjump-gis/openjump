package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

import java.awt.*;

public interface ImageLayerable {

  Image createImage(LayerViewPanel viewPanel);

  double getAlpha();

  void setAlpha(double alpha);
}
