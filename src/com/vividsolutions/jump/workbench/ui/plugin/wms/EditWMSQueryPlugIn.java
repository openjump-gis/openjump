/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.awt.Dimension;
import java.util.Iterator;

import javax.swing.JLabel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;


public class EditWMSQueryPlugIn extends AbstractPlugIn {
    public MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                                     .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                1, Layerable.class));
    }

    public boolean execute(PlugInContext context) throws Exception {
        WMSLayer layer = (WMSLayer) context.getLayerNamePanel()
                                           .selectedNodes(WMSLayer.class)
                                           .iterator().next();
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(),
        		I18N.get("ui.plugin.wms.EditWMSQueryPlugIn.edit-wms-query"), true);
        dialog.setInset(0);
        dialog.setSideBarImage(IconLoader.icon("EditWMSLayer.jpg"));
        dialog.setSideBarDescription(
        		I18N.get("ui.plugin.wms.EditWMSQueryPlugIn.this-dialog-enables-you-to-change-the-layers-being-retrieved-from-a-web-map-server"));

        EditWMSQueryPanel panel = new EditWMSQueryPanel(layer.getService(),
                layer.getLayerNames(), layer.getSRS(), layer.getAlpha(), layer.getFormat());
        panel.setPreferredSize(new Dimension(600, 450));

        //The field name "Chosen Layers" will appear on validation error messages
        //e.g. if the user doesn't pick any layers. [Jon Aquino]
        dialog.addRow(I18N.get("ui.plugin.wms.EditWMSQueryPlugIn.chosen-layers"), new JLabel(""), panel, panel.getEnableChecks(), null);
        dialog.setVisible(true);

        if (dialog.wasOKPressed()) {
            layer.removeAllLayerNames();

            for (Iterator i = panel.getChosenMapLayers().iterator();
                    i.hasNext();) {
                MapLayer mapLayer = (MapLayer) i.next();
                layer.addLayerName(mapLayer.getName());
            }

            layer.setSRS(panel.getSRS());
            layer.setAlpha(panel.getAlpha());
            layer.setFormat(panel.getFormat());
            layer.fireAppearanceChanged();

            return true;
        }

        return false;
    }
}
