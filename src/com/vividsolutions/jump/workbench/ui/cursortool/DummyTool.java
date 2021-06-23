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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class DummyTool implements CursorTool {
  public DummyTool() {
  }

  public Cursor getCursor() {
    return Cursor.getDefaultCursor();
  }

  public void activate(LayerViewPanel layerViewPanel) {
  }

  public void deactivate() {
  }

  public boolean isRightMouseButtonUsed() {
    return false;
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mouseDragged(MouseEvent e) {
  }

  public void mouseMoved(MouseEvent e) {
  }

  public Icon getIcon() {
    return null;
  }

  public boolean isGestureInProgress() {
    return false;
  }

  public void cancelGesture() {
  }

  public String getName() {
    return I18N.getInstance().get("ui.cursortool.DummyTool.dummy-cursor-tool");
  }
}
