package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
import java.awt.Color;
import java.util.Iterator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;


/**
 * Stays in sync with the LayerManager.
 */
public class LayerComboBox extends JComboBox<Layerable> {

    //For the layer name, use " " rather than "" so that the renderer is sized normally. 
    //See comments in #updateModel. [Jon Aquino]
    private static final Layer DUMMY_LAYER = new Layer(" ",
            new Color(0, 0, 0, 0),
            AddNewLayerPlugIn.createBlankFeatureCollection(), new LayerManager());

    private LayerManager layerManager = new LayerManager();
        
    public Object getSelectedItem() {
        return (super.getSelectedItem() != DUMMY_LAYER)
        ? super.getSelectedItem() : null;
    }        

    private LayerListener listener = new LayerListener() {
            public void featuresChanged(FeatureEvent e) {
            }

            public void layerChanged(LayerEvent e) {
                updateModel();
            }

            public void categoryChanged(CategoryEvent e) {
            }
        };

    public LayerComboBox() {
        super(new DefaultComboBoxModel<>());
        setRenderer(new LayerNameRenderer());
    }
    
    private DefaultComboBoxModel<Layerable> getMyModel() {
        return (DefaultComboBoxModel<Layerable>) getModel();
    }

    private void updateModel() {
        GeoReferencedLayerable selectedLayer = getSelectedLayer();
        getMyModel().removeAllElements();

        for (Iterator<GeoReferencedLayerable> i = layerManager.iterator(GeoReferencedLayerable.class); i.hasNext();) {
            GeoReferencedLayerable layerable = i.next();
            getMyModel().addElement(layerable);
        }

        if (getMyModel().getSize() == 0) {
            //Don't leave the combobox empty -- an empty combobox is rendered a few 
            //pixels larger than a non-empty one. If it is in a packed frame's GridBagLayout,
            //the GridBagLayout will collapse all components to minimum size
            //(see Java bug 4247013). [Jon Aquino] 
            getMyModel().addElement(DUMMY_LAYER);
        }

        if (-1 != getMyModel().getIndexOf(selectedLayer)) {
            getMyModel().setSelectedItem(selectedLayer);
        }
    }

    public GeoReferencedLayerable getSelectedLayer() {
        return (GeoReferencedLayerable) getSelectedItem();
    }

    public void setLayerManager(LayerManager layerManager) {
        this.layerManager.removeLayerListener(listener);
        this.layerManager = layerManager;
        this.layerManager.addLayerListener(listener);
        updateModel();
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public void setSelectedLayer(Layer candidateLayer) {
        setSelectedItem(candidateLayer);
    }


}
