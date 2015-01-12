package org.openjump.core.ui.plugin.layer.pirolraster.panel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ColorChooserPanel;
import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.ui.color.ColorGenerator;
import org.openjump.core.ui.plugin.raster.color.RasterColorEditor;
import org.openjump.core.ui.swing.ValueChecker;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version $Rev: 4221 $ Dic 23 2014 [Giuseppe Aruta] - Derived from
 *          RasterColorEditorDialog
 */
public class RasterColorEditorPanel extends JPanel implements ValueChecker,
        ActionListener, StylePanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private RasterImageLayer layer = null;

    private JTextField fromValue;

    private JLabel fromValueLabel;

    private JLabel toValueLabel;

    private JTextField toValue;

    private JLabel layerLabel;

    private JComboBox colorScaleChooser;

    private ColorGenerator colorGenerator;

    private String[] colorTableList = {
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Default-colors"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Green-Yellow-Red"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Blue-Green-Red"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Red-Blue"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Blue-Red"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Black-White"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.White-Black"),
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Rainbow"),
            "Color Relief 1", "Color Relief 2", "Slope",
            "Spectral (colorbrewer)", "Spectral 4", "Spectral 8",
            "Spectral 12", "Landcarpet Europe", "Red", "Yellow", "Blue" };

    private JPanel mainPanel = new JPanel();

    // private OKCancelPanel okCancelPanel = new OKCancelPanel();

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

    private JComboBox combo = new JComboBox();

    private JLabel NodataColor = new JLabel(
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.No-data-color")); //$NON-NLS-1$

    private ColorPanel NoDataColorPanel = new ColorPanel();

    private JCheckBox transparent = new JCheckBox(
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Transparency")); //$NON-NLS-1$

    private int alpha = 255;

    private JPanel panelSeparator = new JPanel();

    private boolean enabled = true;

    String[] value = { "2", "4", "5", "7", "10", "12", "15", "20", "25", "35" };

    private String sToolTip = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Choose-a-color-range-It-will-be-automaticaly-expanded-between-the-2-values");
    private String sColorRange = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Color-range");
    private String sFromValue = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.From-value");
    private String sToValue = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.To-value");
    private String sNoDataValueColor = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.no-data-value-color");
    private String sChange = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.change");
    private String sChoseOtherColor = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Choose-other-color-for-no-data-values");
    private String sToggleTransparency = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Toggle-transparency-for-no-data-values");
    private String sSelectColor = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Select-color");
    private String sLayerName = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Selected-Layer");

    public RasterColorEditorPanel(PlugInContext context,
            RasterImageLayer actualLayer) {
        super(); //$NON-NLS-1$
        plugInContext = context;
        setLayer(actualLayer);

        setVisible(true);
        // setSize(500, 350);

        // GUIUtil.setLocation(this, new GUIUtil.Location(100, true, 100, true),
        // plugInContext.getWorkbenchFrame());

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
        colorScaleChooser.setToolTipText(sToolTip); //$NON-NLS-1$
        colorScaleChooser.setBorder(borderRaised);
        mainPanel.add(colorScaleChooser, c);
        //OpenJUMPSextanteRasterLayer ojraster = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file
        // based image
        //ojraster.create(layer, false);

        fromValue = new JTextField(Double.toString(layer.getMetadata().getStats().getMin(0)), 15);
        fromValueLabel = new JLabel(sFromValue); //$NON-NLS-1$

        fromValue.setCaretPosition(0);
        fromValue.selectAll();

        toValue = new JTextField(Double.toString(layer.getMetadata().getStats().getMax(0)), 15);
        toValue.setCaretPosition(0);
        fromValue.selectAll();
        toValueLabel = new JLabel(sToValue); //$NON-NLS-1$

        // panelSeparator.setSize(300, 50);
        NoDataColorPanel.setFillColor(Color.WHITE);
        NoDataColorPanel.setLineColor(Color.BLACK);
        NoDataColorPanel.setBorder(borderLowerered);
        NoDataColorPanel.setToolTipText(sNoDataValueColor); //$NON-NLS-1$

        NoDataColorButton.setText(sChange); //$NON-NLS-1$
        NoDataColorButton.setToolTipText(sChoseOtherColor); //$NON-NLS-1$
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

        // c.ipady = 100;
        mainPanel.add(panelSeparator, c);

        /*
         * [sstein - 28.Sept.2010] since this stuff doesn't work yet we don't
         * display it.
         * 
         * 
         * 
         * c.gridheight = 1; c.weightx = 0.5; c.gridy = 5; c.gridwidth = 1;
         * c.ipady = 0; mainPanel.add(NodataColor, c);
         * 
         * c.gridx = 1; c.gridwidth = 2; mainPanel.add(NoDataColorPanel, c);
         * 
         * c.gridy = 6; mainPanel.add(NoDataColorButton, c);
         * 
         * c.gridx = 0; mainPanel.add(transparent, c);
         */

        add(mainPanel, BorderLayout.CENTER);

    }

    public void  actionPerformed(ActionEvent e) {
        if (!areValuesOk()) {
            setVisible(false);
            return;
        }

        if (areValuesOk() && Float.parseFloat(fromValue.getText()) < Float
                .parseFloat(toValue.getText())) {

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
            case 8: {
                valuesColors = new Color[] {new Color(110,220,110), //$NON-NLS-1$
                        new Color(240,250,160), new Color(230,220,70),new Color(220,220,220), //$NON-NLS-1$
                        new Color(250,250,250)};
                break;
            }
            case 9: {
                valuesColors = new Color[] {
                        new Color(46,154,88), 
                        new Color(251,255,128), 
                        new Color(224,108,31),
                        new Color(200,55,55), 
                        new Color(215,244,244)};
                break;
            }
            case 10: {
                valuesColors = new Color[] {
                        new Color(0,255,0), 
                        new Color(36,255,0), 
                        new Color(73,255,0),
                        new Color(109,255,0),
                        new Color(146,255,0), 
                        new Color(182,255, 0),
                        new Color(219,255,0),
                        new Color(255,255,0), 
                        new Color(255,219,0), 
                        new Color(255,182,0),
                        new Color(255,146,0),
                        new Color(255,109,0), 
                        new Color(255,73,0),
                        new Color(255,36,0),
                        new Color(255,0,0)};
                break;
            }
            case 11: {
                 valuesColors = new Color[] {
                         new Color(215,25,28),
                         new Color(253,174,97),
                         new Color(171,221,164),
                         new Color(43,131,186)};
            break;
            }
            
            case 12: {
                valuesColors = new Color[] {
                        new Color(255,0,0), 
                        new Color(255,128,0), 
                        new Color(0,255,0),
                        new Color(0,255,128), 
                        new Color(0,255,255), 
                        new Color(0,128,255),
                        new Color(0,0,255), 
                        new Color(255,0,255)};
                break;
            }
            
            case 13: {
                
                valuesColors = new Color[] {  
                        new Color(213,62,79),
                        new Color(244,109,67),
                        new Color(253,174,97),
                        new Color(254,224,139),
                        new Color(230,245,152),
                        new Color(171,221,164),
                        new Color(102,194,165),
                        new Color(50,136,89)};    
                        break;
            }
                case 14: {
                     
                    valuesColors = new Color[] {  
                new Color(158,1,66),
                new Color(213,62,79),
                new Color(244,109,67),
                new Color(253,174,97),
                new Color(254,224,139),
                new Color(255,255,191),
                new Color(230,245,152),
                new Color(171,221,164),
                new Color(102,194,165),
                new Color(50,136,189),
                new Color(94,79,162)};
                    

                    
                break;
            }
                case 15: {
                  
                    valuesColors = new Color[] {                   
                    new Color(218,179,122),
                    new Color(213,213,149),
                    new Color(127,166,122),
                    new Color(151,106,47),
                    new Color(121,117,10), 
                    new Color(254,254,254),
                    new Color(255,255,255)};
            
            break;
                }
                
            
            
            
            
            }
           
            try {
                changeColors(plugInContext.getWorkbenchContext(),
                        valuesColors, GUIUtil.alphaColor(NoDataColorPanel
                                .getFillColor(), alpha), Float.parseFloat(fromValue
                                .getText()), Float.parseFloat(toValue.getText()));
            } catch (NumberFormatException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoninvertibleTransformException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException ex) {
                Logger.getLogger(RasterColorEditorPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            setVisible(false);

            return;
        } else {
            plugInContext.getWorkbenchFrame().warnUser("min > max!"); //$NON-NLS-1$
            return;
        }

    }

    /*
     * private boolean validateInput() {
     * 
     * return (Float.parseFloat(fromValue.getText()) < Float
     * .parseFloat(toValue.getText())); }
     */
    void changeButton_actionPerformed(ActionEvent e) {

        Color newColor = JColorChooser.showDialog(
                SwingUtilities.windowForComponent(this), sSelectColor,
                Color.WHITE); //$NON-NLS-1$

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

    public String getTitle() {
        // TODO Auto-generated method stub
        return I18N
                .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Raster-Color-Editor");

    }

    public String validateInput() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean areValuesOk() {

        return true;
    }

    public void updateStyles() {

        /*
         * Giuseppe Aruta [dic 23 2014] Copied from ActionPerformed. Maybe it is
         * not elegant but it was easier and it did work
         */

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
        case 8: {
            valuesColors = new Color[] {
                    new Color(110, 220, 110), //$NON-NLS-1$
                    new Color(240, 250, 160), new Color(230, 220, 70),
                    new Color(220, 220, 220), //$NON-NLS-1$
                    new Color(250, 250, 250) };
            break;
        }
        case 9: {
            valuesColors = new Color[] { new Color(46, 154, 88),
                    new Color(251, 255, 128), new Color(224, 108, 31),
                    new Color(200, 55, 55), new Color(215, 244, 244) };
            break;
        }
        case 10: {
            valuesColors = new Color[] { new Color(0, 255, 0),
                    new Color(36, 255, 0), new Color(73, 255, 0),
                    new Color(109, 255, 0), new Color(146, 255, 0),
                    new Color(182, 255, 0), new Color(219, 255, 0),
                    new Color(255, 255, 0), new Color(255, 219, 0),
                    new Color(255, 182, 0), new Color(255, 146, 0),
                    new Color(255, 109, 0), new Color(255, 73, 0),
                    new Color(255, 36, 0), new Color(255, 0, 0) };
            break;
        }
        case 11: {
            valuesColors = new Color[] { new Color(215, 25, 28),
                    new Color(253, 174, 97), new Color(171, 221, 164),
                    new Color(43, 131, 186) };
            break;
        }

        case 12: {
            valuesColors = new Color[] { new Color(255, 0, 0),
                    new Color(255, 128, 0), new Color(0, 255, 0),
                    new Color(0, 255, 128), new Color(0, 255, 255),
                    new Color(0, 128, 255), new Color(0, 0, 255),
                    new Color(255, 0, 255) };
            break;
        }

        case 13: {

            valuesColors = new Color[] { new Color(213, 62, 79),
                    new Color(244, 109, 67), new Color(253, 174, 97),
                    new Color(254, 224, 139), new Color(230, 245, 152),
                    new Color(171, 221, 164), new Color(102, 194, 165),
                    new Color(50, 136, 189) };
            break;
        }
        case 14: {

            valuesColors = new Color[] { new Color(158, 1, 66),
                    new Color(213, 62, 79), new Color(244, 109, 67),
                    new Color(253, 174, 97), new Color(254, 224, 139),
                    new Color(255, 255, 191), new Color(230, 245, 152),
                    new Color(171, 221, 164), new Color(102, 194, 165),
                    new Color(50, 136, 189), new Color(94, 79, 162) };

            break;
        }
        case 15: {

            valuesColors = new Color[] { new Color(218, 179, 122),
                    new Color(213, 213, 149), new Color(127, 166, 122),
                    new Color(151, 106, 47), new Color(121, 117, 10),
                    new Color(254, 254, 254), new Color(255, 255, 255) };

            break;
        }
        case 16: {

            valuesColors = new Color[] { Color.WHITE, Color.RED };

            break;
        }
        case 17: {

            valuesColors = new Color[] { Color.WHITE, Color.GREEN };

            break;
        }
        case 18: {

            valuesColors = new Color[] { Color.WHITE, Color.BLUE };

            break;
        }
        }
        RasterColorEditor colorEditor = new RasterColorEditor(layer);
        try {
            colorEditor.changeColors(plugInContext.getWorkbenchContext(),
                    valuesColors,
                    GUIUtil.alphaColor(NoDataColorPanel.getFillColor(), alpha),
                    Float.parseFloat(fromValue.getText()),
                    Float.parseFloat(toValue.getText()));
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoninvertibleTransformException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        setVisible(false);
        layer.fireAppearanceChanged();
        setVisible(true);
        return;
        /*
         * } else { plugInContext.getWorkbenchFrame().warnUser("min > max!");
         * //$NON-NLS-1$ return; }
         */

    }

    public void changeColors(WorkbenchContext context, Color[] colors,
            Color noDataColor, float min, float max)
            throws NoninvertibleTransformException, IOException {

        if (colors == null || colors.length == 0) {
            layer.setNeedToKeepImage(false);
            layer.flushImages(true);
            layer.setWholeImageEnvelope(layer.getWholeImageEnvelope());
            context.getLayerViewPanel().getViewport().update();
            return;
        }

        colorGenerator = new ColorGenerator(35, colors);

        Raster raster = layer.getImage().getRaster();

        /**
         * TODO: make the stuff below work. Not sure how, becasue the three
         * GeoTools classes have a lot of dependencies... so one should use the
         * geotools lib directly???
         */

        /*
         * final String path; final Unit unit = null; final Category[]
         * categories; final CoordinateReferenceSystem crs; final Rectangle2D
         * bounds;
         * 
         * 
         * Category[] categories = new Category[] { new Category("val1", color1,
         * new NumberRange(1, 255), new NumberRange(min, max)), new
         * Category("val2", noDataColor, 0) };
         * 
         * GridSampleDimension GSD = new GridSampleDimension(categories, null);
         * GSD = GSD.geophysics(true);
         * 
         * 
         * int width = image.getData().getWidth(); int height =
         * image.getData().getHeight();
         * 
         * WritableRaster data = RasterFactory.createBandedRaster(
         * java.awt.image.DataBuffer.TYPE_FLOAT, width, height, 1, null);
         * WritableRaster oldData = (WritableRaster) image.getData();
         * 
         * for (int i = 0; i < height; i++) { for (int j = 0; j < width; j++) {
         * data.setSample(j, i, 0, oldData.getSampleFloat(j, i, 0)); } }
         */
        // OpenJUMPSextanteRasterLayer ojraster = new
        // OpenJUMPSextanteRasterLayer();
        // ojraster.create(layer);
        // double rasterMaxValue = ojraster.getMaxValue();
        // double rasterMinValue = ojraster.getMinValue();
        int width = raster.getWidth();
        int height = raster.getHeight();
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        int numOfSteps = colorGenerator.getSteps();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                float value = raster.getSampleFloat(j, i, 0);

                if (value == Double.POSITIVE_INFINITY) {
                    newImage.setRGB(j, i, Color.TRANSLUCENT);
                } else {
                    int intColor = (int) ((value - min) / (max - min) * (numOfSteps - 1));

                    /*
                     * black color indicates that value is out of the min/max
                     * area:
                     */
                    if (intColor >= numOfSteps || intColor < 0) {
                        newImage.setRGB(j, i, Color.BLACK.getRGB());
                        // newImage.setRGB(j, i, (new
                        // Color(Color.BLACK.getRGB(), true)).getRGB());
                        // newImage.setRGB(j, i, Color.BLACK.getRGB());
                    } else {
                        Color newColor = colorGenerator.getColor(intColor);
                        if (newColor == null) {
                            // newImage.setRGB(j, i, Color.BLACK.getRGB());
                        }

                        newImage.setRGB(j, i, newColor.getRGB());
                    }
                }

            }
        }

        /**
         * TODO: make this work
         */
        /*
         * BufferedImage BufImage = new BufferedImage(GSD.getColorModel(), data,
         * false, null);
         */

        //PlanarImage pimage = PlanarImage.wrapRenderedImage(newImage);

        /*
         * System.out.println("databuffer: " +
         * newImage.getRaster().getDataBuffer() + "samplemodel: " +
         * newImage.getRaster().getSampleModel());
         */
        layer.setNeedToKeepImage(true);
        layer.setImage(newImage);
        layer.setWholeImageEnvelope(layer.getWholeImageEnvelope());
        context.getLayerViewPanel().getViewport().update();
    }

}
