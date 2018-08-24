package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;

import com.vividsolutions.jump.I18N;

/**
 *
 * @author GeomaticaEAmbiente
 */
public class ColorsLabelLegendComponent extends JComponent {

    /**
     * Constructor to create the new component formed by a JButton and a JLabel.
     *
     * @param colorMapEntries
     * @param noDataValue
     * @param rasterName
     * @throws Exception
     */
    public ColorsLabelLegendComponent(TreeMap<Double, Color> colorMapEntries,
            double noDataValue, String rasterName) throws Exception {

        colorMapEntries_tm = colorMapEntries;
        this.noDataValue = noDataValue;
        this.rasterName = rasterName;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final int x = 20;
        int y;
        final int startY = 45;
        final int step = 30;
        int maxWidth = 100;
        final FontMetrics m = g.getFontMetrics();

        int i = 0, w;
        Color color;
        Color noDataColor = null;

        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.black);

        g.setFont(new Font("Tahoma", Font.BOLD, 14));
        g.drawString(rasterName, x, 20);

        g.setFont(new Font("Tahoma", Font.PLAIN, 12));

        // for (int n = 0; n < colorMapEntry.length; n++) {//for (Iterator iter
        // = pairs.iterator(); iter.hasNext();) {

        for (final Map.Entry<Double, Color> colorMapEntry : colorMapEntries_tm
                .entrySet()) {

            if (colorMapEntry.getKey() == noDataValue) {
                noDataColor = colorMapEntry.getValue();
                continue;
            }

            y = startY + (step * i++);

            color = colorMapEntry.getValue();

            g.setColor(color);
            g.fillRect(x, y, 40, 25);
            g.setColor(Color.black);
            g.drawRect(x, y, 40, 25);

            // g.setColor(Color.black);
            final String value = Double.toString(colorMapEntry.getKey());
            g.drawString(value, x + 60, y + 18);

            w = m.stringWidth(value);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        y = startY + (step * i++);

        g.setFont(new Font("Tahoma", Font.PLAIN, 12));
        // g.drawString("NoDataValue", x, y + 20);
        final String jLabel_NoDataTitle = I18N
                .get("org.openjump.core.ui.plugin.raster.nodata.nodata");
        g.drawString(jLabel_NoDataTitle, x, y + 20);
        g.setFont(new Font("Tahoma", Font.PLAIN, 11));
        y = startY + (step * i++);

        g.setColor(Color.BLACK);

        g.drawRect(x, y, 40, 25);
        if (noDataColor != null) {
            g.setColor(noDataColor);
            g.fillRect(x, y, 40, 25);
            g.setColor(Color.BLACK);
        }
        g.drawString(Double.toString(noDataValue), x + 60, y + 18);

        dimension = new Dimension(maxWidth, startY + (step * i++));
        setPreferredSize(dimension);

    }

    private final java.util.ResourceBundle bundle = java.util.ResourceBundle
            .getBundle("org/openjump/core/rasterimage/styler/resources/Bundle"); // NOI18N
    private final TreeMap<Double, Color> colorMapEntries_tm;
    private final double noDataValue;
    private Dimension dimension;
    private final String rasterName;

}
