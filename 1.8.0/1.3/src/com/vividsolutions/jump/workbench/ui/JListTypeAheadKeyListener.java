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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;

import javax.swing.JList;


/**
 * When the user types into a JList, scrolls the JList to that item.
 */
public class JListTypeAheadKeyListener extends KeyAdapter {
    private JList list;
    private String buffer = "";
    private Date bufferUpdateTime = new Date();
    private int bufferLifetimeInMilliseconds = 1000;

    public JListTypeAheadKeyListener(JList list) {
        this.list = list;
    }

    public void keyTyped(KeyEvent e) {
        updateBuffer(e.getKeyChar());

        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (list.getModel().getElementAt(i).toString().toUpperCase()
                        .indexOf(buffer.toUpperCase()) == 0) {
                list.setSelectedValue(list.getModel().getElementAt(i), true);

                break;
            }
        }
    }

    private void updateBuffer(char c) {
        Date newBufferUpdateTime = new Date();

        if ((newBufferUpdateTime.getTime() - bufferUpdateTime.getTime()) > bufferLifetimeInMilliseconds) {
            buffer = "";
        }

        bufferUpdateTime = newBufferUpdateTime;

        if (c != KeyEvent.CHAR_UNDEFINED) {
            buffer += c;
        }
    }
}
