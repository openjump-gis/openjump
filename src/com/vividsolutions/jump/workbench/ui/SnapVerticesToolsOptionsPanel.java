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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.SnapVerticesOp;

public class SnapVerticesToolsOptionsPanel extends JPanel implements OptionsPanel {
    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel jPanel1 = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JCheckBox insertVerticesCheckBox = new JCheckBox();
    private JPanel jPanel2 = new JPanel();
    private Blackboard blackboard;       

    public SnapVerticesToolsOptionsPanel(Blackboard blackboard) {
        this.blackboard = blackboard;

        try {
            jbInit();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
        }
    }

    public String validateInput() {
        return null;
    }

    public void okPressed() {
        blackboard.put(SnapVerticesOp.INSERT_VERTICES_IF_NECESSARY_KEY, insertVerticesCheckBox.isSelected());
    }

    public void init() {
        insertVerticesCheckBox.setSelected(
            blackboard.get(SnapVerticesOp.INSERT_VERTICES_IF_NECESSARY_KEY, true));
    }

    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        jPanel1.setLayout(gridBagLayout1);
        insertVerticesCheckBox.setText(I18N.get("ui.SnapVerticeToolsOptionsPanel.insert-vertex-if-none-in-segment"));
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(
        insertVerticesCheckBox,
             new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
        jPanel1.add(
            jPanel2,
            new GridBagConstraints(
                1,
                1,
                1,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));
    }
}
