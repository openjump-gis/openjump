
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

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


/**
 * Filters out middle- and right-clicks.
 */
public class LeftClickFilter implements CursorTool {
    private CursorTool wrappee;

    public LeftClickFilter(CursorTool wrappee) {
        this.wrappee = wrappee;
    }
    
    public CursorTool getWrappee() {
        return wrappee;
    }

    public Icon getIcon() {
        return wrappee.getIcon();
    }

    public String getName() {
        return wrappee.getName();
    }

    public Cursor getCursor() {
        return wrappee.getCursor();
    }

    public void activate(LayerViewPanel panel) {
        wrappee.activate(panel);
    }

    public void deactivate() {
        wrappee.deactivate();
    }

    public void mouseClicked(MouseEvent e) {
        if (isOnlyLeftMouseButton(e) || isRightMouseButtonUsed()) {
            wrappee.mouseClicked(e);
        }
    }

    public void mousePressed(MouseEvent e) {
        if (isOnlyLeftMouseButton(e) || isRightMouseButtonUsed()) {
            wrappee.mousePressed(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (isOnlyLeftMouseButton(e) || isRightMouseButtonUsed()) {
            wrappee.mouseReleased(e);
        }
    }

    public void mouseEntered(MouseEvent e) {
        wrappee.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e) {
        wrappee.mouseExited(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (isOnlyLeftMouseButton(e) || isRightMouseButtonUsed()) {
            wrappee.mouseDragged(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        wrappee.mouseMoved(e);
    }

    public boolean isRightMouseButtonUsed() {
      if (wrappee instanceof AbstractCursorTool) {
        return ((AbstractCursorTool) wrappee)
            .isRightMouseButtonUsed();
      }
      return false;
    }

    public boolean isGestureInProgress() {
        return wrappee.isGestureInProgress();
    }

    public void cancelGesture() {
        wrappee.cancelGesture();
    }

    private boolean isOnlyLeftMouseButton(MouseEvent e) {
        //A future CursorTool may check whether *both* buttons are pressed (to
        //indicate that the interaction should be cancelled). [Jon Aquino]
        return SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger();
    }

    @Override
    public String toString() {
      return super.toString()+"/"+wrappee.toString();
    }

}
