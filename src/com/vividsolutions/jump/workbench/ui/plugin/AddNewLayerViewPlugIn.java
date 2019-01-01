package com.vividsolutions.jump.workbench.ui.plugin;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerView;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;


public class AddNewLayerViewPlugIn extends AbstractPlugIn {

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    @SuppressWarnings( "deprecation" )
    Layer[] layers = context.getLayerNamePanel().getSelectedLayers();
    if (layers.length != 1) return false;
    Layer layer = layers[0];
    LayerView layerView = new LayerView(layer.getName(), layer.getLayerManager());
    int index = context.getLayerManager().getCategory(layer).indexOf(layer);
    context.getLayerManager().getCategory(layer).add(++index, layerView);
    layerView.setEditable(false);
    layerView.setSelectable(false);
    return true;
  }

  @Override
  public EnableCheck getEnableCheck() {
    return EnableCheckFactory.getInstance()
            .createExactlyNLayersMustBeSelectedCheck(1);
  }

}
