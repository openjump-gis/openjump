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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jump.workbench.plugin.EnableCheck;

/**
 * Extends JToolBar to create an {@link JToolBar} with
 * certain buttons enabled (for saving state).
 */

public class EnableableToolBar extends JToolBar {
    protected HashMap buttonToEnableCheckMap = new HashMap();
    protected HashMap buttonToNameMap = new HashMap();
    private InvokeMethodActionListener updateStateListener = new InvokeMethodActionListener(this, "updateEnabledState");

    public EnableCheck getEnableCheck(AbstractButton button) {
        return (EnableCheck) buttonToEnableCheckMap.get(button);
    }

    public void setEnableCheck(AbstractButton button, EnableCheck check) {
        buttonToEnableCheckMap.put(button, check);
    }

    public EnableableToolBar() {
      // OJ is not prepared to handle detached toolbars properly for now, so
      // disable this
      // Note: this class is the parent of _every_ toolbar in OJ , even the ones
      // in AttribTab or SchemaEditor
      setFloatable(false);
    }

    public void updateEnabledState() {
        for (Iterator i = buttonToEnableCheckMap.keySet().iterator();
            i.hasNext();
            ) {
            JComponent component = (JComponent) i.next();
            EnableCheck enableCheck =
                (EnableCheck) buttonToEnableCheckMap.get(component);
            String name = (String) buttonToNameMap.get(component);
            
            String check = enableCheck.check(component);
            if (check!=null){
              component.setEnabled(false);
              component.setToolTipText(name +" - "+ check);
            } else{
              component.setEnabled(true);
              component.setToolTipText(name);
            }
        }
    }

    /**
     * Unlike #addSeparator, works for vertical toolbars.
     */
    public void addSpacer() {
        JPanel filler = new JPanel();
        filler.setPreferredSize(new Dimension(5, 5));
        filler.setMinimumSize(new Dimension(5, 5));
        filler.setMaximumSize(new Dimension(5, 5));
        add(filler);
    }

    public void add(
        AbstractButton button,
        String tooltip,
        Icon icon,
        ActionListener actionListener,
        EnableCheck enableCheck) {
        add(-1, button, tooltip, icon, actionListener, enableCheck);
    }

    public void add(final int index, final AbstractButton button,
      final String tooltip, final Icon icon,
      final ActionListener actionListener, final EnableCheck enableCheck) {
        if (enableCheck != null) {
            buttonToEnableCheckMap.put(button, enableCheck);
        }
        buttonToNameMap.put(button, tooltip);
        button.setIcon(icon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(tooltip);
        button.addActionListener(actionListener);
        button.addActionListener(updateStateListener);
        add(button, index);
    }
}
