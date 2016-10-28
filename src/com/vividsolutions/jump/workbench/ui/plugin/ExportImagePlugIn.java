package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.image.BufferedImage;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public abstract class ExportImagePlugIn extends AbstractPlugIn {

    protected BufferedImage image(LayerViewPanel layerViewPanel) {
        //Don't use TYPE_INT_ARGB, which makes JPEGs pinkish (presumably because
        //JPEGs don't support transparency [Jon Aquino 11/6/2003]
        BufferedImage image = new BufferedImage(layerViewPanel.getWidth(),
                layerViewPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
        layerViewPanel.paintComponent(image.getGraphics());
        return image;
    }

}