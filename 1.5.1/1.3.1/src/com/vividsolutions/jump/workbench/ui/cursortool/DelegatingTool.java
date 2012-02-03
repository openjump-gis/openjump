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

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


/**
 * Enables the behaviour of a CursorTool instance to be overridden.
 */
public abstract class DelegatingTool implements CursorTool {
    private CursorTool delegate = new DummyTool();
    private boolean active = false;
    private LayerViewPanel layerViewPanel;

    public CursorTool getDelegate() { 
        return delegate;
    }

    public DelegatingTool(CursorTool cursorTool) {
        setDelegate(cursorTool);
    }

    public void setDelegate(CursorTool delegate) {
        if (this.delegate == delegate) {
            //Don't activate/deactivate. [Jon Aquino]
            return;
        }

//        if (active) {
//            this.delegate.deactivate();
//        }

        this.delegate = delegate;

        if (active) {
            this.delegate.activate(layerViewPanel);
        }
    }

    public String getName() {
        return delegate.getName();
    }

    public Icon getIcon() {
        return delegate.getIcon();
    }

    public boolean isGestureInProgress() {
        return delegate.isGestureInProgress();
    }

    public void cancelGesture() {
        delegate.cancelGesture();
    }

    public void mousePressed(MouseEvent e) {
        delegate.mousePressed(e);
    }

    public void mouseClicked(MouseEvent e) {
        delegate.mouseClicked(e);
    }

    public void activate(LayerViewPanel layerViewPanel) {
        this.layerViewPanel = layerViewPanel;
        delegate.activate(layerViewPanel);
        active = true;
    }

    public Cursor getCursor() {
        return delegate.getCursor();
    }

    public void deactivate() {
        delegate.deactivate();
        active = false;
    }

    public void mouseReleased(MouseEvent e) {
        delegate.mouseReleased(e);
    }

    public void mouseEntered(MouseEvent e) {
        delegate.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e) {
        delegate.mouseExited(e);
    }

    public void mouseDragged(MouseEvent e) {
        delegate.mouseDragged(e);
    }

    public void mouseMoved(MouseEvent e) {
        delegate.mouseMoved(e);
    }

    public boolean isRightMouseButtonUsed() {
        return delegate.isRightMouseButtonUsed();
    }
}
