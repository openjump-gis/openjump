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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;

import org.locationtech.jts.geom.Geometry;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GeometryEditor;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

//Say "delete" for features but "remove" for layers; otherwise, "delete layers" may
//sound to a user that we're actually deleting the file from the disk. [Jon Aquino]
public class DeleteSelectedItemsPlugIn extends AbstractPlugIn {

  public static ImageIcon ICON = IconLoader.icon("famfam/cross.png");

  public DeleteSelectedItemsPlugIn() {
    this.setShortcutKeys(KeyEvent.VK_DELETE);
  }

  private GeometryEditor geometryEditor = new GeometryEditor();

  public boolean execute(final PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    ArrayList transactions = new ArrayList();
    final SelectionManager selectionManager = ((SelectionManagerProxy) context
        .getActiveInternalFrame()).getSelectionManager();

    for (final Layer layer : selectionManager.getLayersWithSelectedItems()) {
      transactions.add(EditTransaction.createTransactionOnSelection(
          new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems,
                Collection selectedItems) {
              Geometry g = geometryWithSelectedItems;
              for (Iterator i = selectedItems.iterator(); i.hasNext();) {
                Geometry selectedItem = (Geometry) i.next();
                g = geometryEditor.remove(g, selectedItem);
              }
              return g;
            }
          }, ((SelectionManagerProxy) context.getActiveInternalFrame()),
          context.getWorkbenchFrame(), getName(), layer,
          isRollingBackInvalidEdits(context), true));
    }
    return EditTransaction.commit(transactions);
  }

  public static MultiEnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))
        .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
  }

  public ImageIcon getIcon() {
    return ICON;
  }
}
