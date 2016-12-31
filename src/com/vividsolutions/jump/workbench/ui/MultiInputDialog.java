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
import java.awt.Container;
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
import com.vividsolutions.jump.workbench.ui.OKCancelApplyPanel;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;


/**
 * Flexible generic dialog for prompting the user to type in several values.
 * This Dialog is a refactoring of the previous vividsolutions MultiInputDialog
 */
public class MultiInputDialog extends AbstractMultiInputDialog {
    
    // Main components of a MultiInputDialog
    
    //|------------------------------------------------------------------------|
    //| this.contentPane (BorderLayout)                                        |
    //| |--------------------------------------------------------------------| |
    //| | CENTER dialogPanel (BorderLayout)                                  | |
    //| | |----------------------------------------------------------------| | |
    //| | | WEST:           | CENTER : mainComponent                       | | |
    //| | | infoPanel       |                                              | | |
    //| | | |-------------| | any container                                | | |
    //| | | |label        | | - default is simple JPanel                   | | |
    //| | | |-------------| | - option double left/right panel             | | |
    //| | | |image        | | - JSplitPanel                                | | |
    //| | | |             | | - JTabbedPane                                | | |
    //| | | |-------------| | - Widget                                     | | |
    //| | | |description  | |                                              | | |
    //| | | |             | |                                              | | |
    //| | | |------------ | |                                              | | |
    //| | |----------------------------------------------------------------| | |
    //| | | SOUTH : console                                                | | |
    //| | |----------------------------------------------------------------| | |
    //| |--------------------------------------------------------------------| |
    //| | SOUTH : OKCancelApplyPanel                                         | |
    //| |                                                                    | |
    //| |--------------------------------------------------------------------| |
    //|------------------------------------------------------------------------|
    
    // dialogPanel contains everything but the OKCancelApplyPanel
    final private JPanel dialogPanel = new JPanel(new BorderLayout());
        // imagePanel contains an image and a description
        final private MultiInputDialogInfoPanel infoPanel = new MultiInputDialogInfoPanel();
        // mainComponent contains all the components for user inputs
        protected Container mainComponent;
        protected JPanel currentPanel;
        // consolePanel can show warnings or comments to help the user
        final private MultiInputDialogConsole console = new MultiInputDialogConsole();
    // This panel just contains the OK and the Cancel Buttons
    final protected OKCancelApplyPanel okCancelApplyPanel = new OKCancelApplyPanel();
    private int inset = 0;
    
    protected void setMainComponent() {
        mainComponent = new JPanel(new GridBagLayout());
        currentPanel = (JPanel)mainComponent;
        Border mainComponentBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(5+inset, 5+inset, 5+inset, 5+inset)
        );
        currentPanel.setBorder(mainComponentBorder);
    }

    public void setMainComponent(Container container) {
        this.mainComponent = container;
    }
    
    /**
     * @return the JPanel where new Rows are added
     */
    public JPanel getCurrentPanel() {
        return currentPanel;
    }
    
    /**
     * @param panel the JPanel where new Rows are added
     */
    public void setCurrentPanel(JPanel panel) {
        currentPanel = panel;
    }
    
    /**
     * @return the MultiInputDialogConsole panel.
     */ 
    public MultiInputDialogConsole getConsole() {
        return console;
    }

    private int rowCount = 0;
    
    
    /**
     * @param frame the frame on which to make this dialog modal and centred
     */
    public MultiInputDialog(final Frame frame, String title, boolean modal) {
        super(frame, title, modal);
        
        okCancelApplyPanel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            okCancelApplyPanel_actionPerformed(e);
          }
        });
        
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public MultiInputDialog() {
        this(null, "", false);
    }
    
    /**
     * @param visible if true, the MultiInputDialog will be visible.
     */
    public void setVisible(boolean visible) {
        pack();
        super.setVisible(visible);
    }
    
    /**
     * @return the MultiInputDialogInfoPanel.
     */ 
    public MultiInputDialogInfoPanel getDescriptionPanel() {
        return infoPanel;
    }

    public void setSideBarImage(Icon icon) {
        infoPanel.setIcon(icon);
        pack();
    }
    
    public void setSideBarDescription(String description) {
        infoPanel.setDescription(description);
        pack();
    }
    
    public void showConsole() {
        console.setVisible(true);
        pack();
    }
    
    public void setApplyVisible(boolean applyVisible) {
        okCancelApplyPanel.setApplyVisible(applyVisible);
    }
    
    public void setCancelVisible(boolean cancelVisible) {
        okCancelApplyPanel.setCancelVisible(cancelVisible);
    }
    
    public boolean wasApplyPressed() {
        return okCancelApplyPanel.wasApplyPressed();
    }
    
    public boolean wasOKPressed() {
        return okCancelApplyPanel.wasOKPressed();
    }
    
    //Experience suggests that one should avoid using weights when using the
    //GridBagLayout. I find that nonzero weights can cause layout bugs that are
    //hard to track down. [Jon Aquino]
    void jbInit() throws Exception {
  
      // LDB: set the default button for Enter to the OK for all
      this.getRootPane().setDefaultButton(
          okCancelApplyPanel.getButton(I18N.get("ui.OKCancelPanel.ok")));
  
      setMainComponent();
  
      dialogPanel.add(infoPanel, BorderLayout.WEST);
      dialogPanel.add(mainComponent, BorderLayout.CENTER);
      dialogPanel.add(console, BorderLayout.SOUTH);
      console.setVisible(false);
  
      this.setResizable(true);
      this.getContentPane().add(dialogPanel, BorderLayout.CENTER);
      this.getContentPane().add(okCancelApplyPanel, BorderLayout.SOUTH);
    }
    
    public void addOKCancelApplyPanelActionListener(ActionListener actionListener) {
        okCancelApplyPanel.addActionListener(actionListener);
    }
    
    /**
     * Add a row to the current JPanel.
     * The GridBagLayout is used as follows :
     * <ul>
     * <li>For JCheckBox, JRadioButton and JLabel and JPanel the component spreads
     * over 3 columns. Empty label is in the fourth column.</li>
     * <li>For JComboBox and JTextField, label is in first column and component in
     * second column. Third and fourth column or empty.</li>
     * </ul>
     * |---|------------------------|----------------------|-----|---| 
     * | 0 |            1           |             2        |  3  | 4 |
     * |---|-----------------------------------------------------|---|
     * |   | JCheckBox with label associated                     |   |
     * |---|-----------------------------------------------------|---|
     * |   | JRadioButton with label associated                  |   |
     * |---|-----------------------------------------------------|---|
     * |   | JLabel                                              |   |
     * |---|-----------------------------------------------------|---|
     * |   | JPanel                                              |   |
     * |---|------------------------|----------------------------|---|
     * |   | Label                  | JComboBox                  |   |
     * |---|------------------------|----------------------------|---|
     * |   | Label                  | JTextField                 |   |
     * |---|------------------------|----------------------|-----|---|
    
    /**
     * Adds a row (containing either a control or a label) to the Dialog.
     *
     * @param fieldName field name of the control (used as a key)
     * @param label label of the control
     * @param component the control itself (may also be a label or a separator)
     * @param enableChecks checks to validate inputs
     * @param toolTipText
     */
    public void addRow(String fieldName,
                          JComponent label,
                          JComponent component,
                          EnableCheck[] enableChecks,
                          String toolTipText,
                          int labelPos,
                          int fillMode) {
        // register with parent
        addComponent(fieldName, label, component);

        if (label != null && toolTipText != null) {
          label.setToolTipText(toolTipText);
          component.setToolTipText(toolTipText);
        }
        
        if (enableChecks != null) {
            addEnableChecks(fieldName, Arrays.asList(enableChecks));
        }
        
        if (labelPos == NO_LABEL) {
            currentPanel.add(component,
                new GridBagConstraints(1, rowCount, 3, 1, 1.0, 1.0,
                GridBagConstraints.WEST, fillMode,
                new Insets(5+inset, 2+inset, 2+inset, 2+inset), 0, 0));
        }
        else if (labelPos == LEFT_LABEL) {
            if (label != null) {
                currentPanel.add(label,
                        new GridBagConstraints(1, rowCount, 1, 1, 1.0, 1.0,
                                GridBagConstraints.WEST, GridBagConstraints.NONE,
                                new Insets(2+inset, 2+inset, 2+inset, 2+inset), 0, 0));
            }
            currentPanel.add(component,
                new GridBagConstraints(2, rowCount, 2, 1, 1.0, 1.0,
                GridBagConstraints.WEST, fillMode,
                new Insets(2+inset, 2+inset, 2+inset, 2+inset), 0, 0));
        }
        else if (labelPos == RIGHT_LABEL) {
            currentPanel.add(component,
                new GridBagConstraints(1, rowCount, 2, 1, 1.0, 1.0,
                GridBagConstraints.WEST, fillMode,
                new Insets(2+inset, 2+inset, 2+inset, 2+inset), 0, 0));
            if (label != null) {
                currentPanel.add(label,
                        new GridBagConstraints(3, rowCount, 1, 1, 1.0, 1.0,
                                GridBagConstraints.WEST, GridBagConstraints.NONE,
                                new Insets(2+inset, 2+inset, 2+inset, 2+inset), 0, 0));
            }
        }
        
        rowCount++;
    }
    
    // Add a row with a component without label (ex. subtitle, checkbox))
    public void addRow(String fieldName, JComponent component, EnableCheck[] enableChecks, String toolTipText) {
        addRow(fieldName, null, component, enableChecks, toolTipText, NO_LABEL, HORIZONTAL);
    }
    
    // Add a row with a label on the left, and a component on the right (layer, textfield,...) 
    public void addRow(String fieldName, JLabel label, JTextField component, EnableCheck[] enableChecks, String toolTipText) {
        addRow(fieldName, label, component, enableChecks, toolTipText, LEFT_LABEL, NONE);
    }
    
    // Add a row with a label on the left, and a component on the right (layer, textfield,...) 
    public void addRow(String fieldName, JLabel label, JComponent component, EnableCheck[] enableChecks, String toolTipText) {
        addRow(fieldName, label, component, enableChecks, toolTipText, LEFT_LABEL, NONE);
    }
        
    public void addRow(JComponent component) {
        if (component instanceof JLabel) {
            addRow(((JLabel)component).getText(), new JLabel(""), component, null, "", NO_LABEL, HORIZONTAL);
        }
        else addRow("DUMMY", new JLabel(""), component, null, "", NO_LABEL, HORIZONTAL);
    }
    
    public void addRow() {
        currentPanel.add(new JPanel(),
                new GridBagConstraints(1, rowCount, 2, 1, 1.0, 1.0,
                GridBagConstraints.WEST, BOTH, new Insets(2, 2, 2, 2), 0, 0));
    }
    
    public void setInset(int inset) {
        this.inset = inset;
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    void okCancelApplyPanel_actionPerformed(ActionEvent e) {
        if (okCancelApplyPanel.wasApplyPressed()) {
            return;
        } 
        else if (!okCancelApplyPanel.wasOKPressed() || isInputValid()) {
            setVisible(false);
            return;
        }
        reportValidationError(firstValidationErrorMessage());
    }
    
    void this_componentShown(ComponentEvent e) {
        okCancelApplyPanel.setOKPressed(false);
        okCancelApplyPanel.setApplyPressed(false);
    }
    
    /**
     * Indent the label of a field with a MatteBorder having the width of
     * a JCheckBox and the color of the component background.
     * This helps to align JCheckBox label (text on the right of the CheckBox)
     * with other component labels (text on the left of the component).
     *
     * @param fieldName the field to indent
     */
    public void indentLabel(String fieldName) {
        getLabel(fieldName).setBorder(
            BorderFactory.createMatteBorder(
                0,
                (int) new JCheckBox().getPreferredSize().getWidth(),
                0,
                0,
                getLabel(fieldName).getBackground()));
    }
    
    /**
     * @deprecated
     */
    public void startNewColumn() {
        JOptionPane.showMessageDialog(
            this,
            "MultiInputDialog.startNewColumn() is deprecated,\n" +
            "if you want to layout widgets on two panels,\n " +
            "please, use the new DualPaneInputDialog class instead",
            "OpenJUMP",
            JOptionPane.ERROR_MESSAGE);
    }
    
    // Sample to test the class
    public static void main(String[] args) {
        final LayerManager lm = new LayerManager();
        
        // Schema containing a single Geometry attribute
        FeatureSchema fs1 = new FeatureSchema();
        fs1.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        com.vividsolutions.jump.feature.FeatureDataset ds1 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs1);
        lm.addLayer("","LayerWithJustGeometry",ds1);
        
        // Schema containing a Geometry and a String attributes
        FeatureSchema fs2 = new FeatureSchema();
        fs2.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs2.addAttribute("Name", AttributeType.STRING);
        com.vividsolutions.jump.feature.FeatureDataset ds2 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs2);
        lm.addLayer("","LayerWithStringAttribute",ds2);
        
        // Schema containing a Geometry, a String and a Integer attributes
        FeatureSchema fs3 = new FeatureSchema();
        fs3.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs3.addAttribute("Name", AttributeType.STRING);
        fs3.addAttribute("Age", AttributeType.INTEGER);
        com.vividsolutions.jump.feature.FeatureDataset ds3 =
            new com.vividsolutions.jump.feature.FeatureDataset(fs3);
        lm.addLayer("","LayerWithNumericAttribute",ds3);
        
        
        // MultiInputDialog usage demonstration
        final MultiInputDialog d = new MultiInputDialog(null, "Title!", true);
        d.setInset(2);
        d.addSubTitle("Subtitle 1");
        d.addLabel("This is just a label");
        d.addTextField("Name", "", 24, null, "");
        d.addPositiveIntegerField("Age", 0, 6, "");
        d.addNonNegativeDoubleField("Salary", 0, 12, "");
        d.addComboBox("Occupation", "Cadre", java.util.Arrays.asList("Manager","Developper","Technician","Secretary"), "");
        d.indentLabel("Occupation");
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
        final JCheckBox jcb = d.addCheckBox("Display icon", false, "");
        JButton button = d.addButton("Switch image panel");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (d.infoPanel.getDescription().equals("")) {
                    d.infoPanel.setDescription("Description of the dialog box.\nThis description must be helpful for the user. I must give meaningful information about which parameters are mandatory, optional, what they represent and which value they can take.");
                    d.getConsole().flashMessage("Add description");
                }
                else {
                    d.infoPanel.setDescription("");
                    d.getConsole().flashMessage("Remove description");
                }
                d.pack();
            }
        });
        jcb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (jcb.isSelected()) {
                    d.infoPanel.setIcon(new ImageIcon(com.vividsolutions.jump.workbench.ui.images.IconLoader.class.getResource("Butt.gif")));
                    d.getConsole().flashMessage("Add image");
                }
                else {
                    d.infoPanel.setIcon(null);
                    d.getConsole().flashMessage("Remove image");
                }
                d.pack();
            }
        });
        JButton button2 = d.addButton("Second button", "OK", "");
        GUIUtil.centreOnScreen(d);
        d.setVisible(true);
//        System.out.println(d.getLayer("LayerField"));
        System.exit(0);
    }
}
