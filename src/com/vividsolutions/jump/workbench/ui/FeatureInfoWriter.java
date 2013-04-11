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
package com.vividsolutions.jump.workbench.ui;
import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
public class FeatureInfoWriter {
    public static interface Writer {
        public String toHTML(Feature feature);
    }
    public static Writer ATTRIBUTE_WRITER = new Writer() {
        public String toHTML(Feature feature) {
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < feature.getSchema().getAttributeCount(); i++) {
                if (feature.getAttribute(i) instanceof Geometry) {
                    continue;
                }
                s.append(
                    "<br><b>"
                        + GUIUtil.escapeHTML(
                            feature.getSchema().getAttributeName(i),
                            false,
                            false)
                        + ":</b> ");
                if (feature.getAttribute(i) == null) {
                    //do nothing
                } else {
                    s.append(
                        GUIUtil.escapeHTML(
                            feature.getAttribute(i).toString(),
                            false,
                            false));
                }
            }
            return s.toString();
        }
    };

    public static Writer EMPTY_WRITER = new Writer() {
        public String toHTML(Feature feature) {
            return "";
        }
    };
    
    //Row-stripe colour recommended in
    //Java Look and Feel Design Guidelines: Advanced Topics [Jon Aquino]
    private final static String BEIGE = "#E6E6E6";
    private final static String WHITE = "#FFFFFF";
    private final static String COLOR1 = BEIGE;
    private final static String COLOR2 = WHITE;
    
    private boolean workingAroundJEditorPaneBug = true;
    public Color sidebarColor(Layer layer) {
        Color basicColor =
            layer.getBasicStyle().isRenderingFill()
                ? layer.getBasicStyle().getFillColor()
                : layer.getBasicStyle().getLineColor();
        int alpha = layer.getBasicStyle().getAlpha();
        return GUIUtil.toSimulatedTransparency(GUIUtil.alphaColor(basicColor, alpha));
    }
    public String writeGeom(
        Map layerToFeaturesMap,
        Writer featureWriter,
        Writer attributeWriter) {
        if (layerToFeaturesMap.isEmpty()) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (Iterator i = layerToFeaturesMap.keySet().iterator(); i.hasNext();) {
            Layer layer = (Layer) i.next();
            Collection features = (Collection) layerToFeaturesMap.get(layer);
            stringBuffer.append("<table width=100%>");
            stringBuffer.append("  <tr>");
            stringBuffer.append(
                "    <td width=5 bgcolor=" + toHTML(sidebarColor(layer)) + ">");
            stringBuffer.append("    </td>");
            //I'm setting the column 1 width to 5 and the column 2 width to 100%.
            //Theoretically column 2 should smush column 1, but this isn't happening.
            //I hope this behaviour will continue in future Java versions.
            //(See Dahm, Tom. "HTML Tip: Making a Wild Card Column Width." March 2000.
            //Available from http://www.netmechanic.com/news/vol3/html_no3.htm.
            //Internet; accessed 29 October 2002.)
            //[Jon Aquino]
            stringBuffer.append("    <td width=100%>");
            stringBuffer.append("      <table width=100%>");
            stringBuffer.append("        <tr>");
            stringBuffer.append("          <td bgcolor=#FFFFCC>");
            stringBuffer.append("            <B>" + layer.getName() + "</B>");
            stringBuffer.append("          </td>");
            stringBuffer.append("        </tr>");
            String bgcolor = COLOR1;
            for (Iterator j = features.iterator(); j.hasNext();) {
                Feature feature = (Feature) j.next();
                if (!bgcolor.equals(COLOR1)) {
                    bgcolor = COLOR1;
                } else {
                    bgcolor = COLOR2;
                }
                stringBuffer.append("        <tr bgcolor='" + bgcolor + "'>");
                stringBuffer.append("          <td>");
                stringBuffer.append(
                    "            FID <font color='#3300cc'><b>"
                        + feature.getID()
                        + "</b></font>");
                if (featureWriter != EMPTY_WRITER) {
                    append(feature, stringBuffer, featureWriter);
                }
                if (attributeWriter != EMPTY_WRITER) {
                    stringBuffer.append(
                        "            " + attributeWriter.toHTML(feature));
                }
                stringBuffer.append("          </td>");
                stringBuffer.append("        </tr>");
            }
            stringBuffer.append("      </table>");
            stringBuffer.append("    </td>");
            stringBuffer.append("  </tr>");
            stringBuffer.append("</table>");
        }
        return stringBuffer.toString();
    }
    private String pad(String s) {
        return (s.length() == 1) ? ("0" + s) : s;
    }
    private String toHTML(Color color) {
        String colorString = "#";
        colorString += pad(Integer.toHexString(color.getRed()));
        colorString += pad(Integer.toHexString(color.getGreen()));
        colorString += pad(Integer.toHexString(color.getBlue()));
        return colorString;
    }
    private void append(
        Feature feature,
        StringBuffer stringBuffer,
        Writer featureWriter) {
        String text = featureWriter.toHTML(feature);
        if (workingAroundJEditorPaneBug
            && ((stringBuffer.length() + featureWriter.toHTML(feature).length())
                > (32768 - 2000))) {
            //See http://developer.java.sun.com/developer/bugParade/bugs/4775730.html. [Jon Aquino]
            text = I18N.get("ui.FeatureInfoWriter.text-representation-of-geometry-is-too-large");
        }
        stringBuffer.append(text);
    }

}
