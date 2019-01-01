package com.vividsolutions.jump.workbench.ui.renderer;

import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class WmsLayerRendererFactory implements RendererFactory<WMSLayer>{
  public Renderer create(WMSLayer layer, LayerViewPanel panel, int maxFeatures) {
    return new WMSLayerRenderer(layer, panel);
  }
}
