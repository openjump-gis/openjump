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
import java.util.Iterator;

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


public abstract class CompositeTool implements CursorTool {
    protected ArrayList cursorTools = new ArrayList();

    public CompositeTool(CursorTool[] cursorTools) {
        this.cursorTools = new ArrayList(Arrays.asList(cursorTools));
    }

    public void deactivate() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool tool = (CursorTool) i.next();
            tool.deactivate();
        }
    }

    public Cursor getCursor() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();

            //Prefer cursors other than the default cursor. [Jon Aquino]
            if (cursorTool.getCursor() != Cursor.getDefaultCursor()) {
                return cursorTool.getCursor();
            }
        }

        return Cursor.getDefaultCursor();
    }

    public boolean isRightMouseButtonUsed() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();

            if (cursorTool.isRightMouseButtonUsed()) {
                return true;
            }
        }

        return false;
    }

    protected CursorTool firstCursorTool() {
        return (CursorTool) cursorTools.get(0);
    }

    private LayerViewPanel panel = null;
    

    public void activate(LayerViewPanel layerViewPanel) {
        this.panel = layerViewPanel;
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool tool = (CursorTool) i.next();
            tool.activate(layerViewPanel);
        }
    }

    public CompositeTool add(CursorTool tool) {
        cursorTools.add(tool);
        return this;
    }

    public void cancelGesture() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool tool = (CursorTool) i.next();
            tool.cancelGesture();
        }
    }

    public Icon getIcon() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool tool = (CursorTool) i.next();

            if (tool.getIcon() != null) {
                return tool.getIcon();
            }
        }

        return null;
    }

    protected String getName(String delimiter) {
        String name = "";

        for (int i = 0; i < cursorTools.size(); i++) {
            if (i > 0) {
                name += (" " + delimiter + " ");
            }

            name += ((CursorTool) cursorTools.get(i)).getName();
        }

        return name;
    }

    public boolean isGestureInProgress() {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool tool = (CursorTool) i.next();

            if (tool.isGestureInProgress()) {
                return true;
            }
        }

        return false;
    }
    public LayerViewPanel getPanel() {
        return panel;
    }

}
