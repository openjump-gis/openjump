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

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;


public class AddNewCategoryPlugIn extends AbstractPlugIn {
    private final static String NEW_CATEGORY_NAME = I18N.get("ui.plugin.AbstractNewCategoryPlugIn.new-category");

    public AddNewCategoryPlugIn() {
    }

    public boolean execute(final PlugInContext context)
        throws Exception {
        final String categoryName = findNewCategoryName(context.getLayerManager());
        execute(new UndoableCommand(getName()) {
                public void execute() {
                    context.getLayerManager().addCategory(categoryName);
                }

                public void unexecute() {
                    Assert.isTrue(context.getLayerManager()
                                         .getCategory(categoryName).isEmpty(),
                       I18N.get("ui.plugin.AbstractNewCategoryPlugIn.this-can-happen-when-a-plug-in-calls"));
                    context.getLayerManager().removeIfEmpty(context.getLayerManager()
                                                                   .getCategory(categoryName));
                }
            }, context);

        return true;
    }

    private String findNewCategoryName(LayerManager layerManager) {
        if (layerManager.getCategory(NEW_CATEGORY_NAME) == null) {
            return NEW_CATEGORY_NAME;
        }

        int i = 2;
        String newName;

        do {
            newName = NEW_CATEGORY_NAME + " (" + i + ")";
            i++;
        } while (layerManager.getCategory(newName) != null);

        return newName;
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
    }
}
