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
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.undo.UndoManager;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
public class RedoPlugIn extends AbstractPlugIn {
    public RedoPlugIn() {}
    public void initialize(PlugInContext context) throws Exception {}
    public boolean execute(PlugInContext context) throws Exception {
        ((LayerManagerProxy) context.getWorkbenchContext()
                        .getWorkbench()
                        .getFrame()
                        .getActiveInternalFrame())
                        .getLayerManager().getUndoableEditReceiver().getUndoManager().redo();
        //Exclude the plug-in's activity from the undo history [Jon Aquino]
        reportNothingToUndoYet(context);        
        context.getWorkbenchFrame().getToolBar().updateEnabledState();
        return true;
    }
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerManagerMustBeActiveCheck()).add(new EnableCheck() {
            public String check(JComponent component) {
                UndoManager undoManager =
                    ((LayerManagerProxy) workbenchContext
                        .getWorkbench()
                        .getFrame()
                        .getActiveInternalFrame())
                        .getLayerManager()
                        .getUndoableEditReceiver()
                        .getUndoManager();
                component.setToolTipText(undoManager.getRedoPresentationName());
                return (!undoManager.canRedo()) ? "X" : null;
            }
        });
    }
    public ImageIcon getIcon() {
        return IconLoader.icon("Redo.gif");
    }
}
