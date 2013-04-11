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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @deprecated  As of release 1.3, replaced by {@link de.latlon.deejump.plugin.style.DeeRenderingStylePanel}
 */
public class RenderingStylePanel extends BasicStylePanel implements StylePanel {
    private Layer layer;
    //private JTextArea fillPatternTipLabel = new JTextArea();    
    private JCheckBox vertexCheckBox = new JCheckBox();
    private JSlider vertexSlider = new JSlider() {

            {
                addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            updateControls();
                        }
                    });
            }
        };

    private JPanel previewPanel = new JPanel() {

            {
                setBackground(Color.white);
                setBorder(BorderFactory.createLoweredBevelBorder());
                setMaximumSize(new Dimension(200, 38));
                setMinimumSize(new Dimension(200, 38));
                setPreferredSize(new Dimension(200, 38));
            }

            private LayerViewPanel dummyLayerViewPanel = new LayerViewPanel(new LayerManager(),
                    new LayerViewPanelContext() {
                        public void setStatusMessage(String message) {
                        }

                        public void warnUser(String warning) {
                        }

                        public void handleThrowable(Throwable t) {
                        }
                    });

            //Enough of a viewport to satisfy the two styles [Jon Aquino]
            private Viewport viewport = new Viewport(dummyLayerViewPanel) {
                    private AffineTransform transform = new AffineTransform();

                    public Envelope getEnvelopeInModelCoordinates() {
                        return new Envelope(0, 200, 0, 40);
                    }

                    public AffineTransform getModelToViewTransform() {
                        return transform;
                    }

                    public Point2D toViewPoint(Coordinate modelCoordinate) {
                        return new Point2D.Double(modelCoordinate.x,
                            modelCoordinate.y);
                    }
                };

            private void paint(Style style, Graphics2D g) {
                Stroke originalStroke = g.getStroke();

                try {
                    style.paint(feature, g, viewport);
                } catch (Exception e) {
                    //Eat it [Jon Aquino]
                } finally {
                    //Restore original stroke. Otherwise preview-panel's borders
                    //will have dashes. [Jon Aquino]
                    g.setStroke(originalStroke);
                }
            }

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                paint(getBasicStyle(), (Graphics2D) g);

                if (vertexCheckBox.isSelected()) {
                    VertexStyle vertexStyle = getVertexStyle();

                    //Ensure the vertex colour shown on the preview panel stays
                    //up to date. [Jon Aquino]
                    vertexStyle.initialize(new Layer() {
                            public BasicStyle getBasicStyle() {
                                return RenderingStylePanel.this.getBasicStyle();
                            }
                        });
                    paint(vertexStyle, (Graphics2D) g);
                }
            }

            private Feature feature = createFeature();

            private Feature createFeature() {
                try {
                    return FeatureUtil.toFeature(new WKTReader().read(
                            "POLYGON ((-200 80, 100 20, 400 -40, 400 80, -200 80))"),
                        new FeatureSchema() {
                            private static final long serialVersionUID = -8627306219650589202L;
                            {
                                addAttribute("GEOMETRY", AttributeType.GEOMETRY);
                            }
                        });
                } catch (ParseException e) {
                    Assert.shouldNeverReachHere();

                    return null;
                }
            }
        };

    /**
     * Parameterless constructor for JBuilder GUI designer.
     */
    public RenderingStylePanel() {
    }

    public RenderingStylePanel(Blackboard blackboard, Layer layer) {
        super(blackboard, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(5), new JLabel("5"));
        labelTable.put(new Integer(10), new JLabel("10"));
        labelTable.put(new Integer(15), new JLabel("15"));
        labelTable.put(new Integer(20), new JLabel("20"));
        vertexSlider.setLabelTable(labelTable);
        setBasicStyle(layer.getBasicStyle());

        try {
            jbInit();
            updateControls();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Set layer after #jbInit, because both methods initialize the components. [Jon Aquino]
        setLayer(layer);
    }

    public void updateControls() {
        super.updateControls();

        if (vertexSlider == null) {
            //Get here during superclass initialization. [Jon Aquino]
            return;
        }

        previewPanel.repaint();
        vertexSlider.setEnabled(vertexCheckBox.isSelected());

        for (Enumeration e = vertexSlider.getLabelTable().elements();
                e.hasMoreElements();) {
            JLabel label = (JLabel) e.nextElement();
            label.setEnabled(vertexCheckBox.isSelected());
        }
    }

    public String getTitle() {
        return I18N.get("ui.style.RenderingStylePanel.rendering");
    }

    private void setLayer(Layer layer) {
        this.layer = layer;
        setSynchronizingLineColor(layer.isSynchronizingLineColor());
        vertexCheckBox.setSelected(layer.getVertexStyle().isEnabled());
        vertexSlider.setValue(layer.getVertexStyle().getSize());
    }

    //UT made protected
    protected void jbInit() throws Exception {
        if (vertexSlider == null) {
            //Get here during superclass initialization. [Jon Aquino]
            super.jbInit();

            return;
        }

        vertexCheckBox.setText(I18N.get("ui.style.RenderingStylePanel.vertices-size"));
        vertexCheckBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showVerticesCheckBox_actionPerformed(e);
                }
            });
        vertexSlider.setMinorTickSpacing(1);
        vertexSlider.setMajorTickSpacing(0);
        vertexSlider.setPaintLabels(true);
        vertexSlider.setMinimum(4);
        vertexSlider.setValue(4);
        vertexSlider.setMaximum(20);
        vertexSlider.setSnapToTicks(true);
        vertexSlider.setPreferredSize(SLIDER_DIMENSION);
//        fillPatternTipLabel.setFont(new java.awt.Font("SansSerif", 2, 10));
//        fillPatternTipLabel.setOpaque(false);
//        fillPatternTipLabel.setEditable(false);
//        fillPatternTipLabel.setText(I18N.get("ui.style.RenderingStylePanel.tip-after-selecting-a-pattern-use-your-keyboard"));
//        fillPatternTipLabel.setLineWrap(true);
//        fillPatternTipLabel.setWrapStyleWord(true);
        
        centerPanel.add(vertexSlider,
            new GridBagConstraints(1, 35, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(GUIUtil.createSyncdTextField(vertexSlider, SLIDER_TEXT_FIELD_COLUMNS),
            new GridBagConstraints(2, 35, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(vertexCheckBox,
            new GridBagConstraints(0, 35, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(new JLabel(I18N.get("ui.style.RenderingStylePanel.preview")),
            new GridBagConstraints(0, 40, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(2, 2, 0, 2), 0, 0));
        centerPanel.add(previewPanel,
            new GridBagConstraints(0, 45, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 10, 0, 2), 0, 0));
//        centerPanel.add(fillPatternTipLabel,     new GridBagConstraints(0, 8, 3, 1, 0.0, 0.0
//            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));                
    }

    public VertexStyle getVertexStyle() {
        VertexStyle vertexStyle = (VertexStyle) layer.getVertexStyle().clone();
        vertexStyle.setEnabled(vertexCheckBox.isSelected());
        vertexStyle.setSize(vertexSlider.getValue());

        return vertexStyle;
    }

    public void updateStyles() {
        boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);

        try {
            layer.removeStyle(layer.getBasicStyle());
            layer.addStyle(getBasicStyle());

            //Call #getVertexStyle before removing layer's vertex style,
            //because one depends on the other. [Jon Aquino]
            VertexStyle newVertexStyle = getVertexStyle();
            layer.removeStyle(layer.getVertexStyle());
            layer.addStyle(newVertexStyle);
            layer.setSynchronizingLineColor(synchronizeCheckBox.isSelected());
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }

        layer.fireAppearanceChanged();
    }

    void showVerticesCheckBox_actionPerformed(ActionEvent e) {
        updateControls();
    }

    public String validateInput() {
        return null;
    }
}
