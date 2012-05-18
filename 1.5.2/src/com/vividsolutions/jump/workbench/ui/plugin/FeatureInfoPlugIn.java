
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

package com.vividsolutions.jump.workbench.ui.plugin;

import java.util.Iterator;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.InfoFrame;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.TaskFrameProxy;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class FeatureInfoPlugIn extends AbstractPlugIn {
    public FeatureInfoPlugIn() {}

    public static final ImageIcon ICON = IconLoader.icon("information_16x16.png");

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())            
            .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())                        
            .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        //Don't pass in TaskFrame as LayerManagerProxy, because the TaskFrame may
        //be closed and thus the LayerManagerProxy may return null. [Jon Aquino]
        InfoFrame infoFrame =
            new InfoFrame(
                context.getWorkbenchContext(),
                (LayerManagerProxy) context.getActiveInternalFrame(),
                ((TaskFrameProxy) context.getActiveInternalFrame()).getTaskFrame());
        infoFrame.setSize(500, 300);

        for (Iterator i = context.getLayerManager().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();

            if (((SelectionManagerProxy) context.getActiveInternalFrame())
                .getSelectionManager()
                .getFeaturesWithSelectedItems(layer)
                .isEmpty()) {
                continue;
            }

            infoFrame.getModel().add(
                layer,
                ((SelectionManagerProxy) context.getActiveInternalFrame()).getSelectionManager().getFeaturesWithSelectedItems(
                    layer));
        }

        infoFrame.setSelectedTab(infoFrame.getGeometryTab());
        context.getWorkbenchFrame().addInternalFrame(infoFrame);

        return true;
    }
    
    

}
