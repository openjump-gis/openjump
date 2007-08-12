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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.vividsolutions.jump.workbench.plugin.EnableCheck;

/**
 * Extends JToolBar to create an {@link JToolBar} with
 * certain buttons enabled (for saving state).
 */

public class EnableableToolBar extends JToolBar {
    protected HashMap buttonToEnableCheckMap = new HashMap();

    public EnableCheck getEnableCheck(AbstractButton button) {
        return (EnableCheck) buttonToEnableCheckMap.get(button);
    }

    public void setEnableCheck(AbstractButton button, EnableCheck check) {
        buttonToEnableCheckMap.put(button, check);
    }

    public EnableableToolBar() {
    }

    public void updateEnabledState() {
        for (Iterator i = buttonToEnableCheckMap.keySet().iterator();
            i.hasNext();
            ) {
            JComponent component = (JComponent) i.next();
            EnableCheck enableCheck =
                (EnableCheck) buttonToEnableCheckMap.get(component);
            component.setEnabled(enableCheck.check(component) == null);
        }
        //Strange -- occasionally I've seen the depressed cursor tool enabled and
        //all the other tools disabled. Maybe it's a bug in Java 1.3? [Jon Aquino]
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
        if (enableCheck != null) {
            buttonToEnableCheckMap.put(button, enableCheck);
        }
        button.setIcon(icon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(tooltip);
        button.addActionListener(actionListener);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnabledState();
            }
        });
        add(button);
    }

}
