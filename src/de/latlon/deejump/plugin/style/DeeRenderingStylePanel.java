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
package de.latlon.deejump.plugin.style;

import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.SquareVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.XBasicStyle;
import com.vividsolutions.jump.workbench.ui.style.BasicStylePanel;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

/**
 * <code>DeeRenderingStylePanel</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date: 2008-01-30 15:42:50 +0100 (Wed, 30 Jan
 *          2008) $
 */
public class DeeRenderingStylePanel extends BasicStylePanel implements
        StylePanel {

    private static final long serialVersionUID = 2657390245955765563L;

    private Layer layer;

    boolean initialized = false;

    final private JCheckBox vertexCheckBox = new JCheckBox();

    final private VertexStyleChooser vertexStyleChooser = new VertexStyleChooser(
            false);

    final private JSlider vertexSlider = new JSlider();

    final private JTextField vertexSize = new JTextField();

    final private JPanel previewPanel = new JPanel() {

        private static final long serialVersionUID = -2316761329707400966L;

        {
            setBackground(Color.white);
            setBorder(BorderFactory.createLoweredBevelBorder());
            setMaximumSize(new Dimension(200, 40));
            setMinimumSize(new Dimension(200, 40));
            setPreferredSize(new Dimension(200, 40));
        }

        private final LayerViewPanel dummyLayerViewPanel = new LayerViewPanel(
                new LayerManager(), new LayerViewPanelContext() {
                    @Override
                    public void setStatusMessage(String message) {
                    }

                    @Override
                    public void warnUser(String warning) {
                    }

                    @Override
                    public void handleThrowable(Throwable t) {
                    }
                });

        // Enough of a viewport to satisfy the two styles [Jon Aquino]
        private final Viewport viewport = new Viewport(dummyLayerViewPanel) {
            private final AffineTransform transform = new AffineTransform();

            @Override
            public Envelope getEnvelopeInModelCoordinates() {
                return new Envelope(0, 200, 0, 40);
            }

            @Override
            public AffineTransform getModelToViewTransform() {
                return transform;
            }

            @Override
            public Point2D toViewPoint(Coordinate modelCoordinate) {
                return new Point2D.Double(modelCoordinate.x, modelCoordinate.y);
            }
        };

        private void paint(Style style, Graphics2D g) {
            final Stroke originalStroke = g.getStroke();

            try {
                style.paint(feature, g, viewport);
            } catch (final Exception e) {
                // Eat it [Jon Aquino]
            } finally {
                // Restore original stroke. Otherwise preview-panel's borders
                // will have dashes. [Jon Aquino]
                g.setStroke(originalStroke);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            paint(getBasicStyle(), (Graphics2D) g);

            if (vertexCheckBox.isSelected()) {
                final VertexStyle vertexStyle = getVertexStyle();
                // unknown why this was called here:
                // vertexStyleChooser.setSelectedStyle(getCurrentVertexStyle());

                // Ensure the vertex colour shown on the preview panel stays
                // up to date. [Jon Aquino]
                vertexStyle.initialize(new Layer() {
                    @Override
                    public BasicStyle getBasicStyle() {
                        return DeeRenderingStylePanel.this.getBasicStyle();
                    }
                });
                paint(vertexStyle, (Graphics2D) g);
            }
        }

        private final Feature feature = createFeature();

        private Feature createFeature() {
            try {
                return FeatureUtil
                        .toFeature(
                                new WKTReader()
                                        .read("POLYGON ((-200 80, 100 20, 400 -40, 400 80, -200 80))"),
                                new FeatureSchema() {
                                    private static final long serialVersionUID = -8627306219650589202L;
                                    {
                                        addAttribute("GEOMETRY",
                                                AttributeType.GEOMETRY);
                                    }
                                });
            } catch (final ParseException e) {
                Assert.shouldNeverReachHere();

                return null;
            }
        }
    };

    /**
     * Parameterless constructor for JBuilder GUI designer.
     */
    public DeeRenderingStylePanel() {
        vertexStyleChooser.sizeSlider = vertexSlider;
    }

    // GH 2005.09.22 this Methode returns the current VertexStyle
    String getCurrentVertexStyle() {
        final VertexStyle currentVertexStyle = layer.getVertexStyle();
        if (currentVertexStyle instanceof SquareVertexStyle) {
            return VertexStylesFactory.SQUARE_STYLE;
        } else if (currentVertexStyle instanceof CircleVertexStyle) {
            return VertexStylesFactory.CIRCLE_STYLE;
        } else if (currentVertexStyle instanceof CrossVertexStyle) {
            return VertexStylesFactory.CROSS_STYLE;
        } else if (currentVertexStyle instanceof TriangleVertexStyle) {
            return VertexStylesFactory.TRIANGLE_STYLE;
        } else if (currentVertexStyle instanceof StarVertexStyle) {
            return VertexStylesFactory.STAR_STYLE;
        } else if (currentVertexStyle instanceof BitmapVertexStyle) {
            return VertexStylesFactory.BITMAP_STYLE;
        }

        return "";
    }

    public DeeRenderingStylePanel(Blackboard blackboard, Layer layer,
            Blackboard persistentBlackboard) {
        super(blackboard, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        vertexStyleChooser.setBlackboard(persistentBlackboard);
        vertexStyleChooser.setStylePanel(this);
        // Initialize layer before setBasicStyle as setBasicStyle may now call
        // getCurrentVertexStyle in the case of XBasicStyle
        this.layer = layer;
        setBasicStyle(layer.getBasicStyle());

        try {
            jbInit();
            // updateControls();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        // Set layer after #jbInit, because both methods initialize the
        // components. [Jon Aquino]
        setLayer(layer);
        // updateControls();

        if (layer.getVertexStyle() instanceof BitmapVertexStyle) {
            final String fileName = ((BitmapVertexStyle) layer.getVertexStyle())
                    .getFileName();
            // side effects used for the WORST
            vertexStyleChooser.setCurrentFileName(fileName);
        }

        vertexStyleChooser.setSelectedStyle(getCurrentVertexStyle());
    }

    @Override
    public void setBasicStyle(BasicStyle basicStyle) {
        super.setBasicStyle(basicStyle);
        if (basicStyle instanceof XBasicStyle) {
            final VertexStyle vStyle = ((XBasicStyle) basicStyle)
                    .getVertexStyle();
            vertexCheckBox.setSelected(vStyle.isEnabled());
            vertexSlider.setValue(sizeToSlider(vStyle.getSize()));
            vertexSize.setText(Integer.toString(vStyle.getSize()));
            if (vStyle instanceof BitmapVertexStyle) {
                final String fileName = ((BitmapVertexStyle) vStyle)
                        .getFileName();
                // side effects used for the WORST
                vertexStyleChooser.setCurrentFileName(fileName);
            } else {
                vertexStyleChooser.setSelectedStyle(getCurrentVertexStyle());
            }
        }
    }

    @Override
    public void updateControls() {
        super.updateControls();

        // if (vertexSlider == null) {
        // // Get here during superclass initialization. [Jon Aquino]
        // return;
        // }
        if (initialized) {

            vertexSlider.setEnabled(vertexCheckBox.isSelected());
            vertexSize.setEnabled(vertexCheckBox.isSelected());
            vertexStyleChooser.setEnabled(vertexCheckBox.isSelected());

            for (final Enumeration<?> e = vertexSlider.getLabelTable()
                    .elements(); e.hasMoreElements();) {
                final JLabel label = (JLabel) e.nextElement();
                label.setEnabled(vertexCheckBox.isSelected());
            }

            previewPanel.repaint();
        }
    }

    @Override
    public String getTitle() {
        return I18N.get("ui.style.RenderingStylePanel.rendering");
    }

    private void setLayer(Layer layer) {
        this.layer = layer;
        final VertexStyle vStyle = layer.getVertexStyle();
        setSynchronizingLineColor(layer.isSynchronizingLineColor());
        vertexCheckBox.setSelected(vStyle.isEnabled());
        vertexSlider.setEnabled(vStyle.isEnabled());
        vertexSlider.setValue(sizeToSlider(vStyle.getSize()));
        vertexSize.setEnabled(vStyle.isEnabled());
        vertexStyleChooser.setEnabled(vStyle.isEnabled());
        for (final Enumeration<?> e = vertexSlider.getLabelTable().elements(); e
                .hasMoreElements();) {
            ((JLabel) e.nextElement()).setEnabled(vStyle.isEnabled());
        }
        // if (vStyle.isEnabled()) {
        // fillCheckBox.setSelected(layer.getVertexStyle().getFilling());
        // }
        previewPanel.repaint();
    }

    @Override
    protected void jbInit() throws Exception {

        if (vertexSlider == null) {
            // Get here during superclass initialization. [Jon Aquino]
            super.jbInit();
            return;
        }

        vertexCheckBox.setText(I18N
                .get("ui.style.RenderingStylePanel.vertices-size"));

        // GH 2005.09.22 this Listner is better than actionListener for this
        // Checkbox
        vertexCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateControls();
            }
        });

        // vertexSlider initialization
        vertexSlider.setMinorTickSpacing(1);
        vertexSlider.setMajorTickSpacing(0);
        vertexSlider.setPaintLabels(true);
        vertexSlider.setMinimum(0);
        vertexSlider.setValue(0);
        vertexSlider.setMaximum(100);
        vertexSlider.setSnapToTicks(true);
        vertexSlider.setPreferredSize(SLIDER_DIMENSION);
        final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(0), new JLabel("" + sizeFromSlider(0)));
        labelTable.put(new Integer(25), new JLabel("" + sizeFromSlider(25)));
        labelTable.put(new Integer(50), new JLabel("" + sizeFromSlider(50)));
        labelTable.put(new Integer(75), new JLabel("" + sizeFromSlider(75)));
        labelTable.put(new Integer(100), new JLabel("" + sizeFromSlider(100)));
        vertexSlider.setLabelTable(labelTable);
        vertexSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                vertexSize.setText(Integer.toString(sizeFromSlider()));
                // updateControls();
                previewPanel.repaint();
            }
        });

        // vertexSize textField initialization
        vertexSize.setColumns(3);
        vertexSize.setText(Integer.toString(sizeFromSlider()));
        vertexSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final int newValue = Integer.parseInt(vertexSize.getText());
                    if (newValue < sizeFromSlider(0)
                            || newValue > sizeFromSlider(100)) {
                        throw new NumberFormatException();
                    }
                    vertexSlider.setValue(sizeToSlider(newValue));
                    previewPanel.repaint();
                } catch (final NumberFormatException nfe) {
                    vertexSize.setText("" + sizeFromSlider());
                }
            }
        });

        // vertexSize textField and vertexSlider synchronization
        vertexSize.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                final String oldValue = vertexSize.getText();
                int newValue = 0;
                try {
                    newValue = Integer.parseInt(oldValue);
                    if (newValue < sizeFromSlider(0)
                            || newValue > sizeFromSlider(100)) {
                        throw new Exception();
                    }
                    vertexSlider.setValue(sizeToSlider(newValue));
                    previewPanel.repaint();
                } catch (final NumberFormatException nfe) {
                    // Make it possible to empty textArea to type a new number
                    // starting with 1, 2 or 3
                    if (vertexSize.getText().equals("")) {
                        // vertexSize.setText("" + sizeFromSlider(0));
                        vertexSlider.setValue(0);
                    }
                    // If text area contains a non numeric text, keep the
                    // previous value
                    else {
                        vertexSize.setText(""
                                + sizeFromSlider(vertexSlider.getValue()));
                    }
                } catch (final Exception ex) {
                    // Make it possible to type 1, 2 or 3, even if it is out of
                    // the range
                    // as it maybe the first of a longer number
                    if (newValue < sizeFromSlider(0)) {
                        // vertexSize.setText("" + sizeFromSlider(0));
                        vertexSlider.setValue(0);
                    } else if (newValue > sizeFromSlider(100)) {
                        vertexSize.setText("" + sizeFromSlider(100));
                        vertexSlider.setValue(100);
                    }
                }
            }
        });

        centerPanel.add(vertexSlider, new GridBagConstraints(1, 35, 1, 1, 0.0,
                0.0, WEST, NONE, new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(vertexSize, new GridBagConstraints(2, 35, 1, 1, 0.0,
                0.0, WEST, NONE, new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(vertexCheckBox, new GridBagConstraints(0, 35, 2, 1,
                0.0, 0.0, WEST, NONE, new Insets(2, 2, 2, 2), 0, 0));
        centerPanel.add(
                new JLabel(I18N.get("ui.style.RenderingStylePanel.preview")),
                new GridBagConstraints(0, 40, 3, 1, 0.0, 0.0, WEST, NONE,
                        new Insets(2, 2, 0, 2), 0, 0));
        centerPanel.add(previewPanel, new GridBagConstraints(0, 45, 3, 1, 0.0,
                0.0, WEST, NONE, new Insets(0, 10, 2, 2), 0, 0));
        centerPanel.add(vertexStyleChooser, new GridBagConstraints(0, 50, 3, 1,
                0.0, 0.0, WEST, NONE, new Insets(2, 2, 0, 2), 0, 0));//

        // GH 2005.10.26 I have deleted the actionListner of the BitmapButton
        // where is the suitable place to call ChangeVertexStyle()??.

        vertexStyleChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // changeVertexStyle();
                previewPanel.repaint();
            }
        });
        //
        // vertexStyleChooser.addChangeListener(new ChangeListener() {
        // public void stateChanged(ChangeEvent e) {
        // JSlider slider = (JSlider) e.getSource();
        // vertexSlider.setValue(slider.getValue());
        // }
        // });
        //
        final ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeVertexStyle();
            }
        };
        // Refresh vertex preview when fillColor or lineColor are changed
        fillColorChooserPanel.addActionListener(listener);
        lineColorChooserPanel.addActionListener(listener);
        initialized = true;
    }

    // Refresh vertex preview when some composant are changes
    protected void changeVertexStyle() {
        final Style st = layer.getStyle(VertexStyle.class);
        layer.removeStyle(st);
        layer.addStyle(getVertexStyle());
        previewPanel.repaint();
    }

    public VertexStyle getVertexStyle() {
        final VertexStyle vStyle = vertexStyleChooser.getSelectedStyle();
        vStyle.setEnabled(vertexCheckBox.isSelected());
        vStyle.setSize(sizeFromSlider());
        return vStyle;
    }

    /**
     * Replace old BasicStyle and VertexStyle of the layer by the styles
     * specified by this panel, then refresh the view.
     */
    @Override
    public void updateStyles() {
        final boolean firingEvents = layer.getLayerManager().isFiringEvents();
        layer.getLayerManager().setFiringEvents(false);
        try {
            // Call #getVertexStyle before removing layer's vertex style,
            // because one depends on the other. [Jon Aquino]
            final VertexStyle newVertexStyle = getVertexStyle();

            layer.removeStyle(layer.getBasicStyle());
            layer.addStyle(getBasicStyle());

            // Call #getVertexStyle before removing layer's vertex style,
            // because one depends on the other. [Jon Aquino]
            // VertexStyle newVertexStyle = getVertexStyle();
            layer.removeStyle(layer.getVertexStyle());
            layer.addStyle(newVertexStyle);

            // if (ColorThemingStyle.get(layer).isEnabled()) {
            // layer.getVertexStyle().setEnabled(false);
            // }

            if (newVertexStyle.isEnabled()) {
                // layer.getBasicStyle().setEnabled(false);
                layer.getBasicStyle().setRenderingVertices(false);
                // if (layer.getStyle(ColorThemingStyle.class) != null) {
                // layer.getVertexStyle().setEnabled(false);
                // }
            }

            layer.setSynchronizingLineColor(synchronizeCheckBox.isSelected());
        } finally {
            layer.getLayerManager().setFiringEvents(firingEvents);
        }
        layer.fireAppearanceChanged();
    }

    // void showVerticesCheckBox_actionPerformed(ActionEvent e) {
    // updateControls();
    // }

    @Override
    public String validateInput() {
        return null;
    }

    // void showVerticesCheckBox_actionPerformed(ItemEvent e) {
    // updateControls();
    // }

    private int sizeFromSlider() {
        return sizeFromSlider(vertexSlider.getValue());
    }

    private int sizeFromSlider(int i) {
        return (int) (Math.rint(Math.pow(1.6, (i) / 10.0))) + 0;
    }

    private int sizeToSlider(int i) {
        return (int) Math.rint(10 * Math.log(((double) i - 0)) / Math.log(1.6));
    }

}