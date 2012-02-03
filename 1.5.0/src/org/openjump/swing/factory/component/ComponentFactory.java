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
package org.openjump.swing.factory.component;

import java.awt.Component;

import javax.swing.Icon;

/**
 * The ComponentFactory is used to create instances of the {@link Component}
 * type T.
 * 
 * @author Paul Austin
 * @param The type of component created.
 */
public interface ComponentFactory<T extends Component> {
  /**
   * Create an instance of the component.
   * 
   * @return The new component instance.
   */
  T createComponent();

  /**
   * Get the display name of the component. Used in the UI as a Frame or menu
   * title.
   * 
   * @return The name.
   */
  String getName();

  /**
   * Get the icon for the component.
   * 
   * @return The icon.
   */
  Icon getIcon();

  /**
   * Get the tool-tip for the component.
   * 
   * @return The tool-tip.
   */
  String getToolTip();
}
