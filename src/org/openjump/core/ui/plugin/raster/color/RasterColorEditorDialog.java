
package org.openjump.core.ui.plugin.raster.color;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 15 sept. 2005
 *
 * @author  Paul PLOUY
 * 			Laboratoire RESO - 	université de Rennes 2
 * 			FRANCE
 * 
 * Learn more about our contribution to OpenJUMP project:
 * 		http://www.projet-sigle.org/
 * 
 */
public class RasterColorEditorDialog extends JDialog {

    private RasterImageLayer layer = null;

    private JTextField fromValue;

    private JLabel fromValueLabel;

    private JLabel toValueLabel;

    private JTextField toValue;

    private JLabel layerLabel;

    private JComboBox colorScaleChooser;

    private String[] colorTableList = {
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Default-colors"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Green-Yellow-Red"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Blue-Green-Red"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Red-Blue"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Blue-Red"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Black-White"), 
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.White-Black"),
    		I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Rainbow")
    		};

    private JPanel mainPanel = new JPanel();

    private OKCancelPanel okCancelPanel = new OKCancelPanel();

    private PlugInContext plugInContext;

    private Border border = BorderFactory.createEmptyBorder(10, 10, 10, 10);

    private Border borderRaised = BorderFactory.createRaisedBevelBorder();

    private Border borderLowerered = BorderFactory.createLoweredBevelBorder();

    private Color[] valuesColors;

    private Color noDataColor;

    private LayoutManager layout = new BorderLayout();

    private ColorChooserPanel colorChooser;

    private LayoutManager gridBagLayout = new GridBagLayout();

    private JButton NoDataColorButton = new JButton();

    private JLabel NodataColor = new JLabel(I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.No-data-color")); //$NON-NLS-1$

    private ColorPanel NoDataColorPanel = new ColorPanel();

    private JCheckBox transparent = new JCheckBox(I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Transparency")); //$NON-NLS-1$

    private int alpha = 255;

    private JPanel panelSeparator = new JPanel();

    private boolean enabled = true;

    private String sToolTip = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Choose-a-color-range-It-will-be-automaticaly-expanded-between-the-2-values");
    private String sColorRange = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Color-range");
    private String sFromValue = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.From-value");
    private String sToValue = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.To-value");
    private String sNoDataValueColor = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.no-data-value-color");
    private String sChange = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.change");
    private String sChoseOtherColor = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Choose-other-color-for-no-data-values");
    private String sToggleTransparency = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Toggle-transparency-for-no-data-values");
    private String sSelectColor = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Select-color");
    private String sLayerName = I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Selected-Layer");
    
    public RasterColorEditorDialog(PlugInContext context,
            RasterImageLayer actualLayer) {
        super(context.getWorkbenchFrame(), I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Raster-Color-Editor"), true); //$NON-NLS-1$
        plugInContext = context;
        setLayer(actualLayer);

        setResizable(true);
        setSize(500, 350);

        GUIUtil.setLocation(this, new GUIUtil.Location(100, true, 100, true),
                plugInContext.getWorkbenchFrame());

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void setLayer(RasterImageLayer actualLayer) {
        this.layer = actualLayer;

    }

    void jbInit() throws Exception {

        setLayout(layout);

        add(okCancelPanel, BorderLayout.SOUTH);
        okCancelPanel.setOKPressed(false);

        okCancelPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					okCancelPanel_actionPerformed(e);
				} catch (NumberFormatException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (NoninvertibleTransformException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException ex) {
                                        ex.printStackTrace();
                                }
            }
        });

        layerLabel = new JLabel(sLayerName + ": " + layer.getName());
        layerLabel.setBorder(border);
        add(layerLabel, BorderLayout.NORTH);

        mainPanel.setBorder(border);
        mainPanel.setLayout(gridBagLayout);

        GridBagConstraints c = new GridBagConstraints(0, 0, 4, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 10), 0, 0);

        colorScaleChooser = new JComboBox(colorTableList);
        colorScaleChooser.setSelectedIndex(0);
        String fieldName = sColorRange; //$NON-NLS-1$
        colorScaleChooser
                .setToolTipText(sToolTip); //$NON-NLS-1$
        colorScaleChooser.setBorder(borderRaised);
        mainPanel.add(colorScaleChooser, c);
        OpenJUMPSextanteRasterLayer ojraster = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file based image
        ojraster.create(layer, false);
        fromValue = new JTextField(Double.toString(ojraster.getMinValue()), 15);
        fromValueLabel = new JLabel(sFromValue); //$NON-NLS-1$

        fromValue.setCaretPosition(0);
        fromValue.selectAll();

        toValue = new JTextField(Double.toString(ojraster.getMaxValue()), 15);
        toValue.setCaretPosition(0);
        fromValue.selectAll();
        toValueLabel = new JLabel(sToValue); //$NON-NLS-1$
 
        panelSeparator.setSize(300, 50);
        NoDataColorPanel.setFillColor(Color.WHITE);
        NoDataColorPanel.setLineColor(Color.BLACK);
        NoDataColorPanel.setBorder(borderLowerered);
        NoDataColorPanel.setToolTipText(sNoDataValueColor); //$NON-NLS-1$

        NoDataColorButton.setText(sChange); //$NON-NLS-1$
        NoDataColorButton
                .setToolTipText(sChoseOtherColor); //$NON-NLS-1$
        NoDataColorButton
                .addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        changeButton_actionPerformed(e);
                    }
                });
        
        transparent.setToolTipText(sToggleTransparency); //$NON-NLS-1$

        transparent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transparent_actionPerformed(e);
            }
        });


        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        mainPanel.add(fromValueLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        mainPanel.add(fromValue, c);
        c.weightx = 1;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        mainPanel.add(toValueLabel, c);

        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        mainPanel.add(toValue, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 3;
        c.gridheight = 2;
        c.gridwidth = 4;
        c.ipady = 40;
        mainPanel.add(panelSeparator, c);

        /*
         * [sstein - 28.Sept.2010] since this stuff doesn't work yet we don't display it.
         * 
        c.gridheight = 1;
        c.weightx = 0.5;
        c.gridy = 5;
        c.gridwidth = 1;
        c.ipady = 0;
        mainPanel.add(NodataColor, c);

        c.gridx = 1;
        c.gridwidth = 2;
        mainPanel.add(NoDataColorPanel, c);

        c.gridy = 6;
        mainPanel.add(NoDataColorButton, c);

        c.gridx = 0;
        mainPanel.add(transparent, c);
        */

        add(mainPanel, BorderLayout.CENTER);

    }

    void okCancelPanel_actionPerformed(ActionEvent e) throws NumberFormatException, NoninvertibleTransformException, IOException {
        if (!okCancelPanel.wasOKPressed()) {
            setVisible(false);
            return;
        }

        if (okCancelPanel.wasOKPressed() && validateInput()) {

            switch (colorScaleChooser.getSelectedIndex()) {
            case 0: {
                valuesColors = null;
                break;
            }
            case 1: {
                valuesColors = new Color[] { Color.GREEN, Color.YELLOW, Color.RED };
                break;
            }
            case 2: {
                valuesColors = new Color[] { Color.BLUE, Color.GREEN, Color.RED };
                break;
            }
            case 3: {
                valuesColors = new Color[] { Color.RED, Color.BLUE };
                break;
            }
            case 4: {
                valuesColors = new Color[] { Color.BLUE, Color.RED };
                break;
            }
            case 5: {
                valuesColors = new Color[] { Color.WHITE, Color.BLACK };
                break;
            }
            case 6: {
                valuesColors = new Color[] { Color.BLACK, Color.WHITE };
                break;
            }
            case 7: {
                valuesColors = new Color[] { Color.decode("#9400D3"), //$NON-NLS-1$
                        Color.decode("#4B0082"), Color.BLUE, Color.GREEN, //$NON-NLS-1$
                        Color.YELLOW, Color.ORANGE, Color.RED };
                break;
            }

            }
            RasterColorEditor colorEditor = new RasterColorEditor(layer);
            colorEditor.changeColors(plugInContext.getWorkbenchContext(),
                    valuesColors, GUIUtil.alphaColor(NoDataColorPanel
                            .getFillColor(), alpha), Float.parseFloat(fromValue
                            .getText()), Float.parseFloat(toValue.getText()));

            setVisible(false);

            return;
        } else {
            plugInContext.getWorkbenchFrame().warnUser("min > max!"); //$NON-NLS-1$
            return;
        }

    }

    private boolean validateInput() {

        return (Float.parseFloat(fromValue.getText()) < Float
                .parseFloat(toValue.getText()));
    }

    void changeButton_actionPerformed(ActionEvent e) {

        Color newColor = JColorChooser.showDialog(SwingUtilities
                .windowForComponent(this), sSelectColor , Color.WHITE); //$NON-NLS-1$

        if (newColor == null) {
            return;
        }

        NoDataColorPanel.setFillColor(newColor);
        NoDataColorPanel.repaint();

    }

    void transparent_actionPerformed(ActionEvent e) {

        if (transparent.isSelected()) {
            alpha = 0;
            enabled = false;

        } else {
            alpha = 255;
            enabled = true;
        }
        NoDataColorButton.setEnabled(enabled);
        NoDataColorPanel.setVisible(enabled);
        return;

    }

}
