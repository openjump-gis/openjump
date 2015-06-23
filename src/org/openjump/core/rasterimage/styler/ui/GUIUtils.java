package org.openjump.core.rasterimage.styler.ui;

import org.openjump.core.rasterimage.styler.RasterStylesExtension;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JPanel;
import org.openjump.core.rasterimage.styler.ColorMapEntry;
import org.openjump.core.rasterimage.RasterImageLayer;

/**
 *
 * @author GeomaticaEAmbiente
 */
public class GUIUtils {

    /**
     * Method to return the min value of a RasterImageLayer
     *
     * @param rasterData
     * @param rasterImageLayer
     * @return
     */
    public static Range getMinMaxRasterValue(double[] rasterData, RasterImageLayer rasterImageLayer) {

        double minValue = Double.MAX_VALUE;
        double maxValue = -minValue;

        for (int v = 0; v < rasterData.length; v++) {

            if (rasterData[v] < minValue && !(rasterImageLayer.isNoData(rasterData[v]))) {
                minValue = rasterData[v];
            }

            if (rasterData[v] > maxValue && !(rasterImageLayer.isNoData(rasterData[v]))) {
                maxValue = rasterData[v];
            }
        }

        return new Range(minValue, true, maxValue, true);

    }

    /**
     * Method to convert the trasparency value from 0-100 range (0 means that
     * the color is completely opaque and 100 the color is completely
     * trasparent) to 0-255 range (0 menas that the color is completely
     * trasparent and 255 that the color is completely opaque)
     *
     * @param range_0_100_Value
     * @return
     */
    public static int getAlpha_0_255Range(int range_0_100_Value) {

        int traspValue_100 = 100 - range_0_100_Value;
        int alphaValue = (255 * traspValue_100) / 100;

        return alphaValue;
    }

    /*
     * Method to convert the trasparency value from 0-100 range (0 means that the color 
     * is completely opaque and 100 the color is completely trasparent) to 0-1.0 range (0 menas that the color is
     * completely opaque and 1.0 that the color is completely trasparent)
     */
    public static float getAlpha_DecimalRange(int range_0_100_Value) {

//        float traspValue_100 = 100 - range_0_100_Value;
        float alphaValue = range_0_100_Value / 100f;

        return alphaValue;
    }

    /**
     * Method for displayin in a panel a gradient
     *
     * @param panel Panel on which display the gradient
     * @param colorMapEntries Colors that make up the gradient. The quantity
     * element in ColorMapEnty[] must have values from 0 to 1, in ascending
     * order and whitout repetition.
     */
    public void setGradientPanel(JPanel panel, ColorMapEntry[] colorMapEntries) {

        panel.removeAll();

        int width = panel.getPreferredSize().width;
        int height = panel.getPreferredSize().height;

        GradientCanvas gc = new GradientCanvas(colorMapEntries, width, height, GradientCanvas.GradientType.VERTICAL);
        gc.setPreferredSize(new Dimension((int) width, (int) height));
        panel.setLayout(new BorderLayout());
        panel.add(gc, BorderLayout.CENTER);

    }

    /**
     * Method to round a double value to a certain number of decimal place
     *
     * @param value Value to be rounded
     * @param decimalPlace number of decimal places
     * @return rounded value
     */
    public static double round(double value, int decimalPlace) {

        double factor = (double) Math.pow(10d, decimalPlace);
        double roundValue = Math.round(value * factor) / factor;

        return roundValue;
    }

    public static GradientComboBox createStandardGradientComboBox(int width, int height) {

        GradientComboBox jComboBox_Gradient = new GradientComboBox();
        jComboBox_Gradient.setRenderer(new GradientComboRenderer(width, height));

        // Add items
        List<ColorMapEntry[]> colorMaps_l = StandardGradientColors.getStandardGradientColors();
        for(ColorMapEntry[] colorMap : colorMaps_l) {
            
            GradientCanvas gradientCanvas = new GradientCanvas(colorMap,
                200, 18, GradientCanvas.GradientType.HORIZONTAL);
            jComboBox_Gradient.addItem(gradientCanvas);
            
        }
        
        return jComboBox_Gradient;

    }

    /**
     * Method to delete the RasterStylesDialog about a raster from the BlackBoard.
     * The method checks and deletes the properties about RasterStylesDialog for rasters that
     * are no more loaded on the TOC. 
     * @param context
     */
    public static void clearRasterStylerFromBlackBoard(WorkbenchContext context) {

        HashMap start_hm = context.getBlackboard().getProperties();
        String[] start_keys = (String[]) start_hm.keySet().toArray(new String[start_hm.size()]);

        HashMap end_hm = new HashMap();

        List<String> rasterStylerKeys = new ArrayList<String>();
        for (String start_key : start_keys) {
            if (start_key.endsWith(RasterStylesExtension.suffixBlackBKey)) {
                rasterStylerKeys.add(start_key);
            } else {
                end_hm.put(start_key, start_hm.get(start_key));
            }
        }

        Collection layers = context.getLayerManager().getLayerables(RasterImageLayer.class);

        for (String rasterStylerKey : rasterStylerKeys) {
            Iterator iter = layers.iterator();
            while (iter.hasNext()) {
                RasterImageLayer ril = (RasterImageLayer) iter.next();
                if (rasterStylerKey.equals(ril.getUUID() + RasterStylesExtension.suffixBlackBKey)) {
                    end_hm.put(rasterStylerKey, start_hm.get(rasterStylerKey));
                    break;
                }
            }
        }

        context.getBlackboard().setProperties(end_hm);

    }

    /**
     * Method to update gradientComboBoxes with new ColorMapEntry. All
     * gradientComboBoxes must have the same width anf height.
     *
     * @param colorMapEntries new colors for gradient
     * @param width width of gradientComboBoxes
     * @param height height of gradientComboBoxex
     *
     */
    public static void updateGradientComboBoxes(ColorMapEntry[] colorMapEntries, int width, int height) {

        GradientCanvas gc = new GradientCanvas(colorMapEntries, width, height, GradientCanvas.GradientType.HORIZONTAL);

        for (Object gradientComboBoxe : gradientComboBoxes) {
            GradientComboBox gcb = (GradientComboBox) gradientComboBoxe;
            gcb.addItem(gc);
        }
    }

    /**
     * Method to memorize in a List the GradientComboBoxes
     *
     * @param gradientCB GradientComboBox to add to List
     */
    public static void addGradientComboBoxToList(GradientComboBox gradientCB) {

        if (gradientComboBoxes == null) {
            gradientComboBoxes = new ArrayList<GradientComboBox>();
        }

        gradientComboBoxes.add(gradientCB);
    }
    
    public static String getBBKey(String rasterPath){
        
        String key = rasterPath.concat(RasterStylesExtension.suffixBlackBKey);
        
        return key;
    
    }

    private static List gradientComboBoxes = null;

}
