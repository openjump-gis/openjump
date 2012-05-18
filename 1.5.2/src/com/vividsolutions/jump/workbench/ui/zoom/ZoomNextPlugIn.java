
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

package com.vividsolutions.jump.workbench.ui.zoom;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;


public class ZoomNextPlugIn extends AbstractPlugIn {
    public ZoomNextPlugIn() {
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        Viewport viewport = context.getLayerViewPanel().getViewport();
        Assert.isTrue(viewport.getZoomHistory().hasNext());
        viewport.getZoomHistory().setAdding(false);

        try {
            viewport.zoom(viewport.getZoomHistory().next());
        } finally {
            viewport.getZoomHistory().setAdding(true);
        }

        return true;
    }

    public MultiEnableCheck createEnableCheck(
        final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                                     .add(new EnableCheck() {
                public String check(JComponent component) {
                    LayerViewPanel layerViewPanel = workbenchContext.getLayerViewPanel();
                    return ( layerViewPanel == null || //[UT] 20.10.2005 not quite the error mesg
                                    !layerViewPanel.getViewport()
                                             .getZoomHistory().hasNext())
                    ? I18N.get("ui.zoom.ZoomNextPlugIn.already-at-end") : null;
                }
            });
    }

    public ImageIcon getIcon() {
        //return IconLoaderFamFam.icon("application_side_expand.png");
        return IconLoader.icon("Right.gif");
    }
}
