package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;


/**
 * Displays and stays in sync with a single Layer.
 */
public class OneLayerAttributeTab extends AttributeTab {
    public OneLayerAttributeTab(WorkbenchContext context, TaskFrame taskFrame,
        LayerManagerProxy layerManagerProxy) {
        super(new InfoModel(), context, taskFrame, layerManagerProxy, true);
        context.getLayerManager().addLayerListener(new LayerListener() {
                public void featuresChanged(FeatureEvent e) {
                    if (getLayerTableModel() == null) {
                        //Get here after attribute viewer window is closed [Jon Aquino]
                        return;
                    }
                    if ((e.getLayer() == getLayerTableModel().getLayer()) &&
                            (e.getType() == FeatureEventType.ADDED)) {
                        //DELETED events are already handled in LayerTableModel
                        getLayerTableModel().addAll(e.getFeatures());
                    }
                }

                public void layerChanged(LayerEvent e) {
                }

                public void categoryChanged(CategoryEvent e) {
                }
            });
    }

    public OneLayerAttributeTab setLayer(Layer layer) {
        if (!getModel().getLayers().isEmpty()) {
            getModel().remove(getLayer());
        }

        //InfoModel#add must be called after the AttributeTab is created; otherwise
        //layer won't be added to the Attribute Tab -- the AttributeTab listens for
        //the event fired by InfoModel#add. [Jon Aquino]
        getModel().add(layer, layer.getFeatureCollectionWrapper().getFeatures());

        return this;
    }

    public Layer getLayer() {
        //null LayerTableModel if for example the user has just removed the layer
        //from the LayerManager [Jon Aquino]
        return (getLayerTableModel() != null) ? getLayerTableModel().getLayer()
                                              : null;
    }

    public LayerTableModel getLayerTableModel() {
        return (!getModel().getLayerTableModels().isEmpty())
        ? (LayerTableModel) getModel().getLayerTableModels().iterator().next()
        : null;
    }
}
