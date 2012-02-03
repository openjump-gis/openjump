/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.swing.listener;

import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.vividsolutions.jump.workbench.ui.plugin.MenuItemShownListener;

public class MenuItemShownMenuListener implements MenuListener {
  private MenuItemShownListener menuItemShownListener;

  private JMenuItem menuItem;

  public MenuItemShownMenuListener(JMenuItem menuItem,
    MenuItemShownListener menuItemShownListener) {
    this.menuItemShownListener = menuItemShownListener;
    this.menuItem = menuItem;
  }

  public void menuSelected(final MenuEvent e) {
    menuItemShownListener.menuItemShown(menuItem);
  }

  public void menuDeselected(final MenuEvent e) {
  }

  public void menuCanceled(final MenuEvent e) {
  }
}
