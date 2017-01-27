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
package com.vividsolutions.jump.workbench.ui.plugin.clipboard;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;

import org.openjump.core.ui.images.IconLoader;

public class CopySelectedItemsPlugIn extends AbstractPlugIn {
  // Note: Need to copy the data twice: once when the user hits Copy, so she is
  // free to modify the original afterwards, and again when the user hits Paste,
  // so she is free to modify the first copy then hit Paste again. [Jon Aquino]
  public CopySelectedItemsPlugIn() {
    this.setShortcutKeys(KeyEvent.VK_C);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public static ImageIcon ICON = IconLoader.icon("items_copy.png");

  public String getNameWithMnemonic() {
    return StringUtil.replace(getName(), "C", "&C", false);
  }

  public boolean execute(PlugInContext context) throws Exception {
    Toolkit
        .getDefaultToolkit()
        .getSystemClipboard()
        .setContents(
            new CollectionOfFeaturesTransferable(
                ((SelectionManagerProxy) context.getActiveInternalFrame())
                    .getSelectionManager().createFeaturesFromSelectedItems()),
            new DummyClipboardOwner());
    return true;
  }

  public static MultiEnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck().add(
        checkFactory.createWindowWithSelectionManagerMustBeActiveCheck()).add(
        checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
  }

  public ImageIcon getIcon() {
    return ICON;
  }

}
