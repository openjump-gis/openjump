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

package com.vividsolutions.jump.workbench.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

//JDK 1.1

/** Listens to a component; if the component is a container,
 *  recursively listens to all children.  If components are added or
 *  removed, the listener is added or removed.
 *
 *  Usage:
 *  <br><br>
 *  <code>
 *      new RecursiveListener(aComponent);
 *  </code>
 *
 *  <p>
 *  Uses:
 *  <ul>
 *    <li> add a KeyListener to all components in an application to
 *    watch for global function keys </li>
 *    <li> add a FocusListener to all components in a scrollable panel
 *    to allow auto scrolling on tab </li>
 * </ul>
 * </p>
 *
 * <p>
 *    Subclass and implement addListenerTo() and removeListenerFrom().
 * </p>
 *
 * @author DeGroof, Steve. "Java Files."
 *
 * <p> 
 *   Available from http://www.mindspring.com/~degroof/java/index.html.
 *   Internet; accessed 8 January 2003.
 * <br>
 *   From the website: "The source code provided here should be considered example
 * code. That is, you can use or modify it without permission. On the other hand,
 * you're using the code at your own risk."
 * </p>
 *
 */

public abstract class RecursiveListener implements ContainerListener {
    public RecursiveListener(Component component) {
        listenTo(component);
    }

    //if a component is removed, stop listening
    public void componentRemoved(ContainerEvent evt) {
        Component comp = evt.getChild();
        ignore(comp);
    }

    //if a component is added, listen to it
    public void componentAdded(ContainerEvent evt) {
        Component comp = evt.getChild();
        listenTo(comp);
    }

    //add a listener to a component and all its children
    public void listenTo(Component comp) {
        addListenerTo(comp);

        if (comp instanceof Container) {
            Container container = (Container) comp;
            container.addContainerListener(this);

            Component[] components = container.getComponents();

            for (int i = 0; i < container.getComponentCount(); i++) {
                listenTo(components[i]);
            }
        }
    }

    //remove the listener for a component and all its children
    public void ignore(Component comp) {
        removeListenerFrom(comp);

        if (comp instanceof Container) {
            Container container = (Container) comp;
            container.removeContainerListener(this);

            Component[] components = container.getComponents();

            for (int i = 0; i < container.getComponentCount(); i++) {
                ignore(components[i]);
            }
        }
    }

    //add this as a listener (e.g. a FocusListener) to the component
    public abstract void addListenerTo(Component comp);

    //remove this as a listener (e.g. a FocusListener) from the component
    public abstract void removeListenerFrom(Component comp);
}
