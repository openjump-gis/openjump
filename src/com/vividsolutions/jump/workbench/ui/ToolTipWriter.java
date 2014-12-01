package com.vividsolutions.jump.workbench.ui;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.*;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.cursortool.SpecifyFeaturesTool;
/**
 * To customize the LayerViewPanel's tooltips, call LayerViewPanel#setToolTipText.
 * You can specify attribute names in curly brackets e.g. {fid}.
 */
public class ToolTipWriter {
    private boolean enabled = false;
    private LayerViewPanel panel;
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public ToolTipWriter(LayerViewPanel panel) {
        this.panel = panel;
    }
    public String write(String template, Point2D mouseLocation) {
        int PIXEL_BUFFER = 2;
        if (!enabled) {
            return null;
        }
        Map layerToSpecifiedFeaturesMap;
        try {
            layerToSpecifiedFeaturesMap =
                SpecifyFeaturesTool.layerToSpecifiedFeaturesMap(
                    panel.getLayerManager().iterator(),
                    EnvelopeUtil.expand(
                        new Envelope(
                            panel.getViewport().toModelCoordinate(mouseLocation)),
                        PIXEL_BUFFER / panel.getViewport().getScale()));
        } catch (NoninvertibleTransformException e) {
            return "";
        }
        if (layerToSpecifiedFeaturesMap.isEmpty()) {
            return null;
        }
        if (template == null || template.trim().length() == 0) {
            return writeDefaultToolTip(layerToSpecifiedFeaturesMap);
        }
        String toolTip = template;
        for (Iterator i = extractAttributeNames(template).iterator(); i.hasNext();) {
            String attributeName = (String) i.next();
            toolTip =
                StringUtil.replaceAll(
                    toolTip,
                    "{" + attributeName + "}",
                    findValue(attributeName, layerToSpecifiedFeaturesMap));
        }
        return toolTip;
    }
    private String writeDefaultToolTip(Map layerToSpecifiedFeaturesMap) {
        Feature feature =
            (Feature) ((Collection) layerToSpecifiedFeaturesMap
                .values()
                .iterator()
                .next())
                .iterator()
                .next();
        String toolTip = "<html>";
        toolTip += format("FID", "" + feature.getID());
        for (int i = 0; i < Math.min(4, feature.getSchema().getAttributeCount()); i++) {
            if (feature.getSchema().getAttributeType(i) == AttributeType.GEOMETRY) {
                continue;
            }
            toolTip += "<br>"
                + format(feature.getSchema().getAttributeName(i), feature.getAttribute(i));
        }
        toolTip += "</html>";
        return toolTip;
    }
    private String format(String name, Object value) {
        return "<b>" + name + ":</b> " + value;
    }
    private String findValue(String attributeName, Map layerToSpecifiedFeaturesMap) {
        for (Iterator i = layerToSpecifiedFeaturesMap.keySet().iterator();
            i.hasNext();
            ) {
            Layer layer = (Layer) i.next();
            for (int j = 0;
                j
                    < layer
                        .getFeatureCollectionWrapper()
                        .getFeatureSchema()
                        .getAttributeCount();
                j++) {
                if ("fid".equalsIgnoreCase(attributeName)) {
                    return ""
                        + ((Feature) ((Collection) layerToSpecifiedFeaturesMap.get(layer))
                            .iterator()
                            .next())
                            .getID();
                }
                if (layer
                    .getFeatureCollectionWrapper()
                    .getFeatureSchema()
                    .getAttributeName(j)
                    .equalsIgnoreCase(attributeName)) {
                    return ""
                        + (
                            (Feature) ((Collection) layerToSpecifiedFeaturesMap
                                .get(layer))
                                .iterator()
                                .next())
                                .getAttribute(
                            j);
                }
            }
        }
        return "";
    }
    private Set extractAttributeNames(String template) {
        TreeSet attributeNames = new TreeSet();
        String currentAttributeName = "";
        for (int i = 0; i < template.length(); i++) {
            switch (template.charAt(i)) {
                case '{' :
                    currentAttributeName = "";
                    break;
                case '}' :
                    attributeNames.add(currentAttributeName.trim());
                    currentAttributeName = "";
                    break;
                default :
                    currentAttributeName += template.charAt(i);
                    break;
            }
        }
        return attributeNames;
    }
}
