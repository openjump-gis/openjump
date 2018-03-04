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

package com.vividsolutions.jump.workbench.ui.cursortool.editing;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DrawRectangleFenceTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class SnapVerticesToSelectedVertexTool extends QuasimodeTool {
    private static final Cursor SHIFT_DOWN_CURSOR =
        AbstractCursorTool.createCursor(
            IconLoader.icon("SnapVerticesTogetherCursor3.gif").getImage());
    private static final Cursor SHIFT_NOT_DOWN_CURSOR =
        AbstractCursorTool.createCursor(
            IconLoader.icon("SnapVerticesTogetherCursor4.gif").getImage());
    private static final String NAME = I18N
        .get("com.vividsolutions.jump.workbench.ui.cursortool.editing.SnapVerticesToSelectedVertexTool");

    public String getName() {
        return NAME;
    }

    public SnapVerticesToSelectedVertexTool(EnableCheckFactory checkFactory) {
      super(new DrawRectangleFenceTool() {
        public String getName() {
          return NAME;
        }
  
        public void mouseClicked(final MouseEvent e) {
          if (!check(new EnableCheck() {
            public String check(JComponent component) {
              return (!e.isShiftDown()) ? I18N
                  .get("ui.cursortool.editing.SnapVerticesToSelectedVertexTool.shift-click-the-vertex-to-snap-to")
                  : null;
            }
          })) {
            return;
          }
          super.mouseClicked(e);
        }
  
        public Cursor getCursor() {
          return SHIFT_NOT_DOWN_CURSOR;
        }
      });
      add(new ModifierKeySpec(false, true, false),
          new SnapVerticesToSelectedVertexClickTool(checkFactory) {
            public Cursor getCursor() {
              return SHIFT_DOWN_CURSOR;
            }
  
            public String getName() {
              return NAME;
            }
          });
    }

    public Icon getIcon() {
        return IconLoader.icon("SnapVerticesTogether.gif");
    }

}
