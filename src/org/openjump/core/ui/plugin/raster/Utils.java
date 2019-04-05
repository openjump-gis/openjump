package org.openjump.core.ui.plugin.raster;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.util.ColorUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

public class Utils {

    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();

    /**
     * Zoom to the raster layer
     * @param rLayer
     * @throws NoninvertibleTransformException
     */
    public static void zoom(RasterImageLayer rLayer)
            throws NoninvertibleTransformException {
        frame.getContext()
                .getLayerViewPanel()
                .getViewport()
                .zoom(EnvelopeUtil.bufferByFraction(
                        rLayer.getWholeImageEnvelope(), 0.03));
    }

    /**
     * Apply a random generated color set style according to an attribute
     * @param layer to apply the style
     * @param attribute to use for the color classification
     * @throws Exception
     */

    public static void applyRandomGradualStyle(Layer layer, String attribute)
            throws Exception {
        final FeatureCollectionWrapper featDataset = layer
                .getFeatureCollectionWrapper();
        final ColorScheme colorScheme = ColorUtil
                .createRandomColorSchema(featDataset.size());
        final Map<Object, BasicStyle> attributeToStyleMap = new TreeMap<Object, BasicStyle>();
        for (final Iterator<Feature> i = featDataset.iterator(); i.hasNext();) {
            final Feature feature = i.next();
            attributeToStyleMap.put(feature.getAttribute(attribute),
                    new BasicStyle(colorScheme.next()));
        }
        layer.getBasicStyle().setEnabled(false);
        final ColorThemingStyle themeStyle = new ColorThemingStyle(attribute,
                attributeToStyleMap, new BasicStyle(Color.gray));
        themeStyle.setEnabled(true);
        layer.addStyle(themeStyle);
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.removeStyle(ColorThemingStyle.get(layer));
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.getBasicStyle().setEnabled(false);
        layer.fireAppearanceChanged();
    }

    /**
    * Apply a random generated color set style according to an attribute, defining a start color (lower value) and an end color (upper value)
     * @param layer
     * @param attribute
     * @param startColor
     * @param endColor
     * @throws Exception
     */
    public static void applyGradualStyle(Layer layer, String attribute,
            Color startColor, Color endColor) throws Exception {
        final FeatureCollectionWrapper featDataset = layer
                .getFeatureCollectionWrapper();
        final ColorScheme colorScheme = ColorUtil.createColorSchema(
                featDataset.size(), startColor, endColor);

        final Map<Object, BasicStyle> attributeToStyleMap = new TreeMap<Object, BasicStyle>();
        for (final Iterator<Feature> i = featDataset.iterator(); i.hasNext();) {
            final Feature feature = i.next();
            attributeToStyleMap.put(feature.getAttribute(attribute),
                    new BasicStyle(colorScheme.next()));
        }
        layer.getBasicStyle().setEnabled(false);
        final ColorThemingStyle themeStyle = new ColorThemingStyle(attribute,
                attributeToStyleMap, new BasicStyle(Color.gray));
        themeStyle.setEnabled(true);
        layer.addStyle(themeStyle);
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.removeStyle(ColorThemingStyle.get(layer));
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.getBasicStyle().setEnabled(false);
        layer.fireAppearanceChanged();
    }

    public static void applyAlternateStyle(Layer layer, String attribute,
            int interval, int subInterval, Color color1, Color color2)
            throws Exception {
        final FeatureCollectionWrapper featDataset = layer
                .getFeatureCollectionWrapper();

        final ArrayList<Color> arrayColor = new ArrayList<Color>();

        for (int c = 1; c < interval; c++) {

            if (c % subInterval == 0) {
                arrayColor.add(color1);
            } else {
                arrayColor.add(color2);
            }
        }

        final ColorScheme colorScheme = new ColorScheme("test", arrayColor);
        final Map<Object, BasicStyle> attributeToStyleMap = new TreeMap<Object, BasicStyle>();
        for (final Iterator<Feature> i = featDataset.iterator(); i.hasNext();) {
            final Feature feature = i.next();
            attributeToStyleMap.put(feature.getAttribute(attribute),
                    new BasicStyle(colorScheme.next()));
        }
        layer.getBasicStyle().setEnabled(false);
        final ColorThemingStyle themeStyle = new ColorThemingStyle(attribute,
                attributeToStyleMap, new BasicStyle(Color.gray));
        themeStyle.setEnabled(true);
        layer.addStyle(themeStyle);
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.removeStyle(ColorThemingStyle.get(layer));
        ColorThemingStyle.get(layer).setEnabled(true);
        layer.getBasicStyle().setEnabled(false);
        layer.fireAppearanceChanged();

    }

}
