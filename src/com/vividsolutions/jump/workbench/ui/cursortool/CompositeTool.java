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

package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


public abstract class CompositeTool implements CursorTool {

    protected List<CursorTool> cursorTools;

    public CompositeTool(CursorTool[] cursorTools) {
        this.cursorTools = new ArrayList<>(Arrays.asList(cursorTools));
    }

    public void deactivate() {
        for (CursorTool cursorTool : cursorTools) {
            cursorTool.deactivate();
        }
    }

    public Cursor getCursor() {
        for (CursorTool cursorTool : cursorTools) {

            //Prefer cursors other than the default cursor. [Jon Aquino]
            if (cursorTool.getCursor() != Cursor.getDefaultCursor()) {
                return cursorTool.getCursor();
            }
        }

        return Cursor.getDefaultCursor();
    }

    public boolean isRightMouseButtonUsed() {
        for (CursorTool cursorTool : cursorTools) {
            if (cursorTool.isRightMouseButtonUsed()) {
                return true;
            }
        }

        return false;
    }

    protected CursorTool firstCursorTool() {
        return cursorTools.get(0);
    }

    private LayerViewPanel panel = null;
    

    public void activate(LayerViewPanel layerViewPanel) {
        this.panel = layerViewPanel;
        for (CursorTool cursorTool : cursorTools) {
            cursorTool.activate(layerViewPanel);
        }
    }

    public CompositeTool add(CursorTool tool) {
        cursorTools.add(tool);
        return this;
    }

    public void cancelGesture() {
        for (CursorTool cursorTool : cursorTools) {
            cursorTool.cancelGesture();
        }
    }

    public Icon getIcon() {
        for (CursorTool cursorTool : cursorTools) {
            if (cursorTool.getIcon() != null) {
                return cursorTool.getIcon();
            }
        }
        return null;
    }

    protected String getName(String delimiter) {
        StringBuilder name = new StringBuilder();

        for (int i = 0; i < cursorTools.size(); i++) {
            if (i > 0) {
                name.append(" ").append(delimiter).append(" ");
            }
            name.append(cursorTools.get(i).getName());
        }

        return name.toString();
    }

    public boolean isGestureInProgress() {
        for (CursorTool cursorTool : cursorTools) {
            if (cursorTool.isGestureInProgress()) {
                return true;
            }
        }
        return false;
    }

    public LayerViewPanel getPanel() {
        return panel;
    }

}
