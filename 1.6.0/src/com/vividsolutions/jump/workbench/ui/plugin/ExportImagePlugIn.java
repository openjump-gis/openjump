package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public abstract class ExportImagePlugIn extends AbstractPlugIn {

    // TODO : remove before final OpenJUMP 1.6 release
    /** @deprecated */
    protected static boolean java14OrNewer() {
        String version = System.getProperty("java.version");
        if (version.indexOf("1.0") == 0) {
            return false;
        }
        if (version.indexOf("1.1") == 0) {
            return false;
        }
        if (version.indexOf("1.2") == 0) {
            return false;
        }
        if (version.indexOf("1.3") == 0) {
            return false;
        }
        //Allow 1.4, 1.5, 1.6, 2.0, etc. [Jon Aquino]
        return true;
    }

    protected BufferedImage image(LayerViewPanel layerViewPanel) {
        //Don't use TYPE_INT_ARGB, which makes JPEGs pinkish (presumably because
        //JPEGs don't support transparency [Jon Aquino 11/6/2003]
        BufferedImage image = new BufferedImage(layerViewPanel.getWidth(),
                layerViewPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
        layerViewPanel.paintComponent((Graphics2D) image.getGraphics());
        return image;
    }

}