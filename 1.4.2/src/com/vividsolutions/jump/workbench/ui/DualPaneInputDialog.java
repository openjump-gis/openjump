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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;


/**
 * Flexible generic dialog for prompting the user to type in several values.
 * The dialog consists of a left pane and a right pane and replace the old
 * MultiInputDialog when used with addColumn.
 */
public class DualPaneInputDialog extends MultiInputDialog {
    
    // Main components of a MultiInputDialog
    
    //|------------------------------------------------------------------------|
    //| this.contentPane (BorderLayout)                                        |
    //| |--------------------------------------------------------------------| |
    //| | CENTER dialogPanel (BorderLayout)                                  | |
    //| | |----------------------------------------------------------------| | |
    //| | | WEST:imagePanel | CENTER : mainComponent                       | | |
    //| | | |-------------| |                     |                        | | |
    //| | | |label        | |                     |                        | | |
    //| | | |-------------| |                     |                        | | |
    //| | | |image        | |                     |                        | | |
    //| | | |             | |                     |                        | | |
    //| | | |-------------| |                     |                        | | |
    //| | | |description  | |                     |                        | | |
    //| | | |             | |                     |                        | | |
    //| | | |------------ | |                     |                        | | |
    //| | |----------------------------------------------------------------| | |
    //| | | SOUTH : console                                                | | |
    //| | |----------------------------------------------------------------| | |
    //| |--------------------------------------------------------------------| |
    //| | SOUTH : OKCancelPanel                                              | |
    //| |                                                                    | |
    //| |--------------------------------------------------------------------| |
    //|------------------------------------------------------------------------|
    
    /**
     * @param frame the frame on which to make this dialog modal and centred
     */
    public DualPaneInputDialog(final Frame frame, String title, boolean modal) {
        super(frame, title, modal);
    }
    
    public DualPaneInputDialog() {
        this(null, "", false);
    }
    
    JPanel leftPanel;
    JPanel rightPanel;
    
    protected void setMainComponent() {
        mainComponent = new JPanel();
        ((JPanel)mainComponent).setBorder(BorderFactory.createEtchedBorder());
        mainComponent.setLayout(new BoxLayout(mainComponent, BoxLayout.X_AXIS));
        leftPanel = new JPanel(new GridBagLayout());
        JPanel separator = new JPanel();
        separator.setBackground(Color.BLACK);
        separator.setMinimumSize(new Dimension(1, 1));
        separator.setPreferredSize(new Dimension(1, 1));
        rightPanel = new JPanel(new GridBagLayout());
        mainComponent.add(leftPanel);
        mainComponent.add(separator);
        mainComponent.add(rightPanel);
        currentPanel = leftPanel;
    }
    
    
    public void setLeftPane() {currentPanel = leftPanel;}
    
    public void setRightPane() {currentPanel = rightPanel;}
    
    // Demonstrates DualPaneInputDialog usage
    public static void main(String[] args) {
        final LayerManager lm = new LayerManager();
        
        FeatureSchema fs1 = new FeatureSchema();
        fs1.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        com.vividsolutions.jump.feature.FeatureDataset ds1 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs1);
        lm.addLayer("","LayerWithJustGeometry",ds1);
        
        FeatureSchema fs2 = new FeatureSchema();
        fs2.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs2.addAttribute("Name", AttributeType.STRING);
        com.vividsolutions.jump.feature.FeatureDataset ds2 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs2);
        lm.addLayer("","LayerWithStringAttribute",ds2);
        
        FeatureSchema fs3 = new FeatureSchema();
        fs3.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs3.addAttribute("Name", AttributeType.STRING);
        fs3.addAttribute("Age", AttributeType.INTEGER);
        com.vividsolutions.jump.feature.FeatureDataset ds3 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs3);
        lm.addLayer("","LayerWithNumericAttribute",ds3);
        
        
        // MultiInputDialog usage demonstration
        final DualPaneInputDialog d = new DualPaneInputDialog(null, "Title!", true);
        d.addSubTitle("Sous-titre 1");
        d.addLabel("This is just a label");
        d.addTextField("Nom", "", 24, null, "");
        d.addPositiveIntegerField("Age", 0, 6, "");
        d.addNonNegativeDoubleField("Salaire", 0, 12, "");
        d.addComboBox("Métier", "Cadre", Arrays.asList("Cadre","Charpentier","Maçon","Boulanger"), "");
        
        d.addSubTitle("Layer and attribute selection");
        AttributeTypeFilter STRING_FILTER = new AttributeTypeFilter(AttributeTypeFilter.STRING);
        AttributeTypeFilter NUMERIC_FILTER = AttributeTypeFilter.NUMERIC_FILTER;
        AttributeTypeFilter NOGEOM_FILTER = AttributeTypeFilter.NO_GEOMETRY_FILTER;
        AttributeTypeFilter ALL_FILTER = AttributeTypeFilter.ALL_FILTER;
        final JComboBox typeChooser = d.addComboBox("Choose Attribute Type", "ALL",
                Arrays.asList(STRING_FILTER,NUMERIC_FILTER,ALL_FILTER,NOGEOM_FILTER), "");
        final JComboBox layerChooser = d.addLayerComboBox("LayerField", null, "ToolTip", lm);
        final JComboBox attributeChooser = d.addAttributeComboBox("Attribute field", "LayerField", NUMERIC_FILTER, "");
        typeChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AttributeTypeFilter atf = (AttributeTypeFilter)typeChooser.getSelectedItem();
                layerChooser.setModel(new DefaultComboBoxModel(atf.filter(lm).toArray(new Layer[0])));
            }
        });
        
        d.addSeparator();
        
        d.setRightPane();
        final JCheckBox jcb = d.addCheckBox("Afficher l'icone", false, "");
        JButton button = d.addButton("switch image panel");
        
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (d.getDescriptionPanel().getDescription().equals("")) {
                    d.setSideBarDescription("Description de la boîte de dialogue et des paramètres pour aider l'utilisateur");
                    d.getConsole().flashMessage("Add description");
                }
                else {
                    d.setSideBarDescription("");
                    d.getConsole().flashMessage("Remove description");
                }
            }
        });
        jcb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (jcb.isSelected()) {
                    d.setSideBarImage(new ImageIcon(MultiInputDialog.class.getResource("Butt.gif")));
                    d.getConsole().flashMessage("Add image");
                }
                else {
                    d.setSideBarImage(null);
                    d.getConsole().flashMessage("Remove image");
                }
            }
        });
        JButton button2 = d.addButton("Deuxième bouton", "OK", "");
        d.addRow();
        d.setVisible(true);
        GUIUtil.centreOnScreen(d);
        System.out.println(d.getLayer("LayerField"));
        System.exit(0);
    }
}
