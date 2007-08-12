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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.FontChooser;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;


public class LabelStylePanel extends JPanel implements StylePanel {
    private static final String NONE = "("+I18N.get("ui.style.LabelStylePanel.none")+")";
    private BorderLayout borderLayout1 = new BorderLayout();
    private GridBagLayout gridBagLayout6 = new GridBagLayout();
    private JLabel attributeLabel = new JLabel();
    private JComboBox attributeComboBox = new JComboBox();
    private JComboBox angleAttributeComboBox = new JComboBox();
	public String getTitle() {
		return I18N.get("ui.style.LabelStylePanel.labels");
	}
	public String validateInput() {
        return null;
    }

    private JPanel previewPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                LabelStyle labelStyle = createLabelStyle(layer.getLabelStyle());

                if (!labelStyle.isEnabled()) {
                    return;
                }

                labelStyle.initialize(layer);
                labelStyle.paint((Graphics2D) g, sampleText(),
                    layerViewPanel.getViewport().getScale(),
                    new Point2D.Double(getWidth() / 2d, getHeight() / 2d),
                    layer.getFeatureCollectionWrapper().isEmpty() ? 0
                                                           : LabelStyle.angle(
                        (Feature) layer.getFeatureCollectionWrapper().iterator().next(),
                        getAngleAttribute(), 0),
                    layer.getFeatureCollectionWrapper().isEmpty() ? getLabelHeight()
                                                           : LabelStyle.height(
                        (Feature) layer.getFeatureCollectionWrapper().iterator().next(),
                        getHeightAttribute(), getLabelHeight()), false);
            }
        };

    private JCheckBox scaleCheckBox = new JCheckBox();
    private Layer layer;
    private JCheckBox labellingCheckBox = new JCheckBox();
    private Border border1;
    private LayerViewPanel layerViewPanel;
    private JDialog parent;
    private Color color;
    private Font labelFont;

    private ValidatingTextField heightTextField = new ValidatingTextField("999",
            7,
            new ValidatingTextField.Validator() {
                public boolean isValid(String text) {
                    if (text.length() == 0) {
                        return true;
                    }

                    try {
                        Double.parseDouble(text);

                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
    private JLabel heightLabel = new JLabel();
    private JLabel previewLabel = new JLabel();
    private JPanel fillerPanel = new JPanel();
    private JPanel buttonPanel = new JPanel();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private JButton colorButton = new JButton();
    private JButton fontButton = new JButton();
    private JPanel jPanel3 = new JPanel();
    private JLabel verticalAlignmentLabel = new JLabel();
    private JComboBox verticalAlignmentComboBox = new JComboBox();
    private JLabel angleLabel = new JLabel();
    private JCheckBox hideOverlappingLabelsCheckBox = new JCheckBox();
    private JLabel heightAttributeLabel = new JLabel();
    private JComboBox heightAttributeComboBox = new JComboBox();

    public LabelStylePanel(Layer layer, LayerViewPanel layerViewPanel,
        JDialog parent, ErrorHandler errorHandler) {
        try {
            this.parent = parent;
            this.layerViewPanel = layerViewPanel;

            //Populate verticalAlignmentComboBox before calling #setLayer so that
            //initially selected item can be properly set. [Jon Aquino]
            verticalAlignmentComboBox.addItem(LabelStyle.ABOVE_LINE);
            verticalAlignmentComboBox.addItem(LabelStyle.ON_LINE);
            verticalAlignmentComboBox.addItem(LabelStyle.BELOW_LINE);

            //Call #setLayer before #jbInit, so no events will be fired. Otherwise,
            //NullPointerExceptions will be thrown. [Jon Aquino]
            setLayer(layer);
            jbInit();
            heightTextField.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        documentChanged();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        documentChanged();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        documentChanged();
                    }

                    private void documentChanged() {
                        updateControls();
                    }
                });
            colorButton.setToolTipText(I18N.get("ui.style.LabelStylePanel.browse"));
            fontButton.setToolTipText(I18N.get("ui.style.LabelStylePanel.browse"));
            updateControls();
            verticalAlignmentComboBox.setRenderer(new ListCellRenderer() {
                    private Icon aboveIcon = IconLoader.icon(
                            "BigLabelAbove.gif");
                    private Icon onIcon = IconLoader.icon("BigLabelOn.gif");
                    private Icon belowIcon = IconLoader.icon(
                            "BigLabelBelow.gif");
                    private DefaultListCellRenderer renderer = new DefaultListCellRenderer();

                    public Component getListCellRendererComponent(JList list,
                        Object value, int index, boolean isSelected,
                        boolean cellHasFocus) {
                        JLabel label = (JLabel) renderer.getListCellRendererComponent(list,
                                "", index, isSelected, cellHasFocus);
                        label.setIcon(value.equals(LabelStyle.ABOVE_LINE)
                            ? aboveIcon
                            : (value.equals(LabelStyle.ON_LINE) ? onIcon
                                                                : belowIcon));

                        return label;
                    }
                });
        } catch (Throwable t) {
            errorHandler.handleThrowable(t);
        }
    }

    private String sampleText() {
        String sampleText = "Abc123";

        if (layer.getFeatureCollectionWrapper().isEmpty()) {
            return sampleText;
        }

        Feature firstFeature = (Feature) layer.getFeatureCollectionWrapper().iterator()
                                              .next();
        Object attribute = getLabelAttribute().equals(LabelStyle.FID_COLUMN)
            ? (firstFeature.getID() + "")
            : firstFeature.getAttribute(getLabelAttribute());

        if (attribute == null) {
            return sampleText;
        }

        if (attribute.toString().trim().length() == 0) {
            return sampleText;
        }

        return attribute.toString().trim();
    }

    private void setLayer(Layer layer) {
        this.layer = layer;
        setLabelling(layer.getLabelStyle().isEnabled());
        setAttributes(layer.getFeatureCollectionWrapper().getFeatureSchema());
        setAttribute(layer.getLabelStyle().getAttribute());
        setAngleAttribute(layer.getLabelStyle().getAngleAttribute());
        setHeightAttribute(layer.getLabelStyle().getHeightAttribute());
        setColor(layer.getLabelStyle().getColor());
        setLabelFont(layer.getLabelStyle().getFont());
        setScaling(layer.getLabelStyle().isScaling());
        hideOverlappingLabelsCheckBox.setSelected(layer.getLabelStyle()
                                                       .isHidingOverlappingLabels());
        heightTextField.setText(layer.getLabelStyle().getHeight() + "");
        verticalAlignmentComboBox.setSelectedItem(layer.getLabelStyle()
                                                       .getVerticalAlignment());
    }

    private void setAttributes(FeatureSchema schema) {
        attributeComboBox.removeAllItems();
        angleAttributeComboBox.removeAllItems();
        heightAttributeComboBox.removeAllItems();
        attributeComboBox.addItem(LabelStyle.FID_COLUMN);
        angleAttributeComboBox.addItem(NONE);
        heightAttributeComboBox.addItem(NONE);

        for (int i = 0; i < schema.getAttributeCount(); i++) {
            attributeComboBox.addItem(schema.getAttributeName(i));

            if ((schema.getAttributeType(i) == AttributeType.DOUBLE) ||
                    (schema.getAttributeType(i) == AttributeType.INTEGER)) {
                angleAttributeComboBox.addItem(schema.getAttributeName(i));
                heightAttributeComboBox.addItem(schema.getAttributeName(i));
            }
        }
    }

    private void setLabelling(boolean labelling) {
        labellingCheckBox.setSelected(labelling);
    }

    private void setColor(Color color) {
        this.color = color;
    }

    private void setAttribute(String attribute) {
        Assert.isTrue(!attribute.equals(""));
        attributeComboBox.setSelectedItem(attribute);
    }

    private void setAngleAttribute(String angleAttribute) {
        if (angleAttribute.equals("")) {
            angleAttributeComboBox.setSelectedItem(NONE);

            return;
        }

        angleAttributeComboBox.setSelectedItem(angleAttribute);
    }

    private void setHeightAttribute(String heightAttribute) {
        if (heightAttribute.equals("")) {
            heightAttributeComboBox.setSelectedItem(NONE);

            return;
        }

        heightAttributeComboBox.setSelectedItem(heightAttribute);
    }

    private void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }

    private void setScaling(boolean scaling) {
        scaleCheckBox.setSelected(scaling);
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        this.setLayout(borderLayout1);
        setLayout(gridBagLayout6);
        attributeLabel.setText(I18N.get("ui.style.LabelStylePanel.label-attribute"));
        previewPanel.setBackground(Color.white);
        previewPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        previewPanel.setMaximumSize(new Dimension(200, 40));
        previewPanel.setMinimumSize(new Dimension(200, 40));
        previewPanel.setPreferredSize(new Dimension(200, 40));
        scaleCheckBox.setText(I18N.get("ui.style.LabelStylePanel.scale-labels-with-the-zoom-level"));
        scaleCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scaleCheckBox_actionPerformed(e);
                }
            });
        setBorder(border1);
        labellingCheckBox.setText(I18N.get("ui.style.LabelStylePanel.enable-labelling"));
        labellingCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    enableLabellingCheckBox_actionPerformed(e);
                }
            });
        heightLabel.setText(I18N.get("ui.style.LabelStylePanel.height"));
        attributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    attributeComboBox_actionPerformed(e);
                }
            });
        angleAttributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    angleAttributeComboBox_actionPerformed(e);
                }
            });
        previewLabel.setText(I18N.get("ui.style.LabelStylePanel.preview-at-current-zoom-level"));
        buttonPanel.setLayout(gridBagLayout2);
        colorButton.setText(I18N.get("ui.style.LabelStylePanel.change-colour"));
        colorButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    colorButton_actionPerformed(e);
                }
            });
        fontButton.setText(I18N.get("ui.style.LabelStylePanel.change-font"));
        fontButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fontButton_actionPerformed(e);
                }
            });
        verticalAlignmentLabel.setText(I18N.get("ui.style.LabelStylePanel.vertical-alignment-for-lines"));
        verticalAlignmentComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    verticalAlignmentComboBox_actionPerformed(e);
                }
            });
        angleLabel.setText(I18N.get("ui.style.LabelStylePanel.angle-attribute-degrees"));
        hideOverlappingLabelsCheckBox.setText(I18N.get("ui.style.LabelStylePanel.hide-overlapping-labels"));
        hideOverlappingLabelsCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    hideOverlappingLabelsCheckBox_actionPerformed(e);
                }
            });
        heightAttributeLabel.setText(I18N.get("ui.style.LabelStylePanel.height-attribute"));
        heightAttributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    heightAttributeComboBox_actionPerformed(e);
                }
            });
        add(attributeLabel,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        add(attributeComboBox,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 4, 2, 0), 0, 0));
        add(angleAttributeComboBox,
            new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 4, 2, 0), 0, 0));
        add(previewPanel,
            new GridBagConstraints(0, 13, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 10, 4, 4), 0, 0));
        add(scaleCheckBox,
            new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        add(labellingCheckBox,
            new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(heightTextField,
              new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0));
        this.add(heightLabel,
            new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(previewLabel,
            new GridBagConstraints(0, 12, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(98, 104, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(buttonPanel,
            new GridBagConstraints(0, 11, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(colorButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        buttonPanel.add(fontButton,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        buttonPanel.add(jPanel3,
            new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(verticalAlignmentLabel,
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(verticalAlignmentComboBox,
            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 4, 2, 0), 0, 0));
        this.add(angleLabel,
            new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(hideOverlappingLabelsCheckBox,
            new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(heightAttributeLabel,
            new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(heightAttributeComboBox,
             new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 0), 0, 0));
    }

    public void updateStyles() {
        boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);

        try {
            //
            LabelStyle newLabelStyle = createLabelStyle(layer.getLabelStyle());
            layer.removeStyle(layer.getLabelStyle());
            layer.addStyle(newLabelStyle);
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }

        layer.fireAppearanceChanged();
    }

    private String getAngleAttribute() {
        if (NONE == angleAttributeComboBox.getSelectedItem()) {
            return "";
        }

        return (String) angleAttributeComboBox.getSelectedItem();
    }

    private String getHeightAttribute() {
        if (NONE == heightAttributeComboBox.getSelectedItem()) {
            return "";
        }

        return (String) heightAttributeComboBox.getSelectedItem();
    }

    private String getLabelAttribute() {
        return (String) attributeComboBox.getSelectedItem();
    }

    public LabelStyle createLabelStyle(LabelStyle defaultValues) {
        LabelStyle labelStyle = (LabelStyle) defaultValues.clone();
        labelStyle.setEnabled(labellingCheckBox.isSelected());
        Assert.isTrue(attributeComboBox.getSelectedIndex() != -1);
        labelStyle.setAttribute(getLabelAttribute());
        labelStyle.setAngleAttribute(getAngleAttribute());
        labelStyle.setHeightAttribute(getHeightAttribute());
        labelStyle.setColor(color);
        labelStyle.setFont(labelFont);
        labelStyle.setScaling(scaleCheckBox.isSelected());
        labelStyle.setHidingOverlappingLabels(hideOverlappingLabelsCheckBox.isSelected());
        labelStyle.setHeight(getLabelHeight());
        labelStyle.setVerticalAlignment((String) verticalAlignmentComboBox.getSelectedItem());

        return labelStyle;
    }

    private double getLabelHeight() {
        return (heightTextField.getText().length() == 0)
        ? LabelStyle.FONT_BASE_SIZE
        : Double.parseDouble(heightTextField.getText());
    }

    public void updateControls() {
        previewPanel.repaint();
        attributeLabel.setEnabled(labellingCheckBox.isSelected());
        angleLabel.setEnabled(labellingCheckBox.isSelected());
        heightAttributeLabel.setEnabled(labellingCheckBox.isSelected());
        attributeComboBox.setEnabled(labellingCheckBox.isSelected());
        angleAttributeComboBox.setEnabled(labellingCheckBox.isSelected());
        heightAttributeComboBox.setEnabled(labellingCheckBox.isSelected());
        colorButton.setEnabled(labellingCheckBox.isSelected());
        fontButton.setEnabled(labellingCheckBox.isSelected());
        heightLabel.setEnabled(labellingCheckBox.isSelected() &&
            getHeightAttribute().equals(""));
        heightTextField.setEnabled(labellingCheckBox.isSelected() &&
            getHeightAttribute().equals(""));
        scaleCheckBox.setEnabled(labellingCheckBox.isSelected());
        hideOverlappingLabelsCheckBox.setEnabled(labellingCheckBox.isSelected());
        previewLabel.setEnabled(labellingCheckBox.isSelected());
        previewPanel.setEnabled(labellingCheckBox.isSelected());
        verticalAlignmentLabel.setEnabled(labellingCheckBox.isSelected());
        verticalAlignmentComboBox.setEnabled(labellingCheckBox.isSelected());
    }

    void colorButton_actionPerformed(ActionEvent e) {
        Color newColor = JColorChooser.showDialog(this, I18N.get("ui.style.LabelStylePanel.choose-colour"), color);

        if (newColor == null) {
            return;
        }

        setColor(newColor);
        updateControls();
    }

    void fontButton_actionPerformed(ActionEvent e) {
        Font newFont = FontChooser.showDialog(parent, I18N.get("ui.style.LabelStylePanel.choose-font"), labelFont);

        if (newFont == null) {
            return;
        }

        setLabelFont(newFont);
        updateControls();
    }

    void enableLabellingCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void attributeComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void angleAttributeComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void scaleCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void verticalAlignmentComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void hideOverlappingLabelsCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void heightAttributeComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
}
