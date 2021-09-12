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
import java.awt.event.MouseEvent;


/**
 * Permits only one CursorTool to be active at a time. As long as one CursorTool
 * has a shape on the screen, all events are directed to that CursorTool alone.
 * @see AndCompositeTool
 */
public class OrCompositeTool extends CompositeTool {

    public OrCompositeTool() {
        this(new CursorTool[] {  });
    }

    public OrCompositeTool(CursorTool[] cursorTools) {
        super(cursorTools);
    }

    private CursorTool currentTool() {
        for (CursorTool cursorTool : cursorTools) {
            if (cursorTool.isGestureInProgress()) {
                return cursorTool;
            }
        }
        return null;
    }

    public Cursor getCursor() {
        if (currentTool() == null) {
            return firstCursorTool().getCursor();
        }
        return super.getCursor();
    }

    /**
     * Clears the on-screen shapes of all tools other than the current tool.
     * Called in case some other tools have something drawn on the screen
     * (such as the SnapIndicatorTool).
     */
    private void clearOtherTools() {
        for (CursorTool cursorTool : cursorTools) {
            if (cursorTool != currentTool()) {
                cursorTool.cancelGesture();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mouseClicked(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseClicked(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mousePressed(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mousePressed(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public String getName() {
        return getName("|");
    }

    public void mouseReleased(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mouseReleased(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseReleased(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mouseEntered(e);
            clearOtherTools();

            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseEntered(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mouseExited(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseExited(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (currentTool() != null) {
            currentTool().mouseDragged(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseDragged(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        CursorTool currentTool = currentTool();
        if (currentTool != null) {
            currentTool.mouseMoved(e);
            clearOtherTools();
            return;
        }

        for (CursorTool cursorTool : cursorTools) {
            cursorTool.mouseMoved(e);
            if (currentTool() != null) {
                clearOtherTools();
                return;
            }
        }
    }
}
