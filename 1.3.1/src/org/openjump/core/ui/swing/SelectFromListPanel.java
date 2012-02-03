//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.openjump.core.ui.swing;

import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BoxLayout.Y_AXIS;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * <code>SelectFromListPanel</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class SelectFromListPanel extends JPanel {

    private static final long serialVersionUID = -7630339015307656617L;

    private String description;

    /**
     * 
     */
    public JList list;

    /**
     * @param description
     * 
     */
    public SelectFromListPanel(String description) {
        this.description = description;
        GridBagConstraints gb = initPanel(this);

        list = new JList();
        list.setSelectionMode(SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        gb.fill = BOTH;
        add(addWithSize(sp, 300, 300), gb);
    }

    static JPanel addWithSize(Component c, int width, int height) {
        Dimension d = c.getPreferredSize();
        if (width <= 0) {
            width = d.width;
        }
        if (height <= 0) {
            height = d.height;
        }

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, Y_AXIS));
        p.add(c);
        c.setPreferredSize(new Dimension(width, height));
        return p;
    }

    static GridBagConstraints initPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());
        panel.setBorder(createEmptyBorder(2, 2, 2, 2));
        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0;
        gb.gridy = 0;
        gb.insets = new Insets(2, 2, 2, 2);
        return gb;
    }

    @Override
    public String toString() {
        return description;
    }

}
