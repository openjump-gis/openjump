
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

import java.awt.event.MouseEvent;
import java.util.Iterator;


/**
 * Enables multiple CursorTools to operate at the same time. Events are passed
 * to all CursorTools in sequence.
 * @see OrCompositeTool
 */
public class AndCompositeTool extends CompositeTool {
    public AndCompositeTool() {
        this(new CursorTool[] {  });
    }

    public AndCompositeTool(CursorTool[] cursorTools) {
        super(cursorTools);
    }

    public String getName() {
        return getName("&");
    }

    public void setCursorTool(CursorTool cursorTool) {
        cursorTools.clear();
        add(cursorTool);
    }

    public void mouseClicked(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseClicked(e);
        }
    }

    public void mousePressed(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mousePressed(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseReleased(e);
        }
    }

    public void mouseEntered(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseEntered(e);
        }
    }

    public void mouseExited(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseExited(e);
        }
    }

    public void mouseDragged(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseDragged(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        for (Iterator i = cursorTools.iterator(); i.hasNext();) {
            CursorTool cursorTool = (CursorTool) i.next();
            cursorTool.mouseMoved(e);
        }
    }
}
