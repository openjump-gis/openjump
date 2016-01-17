/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jump.datastore.DataStoreLayer;
import java.awt.Color;
import javax.swing.UIManager;
import org.netbeans.swing.outline.RenderDataProvider;

public class DataStoreLayerRenderData implements RenderDataProvider {

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
        return null;

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
