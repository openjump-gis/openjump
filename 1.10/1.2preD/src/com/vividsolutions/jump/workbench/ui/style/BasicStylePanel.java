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
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.TransparencyPanel;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicFillPattern;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.FillPatternFactory;


public class BasicStylePanel extends JPanel {
    protected static final int SLIDER_TEXT_FIELD_COLUMNS = 3;
    protected static final Dimension SLIDER_DIMENSION = new Dimension(130, 49);
    private Paint[] fillPatterns = new FillPatternFactory().createFillPatterns();
    protected JPanel centerPanel = new JPanel();
    private AbstractPalettePanel palettePanel;
    protected JCheckBox fillCheckBox = new JCheckBox();
    protected JCheckBox lineCheckBox = new JCheckBox();
    protected TransparencyPanel transparencyPanel = new TransparencyPanel();
    protected JLabel transparencyLabel = new JLabel();
    protected ColorChooserPanel lineColorChooserPanel = new ColorChooserPanel();
    protected ColorChooserPanel fillColorChooserPanel = new ColorChooserPanel();
    protected JLabel lineWidthLabel = new JLabel();
    protected JCheckBox synchronizeCheckBox = new JCheckBox();
    private JCheckBox linePatternCheckBox = new JCheckBox();
    private JCheckBox fillPatternCheckBox = new JCheckBox();
    private String[] linePatterns = new String[] {
            "1", "3", "5", "5,1", "7", "7,12", "9", "9,2", "15,6", "20,3"
        };
    private JComboBox linePatternComboBox = new JComboBox(linePatterns) {

            {
                final ValidatingTextField.Cleaner cleaner = new ValidatingTextField.Cleaner() {
                        public String clean(String text) {
                            String pattern = "";
                            StringTokenizer tokenizer = new StringTokenizer(StringUtil.replaceAll(
                                        text, ",", " "));

                            while (tokenizer.hasMoreTokens()) {
                                pattern += (tokenizer.nextToken() + " ");
                            }

                            return StringUtil.replaceAll(pattern.trim(), " ",
                                ",");
                        }
                    };

                BasicComboBoxEditor editor = new BasicComboBoxEditor();
                setEditor(editor);
                setEditable(true);
                addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateControls();
                        }
                    });
                ValidatingTextField.installValidationBehavior((JTextField) editor.getEditorComponent(),
                    new ValidatingTextField.Validator() {
                        public boolean isValid(String text) {
                            try {
                                BasicStyle.toArray(cleaner.clean(text), 1);

                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    }, cleaner);
                ((JTextField) editor.getEditorComponent()).getDocument()
                 .addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) {
                            updateControls();
                        }

                        public void insertUpdate(DocumentEvent e) {
                            updateControls();
                        }

                        public void removeUpdate(DocumentEvent e) {
                            updateControls();
                        }
                    });
                setRenderer(new ListCellRenderer() {
                        private JPanel panel = new JPanel() {
                                private int lineWidth = 2;

                                protected void paintComponent(Graphics g) {
                                    super.paintComponent(g);

                                    Graphics2D g2 = (Graphics2D) g;
                                    g2.setStroke(new BasicStroke(lineWidth,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_BEVEL, 1.0f,
                                            BasicStyle.toArray(linePattern,
                                                lineWidth), 0));
                                    g2.draw(new Line2D.Double(0,
                                            panel.getHeight() / 2.0,
                                            panel.getWidth(),
                                            panel.getHeight() / 2.0));
                                }
                            };

                        private String linePattern;

                        public Component getListCellRendererComponent(
                            JList list, Object value, int index,
                            boolean isSelected, boolean cellHasFocus) {
                            linePattern = (String) value;
                            panel.setForeground(UIManager.getColor(isSelected
                                    ? "ComboBox.selectionForeground"
                                    : "ComboBox.foreground"));
                            panel.setBackground(UIManager.getColor(isSelected
                                    ? "ComboBox.selectionBackground"
                                    : "ComboBox.background"));

                            return panel;
                        }
                    });
            }
        };

    private JComboBox fillPatternComboBox = new JComboBox(fillPatterns) {

            {
                setMaximumRowCount(24);
                setEditable(false);
                addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            updateControls();
                        }
                    });
                setRenderer(new ListCellRenderer() {
                        private Paint fillPattern;
                        private JLabel label = new JLabel(" ");
                        private JPanel panel = new JPanel(new BorderLayout()) {

                                {
                                    label.setPreferredSize(new Dimension(150,
                                            (int) label.getPreferredSize()
                                                       .getHeight()));
                                    add(label, BorderLayout.CENTER);
                                }

                                protected void paintComponent(Graphics g) {
                                    super.paintComponent(g);
                                    ((Graphics2D) g).setPaint(fillPattern);
                                    ((Graphics2D) g).fill(new Rectangle2D.Double(
                                            0, 0, getWidth(), getHeight()));
                                }
                            };

                        public Component getListCellRendererComponent(
                            JList list, Object value, int index,
                            boolean isSelected, boolean cellHasFocus) {
                            fillPattern = (Paint) value;
                            label.setText("" +
                                (1 +
                                CollectionUtil.indexOf(fillPattern, fillPatterns)));
                            label.setForeground(UIManager.getColor(isSelected
                                    ? "ComboBox.selectionForeground"
                                    : "ComboBox.foreground"));
                            panel.setBackground(UIManager.getColor(isSelected
                                    ? "ComboBox.selectionBackground"
                                    : "ComboBox.background"));

                            return panel;
                        }
                    });
            }
        };

    protected JSlider lineWidthSlider = new JSlider() {

            {
                addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            updateControls();
                        }
                    });
            }
        };

    private Blackboard blackboard;

    /**
     * Parameterless constructor for JBuilder GUI designer.
     */
    public BasicStylePanel() {
        this(null, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    }

    public BasicStylePanel(Blackboard blackboard,
        int palettePanelVerticalScrollBarPolicy) {
        this.blackboard = blackboard;
        palettePanel = new ListPalettePanel(palettePanelVerticalScrollBarPolicy);

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assert.shouldNeverReachHere();
        }

        transparencyPanel.getSlider().getModel().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateControls();
                }
            });
        palettePanel.add(new GridPalettePanel.Listener() {
                public void basicStyleChosen(BasicStyle basicStyle) {
                    //Preserve some settings e.g. line and fill patterns, alpha;
                    BasicStyle newBasicStyle = getBasicStyle();
                    newBasicStyle.setFillColor(basicStyle.getFillColor());
                    newBasicStyle.setLineColor(basicStyle.getLineColor());
                    newBasicStyle.setLineWidth(basicStyle.getLineWidth());
                    newBasicStyle.setLinePattern(basicStyle.getLinePattern());
                    newBasicStyle.setRenderingLinePattern(basicStyle.isRenderingLinePattern());
                    newBasicStyle.setRenderingFill(basicStyle.isRenderingFill());
                    newBasicStyle.setRenderingLine(basicStyle.isRenderingLine());
                    setBasicStyle(newBasicStyle);
                }
            });
        updateControls();
    }

    /**
     * Remove extra commas
     */
    private String clean(String linePattern) {
        String pattern = "";
        StringTokenizer tokenizer = new StringTokenizer(StringUtil.replaceAll(
                    linePattern, ",", " "));

        while (tokenizer.hasMoreTokens()) {
            pattern += (tokenizer.nextToken() + " ");
        }

        return StringUtil.replaceAll(pattern.trim(), " ", ",");
    }

    //UT made it protected for testing
    protected void jbInit() throws Exception {
	    lineWidthSlider.setPreferredSize(SLIDER_DIMENSION);
	    lineWidthSlider.setPaintLabels(true);
	    lineWidthSlider.setValue(1);
	    lineWidthSlider.setLabelTable(lineWidthSlider.createStandardLabels(10));
	    lineWidthSlider.setMajorTickSpacing(5);
	    lineWidthSlider.setMaximum(30);
	    lineWidthSlider.setMinorTickSpacing(1);
	    setLayout(new GridBagLayout());
	    linePatternCheckBox.setText(I18N.get("ui.style.BasicStylePanel.line-pattern"));
	    fillPatternCheckBox.setText(I18N.get("ui.style.BasicStylePanel.fill-pattern"));
	    linePatternCheckBox.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                linePatternCheckBox_actionPerformed(e);
	            }
	        });
	    fillPatternCheckBox.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                fillPatternCheckBox_actionPerformed(e);
	            }
	        });
	    add(centerPanel,
	        new GridBagConstraints(0, 0, 1, 2, 0, 0, GridBagConstraints.WEST,
	            GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
	    add(new JPanel(),
	        new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST,
	            GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
	    add(new JLabel(I18N.get("ui.style.BasicStylePanel.presets")),
	        new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
	            GridBagConstraints.NONE, new Insets(0, 30, 0, 0), 0, 0));
	    add(palettePanel,
	        new GridBagConstraints(2, 1, 1, 1, 0, 1, GridBagConstraints.WEST,
	            GridBagConstraints.VERTICAL, new Insets(0, 30, 0, 0), 0, 0));
	    centerPanel.setLayout(new GridBagLayout());
	    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    fillColorChooserPanel.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                fillColorChooserPanel_actionPerformed(e);
	            }
	        });
	    lineColorChooserPanel.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                lineColorChooserPanel_actionPerformed(e);
	            }
	        });
	    synchronizeCheckBox.setText(I18N.get("ui.style.BasicStylePanel.sync-line-colour-with-fill-colour"));
	    synchronizeCheckBox.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                synchronizeCheckBox_actionPerformed(e);
	            }
	        });
	    fillCheckBox.setText(I18N.get("ui.style.BasicStylePanel.fill"));
	    fillCheckBox.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                fillCheckBox_actionPerformed(e);
	            }
	        });
	    lineCheckBox.setText(I18N.get("ui.style.BasicStylePanel.line"));
	    lineCheckBox.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                lineCheckBox_actionPerformed(e);
	            }
	        });
	    centerPanel.add(GUIUtil.createSyncdTextField(
	            transparencyPanel.getSlider(), SLIDER_TEXT_FIELD_COLUMNS),
	        new GridBagConstraints(2, 21, 1, 1, 0.0, 0.0,
	            GridBagConstraints.CENTER, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(lineWidthSlider,
	        new GridBagConstraints(1, 19, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(GUIUtil.createSyncdTextField(lineWidthSlider,
	            SLIDER_TEXT_FIELD_COLUMNS),
	        new GridBagConstraints(2, 19, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    fillColorChooserPanel.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                fillColorChooserPanel_actionPerformed(e);
	            }
	        });
	    lineWidthLabel.setText(I18N.get("ui.style.BasicStylePanel.line-width"));
	    transparencyLabel.setText(I18N.get("ui.style.BasicStylePanel.transparency"));
	    centerPanel.add(synchronizeCheckBox,
	        new GridBagConstraints(0, 18, 3, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(transparencyLabel,
	        new GridBagConstraints(0, 21, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(fillColorChooserPanel,
	        new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(lineColorChooserPanel,
	        new GridBagConstraints(1, 11, 2, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(transparencyPanel,
	        new GridBagConstraints(1, 21, 1, 1, 0.0, 0.0,
	            GridBagConstraints.CENTER, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(fillCheckBox,
	        new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(lineCheckBox,
	        new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(lineWidthLabel,
	        new GridBagConstraints(0, 19, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(linePatternCheckBox,
	        new GridBagConstraints(0, 16, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(fillPatternCheckBox,
	        new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(linePatternComboBox,
	        new GridBagConstraints(1, 16, 2, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 2, 2), 0, 0));
	    centerPanel.add(fillPatternComboBox,
	        new GridBagConstraints(1, 7, 2, 1, 0.0, 0.0,
	            GridBagConstraints.WEST, GridBagConstraints.NONE,
	            new Insets(2, 2, 0, 2), 0, 0));
	}

    public JSlider getTransparencySlider() {
        return transparencyPanel.getSlider();
    }

    protected void setAlpha(int alpha) {
        transparencyPanel.getSlider().setValue(255 - alpha);
    }

    protected int getAlpha() {
        return 255 - transparencyPanel.getSlider().getValue();
    }

    public void setBasicStyle(BasicStyle basicStyle) {
        addCustomFillPatterns();
        fillColorChooserPanel.setColor(basicStyle.getFillColor());
        lineColorChooserPanel.setColor(basicStyle.getLineColor());
        setAlpha(basicStyle.getAlpha());
        fillCheckBox.setSelected(basicStyle.isRenderingFill());
        lineCheckBox.setSelected(basicStyle.isRenderingLine());
        lineWidthSlider.setValue(basicStyle.getLineWidth());
        linePatternCheckBox.setSelected(basicStyle.isRenderingLinePattern());
        fillPatternCheckBox.setSelected(basicStyle.isRenderingFillPattern());
        linePatternComboBox.setSelectedItem(basicStyle.getLinePattern());

        //Update fill pattern colors before finding the basic style's current fill
        //pattern in the combobox. [Jon Aquino]
        updateFillPatternColors();

        //Because fillPatternComboBox is not editable, we must use findEquivalentItem,
        //otherwise the combobox gets confused and a stack overflow occurs
        //if the two items are equal but not == . [Jon Aquino]

				Object fill = findEquivalentItem(basicStyle.getFillPattern(), fillPatternComboBox);

				if (fill != null)
					fillPatternComboBox.setSelectedItem(fill);

        updateControls();
    }

    private void addCustomFillPatterns() {
        for (Iterator i = ((Collection) blackboard.get(
                    FillPatternFactory.CUSTOM_FILL_PATTERNS_KEY, new ArrayList())).iterator();
                i.hasNext();) {
            Paint fillPattern = (Paint) i.next();

            if (null == findEquivalentItem(fillPattern, fillPatternComboBox)) {
                //Clone it because several BasicStylePanels access the collection of
                //custom fill patterns [Jon Aquino]
                ((DefaultComboBoxModel) fillPatternComboBox.getModel()).addElement(cloneIfBasicFillPattern(fillPattern));
            }
        }
    }

    private Object findEquivalentItem(Object item, JComboBox comboBox) {

				if (comboBox == null)
					return null;

				if (item == null) {
					return comboBox.getItemCount() > 0
						? comboBox.getItemAt(0)
						: null;
				}

        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (item.equals(comboBox.getItemAt(i))) {
                return comboBox.getItemAt(i);
            }
        }

        return null;
    }

    public BasicStyle getBasicStyle() {
        BasicStyle basicStyle = new BasicStyle();
        basicStyle.setFillColor(fillColorChooserPanel.getColor());
        basicStyle.setLineColor(lineColorChooserPanel.getColor());
        basicStyle.setAlpha(getAlpha());
        basicStyle.setRenderingFill(fillCheckBox.isSelected());
        basicStyle.setRenderingLine(lineCheckBox.isSelected());
        basicStyle.setRenderingLinePattern(linePatternCheckBox.isSelected());
        basicStyle.setRenderingFillPattern(fillPatternCheckBox.isSelected());
        basicStyle.setLinePattern(clean((String) linePatternComboBox.getEditor()
                                                                    .getItem()));
        basicStyle.setFillPattern(cloneIfBasicFillPattern((Paint)fillPatternComboBox.getSelectedItem()));
        basicStyle.setLineWidth(lineWidthSlider.getValue());

        return basicStyle;
    }

    private Paint cloneIfBasicFillPattern(Paint fillPattern) {
        return (fillPattern instanceof BasicFillPattern)
        ? (Paint) ((BasicFillPattern) fillPattern).clone() : fillPattern;
    }

    protected void setFillColor(Color newColor) {
        fillColorChooserPanel.setColor(newColor);
        transparencyPanel.setColor(newColor);
    }

    protected void updateControls() {
        linePatternComboBox.setEnabled(linePatternCheckBox.isSelected());
        fillPatternComboBox.setEnabled(fillPatternCheckBox.isSelected());
        lineColorChooserPanel.setEnabled(lineCheckBox.isSelected());
        fillColorChooserPanel.setEnabled(fillCheckBox.isSelected());
        fillColorChooserPanel.setAlpha(getAlpha());
        lineColorChooserPanel.setAlpha(getAlpha());
        palettePanel.setAlpha(getAlpha());
        transparencyPanel.setColor((lineCheckBox.isSelected() &&
            !fillCheckBox.isSelected()) ? lineColorChooserPanel.getColor()
                                        : fillColorChooserPanel.getColor());
        updateFillPatternColors();
        fillPatternComboBox.repaint();
    }

    private void updateFillPatternColors() {
        //Iterate through combo box contents rather than fillPatterns field, because
        //the combo box contents = fillPatterns + customFillPatterns [Jon Aquino]
        for (int i = 0; i < fillPatternComboBox.getItemCount(); i++) {
            if (fillPatternComboBox.getItemAt(i) instanceof BasicFillPattern) {
                ((BasicFillPattern) fillPatternComboBox.getItemAt(i)).setColor(GUIUtil.alphaColor(
                        fillColorChooserPanel.getColor(), getAlpha()));
            }
        }
    }

    void fillCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void fillColorChooserPanel_actionPerformed(ActionEvent e) {
        if (synchronizeCheckBox.isSelected()) {
            syncLineColor();
        }

        updateControls();
    }

    private void syncLineColor() {
        lineColorChooserPanel.setColor(fillColorChooserPanel.getColor().darker());
    }

    void lineColorChooserPanel_actionPerformed(ActionEvent e) {
        if (synchronizeCheckBox.isSelected()) {
            fillColorChooserPanel.setColor(lineColorChooserPanel.getColor()
                                                                .brighter());
        }

        updateControls();
    }

    void lineCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    public void setSynchronizingLineColor(boolean newSynchronizingLineColor) {
        synchronizeCheckBox.setSelected(newSynchronizingLineColor);
    }

    protected void synchronizeCheckBox_actionPerformed(ActionEvent e) {
        if (synchronizeCheckBox.isSelected()) {
            syncLineColor();
        }

        updateControls();
    }

    public JCheckBox getSynchronizeCheckBox() {
        return synchronizeCheckBox;
    }

    void linePatternCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    void fillPatternCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }
}
