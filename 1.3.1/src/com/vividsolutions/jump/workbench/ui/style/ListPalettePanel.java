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
package com.vividsolutions.jump.workbench.ui.style;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyleListCellRenderer;

public class ListPalettePanel extends AbstractPalettePanel {

    private BasicStyleListCellRenderer basicStyleListCellRenderer =
        new BasicStyleListCellRenderer() ;

    /**
     * @param verticalScrollBarPolicy unfortunately needs to be set because
     * there seems to be a JScrollPane bug with VERTICAL_SCROLLBAR_AS_NEEDED:
     * an ugly strip of whitespace is left where the scrollbar would be (J2SE 1.4.1)
     */
    public ListPalettePanel(int verticalScrollBarPolicy) {
        //I've submitted a bug report to Sun (internal review ID 185722). [Jon Aquino]
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(verticalScrollBarPolicy);
        final JList list = new JList(new Vector(basicStyles()));
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(list);
        list.setCellRenderer(basicStyleListCellRenderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list
            .getSelectionModel()
            .addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fireBasicStyleChosen((BasicStyle) list.getSelectedValue());
            }
        });
    }

    public void setAlpha(int alpha) {
        basicStyleListCellRenderer.setAlpha(alpha);
        repaint();
    }

    public DefaultListCellRenderer testRenderer =
        new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            return super.getListCellRendererComponent(
                list,
                "test",
                index,
                isSelected,
                cellHasFocus);
        }
    };

}
