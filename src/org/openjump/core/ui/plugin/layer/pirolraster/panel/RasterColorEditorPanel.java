package org.openjump.core.ui.plugin.layer.pirolraster.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;
import org.openjump.core.ui.color.ColorGenerator;
import org.openjump.core.ui.plugin.layer.pirolraster.ChangeRasterImagePropertiesPlugIn;
import org.openjump.core.ui.swing.ValueChecker;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.style.StylePanel;

/**
 * @version $Rev: 4221 $ Dic 23 2014 [Giuseppe Aruta] - Derived from
 *          RasterColorEditorDialog
 * @version $Rev: 4403 $ Apr 22 2015 [Giuseppe Aruta] - Added inverse color
 *          ramps and transparency to values outside choosen range
 * @version $Rev: 4463 $ May 23 2015 [Giuseppe Aruta] - Added Color intervals
 *          option
 */
public class RasterColorEditorPanel extends JPanel implements ValueChecker,
        ActionListener, StylePanel {

    public static final String COLOR_KEY = RasterColorEditorPanel.class
            .getName() + " - COLOR_TYPE";
    public static final String MIN_KEY = RasterColorEditorPanel.class.getName()
            + " - MIN_VAL";
    public static final String MAX_KEY = RasterColorEditorPanel.class.getName()
            + " - MAX_VAL";

    private static final long serialVersionUID = 1L;

    protected RasterImageLayer layer = null;
    public JTextField fromValue;
    private JLabel fromValueLabel;
    private JLabel toValueLabel;
    public JTextField toValue;
    public JTextField Chooser;
    public JComboBox colorScaleChooser;
    public JComboBox colorRampChooser;
    public JComboBox typeChooser;
    private ColorGenerator colorGenerator;
    private JSpinner spinnerbox;

    private String[] colorTableList = {
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Default-colors"),// 0
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Green-Yellow-Red"),// 1
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Blue-Green-Red"),// 2
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Red-Blue"),// 3
            "Red-Yellow", // 4
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Black-White"),// 5
            "Stripes sixties", // 6
            I18N.get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorDialog.Rainbow"),// 7
            "Color Relief", // 8
            "Topo", // 9
            "Spectral (Color brewer)", // 10
            "BrBG (Color brewer)", // 11
            "RdBu (Color Brewer)", // 12
            "RdYlBu (Color Bewer)", // 13
            "RdYlGn (Color Bewer)", // 14
            "Reds", // 15
            "Greens", // 16
            "Blues" }; // 17

    private JPanel strechedPanel = new JPanel();
    private JPanel statisticPanel = new JPanel(new GridBagLayout());
    private JTextField nodataField = new JTextField();
    private JTextField maxdataField = new JTextField();
    private JTextField mindataField = new JTextField();
    public JCheckBox transparentBox = new JCheckBox();
    public JCheckBox intervalsBox = new JCheckBox();
    public JCheckBox discreteBox = new JCheckBox();
    public JCheckBox invertBox = new JCheckBox();
    public JCheckBox intervalBox = new JCheckBox();
    public String fromValueText = new String();
    public String toValueText = new String();
    private JLabel classes = new JLabel();
    private PlugInContext plugInContext;
    private Border border = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    private Border borderRaised = BorderFactory.createRaisedBevelBorder();
    private Color[] valuesColors;
    private LayoutManager layout = new BorderLayout();
    private JComponent comp;
    private LayoutManager gridBagLayout = new GridBagLayout();
    private ColorPanel NoDataColorPanel = new ColorPanel();
    private int alpha = 255;
    public int intColor;

    private String sToolTip = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Choose-a-color-range-It-will-be-automaticaly-expanded-between-the-2-values");
    private String sFromValue = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.From-value");
    private String sToValue = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.To-value");
    private static String STATISTICS = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.CellStatistics");
    private static String TITLE = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Raster-Color-Editor");
    private static String NUMBER = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.ClassifyAttributesPlugin.Number-of-classes")
            + ":";
    private static String NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.nodata");
    private static String MIN = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.min");
    private static String MAX = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.max");
    private static String TRANSPARENT = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Set-values-outside-transparent");
    private static String INVERT = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Invert-colors");
    private static String INTERVALS = I18N
            .get("org.openjump.core.ui.plugin.raster.color.RasterColorEditorPlugIn.Intervals");

    SpinnerModel spinner;

    public RasterColorEditorPanel(PlugInContext context,
            RasterImageLayer actualLayer) {
        super();
        plugInContext = context;
        this.layer= actualLayer;
        setVisible(true);
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void jbInit() throws Exception {

        setLayout(layout);

        // Giuseppe Aruta - 2015_4_21
        // First panel: show statistics of cells of the layer
        // Deactivate for now
        statisticPanel.setBorder(BorderFactory.createTitledBorder(STATISTICS));
        nodataField = new JTextField(String.valueOf(layer.getNoDataValue()));
        nodataField.setEditable(false);
        maxdataField = new JTextField(String.valueOf(layer.getMetadata()
                .getStats().getMax(0)));
        maxdataField.setEditable(false);
        mindataField = new JTextField(String.valueOf(layer.getMetadata()
                .getStats().getMin(0)));
        mindataField.setEditable(false);
        JLabel nd_label = new JLabel(NODATA);
        JLabel min_label = new JLabel(MIN);
        JLabel max_label = new JLabel(MAX);
        FormUtils.addRowInGBL(statisticPanel, 1, 0, nd_label, nodataField);
        FormUtils.addRowInGBL(statisticPanel, 1, 2, min_label, mindataField);
        FormUtils.addRowInGBL(statisticPanel, 1, 4, max_label, maxdataField);
        // add(statisticPanel, BorderLayout.PAGE_START);

        // Giuseppe Aruta - 2015_4_21
        // Second panel: Change color model
        strechedPanel.setBorder(border);
        strechedPanel.setLayout(gridBagLayout);
        strechedPanel.setBorder(BorderFactory.createTitledBorder(TITLE ));
        colorScaleChooser = new JComboBox(colorTableList);
        colorScaleChooser.setToolTipText(sToolTip); //$NON-NLS-1$
        colorScaleChooser.setBorder(borderRaised);
        intColor = colorScaleChooser.getSelectedIndex();
        colorScaleChooser.setSelectedIndex(intColor);
        FormUtils.addRowInGBL(strechedPanel, 2, 0, colorScaleChooser);
        fromValueLabel = new JLabel(sFromValue);
        toValueLabel = new JLabel(sToValue); //$NON-NLS-1$
        fromValueLabel.setPreferredSize(new Dimension(83, 20));
        toValueLabel.setPreferredSize(new Dimension(83, 20));

        fromValue = new JTextField(Double.toString(layer.getMetadata()
                .getStats().getMin(0)), 15);
        fromValue.setCaretPosition(0);
        fromValue.selectAll();
        toValue = new JTextField(Double.toString(layer.getMetadata().getStats()
                .getMax(0)), 15);
        toValue.setCaretPosition(0);
        toValue.selectAll();

        fromValue.setPreferredSize(new Dimension(83, 20));
        fromValue.setCaretPosition(fromValue.getText().length());
        toValue.setPreferredSize(new Dimension(83, 20));
        toValue.setCaretPosition(toValue.getText().length());

        FormUtils.addRowInGBL(strechedPanel, 3, 0, fromValueLabel, toValueLabel);
        FormUtils.addRowInGBL(strechedPanel, 4, 0, fromValue, toValue);

        classes.setText(NUMBER);
        classes.setEnabled(false);

        intervalsBox = new JCheckBox("Show colors as intervals");
        intervalsBox.setSelected(false);
        intervalsBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateComponents();

            }
        });

        spinner = new SpinnerNumberModel(6, 2, 65, 1);
        spinnerbox = new JSpinner(spinner);
        spinnerbox.setEnabled(false);

        spinnerbox.setMinimumSize(new Dimension(60, 20));
        spinnerbox.setPreferredSize(new Dimension(60, 20));

        invertBox = new JCheckBox(INVERT);
        invertBox.setSelected(false);

        transparentBox = new JCheckBox(TRANSPARENT);
        transparentBox.setSelected(false);

        FormUtils.addRowInGBL(strechedPanel, 5, 0, getIntervalBox());
        FormUtils.addRowInGBL(strechedPanel, 6, 0, classes);
        FormUtils.addRowInGBL(strechedPanel, 6, 1, spinnerbox, true, true);

        FormUtils.addRowInGBL(strechedPanel, 7, 0, invertBox);
        FormUtils.addRowInGBL(strechedPanel, 8, 0, transparentBox);

        if (layer.getNumBands() == 1) {
        	add(statisticPanel,BorderLayout.NORTH);
            add(strechedPanel, BorderLayout.SOUTH);}
       

    }


    private JCheckBox getIntervalBox() {
        if (intervalsBox == null) {
            intervalsBox = new JCheckBox();
            intervalsBox.setText(INTERVALS);
            intervalsBox.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent e) {
                    updateComponents();
                }
            });
        }
        return intervalsBox;
    }

    private void updateComponents() {
        spinnerbox.setEnabled(intervalsBox.isSelected() == true);
        classes.setEnabled(intervalsBox.isSelected() == true);
    }

    public String getTitle() {
        return TITLE;
    }

    public String validateInput() {
        return null;
    }

    public boolean areValuesOk() {
        return true;
    }
    
    public void changeColorsRamp(WorkbenchContext context, Color[] colors,
            Color noDataColor, double min, double max)
            throws NoninvertibleTransformException, IOException {

        if (colors == null || colors.length == 0) {
            layer.setNeedToKeepImage(false);
            layer.flushImages(true);
            // layer.setWholeImageEnvelope(layer.getWholeImageEnvelope());
            context.getLayerViewPanel().getViewport().update();
            return;
        }
        int step = 9;
        colorGenerator = new ColorGenerator(step, colors);
        // Deactivated. As 6 steps seems to work better than 35
        // colorGenerator = new ColorGenerator(35, colors);
        
        RasterSymbology symbology = new RasterSymbology(
                RasterSymbology.TYPE_RAMP);
         
        CopyColorIntoBlackBoard(context, layer, symbology);
        
        min = Double.parseDouble(fromValue.getText());
        max = Double.parseDouble(toValue.getText());
        // Max cell value taken from raster statistics. Need to exclude upper
        // values for the symbolizing
        double maxlayer = layer.getMetadata().getStats().getMax(0);
        double minlayer = layer.getMetadata().getStats().getMin(0);
        double interval = (max - min) / colorGenerator.getSteps();
        symbology.addColorMapEntry(layer.getNoDataValue(), noDataColor);
        // Giuseppe Aruta 2015_4_17 Set value outside min-max range to a light
        // grey-green color
        // Than the color can be set to transparent
        for (double i = minlayer; i < min; i++) {
            symbology.addColorMapEntry(i, new Color(202, 218, 186));// Color.BLACK);
        }
        for (double j = maxlayer; j > max; j--) {
            symbology.addColorMapEntry(j, new Color(202, 218, 186));// Color.BLACK);
        }
        for (int c = 0; c < colorGenerator.getSteps(); c++) {
            Color color = colorGenerator.getColor(c);
            double value = min + c * interval;
            symbology.addColorMapEntry(value, color);
        }
        layer.setSymbology(symbology);
        if (transparentBox.isSelected()) {
            layer.setTransparentColor(new Color(202, 218, 186));
        }
    }

    public void changeColorsIntervals(WorkbenchContext context, Color[] colors,
            Color noDataColor, double min, double max)
            throws NoninvertibleTransformException, IOException {

        if (colors == null || colors.length == 0) {
            layer.setNeedToKeepImage(false);
            layer.flushImages(true);
            // layer.setWholeImageEnvelope(layer.getWholeImageEnvelope());
            context.getLayerViewPanel().getViewport().update();
            return;
        }
        int step = (Integer) spinner.getValue();
        colorGenerator = new ColorGenerator(step, colors);
        // Deactivated. As 6 steps seems to work better than 35
        // colorGenerator = new ColorGenerator(35, colors);
        RasterSymbology symbology = new RasterSymbology(
                RasterSymbology.TYPE_INTERVALS);
       
        CopyColorIntoBlackBoard(context, layer, symbology);
        
        min = Double.parseDouble(fromValue.getText());
        max = Double.parseDouble(toValue.getText());
        // Max cell value taken from raster statistics. Need to exclude upper
        // values for the symbolizing
        double maxlayer = layer.getMetadata().getStats().getMax(0);
        double minlayer = layer.getMetadata().getStats().getMin(0);
        double interval = (max - min) / colorGenerator.getSteps();
        symbology.addColorMapEntry(layer.getNoDataValue(), noDataColor);
        // Giuseppe Aruta 2015_4_17 Set value outside min-max range to a light
        // grey-green color
        // Than the color can be set to transparent
        for (double i = minlayer; i < min; i++) {
            symbology.addColorMapEntry(i, new Color(202, 218, 186));// Color.BLACK);
        }
        for (double j = maxlayer; j > max; j--) {
            symbology.addColorMapEntry(j, new Color(202, 218, 186));// Color.BLACK);
        }
        for (int c = 0; c < colorGenerator.getSteps(); c++) {
            Color color = colorGenerator.getColor(c);
            double value = min + c * interval;
            symbology.addColorMapEntry(value, color);
        }
        layer.setSymbology(symbology);
        if (transparentBox.isSelected()) {
            layer.setTransparentColor(new Color(202, 218, 186));
        }
        
        
        
    }

    public void updateStyles() {
        switch (colorScaleChooser.getSelectedIndex()) {

        case 0: {
        	
        	if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.BLACK, Color.WHITE };
            } else {
                valuesColors = new Color[] {  Color.WHITE, Color.BLACK };
            }
        	break;
        }
        case 1: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.RED, Color.YELLOW,
                        Color.GREEN };
            } else {
                valuesColors = new Color[] { Color.GREEN, Color.YELLOW,
                        Color.RED };
            }
            break;
        }
        case 2: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.RED, Color.GREEN, Color.BLUE };
            } else {
                valuesColors = new Color[] { Color.BLUE, Color.GREEN, Color.RED };
            }
            break;
        }
        case 3: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.BLUE, Color.RED };
            } else {
                valuesColors = new Color[] { Color.RED, Color.BLUE };
            }
            break;
        }
        case 4: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.YELLOW, Color.RED };
            } else {
                valuesColors = new Color[] { Color.RED, Color.YELLOW };
            }
            break;
        }
        case 5: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.BLACK, Color.WHITE };
            } else {
                valuesColors = new Color[] { Color.WHITE, Color.BLACK };
            }
            break;
        }
        case 6: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.WHITE, Color.BLACK,
                        Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                        Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                        Color.WHITE, Color.BLACK };
            } else {
                valuesColors = new Color[] { Color.BLACK, Color.WHITE,
                        Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE,
                        Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE,
                        Color.BLACK, Color.WHITE };
            }
            break;
        }
        case 7: {
            // Rainbow
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.RED, Color.ORANGE,
                        Color.YELLOW,
                        Color.GREEN, //$NON-NLS-1$
                        Color.BLUE, Color.decode("#4B0082"),
                        Color.decode("#9400D3") };
            } else {

                valuesColors = new Color[] { Color.decode("#9400D3"), //$NON-NLS-1$
                        Color.decode("#4B0082"), Color.BLUE, Color.GREEN, //$NON-NLS-1$
                        Color.YELLOW, Color.ORANGE, Color.RED };
            }
            break;
        }
        case 8: {// Color Relief
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(215, 244, 244),
                        new Color(200, 55, 55), new Color(224, 108, 31),
                        new Color(251, 255, 128), new Color(46, 154, 88) };
            } else {
                valuesColors = new Color[] { new Color(46, 154, 88),
                        new Color(251, 255, 128), new Color(224, 108, 31),
                        new Color(200, 55, 55), new Color(215, 244, 244) };
            }
            break;
        }
        case 9: {
            // Topo
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(76, 0, 255),
                        new Color(0, 46, 255),
                        new Color(0, 229, 255),
                        new Color(77, 255, 0), //$NON-NLS-1$
                        new Color(255, 255, 0), new Color(255, 222, 89),
                        new Color(255, 224, 179) };

            } else {
                valuesColors = new Color[] { new Color(255, 224, 179),
                        new Color(255, 222, 89),
                        new Color(255, 255, 0),
                        new Color(77, 255, 0), //$NON-NLS-1$
                        new Color(0, 229, 255), new Color(0, 46, 255),
                        new Color(76, 0, 255) };
            }
            break;
        }
        case 10: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(43, 131, 186),
                        new Color(171, 221, 164), new Color(255, 255, 191),
                        new Color(253, 174, 97), new Color(215, 25, 28) };
            } else {
                valuesColors = new Color[] { new Color(215, 25, 28),
                        new Color(253, 174, 97), new Color(255, 255, 191),
                        new Color(171, 221, 164), new Color(43, 131, 186) };
            }
            break;
        }
        case 11: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(1, 133, 113),
                        new Color(128, 205, 193), new Color(245, 245, 245),
                        new Color(223, 194, 125), new Color(166, 97, 26) };
            } else {
                valuesColors = new Color[] { new Color(166, 97, 26),
                        new Color(223, 194, 125), new Color(245, 245, 245),
                        new Color(128, 205, 193), new Color(1, 133, 113) };
            }
            break;
        }
        case 12: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(5, 113, 176),
                        new Color(146, 197, 222), new Color(247, 247, 247),
                        new Color(244, 165, 130), new Color(202, 0, 32) };
            } else {
                valuesColors = new Color[] { new Color(202, 0, 32),
                        new Color(244, 165, 130), new Color(247, 247, 247),
                        new Color(146, 197, 222), new Color(5, 113, 176) };
            }
            break;
        }
        case 13: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(44, 123, 182),
                        new Color(171, 217, 233), new Color(255, 255, 191),
                        new Color(253, 174, 97), new Color(215, 25, 28) };
            } else {
                valuesColors = new Color[] { new Color(215, 25, 28),
                        new Color(253, 174, 97), new Color(255, 255, 191),
                        new Color(171, 217, 233), new Color(44, 123, 182) };
            }
            break;
        }
        case 14: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { new Color(26, 150, 65),
                        new Color(166, 217, 106), new Color(255, 255, 191),
                        new Color(253, 174, 97), new Color(215, 25, 28) };
            } else {
                valuesColors = new Color[] { new Color(215, 25, 28),
                        new Color(253, 174, 97), new Color(255, 255, 191),
                        new Color(166, 217, 106), new Color(26, 150, 65) };
            }
            break;
        }
        case 15: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.RED, Color.WHITE };
            } else {
                valuesColors = new Color[] { Color.WHITE, Color.RED };
            }
            break;
        }
        case 16: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.GREEN, Color.WHITE };
            } else {
                valuesColors = new Color[] { Color.WHITE, Color.GREEN };
            }
            break;
        }
        case 17: {
            if (invertBox.isSelected()) {
                valuesColors = new Color[] { Color.BLUE, Color.WHITE };
            } else {
                valuesColors = new Color[] { Color.WHITE, Color.BLUE };
            }
            break;
        }
        }
        try {

            if (intervalsBox.isSelected()) {
                changeColorsIntervals(plugInContext.getWorkbenchContext(),
                        valuesColors, GUIUtil.alphaColor(
                                NoDataColorPanel.getFillColor(), alpha),
                        Float.parseFloat(fromValue.getText()),
                        Float.parseFloat(toValue.getText()));

            } else {
                changeColorsRamp(plugInContext.getWorkbenchContext(),
                        valuesColors, GUIUtil.alphaColor(
                                NoDataColorPanel.getFillColor(), alpha),
                        Float.parseFloat(fromValue.getText()),
                        Float.parseFloat(toValue.getText()));
            }

            
            
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
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateStyles();

    }

    
    public void CopyColorIntoBlackBoard(WorkbenchContext context, RasterImageLayer layer,RasterSymbology symbology) 
   		 throws NoninvertibleTransformException{
   	 // String bboardKey = GUIUtils.getBBKey(String.valueOf(layer.getUUID()));
   	 String bboardKey = ChangeRasterImagePropertiesPlugIn.class.getName() +"-"+layer.getUUID()+ " - COLORSTYLE";
         context.getBlackboard().put(bboardKey, this);
         layer.setSymbology(symbology);
         context.getLayerManager().fireLayerChanged(layer, LayerEventType.APPEARANCE_CHANGED);
   	 
    }
    
}
