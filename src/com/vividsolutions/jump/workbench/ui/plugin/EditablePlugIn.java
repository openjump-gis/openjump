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

import java.util.Arrays;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class EditablePlugIn extends AbstractPlugIn {

    private EditingPlugIn editingPlugIn;

    public static final ImageIcon ICON = IconLoader.icon("edit.gif");

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        boolean makeEditable = !context.getSelectedLayer(0).isEditable();
        for (Iterator i = Arrays.asList(context.getSelectedLayers()).iterator(); i.hasNext(); ) {
            Layer selectedLayer = (Layer) i.next();
            selectedLayer.setEditable(makeEditable);
        }
        if (makeEditable && !editingPlugIn.getToolbox(context.getWorkbenchContext()).isVisible()) {
            editingPlugIn.execute(context);
        }
        return true;
    }

    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck mec = new MultiEnableCheck();

        mec.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck());
        mec.add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));

        mec.add(new EnableCheck() {
            public String check(JComponent component) {
                ((JCheckBoxMenuItem) component).setSelected(
                    workbenchContext.createPlugInContext().getSelectedLayer(0).isEditable());
                return null;
            }
        });

        mec.add(new EnableCheck() {
            public String check(JComponent component) {
                String errMsg = null;
                if ( workbenchContext.createPlugInContext().getSelectedLayer(0).isReadonly()) {
                    errMsg = I18N.get("ui.plugin.EditablePlugIn.The-selected-layer-cannot-be-made-editable");
                }
                return errMsg;
            }
        });

        return mec;
    }

    public EditablePlugIn(EditingPlugIn editingPlugIn) {
        this.editingPlugIn = editingPlugIn;
    }

}
