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
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;


public class LabelStylePanel extends JPanel implements StylePanel {
    private static final String NONE = "("+
    	I18N.get("ui.style.LabelStylePanel.none")+")";
    private static final String CHOOSE_COLOUR = 
    	I18N.get("ui.style.LabelStylePanel.choose-colour");
    private static final String CHOOSE_FONT = 
    	I18N.get("ui.style.LabelStylePanel.choose-font");
    private static final String BROWSE = 
    	I18N.get("ui.style.LabelStylePanel.browse");
    private static final String LABEL_ATTRIBUTE = 
    	I18N.get("ui.style.LabelStylePanel.label-attribute");
    private static final String SCALE_LABELS_WITH_THE_ZOOM_LEVEL = 
    	I18N.get("ui.style.LabelStylePanel.scale-labels-with-the-zoom-level");
    private static final String HIDE_LABELS_WHEN = 
    	I18N.get("ui.style.LabelStylePanel.hide-labels-when"); 
    private static final String DRAW_OUTLINE_HALO_AROUND_LABELS = 
    	I18N.get("ui.style.LabelStylePanel.draw-outline-halo-around-labels"); 
    private static final String ENABLE_LABELLING = 
    	I18N.get("ui.style.LabelStylePanel.enable-labelling");
    private static final String HEIGHT = 
    	I18N.get("ui.style.LabelStylePanel.height");
    private static final String OUTLINE_WIDTH = 
    	I18N.get("ui.style.LabelStylePanel.outline-width"); 
    private static final String PREVIEW_AT_CURRENT_ZOOM_LEVEL = 
    	I18N.get("ui.style.LabelStylePanel.preview-at-current-zoom-level");
    private static final String VERTICAL_ALIGNMENT_FOR_POINTS_AND_LINES = 
    	I18N.get("ui.style.LabelStylePanel.vertical-alignment-for-lines");  
    private static final String HORIZONTAL_ALIGNMENT_FOR_POINTS_AND_LINES = 
    	I18N.get("ui.style.LabelStylePanel.horizontal-alignment-for-points_and_lines");
    private static final String CHANGE_FONT = 
    	I18N.get("ui.style.LabelStylePanel.change-font");
    private static final String CHANGE_COLOUR = 
    	I18N.get("ui.style.LabelStylePanel.change-colour");  
    private static final String ANGLE_ATTRIBUTE_DEGREES = 
    	I18N.get("ui.style.LabelStylePanel.angle-attribute-degrees");
    private static final String HIDE_OVERLAPPING_LABELS = 
    	I18N.get("ui.style.LabelStylePanel.hide-overlapping-labels");
    private static final String HEIGHT_ATTRIBUTE = 
    	I18N.get("ui.style.LabelStylePanel.height-attribute");
    private static final String SCALE_IS_BELOW = 
    	I18N.get("ui.style.LabelStylePanel.scale-is-below");

    private static final String HIDE_AT_SCALE_TEXT = SCALE_IS_BELOW+"   1:";
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
            layerViewPanel.getViewport(),//.getScale(),
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
    private Color outlineColor;
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
    private ValidatingTextField outlineWidthField = new 
    		ValidatingTextField("4",7,
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
    private JLabel outlineLabel = new JLabel();
     private JLabel previewLabel = new JLabel();
    private JPanel fillerPanel = new JPanel();
    private JPanel buttonPanel = new JPanel();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private JButton colorButton = new JButton();
    private JButton outlineColorButton = new JButton();
    private JPanel outlineButtonPanel = new JPanel();
    private JButton fontButton = new JButton();
    private JPanel jPanel3 = new JPanel();
    private JLabel verticalAlignmentLabel = new JLabel();
    private JComboBox verticalAlignmentComboBox = new JComboBox();
    private JLabel angleLabel = new JLabel();
    private JCheckBox hideOverlappingLabelsCheckBox = new JCheckBox();
    private JLabel heightAttributeLabel = new JLabel();
    private JComboBox heightAttributeComboBox = new JComboBox();
    private JLabel horizontalAlignmentLabel = new JLabel();
    private JComboBox horizontalAlignmentComboBox = new JComboBox();

    private JCheckBox showOutlineCheckBox = new JCheckBox();
    private JPanel hideAtScaleButtonPanel = new JPanel();
    private JCheckBox hideAtScaleCheckBox = new JCheckBox();
    private JLabel hideAtScaleLabel = new JLabel();
    private ValidatingTextField hideAtScaleField = new 
	ValidatingTextField("999999",7,
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
    
    public LabelStylePanel(Layer layer, LayerViewPanel layerViewPanel,
        JDialog parent, ErrorHandler errorHandler) {
        try {
            this.parent = parent;
            this.layerViewPanel = layerViewPanel;

            //Populate verticalAlignmentComboBox before calling #setLayer so that
            //initially selected item can be properly set. [Jon Aquino]
            verticalAlignmentComboBox.addItem(LabelStyle.ABOVE_LINE_TEXT);
            verticalAlignmentComboBox.addItem(LabelStyle.ON_LINE_TEXT);
            verticalAlignmentComboBox.addItem(LabelStyle.BELOW_LINE_TEXT);

            horizontalAlignmentComboBox.addItem(LabelStyle.JUSTIFY_CENTER_TEXT);
            horizontalAlignmentComboBox.addItem(LabelStyle.JUSTIFY_LEFT_TEXT);
            horizontalAlignmentComboBox.addItem(LabelStyle.JUSTIFY_RIGHT_TEXT);
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
            outlineWidthField.getDocument().addDocumentListener(new DocumentListener() {
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
            hideAtScaleField.getDocument().addDocumentListener(new DocumentListener() {
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
            
            colorButton.setToolTipText(BROWSE);
            outlineColorButton.setToolTipText(BROWSE);
            fontButton.setToolTipText(BROWSE);
            updateControls();
            // Disabled image in ComboBox and replaced with existing I18N text [LDB 2007-08-27]
//            verticalAlignmentComboBox.setRenderer(new ListCellRenderer() {
//                    private Icon aboveIcon = IconLoader.icon(
//                            "BigLabelAbove.gif");
//                    private Icon onIcon = IconLoader.icon("BigLabelOn.gif");
//                    private Icon belowIcon = IconLoader.icon(
//                            "BigLabelBelow.gif");
//                    private DefaultListCellRenderer renderer = new DefaultListCellRenderer();
//
//                    public Component getListCellRendererComponent(JList list,
//                        Object value, int index, boolean isSelected,
//                        boolean cellHasFocus) {
//                        JLabel label = (JLabel) renderer.getListCellRendererComponent(list,
//                                "", index, isSelected, cellHasFocus);
//                        label.setIcon(value.equals(LabelStyle.ABOVE_LINE)
//                            ? aboveIcon
//                            : (value.equals(LabelStyle.ON_LINE) ? onIcon
//                                                                : belowIcon));
//
//                        return label;
//                    }
//                });
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
        Object attributeValue = getLabelAttribute().equals(LabelStyle.FID_COLUMN)
            ? (firstFeature.getID() + "")
            : firstFeature.getAttribute(getLabelAttribute());

        if (attributeValue == null) {
            return sampleText;
        }

		if (attributeValue instanceof Date) {
			DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT);
			attributeValue = dateFormat.format((Date) attributeValue);
		} else if (attributeValue instanceof Double) {
			NumberFormat numberFormat = NumberFormat.getNumberInstance();
			attributeValue = numberFormat.format(((Double) attributeValue).doubleValue());
		} else if (attributeValue instanceof Integer) {
			NumberFormat numberFormat = NumberFormat.getIntegerInstance();
			attributeValue = numberFormat.format(((Integer) attributeValue).intValue());
		} else if (attributeValue.toString().trim().length() == 0) {
            return sampleText;
        }

        return attributeValue.toString().trim();
    }

    private void setLayer(Layer layer) {
        this.layer = layer;
        LabelStyle labelStyle = layer.getLabelStyle();
        setLabelling(labelStyle.isEnabled());
        setAttributes(layer.getFeatureCollectionWrapper().getFeatureSchema());
        setAttribute(labelStyle.getAttribute());
        setAngleAttribute(labelStyle.getAngleAttribute());
        setHeightAttribute(labelStyle.getHeightAttribute());
        setColor(labelStyle.getColor());
        setLabelFont(labelStyle.getFont());
        setScaling(labelStyle.isScaling());
        setHideAtScale(labelStyle.isHidingAtScale());
        setHideAtScaleField(labelStyle.getScaleToHideAt());
        setOutline(labelStyle.getOutlineShowing());
        
        hideOverlappingLabelsCheckBox.setSelected(labelStyle.isHidingOverlappingLabels());
        heightTextField.setText(labelStyle.getHeight() + "");
        outlineWidthField.setText(labelStyle.getOutlineWidth() + "");
        String va = labelStyle.getVerticalAlignment();
        int index = 0;
        for (index=LabelStyle.verticalAlignmentLookup.length-1; index > 0 && 
        	(!va.equalsIgnoreCase(LabelStyle.verticalAlignmentLookup[index])); index--) ;
        verticalAlignmentComboBox.setSelectedIndex(index);
        horizontalAlignmentComboBox.setSelectedIndex(labelStyle.getHorizontalAlignment());
    }
    
    private void setHideAtScaleField(double scaleField) {
    	hideAtScaleField.setText(scaleField + "");
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

    private void setOutlineColor(Color color) {
        this.outlineColor = color;
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

    private void setOutline(boolean showOutline){
    	showOutlineCheckBox.setSelected(showOutline);
    }

    private void setHideAtScale(boolean hideAtScale){
    	 hideAtScaleCheckBox.setSelected(hideAtScale);
    }
    
    void jbInit() throws Exception {
        border1 = BorderFactory.createEmptyBorder(2, 10, 2, 5);
        this.setLayout(borderLayout1);
        setLayout(gridBagLayout6);
        attributeLabel.setText(LABEL_ATTRIBUTE);
        previewPanel.setBackground(Color.white);
        previewPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        previewPanel.setMaximumSize(new Dimension(200, 38));
        previewPanel.setMinimumSize(new Dimension(200, 38));
        previewPanel.setPreferredSize(new Dimension(200, 38));
        scaleCheckBox.setText(SCALE_LABELS_WITH_THE_ZOOM_LEVEL);
        scaleCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scaleCheckBox_actionPerformed(e);
                }
            });
        hideAtScaleCheckBox.setText(HIDE_LABELS_WHEN); 
        hideAtScaleCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	hideAtScaleCheckBox_actionPerformed(e);
            }
        });
        hideAtScaleLabel.setText(HIDE_AT_SCALE_TEXT);
        showOutlineCheckBox.setText(DRAW_OUTLINE_HALO_AROUND_LABELS);
        showOutlineCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	showOutlineCheckBox_actionPerformed(e);
            }
        });
        setBorder(border1);
        labellingCheckBox.setText(ENABLE_LABELLING);
        labellingCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    enableLabellingCheckBox_actionPerformed(e);
                }
            });
        heightLabel.setText(HEIGHT);
        attributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    attributeComboBox_actionPerformed(e);
                }
            });
        outlineLabel.setText(OUTLINE_WIDTH);
        angleAttributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    angleAttributeComboBox_actionPerformed(e);
                }
            });
        previewLabel.setText(PREVIEW_AT_CURRENT_ZOOM_LEVEL);
        buttonPanel.setLayout(gridBagLayout2);
        outlineButtonPanel.setLayout(gridBagLayout2);
        colorButton.setText(CHANGE_COLOUR);
        colorButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    colorButton_actionPerformed(e);
                }
            });
        outlineColorButton.setText(CHANGE_COLOUR);
        outlineColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	outlineColorButton_actionPerformed(e);
            }
        });
        fontButton.setText(CHANGE_FONT);
        fontButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fontButton_actionPerformed(e);
                }
            });
        verticalAlignmentLabel.setText(VERTICAL_ALIGNMENT_FOR_POINTS_AND_LINES);
        verticalAlignmentComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    verticalAlignmentComboBox_actionPerformed(e);
                }
            });
        horizontalAlignmentLabel.setText(HORIZONTAL_ALIGNMENT_FOR_POINTS_AND_LINES);
        horizontalAlignmentComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	horizontalAlignmentComboBox_actionPerformed(e);
                }
            });
        angleLabel.setText(ANGLE_ATTRIBUTE_DEGREES);
        hideOverlappingLabelsCheckBox.setText(HIDE_OVERLAPPING_LABELS);
        hideOverlappingLabelsCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    hideOverlappingLabelsCheckBox_actionPerformed(e);
                }
            });
        heightAttributeLabel.setText(HEIGHT_ATTRIBUTE);
        heightAttributeComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    heightAttributeComboBox_actionPerformed(e);
                }
            });
        int row = 0;
        add(labellingCheckBox,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        add(attributeLabel,
            new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        add(attributeComboBox,
            new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 4, 2, 0), 0, 0));
        this.add(verticalAlignmentLabel,
            new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(verticalAlignmentComboBox,
            new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 4, 2, 0), 0, 0));
        this.add(horizontalAlignmentLabel,
		    new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
		        GridBagConstraints.WEST, GridBagConstraints.NONE,
		        new Insets(0, 0, 0, 0), 0, 0));
		this.add(horizontalAlignmentComboBox,
		    new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
		        GridBagConstraints.EAST, GridBagConstraints.NONE,
		        new Insets(0, 4, 2, 0), 0, 0));
        this.add(angleLabel,
            new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(angleAttributeComboBox,
            new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 4, 2, 0), 0, 0));
        this.add(heightAttributeLabel,
            new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(heightAttributeComboBox,
            new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0,
	            GridBagConstraints.EAST, GridBagConstraints.NONE, 
	            new Insets(0, 4, 2, 0), 0, 0));
        this.add(heightTextField,
            new GridBagConstraints(1, row, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
            	new Insets(0, 4, 0, 0), 0, 0));
        this.add(heightLabel,
            new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(scaleCheckBox,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        
        hideAtScaleButtonPanel.setLayout(gridBagLayout2);
        this.add(hideAtScaleButtonPanel,
        		new GridBagConstraints(0, row++, 4, 1, 0.0, 0.0,
        				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        				new Insets(0, 0, 0, 0), 0, 0));
        hideAtScaleButtonPanel.add(hideAtScaleCheckBox,
        		new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.WEST, GridBagConstraints.NONE,
        				new Insets(0, 0, 0, 0), 0, 0));
        hideAtScaleButtonPanel.add(hideAtScaleLabel,
        		new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.EAST, GridBagConstraints.NONE,
        				new Insets(0, 0, 0, 0), 0, 0));
        hideAtScaleButtonPanel.add(hideAtScaleField,
        		new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.WEST, GridBagConstraints.NONE, 
        				new Insets(0, 4, 0, 0), 0, 0));
        hideAtScaleButtonPanel.add(new JPanel(),
        		new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,
        				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        				new Insets(0, 0, 0, 0), 0, 0));

        this.add(hideOverlappingLabelsCheckBox,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(buttonPanel,
        		new GridBagConstraints(0, row++, 3, 1, 0.0, 0.0,
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
       
        this.add(showOutlineCheckBox,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(outlineButtonPanel,
        		new GridBagConstraints(0, row++, 3, 1, 0.0, 0.0,
        				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        				new Insets(0, 0, 0, 0), 0, 0));
        outlineButtonPanel.add(outlineColorButton,
        		new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.CENTER, GridBagConstraints.NONE,
        				new Insets(0, 2, 0, 2), 0, 0));
        outlineButtonPanel.add(outlineLabel,
        		new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.CENTER, GridBagConstraints.NONE,
        				new Insets(2, 2, 2, 2), 0, 0));
        outlineButtonPanel.add(outlineWidthField,
        		new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
        				GridBagConstraints.EAST, GridBagConstraints.NONE, 
        				new Insets(0, 4, 0, 0), 0, 0));

        this.add(previewLabel,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(previewPanel,
            new GridBagConstraints(0, row++, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 10, 0, 4), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(98, 104, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
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
        labelStyle.setOutlineColor(outlineColor);
        labelStyle.setFont(labelFont);
        labelStyle.setScaling(scaleCheckBox.isSelected());
        labelStyle.setHideAtScale(hideAtScaleCheckBox.isSelected());
        labelStyle.setOutlineShowing(showOutlineCheckBox.isSelected());
        labelStyle.setHidingOverlappingLabels(hideOverlappingLabelsCheckBox.isSelected());
        labelStyle.setHeight(getLabelHeight());
        labelStyle.setOutlineWidth(getOutlineWidth());
        labelStyle.setScaleToHideAt(getScaleToHideAt());
        //labelStyle.setVerticalAlignment((String) verticalAlignmentComboBox.getSelectedItem());
        labelStyle.setVerticalAlignment(LabelStyle.verticalAlignmentLookup[verticalAlignmentComboBox.getSelectedIndex()]);
        labelStyle.setHorizontalAlignment(horizontalAlignmentComboBox.getSelectedIndex());

        return labelStyle;
    }

    private double getLabelHeight() {
        return (heightTextField.getText().length() == 0)
        ? LabelStyle.FONT_BASE_SIZE
        : Double.parseDouble(heightTextField.getText());
    }

    private double getOutlineWidth() {
        return (outlineWidthField.getText().length() == 0)
        ? LabelStyle.FONT_BASE_SIZE
        : Double.parseDouble(outlineWidthField.getText());
    }
    
    private double getScaleToHideAt() {
        double scale = (hideAtScaleField.getText().length() == 0)
        ? 20000d
        : Double.parseDouble(hideAtScaleField.getText());
        return scale;
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
        showOutlineCheckBox.setEnabled(labellingCheckBox.isSelected());
        outlineWidthField.setEnabled(labellingCheckBox.isSelected() &&
        		showOutlineCheckBox.isSelected());
        outlineLabel.setEnabled(labellingCheckBox.isSelected() &&
        		showOutlineCheckBox.isSelected());
        outlineColorButton.setEnabled(labellingCheckBox.isSelected() &&
        		showOutlineCheckBox.isSelected());
        scaleCheckBox.setEnabled(labellingCheckBox.isSelected());
        hideOverlappingLabelsCheckBox.setEnabled(labellingCheckBox.isSelected());
        hideAtScaleCheckBox.setEnabled(labellingCheckBox.isSelected());
        hideAtScaleLabel.setEnabled(labellingCheckBox.isSelected() &&
        		hideAtScaleCheckBox.isSelected());
        hideAtScaleField.setEnabled(labellingCheckBox.isSelected() &&
        		hideAtScaleCheckBox.isSelected());
        previewLabel.setEnabled(labellingCheckBox.isSelected());
        previewPanel.setEnabled(labellingCheckBox.isSelected());
        verticalAlignmentLabel.setEnabled(labellingCheckBox.isSelected());
        verticalAlignmentComboBox.setEnabled(labellingCheckBox.isSelected());
        horizontalAlignmentLabel.setEnabled(labellingCheckBox.isSelected());
        horizontalAlignmentComboBox.setEnabled(labellingCheckBox.isSelected());
    }
    void colorButton_actionPerformed(ActionEvent e) {
        Color newColor = JColorChooser.showDialog(this, CHOOSE_COLOUR, color);

        if (newColor == null) {
            return;
        }

        setColor(newColor);
        updateControls();
    }

    void outlineColorButton_actionPerformed(ActionEvent e) {
        Color newColor = JColorChooser.showDialog(this, CHOOSE_COLOUR, color);
        if (newColor == null) {
            return;
        }
        setOutlineColor(newColor);
        updateControls();
    }
    void fontButton_actionPerformed(ActionEvent e) {
        Font newFont = FontChooser.showDialog(parent, CHOOSE_FONT, labelFont);

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
    void hideAtScaleCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
    void showOutlineCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
    
    void verticalAlignmentComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
    
    void horizontalAlignmentComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void hideOverlappingLabelsCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void heightAttributeComboBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
}
