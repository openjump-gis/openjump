//$HeadURL: https://sushibar/svn/deegree/base/trunk/resources/eclipse/svn_classfile_header_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.openjump.util;

import static java.awt.Color.decode;
import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;
import static java.awt.Font.PLAIN;
import static java.lang.Double.parseDouble;
import static java.lang.Math.round;
import static org.openjump.util.XPathUtils.getElement;
import static org.openjump.util.XPathUtils.getElements;
import static org.openjump.util.XPathUtils.getInt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.SquareVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

import de.latlon.deejump.plugin.style.BitmapVertexStyle;
import de.latlon.deejump.plugin.style.CircleVertexStyle;
import de.latlon.deejump.plugin.style.CrossVertexStyle;
import de.latlon.deejump.plugin.style.StarVertexStyle;
import de.latlon.deejump.plugin.style.TriangleVertexStyle;

/**
 * <code>SLDImporter</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class SLDImporter {

    private static final Logger LOG = Logger.getLogger(SLDImporter.class);

    /**
     * The SLD namespace URI.
     */
    public static final String SLDNS = "http://www.opengis.net/sld";

    /**
     * The OGC namespace URI.
     */
    public static final String OGCNS = "http://www.opengis.net/ogc";

    /**
     * Namespace context with sld and ogc namespaces.
     */
    public static final NamespaceContext NSCONTEXT = new NamespaceContext() {

        public String getNamespaceURI(String prefix) {
            if (prefix.equals("sld")) {
                return SLDNS;
            }
            if (prefix.equals("ogc")) {
                return OGCNS;
            }

            return null;
        }

        public String getPrefix(String namespace) {
            if (namespace.equals(SLDNS)) {
                return "sld";
            }

            if (namespace.equals(OGCNS)) {
                return "ogc";
            }

            return null;
        }

        public Iterator<String> getPrefixes(String namespace) {
            if (namespace.equals(SLDNS)) {
                return new Iterator<String>() {
                    boolean done = false;

                    public boolean hasNext() {
                        return !done;
                    }

                    public String next() {
                        done = true;
                        return "sld";
                    }

                    public void remove() {
                        // ignore
                    }
                };
            }

            if (namespace.equals(OGCNS)) {
                return new Iterator<String>() {
                    boolean done = false;

                    public boolean hasNext() {
                        return !done;
                    }

                    public String next() {
                        done = true;
                        return "ogc";
                    }

                    public void remove() {
                        // ignore
                    }
                };
            }

            return null;
        }
    };

    /**
     * @param doc
     * @return a list of SLD rule names
     */
    public static LinkedList<String> getRuleNames(Document doc) {
        LinkedList<String> list = new LinkedList<String>();

        try {
            LinkedList<Element> elems = getElements("//sld:Rule/sld:Name", doc.getDocumentElement(), NSCONTEXT);
            for (Element e : elems) {
                list.add(e.getTextContent());
            }
        } catch (XPathExpressionException e) {
            // only happens if the xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }

        return list;
    }

    /**
     * @param doc
     * @return a list of SLD rule names
     */
    public static LinkedList<String> getRuleNamesWithGeometrySymbolizers(Document doc) {
        LinkedList<String> list = new LinkedList<String>();

        try {
            LinkedList<Element> elems = getElements("//sld:Rule[(count(sld:PointSymbolizer)+count(sld:LineSymbolizer)+"
                    + "count(sld:PolygonSymbolizer))>0]/sld:Name", doc.getDocumentElement(), NSCONTEXT);
            for (Element e : elems) {
                list.add(e.getTextContent());
            }
        } catch (XPathExpressionException e) {
            // only happens if the xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }

        return list;
    }

    /**
     * @param doc
     * @return a list of SLD FeatureTypeStyle names
     */
    public static LinkedList<String> getPossibleColorThemingStyleNames(Document doc) {
        LinkedList<String> list = new LinkedList<String>();

        try {
            LinkedList<Element> elems = getElements("//sld:UserStyle[count(sld:FeatureTypeStyle"
                    + "/sld:Rule/ogc:Filter) > 0]/sld:Name", doc.getDocumentElement(), NSCONTEXT);
            for (Element e : elems) {
                list.add(e.getTextContent());
            }
        } catch (XPathExpressionException e) {
            // only happens if the xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }

        return list;
    }

    /**
     * @param doc
     * @return a list of SLD rule names
     */
    public static LinkedList<String> getRuleNamesWithTextSymbolizers(Document doc) {
        LinkedList<String> list = new LinkedList<String>();

        try {
            LinkedList<Element> elems = getElements("//sld:Rule[count(sld:TextSymbolizer)>0]/sld:Name", doc
                    .getDocumentElement(), NSCONTEXT);
            for (Element e : elems) {
                list.add(e.getTextContent());
            }
        } catch (XPathExpressionException e) {
            // only happens if the xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }

        return list;
    }

    /**
     * @param name
     * @param doc
     * @return a corresponding BasicStyle
     * @see #getBasicStyle(Element)
     */
    public static BasicStyle getBasicStyle(String name, Document doc) {
        try {
            return getBasicStyle(getElement("//sld:Rule[sld:Name='" + name + "']", doc.getDocumentElement(), NSCONTEXT));
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ignores any filters, and uses the information from Point-, Line- and
     * PolygonSymbolizers.
     * 
     * @param rule
     * @return a corresponding BasicStyle
     */
    public static BasicStyle getBasicStyle(Element rule) {
        if (rule == null) {
            return null;
        }

        try {
            BasicStyle style = new BasicStyle();
            style.setRenderingFill(false);
            style.setRenderingFillPattern(false);
            style.setRenderingLine(false);
            style.setRenderingLinePattern(false);

            boolean oneApplied = false;

            Element symbolizer = getElement("sld:PointSymbolizer", rule, NSCONTEXT);
            if (symbolizer != null) {
                oneApplied = true;
            }
            applyPointSymbolizer(symbolizer, style);

            symbolizer = getElement("sld:LineSymbolizer", rule, NSCONTEXT);
            if (symbolizer != null) {
                oneApplied = true;
            }
            applyLineSymbolizer(symbolizer, style);

            symbolizer = getElement("sld:PolygonSymbolizer", rule, NSCONTEXT);
            if (symbolizer != null) {
                oneApplied = true;
            }
            applyPolygonSymbolizer(symbolizer, style);

            if (!oneApplied) {
                return null;
            }

            return style;
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param name
     * @param doc
     * @return a vertex style, if a special one was found (use the basic style
     *         from #getBasicStyle if this is null)
     */
    public static VertexStyle getVertexStyle(String name, Document doc) {
        try {
            Element rule = getElement("//sld:Rule[sld:Name='" + name + "']", doc.getDocumentElement(), NSCONTEXT);

            if (rule == null) {
                return null;
            }

            Element symbolizer = getElement("sld:PointSymbolizer", rule, NSCONTEXT);
            return applyPointSymbolizer(symbolizer, new BasicStyle());
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    private static VertexStyle applyPointSymbolizer(Element symbolizer, BasicStyle style)
            throws XPathExpressionException {
        if (symbolizer == null) {
            return null;
        }

        Element e = getElement(".//sld:WellKnownName", symbolizer, NSCONTEXT);

        VertexStyle extra = null;

        if (e != null) {
            String n = e.getTextContent();
            if (n != null) {
                n = n.trim();
            }

            if (n != null) {
                if (n.equalsIgnoreCase("circle")) {
                    extra = new CircleVertexStyle();
                }
                if (n.equalsIgnoreCase("cross")) {
                    extra = new CrossVertexStyle();
                }
                if (n.equalsIgnoreCase("square")) {
                    extra = new SquareVertexStyle();
                }
                if (n.equalsIgnoreCase("star")) {
                    extra = new StarVertexStyle();
                }
                if (n.equalsIgnoreCase("triangle")) {
                    extra = new TriangleVertexStyle();
                }
            }
        }

        if (extra == null) {
            extra = parseGraphic(symbolizer);
        }

        int size = getInt("sld:size", symbolizer, NSCONTEXT);

        if (size != 0) {
            extra.setSize(size / 2);
        }

        Element fill = getElement(".//sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = getElement(".//sld:Stroke", symbolizer, NSCONTEXT);

        applyFill(fill, style);
        applyStroke(stroke, style);
        if (extra != null) {
            applyFill(fill, extra);
            applyStroke(stroke, extra);
        }

        return extra;
    }

    private static BasicStyle applyLineSymbolizer(Element symbolizer, BasicStyle style) throws XPathExpressionException {
        if (symbolizer == null) {
            return null;
        }

        Element fill = getElement("sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = getElement("sld:Stroke", symbolizer, NSCONTEXT);

        applyFill(fill, style);
        applyStroke(stroke, style);

        return style;
    }

    private static BasicStyle applyPolygonSymbolizer(Element symbolizer, BasicStyle style)
            throws XPathExpressionException {
        if (symbolizer == null) {
            return null;
        }

        Element fill = getElement("sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = getElement("sld:Stroke", symbolizer, NSCONTEXT);

        URL u = parseGraphicURL(symbolizer);
        if (u != null) {
            Paint p = new CustomTexturePaint(u);
            style.setFillPattern(p);
            style.setRenderingFillPattern(true);
        }

        applyFill(fill, style);
        applyStroke(stroke, style);

        return style;
    }

    private static void applyFill(Element fill, StrokeFillStyle style) throws XPathExpressionException {
        if (fill == null) {
            return;
        }

        if (style instanceof BasicStyle) {
            ((BasicStyle) style).setRenderingFill(true);
        }

        LinkedList<Element> params = getElements("sld:CssParameter", fill, NSCONTEXT);

        for (Element p : params) {
            String type = p.getAttribute("name");
            String a = p.getTextContent();
            if (a == null || a.trim().length() == 0) {
                continue;
            }

            a = a.trim();

            if (type.equals("fill")) {
                style.setFillColor(decode(a));
            }

            if (type.equals("fill-opacity")) {
                style.setAlpha((int) (255 * parseDouble(a)));
            }
        }
    }

    private static void applyStroke(Element stroke, StrokeFillStyle style) throws XPathExpressionException {
        if (stroke == null) {
            return;
        }

        if (style instanceof BasicStyle) {
            ((BasicStyle) style).setRenderingLine(true);
        }

        LinkedList<Element> params = getElements("sld:CssParameter", stroke, NSCONTEXT);

        for (Element p : params) {
            String type = p.getAttribute("name");
            String a = p.getTextContent();
            if (a == null || a.trim().length() == 0) {
                continue;
            }

            a = a.trim();

            if (type.equals("stroke")) {
                style.setLineColor(decode(a));
            }

            if (type.equals("stroke-width")) {
                style.setLineWidth((int) parseDouble(a));
            }

            if (type.equals("stroke-opacity")) {
                style.setAlpha((int) (255 * parseDouble(a)));
            }

            if (type.equals("stroke-dasharray")) {
                style.setLinePattern(a.replace(' ', ','));
                style.setRenderingLinePattern(true);
            }
        }
    }

    private static URL parseGraphicURL(Element e) throws XPathExpressionException {
        e = getElement(".//sld:OnlineResource", e, NSCONTEXT);

        if (e == null) {
            return null;
        }

        // assume, it's an external graphic
        String s = e.getAttributeNS("http://www.w3.org/1999/xlink", "href");
        URL u = null;
        try {
            u = new URL(s);
        } catch (MalformedURLException ex) {
            try {
                u = new File(s).toURI().toURL();
            } catch (MalformedURLException e1) {
                // ignore it
            }
        }

        return u;
    }

    private static VertexStyle parseGraphic(Element e) throws XPathExpressionException {
        URL u = parseGraphicURL(e);
        if (u != null) {
            return new BitmapVertexStyle(u.getFile());
        }

        return null;
    }

    /**
     * Converts a TextSymbolizer.
     * 
     * @param name
     * @param doc
     * @return the label style or null, if none was found
     */
    public static LabelStyle getLabelStyle(String name, Document doc) {
        try {
            LabelStyle style = new LabelStyle();

            Element symbolizer = getElement("//sld:Rule[sld:Name='" + name + "']/sld:TextSymbolizer", doc
                    .getDocumentElement(), NSCONTEXT);

            if (symbolizer == null) {
                return null;
            }

            Element label = getElement("sld:Label", symbolizer, NSCONTEXT);

            String lAtt = getElement("ogc:PropertyName", label, NSCONTEXT).getTextContent();
            lAtt = lAtt.substring(lAtt.indexOf(':') + 1);
            style.setAttribute(lAtt);

            Element fill = getElement("sld:Fill", symbolizer, NSCONTEXT);

            if (fill != null) {
                LinkedList<Element> params = getElements("sld:CssParameter", fill, NSCONTEXT);

                for (Element p : params) {
                    String type = p.getAttribute("name");
                    String a = p.getTextContent();
                    if (a == null || a.trim().length() == 0) {
                        continue;
                    }

                    a = a.trim();

                    if (type.equals("fill")) {
                        style.setColor(decode(a));
                    }
                }
            }

            Element font = getElement("sld:Font", symbolizer, NSCONTEXT);

            LinkedList<Element> params = getElements("sld:CssParameter", font, NSCONTEXT);

            String fFamily = null;
            int fStyle = 0, fSize = 0;
            for (Element p : params) {
                String type = p.getAttribute("name");
                String a = p.getTextContent();
                if (a == null || a.trim().length() == 0) {
                    continue;
                }

                a = a.trim();

                if (type.equals("font-family")) {
                    fFamily = a;
                }

                if (type.equals("font-style")) {
                    if (a.equalsIgnoreCase("normal")) {
                        fStyle |= PLAIN;
                    }
                    if (a.equalsIgnoreCase("italic")) {
                        fStyle |= ITALIC;
                    }
                }

                if (type.equals("font-weight")) {
                    if (a.equalsIgnoreCase("normal")) {
                        fStyle |= PLAIN;
                    }
                    if (a.equalsIgnoreCase("bold")) {
                        fStyle |= BOLD;
                    }
                }

                if (type.equals("font-size")) {
                    fSize = (int) round(parseDouble(a));
                }
            }

            style.setFont(new Font(fFamily, fStyle, fSize));
            style.setEnabled(true);

            Element halo = getElement("sld:Halo", symbolizer, NSCONTEXT);
            if (halo != null) {
                style.setOutlineShowing(true);
                Element rad = getElement("sld:Radius", halo, NSCONTEXT);
                if (rad != null) {
                    style.setOutlineWidth((int) parseDouble(rad.getTextContent()));
                }

                params = getElements("sld:CssParameter", getElement("sld:Fill", halo, NSCONTEXT), NSCONTEXT);
                for (Element p : params) {
                    String type = p.getAttribute("name");
                    String a = p.getTextContent();
                    if (a == null || a.trim().length() == 0) {
                        continue;
                    }

                    a = a.trim();

                    if (type.equals("fill")) {
                        style.setOutlineColor(decode(a));
                    }
                }
            }
            return style;
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    // TODO this method does not really check for the content of the filter
    // expressions
    // it assumes ogc:And structures, so in most cases this method will actually
    // return something that's WRONG
    private static Object parseValues(Element filter) throws XPathExpressionException {
        if (filter == null) {
            LOG.warn("An ogc:filter could not be found while trying to parse a color theming style.");
            return null;
        }

        Element lower = getElement(".//ogc:LowerBoundary", filter, NSCONTEXT);
        Element upper = getElement(".//ogc:UpperBoundary", filter, NSCONTEXT);
        if (lower != null && upper != null) {
            String s1 = getElement("ogc:Literal", lower, NSCONTEXT).getTextContent().trim();
            String s2 = getElement("ogc:Literal", upper, NSCONTEXT).getTextContent().trim();
            return new Range(s1, true, s2, false);
        }

        // try different filters, as used by uDig
        boolean lowerEqual = false;
        boolean upperEqual = false;
        upper = getElement(".//ogc:PropertyIsLessThan", filter, NSCONTEXT);
        if (upper == null) {
            upper = getElement(".//ogc:PropertyIsLessThanOrEqualTo", filter, NSCONTEXT);
            upperEqual = true;
        }
        lower = getElement(".//ogc:PropertyIsGreaterThan", filter, NSCONTEXT);
        if (lower == null) {
            lower = getElement(".//ogc:PropertyIsGreaterThanOrEqualTo", filter, NSCONTEXT);
            lowerEqual = true;
        }
        if (lower != null && upper != null) {
            String s1 = getElement("ogc:Literal", lower, NSCONTEXT).getTextContent().trim();
            String s2 = getElement("ogc:Literal", upper, NSCONTEXT).getTextContent().trim();
            return new Range(s1, lowerEqual, s2, upperEqual);
        }

        return getElement(".//ogc:Literal", filter, NSCONTEXT).getTextContent().trim();
    }

    /**
     * @param name
     *            the name of the feature type style
     * @param doc
     * @return the color theming style
     */
    public static ColorThemingStyle getColorThemingStyle(String name, Document doc) {
        try {
            Element featureTypeStyle = getElement("//sld:UserStyle[(count(sld:FeatureTypeStyle"
                    + "/sld:Rule/ogc:Filter) > 0) and sld:Name='" + name + "']/sld:FeatureTypeStyle", doc
                    .getDocumentElement(), NSCONTEXT);

            if (featureTypeStyle == null) {
                return null;
            }

            ColorThemingStyle style = new ColorThemingStyle();
            String att = getElement(".//ogc:PropertyName", featureTypeStyle, NSCONTEXT).getTextContent();
            style.setAttributeName(att);

            //HashMap<Object, StrokeFillStyle> map = new HashMap<Object, StrokeFillStyle>();
            HashMap<Object, BasicStyle> map = new HashMap<Object, BasicStyle>();
            HashMap<Object, String> labelMap = new HashMap<Object, String>();

            LinkedList<Element> rules = getElements("sld:Rule", featureTypeStyle, NSCONTEXT);
            for (Element rule : rules) {
                BasicStyle basic = getBasicStyle(rule);
                Object val = parseValues(getElement("ogc:Filter", rule, NSCONTEXT));
                if (val != null) {
                    map.put(val, basic);
                    labelMap.put(val, val.toString());
                }
            }

            style.setAttributeValueToBasicStyleMap(map);
            style.setAttributeValueToLabelMap(labelMap);

            return style;
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <code>FillStyle</code>
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:$
     * 
     * @version $Revision:$, $Date:$
     */
    public static interface FillStyle {

        /**
         * @param c
         */
        public void setFillColor(Color c);

        /**
         * @param a
         */
        public void setAlpha(int a);

    }

    /**
     * <code>StrokeStyle</code>
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:$
     * 
     * @version $Revision:$, $Date:$
     */
    public static interface StrokeStyle {

        /**
         * @param c
         */
        public void setLineColor(Color c);

        /**
         * @param w
         */
        public void setLineWidth(int w);

        /**
         * @param a
         */
        public void setAlpha(int a);

        /**
         * @param b
         * @return a basic style
         */
        public BasicStyle setRenderingLinePattern(boolean b);

        /**
         * @param p
         * @return a basic style
         */
        public BasicStyle setLinePattern(String p);

    }

    /**
     * <code>SizedStyle</code>
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:$
     * 
     * @version $Revision:$, $Date:$
     */
    public static interface SizedStyle {

        /**
         * @param s
         */
        public void setSize(int s);

    }

    /**
     * <code>StrokeFillStyle</code>
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:$
     * 
     * @version $Revision:$, $Date:$
     */
    public static interface StrokeFillStyle extends StrokeStyle, FillStyle, Style {

        // no methods, they're combined by the stroke and fill interfaces

    }

    /**
     * <code>SizedStrokeFillStyle</code>
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:$
     * 
     * @version $Revision:$, $Date:$
     */
    public static interface SizedStrokeFillStyle extends StrokeFillStyle, SizedStyle {
        // no methods, they're combined
    }

}
