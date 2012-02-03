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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.model.LayerManager;


/**
 * Flexible generic dialog for prompting the user to type in several values.
 * This dialog extends MultiInputDialog making it possible to create multi
 * tabbed input panels.
 */
public class MultiTabInputDialog extends MultiInputDialog {
    
    
    /**
     * @param frame the frame on which to make this dialog modal and centred
     */
    public MultiTabInputDialog(final Frame frame, String title, String firstPaneTitle, boolean modal) {
        super(frame, title, modal);
        addPane(firstPaneTitle);
    }
    
    private JTabbedPane tabbedPane;
    
    protected void setMainComponent() {
        mainComponent = new JTabbedPane();
        tabbedPane = (JTabbedPane)mainComponent;
    }
    
    public JPanel addPane(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        tabbedPane.add(title, panel);
        currentPanel = panel;
        return panel;
    }
    
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    
    public void setTabEnabled(String tabTitle, boolean enable) {
        tabbedPane.setEnabledAt(tabbedPane.indexOfTab(tabTitle), enable);
    }
    
    public static void main(String[] args) {
        MultiTabInputDialog d = new MultiTabInputDialog(null, "Title!", "1st pane", true);
        //d.addPane("1er panneau");
        d.addSubTitle("Sous-titre 1");
        d.addLabel("This is just a label");
        d.addTextField("Nom", "", 24, null, "");
        d.addPositiveIntegerField("Age", 0, 6, "");
        d.addNonNegativeDoubleField("Salaire", 0, 12, "");
        d.addComboBox("Métier", "Cadre", java.util.Arrays.asList("Cadre","Charpentier","Maçon","Boulanger"), "");
        
        d.addPane("2nd pane");
        d.addLabel("Yay!");
        d.setTabEnabled("1st pane", false);
        //d.addLayerComboBox("LayerField", null, "ToolTip", new LayerManager());
        d.setVisible(true);
        //System.out.println(d.getLayer("LayerField"));
        System.exit(0);
    }
}
