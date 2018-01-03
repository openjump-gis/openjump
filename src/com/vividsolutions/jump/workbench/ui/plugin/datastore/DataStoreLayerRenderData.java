/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jump.datastore.DataStoreLayer;
import java.awt.Color;

import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import org.netbeans.swing.outline.RenderDataProvider;

import javax.swing.*;

public class DataStoreLayerRenderData implements RenderDataProvider {

    // icons to display according to node geometry type
    private ImageIcon gc = IconLoader.icon("EditGeometryCollection.gif");
    private ImageIcon point = IconLoader.icon("EditPoint.gif");
    private ImageIcon mpoint = IconLoader.icon("EditMultiPoint.gif");
    private ImageIcon line = IconLoader.icon("EditLineString.gif");
    private ImageIcon mline = IconLoader.icon("EditMultiLineString.gif");
    private ImageIcon poly = IconLoader.icon("EditPolygon.gif");
    private ImageIcon mpoly = IconLoader.icon("EditMultiPolygon.gif");
    private ImageIcon lring = IconLoader.icon("EditLinearRing.gif");

    @Override
    public java.awt.Color getBackground(Object o) {
        if (!(o instanceof String)) {
            return new Color(230,230,230);
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName(Object o) {
        if (o instanceof String) {
            return ((String) o);
        } else {
            return ((DataStoreLayer) o).getName();
        }
    }

    @Override
    public java.awt.Color getForeground(Object o) {
        if (o instanceof String) {
//            return UIManager.getColor("controlShadow");
            return null;
        } else {
            return null;
        }
    }

    @Override
    public javax.swing.Icon getIcon(Object o) {
        // Nicolas Ribot: 03 jan 2018: adds custom icons according to layer geometry type
        if (o instanceof DataStoreLayer) {
            switch (((DataStoreLayer) o).getGeoCol().getType().toLowerCase()) {
                case "geometrycollection":
                case "geometry":
                case "sdo_geometry":
                    return this.gc;
                case "point":
                    return this.point;
                case "multipoint":
                    return this.mpoint;
                case "linestring":
                    return this.line;
                case "multilinestring":
                    return this.mline;
                case "polygon":
                    return this.poly;
                case "multipolygon":
                    return this.mpoly;
                case "linearring":
                    return this.lring;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String getTooltipText(Object o) {
        if (o instanceof String) {
            return "Schema: " + o;
        } else {
            return ((DataStoreLayer) o).getFullName() + " " + ((DataStoreLayer) o).getGeoCol();
        }
    }

    @Override
    public boolean isHtmlDisplayName(Object o) {
        return false;
    }

}
