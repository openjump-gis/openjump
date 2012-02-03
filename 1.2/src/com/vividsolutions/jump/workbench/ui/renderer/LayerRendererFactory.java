package com.vividsolutions.jump.workbench.ui.renderer;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class LayerRendererFactory implements RendererFactory<Layer> {
  public Renderer create(final Layer layer, final LayerViewPanel panel,
    final int maxFeatures) {
    LayerRenderer renderer = new LayerRenderer(layer, panel);
    renderer.setMaxFeatures(maxFeatures);
    return renderer;
  }
}
