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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class MoveLayerablePlugIn extends AbstractPlugIn {
    
    public static final ImageIcon UPICON = IconLoader.icon("arrow_up.png");
    public static final ImageIcon DOWNICON = IconLoader.icon("arrow_down.png");

   public static final MoveLayerablePlugIn UP = new MoveLayerablePlugIn(-1) {
        public String getName() {
            return I18N.get("ui.plugin.MoveLayerablePlugIn.move-layer-up");
        }

        public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
            return super.createEnableCheck(workbenchContext).add(new EnableCheck() {
                public String check(JComponent component) {
                    return (index(selectedLayerable(workbenchContext.getLayerNamePanel())) == 0)
                        ? I18N.get("ui.plugin.MoveLayerablePlugIn.layer-is-already-at-the-top")
                        : null;
                }
            });
        }
    };

    public static final MoveLayerablePlugIn DOWN = new MoveLayerablePlugIn(1) {
        public String getName() {
            return I18N.get("ui.plugin.MoveLayerablePlugIn.move-layer-down");
        }

        public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
            return super.createEnableCheck(workbenchContext).add(new EnableCheck() {
                public String check(JComponent component) {
                    return (
                        index(selectedLayerable(workbenchContext.getLayerNamePanel()))
                            == (workbenchContext
                                .getLayerViewPanel()
                                .getLayerManager()
                                .getCategory(
                                    selectedLayerable(workbenchContext.getLayerNamePanel()))
                                .getLayerables()
                                .size()
                                - 1))
                        ? I18N.get("ui.plugin.MoveLayerablePlugIn.layer-is-already-at-the-bottom")
                        : null;
                }
            });
        }
    };

    private int displacement;

    private MoveLayerablePlugIn(int displacement) {
        this.displacement = displacement;
    }

    protected Layerable selectedLayerable(LayerNamePanel layerNamePanel) {
        return (Layerable) layerNamePanel.selectedNodes(Layerable.class).iterator().next();
    }

    public boolean execute(final PlugInContext context) throws Exception {
        final Layerable layerable = selectedLayerable(context.getLayerNamePanel());
        final int index = index(layerable);
        final Category category = context.getLayerManager().getCategory(layerable);
        execute(new UndoableCommand(getName()) {
            public void execute() {
                moveLayerable(index + displacement);
            }

            public void unexecute() {
                moveLayerable(index);
            }

            private void moveLayerable(int newIndex) {
                context.getLayerManager().remove(layerable);
                category.add(newIndex, layerable);
            }
        }, context);

        return true;
    }

    protected int index(Layerable layerable) {
        return layerable.getLayerManager().getCategory(layerable).indexOf(layerable);
    }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, Layerable.class));
    }
}
