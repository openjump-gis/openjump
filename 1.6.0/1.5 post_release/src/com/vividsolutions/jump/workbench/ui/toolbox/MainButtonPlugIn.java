package com.vividsolutions.jump.workbench.ui.toolbox;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.LayerStyleUtil;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 * Convenience superclass used in toolboxes that have one primary button.
 */
public abstract class MainButtonPlugIn extends ThreadedBasePlugIn {
    public static final String GENERATED_KEY = MainButtonPlugIn.class.getName() +
        " - GENERATED";
    private String taskMonitorTitle;
    private Component toolboxPanel;

    public MainButtonPlugIn(String taskMonitorTitle, Component toolboxPanel) {
        this.taskMonitorTitle = taskMonitorTitle;
        this.toolboxPanel = toolboxPanel;
    }

    public String getName() {
        return taskMonitorTitle;
    }

    protected Layer generateLayer(String name, String category, Color color,
        LayerManagerProxy proxy, FeatureCollection featureCollection,
        String description) {
        return generateLayer(false, name, category, color, proxy,
            featureCollection, description);
    }

    protected Layer generateLineLayer(String name, String category,
        Color color, LayerManagerProxy proxy,
        FeatureCollection featureCollection, String description) {
        return generateLayer(true, name, category, color, proxy,
            featureCollection, description);
    }

    private Layer generateLayer(boolean line, String name, String category,
        Color color, LayerManagerProxy proxy,
        FeatureCollection featureCollection, String description) {
        Layer layer = proxy.getLayerManager().getLayer(name);

        if (layer == null) {
            layer = new Layer(name, color, featureCollection,
                    proxy.getLayerManager());
            proxy.getLayerManager().addLayer(category, layer);
            layer.setVisible(false);                                

            if (line) {
                LayerStyleUtil.setLinearStyle(layer, color, 2, 4);
            }

        } else {
            layer.setFeatureCollection(featureCollection);
        }

        layer.setDescription(description);

        //May have been loaded from a file [Jon Aquino]
        layer.getBlackboard().put(GENERATED_KEY, new Object());

        return layer;
    }

    public boolean execute(PlugInContext context) throws Exception {
        if (validateInput() != null) {
            reportValidationError(validateInput());
        }

        return validateInput() == null;
    }

    private void reportValidationError(String errorMessage) {
        JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(
                toolboxPanel), errorMessage, "JUMP", JOptionPane.ERROR_MESSAGE);
    }

    protected void removeAndDisposeLayer(String name, PlugInContext context) {
        Layer layer = context.getLayerManager().getLayer(name);

        if (layer == null) {
            return;
        }

        context.getLayerManager().remove(layer);
        layer.dispose();
    }

    public abstract String validateInput();
}
