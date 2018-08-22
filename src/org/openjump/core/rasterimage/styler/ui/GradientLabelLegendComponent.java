package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openjump.core.rasterimage.styler.ColorMapEntry;

import com.vividsolutions.jump.I18N;

/**
 * Class to create the component formed by a JPanel and two JLabels. The JPanel
 * contain the gradient and the JLabels contain the values.
 * 
 * @author GeomaticaEAmbiente
 */
public class GradientLabelLegendComponent extends JComponent {

    public GradientLabelLegendComponent(TreeMap<Double, Color> colorMapEntries,
            double noDataValue, String rasterName) {
        colorMapEntries_tm = colorMapEntries;
        this.noDataValue = noDataValue;
        this.rasterName = rasterName;
        jInit();
    }

    /**
     * Method that initializes the component.
     */
    private void jInit() {

        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        // Find min and max values
        for (final Map.Entry<Double, Color> colorMapEntry : colorMapEntries_tm
                .entrySet()) {

            if (colorMapEntry.getKey() != noDataValue) {

                if (colorMapEntry.getKey() < minValue) {
                    minValue = colorMapEntry.getKey();
                }

                if (colorMapEntry.getKey() > maxValue) {
                    maxValue = colorMapEntry.getKey();
                }
            }
        }

        // Set color gradient
        // from Raster ColorMapEntry to Gradient colorMapEntry
        final ColorMapEntry[] paletteColorMapEntry = new ColorMapEntry[colorMapEntries_tm
                .size() - 1]; // without noDataValue
        int count = 0;
        Color noDataColor = null;
        for (final Map.Entry<Double, Color> colorMapEntry : colorMapEntries_tm
                .entrySet()) {
            if (colorMapEntry.getKey() != noDataValue) {
                final double quantity = (colorMapEntry.getKey() - minValue)
                        / (maxValue - minValue);
                paletteColorMapEntry[count] = new ColorMapEntry(quantity,
                        colorMapEntry.getValue());
                count++;
            } else {
                noDataColor = colorMapEntry.getValue();
            }
        }

        // Components
        final JPanel jPanel_Gradient = new JPanel();
        final JLabel jLabel_MaxValue = new JLabel();
        final JLabel jLabel_MinValue = new JLabel();
        final JLabel jLabel_RasterName = new JLabel(rasterName);
        // final JLabel jLabel_NoDataTitle = new JLabel(
        // bundle.getString("LegendDialog.NoDataValue.text"));
        final JLabel jLabel_NoDataTitle = new JLabel(
                I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata"));
        final JLabel jLabel_NoDataColor = new JLabel();
        final JLabel jLabel_NoDataValue = new JLabel(
                Double.toString(noDataValue));

        java.awt.GridBagConstraints gridBagConstraints = new GridBagConstraints();
        setLayout(new GridBagLayout());

        // Set components aspect
        // Raster name
        jLabel_RasterName.setFont(new Font("Tahoma", Font.BOLD, 12));
        jLabel_RasterName.setPreferredSize(new Dimension(50, 14));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
        add(jLabel_RasterName, gridBagConstraints);

        // Gradient panel
        jPanel_Gradient.setBorder(javax.swing.BorderFactory
                .createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel_Gradient.setMinimumSize(new java.awt.Dimension(40, 100));
        jPanel_Gradient.setPreferredSize(new java.awt.Dimension(40, 100));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 11, 0);
        add(jPanel_Gradient, gridBagConstraints);

        // Max value label
        jLabel_MaxValue.setText(Double.toString(GUIUtils.round(maxValue, 3)));
        jLabel_MaxValue.setMinimumSize(new java.awt.Dimension(50, 14));
        jLabel_MaxValue.setPreferredSize(new java.awt.Dimension(50, 14));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 5, 0, 0);
        add(jLabel_MaxValue, gridBagConstraints);

        // Min value label
        jLabel_MinValue.setText(Double.toString(GUIUtils.round(minValue, 3)));
        jLabel_MinValue.setMinimumSize(new java.awt.Dimension(50, 14));
        jLabel_MinValue.setPreferredSize(new java.awt.Dimension(50, 14));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 11, 0);
        add(jLabel_MinValue, gridBagConstraints);

        // NoData title label
        jLabel_NoDataTitle.setMinimumSize(new java.awt.Dimension(70, 14));
        jLabel_NoDataTitle.setPreferredSize(new java.awt.Dimension(70, 14));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        add(jLabel_NoDataTitle, gridBagConstraints);

        // NoData color label
        jLabel_NoDataColor.setBackground(noDataColor);
        jLabel_NoDataColor.setBorder(javax.swing.BorderFactory
                .createLineBorder(new java.awt.Color(0, 0, 0)));
        jLabel_NoDataColor.setMaximumSize(new java.awt.Dimension(40, 25));
        jLabel_NoDataColor.setMinimumSize(new java.awt.Dimension(40, 25));
        jLabel_NoDataColor.setPreferredSize(new java.awt.Dimension(40, 25));
        jLabel_NoDataColor.setOpaque(true);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        add(jLabel_NoDataColor, gridBagConstraints);

        // NoData value label
        jLabel_NoDataValue.setPreferredSize(new java.awt.Dimension(50, 14));
        jLabel_NoDataValue.setFont(new Font("Tahoma", Font.ITALIC, 11));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        add(jLabel_NoDataValue, gridBagConstraints);

        // Set gradient color panel
        final GUIUtils updatePanel = new GUIUtils();
        updatePanel.setGradientPanel(jPanel_Gradient, paletteColorMapEntry);

        setPreferredSize(new Dimension(200, 250));
    }

    private final TreeMap<Double, Color> colorMapEntries_tm;
    private final double noDataValue;
    private final String rasterName;
    private final java.util.ResourceBundle bundle = java.util.ResourceBundle
            .getBundle("org/openjump/core/rasterimage/styler/resources/Bundle"); // NOI18N
}
