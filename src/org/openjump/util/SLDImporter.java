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
import static java.lang.Double.parseDouble;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
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

    private static final NamespaceContext NSCONTEXT = new NamespaceContext() {

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
            LinkedList<Element> elems = XPathUtils.getElements("//sld:Rule/sld:Name", doc.getDocumentElement(),
                    NSCONTEXT);
            for (Element e : elems) {
                list.add(e.getTextContent());
            }
        } catch (XPathExpressionException e) {
            // only happens if the xpath is not valid
            LOG.error(e);
            return null;
        }

        return list;
    }

    /**
     * Ignores any filters, and uses the information from Point-, Line- and
     * PolygonSymbolizers.
     * 
     * @param name
     * @param doc
     * @return a corresponding BasicStyle
     */
    public static BasicStyle getBasicStyle(String name, Document doc) {
        try {
            BasicStyle style = new BasicStyle();
            style.setRenderingFill(false);
            style.setRenderingFillPattern(false);
            style.setRenderingLine(false);
            style.setRenderingLinePattern(false);

            Element rule = XPathUtils.getElement("//sld:Rule[sld:Name='" + name + "']", doc.getDocumentElement(),
                    NSCONTEXT);

            Element symbolizer = XPathUtils.getElement("sld:PointSymbolizer", rule, NSCONTEXT);
            applyPointSymbolizer(symbolizer, style);

            symbolizer = XPathUtils.getElement("sld:LineSymbolizer", rule, NSCONTEXT);
            applyLineSymbolizer(symbolizer, style);

            symbolizer = XPathUtils.getElement("sld:PolygonSymbolizer", rule, NSCONTEXT);
            applyPolygonSymbolizer(symbolizer, style);

            return style;
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            return null;
        }
    }

    /**
     * @param name
     * @param doc
     * @return a vertex style, if a special one was found (use the basic style from #getBasicStyle if this is null)
     */
    public static VertexStyle getVertexStyle(String name, Document doc) {
        try {
            Element rule = XPathUtils.getElement("//sld:Rule[sld:Name='" + name + "']", doc.getDocumentElement(),
                    NSCONTEXT);

            Element symbolizer = XPathUtils.getElement("sld:PointSymbolizer", rule, NSCONTEXT);
            return applyPointSymbolizer(symbolizer, new BasicStyle());
        } catch (XPathExpressionException e) {
            // only happens if some xpath is not valid
            LOG.error(e);
            return null;
        }
    }

    private static VertexStyle applyPointSymbolizer(Element symbolizer, BasicStyle style)
            throws XPathExpressionException {
        if (symbolizer == null) {
            return null;
        }

        Element e = XPathUtils.getElement(".//sld:WellKnownName", symbolizer, NSCONTEXT);

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

        int size = 0;

        if (size != 0) {
            extra.setSize(size / 2);
        }

        Element fill = XPathUtils.getElement(".//sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = XPathUtils.getElement(".//sld:Stroke", symbolizer, NSCONTEXT);

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

        Element fill = XPathUtils.getElement("sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = XPathUtils.getElement("sld:Stroke", symbolizer, NSCONTEXT);

        applyFill(fill, style);
        applyStroke(stroke, style);

        return style;
    }

    private static BasicStyle applyPolygonSymbolizer(Element symbolizer, BasicStyle style)
            throws XPathExpressionException {
        if (symbolizer == null) {
            return null;
        }

        Element fill = XPathUtils.getElement("sld:Fill", symbolizer, NSCONTEXT);
        Element stroke = XPathUtils.getElement("sld:Stroke", symbolizer, NSCONTEXT);

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

        LinkedList<Element> params = XPathUtils.getElements("sld:CssParameter", fill, NSCONTEXT);

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

        LinkedList<Element> params = XPathUtils.getElements("sld:CssParameter", stroke, NSCONTEXT);

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
        e = XPathUtils.getElement("sld:OnlineResource", e, NSCONTEXT);

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

    // **************************************************************

    // private static LinkedList<LabelStyle> parseTextSymbolizer(Element
    // symbolizer) {
    // LabelStyle style = new LabelStyle();
    //
    // Element label = getElement("Label", symbolizer);
    //
    // String lAtt = getElement("PropertyName", OGCNS, label).getTextContent();
    // lAtt = lAtt.substring(lAtt.indexOf(':') + 1);
    // style.setAttribute(lAtt);
    //
    // List<Element> fills = getElements("Fill", symbolizer);
    // Element fill = null;
    // for (Element f : fills) {
    // if (f.getParentNode().getLocalName().equals("TextSymbolizer")) {
    // fill = f;
    // }
    // }
    //
    // LinkedList<Element> params = getElements("CssParameter", fill);
    //
    // for (Element p : params) {
    // String type = p.getAttribute("name");
    // String a = p.getTextContent();
    // if (a == null || a.trim().length() == 0) {
    // continue;
    // }
    //
    // a = a.trim();
    //
    // if (type.equals("fill")) {
    // style.setColor(decode(a));
    // }
    // }
    //
    // Element font = getElement("Font", symbolizer);
    //
    // params = getElements("CssParameter", font);
    //
    // String fFamily = null;
    // int fStyle = 0, fSize = 0;
    // for (Element p : params) {
    // String type = p.getAttribute("name");
    // String a = p.getTextContent();
    // if (a == null || a.trim().length() == 0) {
    // continue;
    // }
    //
    // a = a.trim();
    //
    // if (type.equals("font-family")) {
    // fFamily = a;
    // }
    //
    // if (type.equals("font-style")) {
    // if (a.equalsIgnoreCase("normal")) {
    // fStyle |= PLAIN;
    // }
    // if (a.equalsIgnoreCase("italic")) {
    // fStyle |= ITALIC;
    // }
    // }
    //
    // if (type.equals("font-weight")) {
    // if (a.equalsIgnoreCase("normal")) {
    // fStyle |= PLAIN;
    // }
    // if (a.equalsIgnoreCase("bold")) {
    // fStyle |= BOLD;
    // }
    // }
    //
    // if (type.equals("font-size")) {
    // fSize = (int) round(parseDouble(a));
    // }
    // }
    //
    // style.setFont(new Font(fFamily, fStyle, fSize));
    // style.setEnabled(true);
    //
    // Element halo = getElement("Halo", symbolizer);
    // if (halo != null) {
    // style.setOutlineShowing(true);
    // Element rad = getElement("Radius", halo);
    // if (rad != null) {
    // style.setOutlineWidth((int) parseDouble(rad.getTextContent()));
    // }
    //
    // params = getElements("CssParameter", getElement("Fill", halo));
    // for (Element p : params) {
    // String type = p.getAttribute("name");
    // String a = p.getTextContent();
    // if (a == null || a.trim().length() == 0) {
    // continue;
    // }
    //
    // a = a.trim();
    //
    // if (type.equals("fill")) {
    // style.setOutlineColor(decode(a));
    // }
    // }
    // }
    //
    // LinkedList<LabelStyle> list = new LinkedList<LabelStyle>();
    // list.add(style);
    //
    // return list;
    // }
    //
    // private static Object parseValues(Element filter) {
    // Element lower = getElement("LowerBoundary", OGCNS, filter);
    // Element upper = getElement("UpperBoundary", OGCNS, filter);
    // if (lower != null && upper != null) {
    // String s1 = getElement("Literal", OGCNS, lower).getTextContent().trim();
    // String s2 = getElement("Literal", OGCNS, upper).getTextContent().trim();
    // return new Range(s1, true, s2, false);
    // }
    //
    // return getElement("Literal", OGCNS, filter).getTextContent().trim();
    // }

    // note that not at all are all filters supported, they're (informally)
    // expected to be in the format of the SLD exporter
    // private static ColorThemingStyle parseColorThemingStyle(List<Element>
    // rules, List<Element> filters) {
    // ColorThemingStyle style = new ColorThemingStyle();
    //
    // String att = rules.get(0).getElementsByTagNameNS(OGCNS,
    // "PropertyName").item(0).getTextContent();
    // att = att.substring(att.indexOf(':') + 1);
    //
    // style.setAttributeName(att);
    // HashMap<Object, StrokeFillStyle> map = new HashMap<Object,
    // StrokeFillStyle>();
    // HashMap<Object, String> labelMap = new HashMap<Object, String>();
    //
    // Iterator<Element> rulesI = rules.iterator();
    // Iterator<Element> filtersI = filters.iterator();
    // while (rulesI.hasNext() && filtersI.hasNext()) {
    // Element filter = filtersI.next();
    // Element rule = rulesI.next();
    // Element symbolizer = getElement("PointSymbolizer", rule);
    //
    // if (symbolizer != null) {
    // StrokeFillStyle s = parsePointSymbolizer(symbolizer, null).getFirst();
    // s.setEnabled(true);
    // Object val = parseValues(filter);
    // map.put(val, s);
    // labelMap.put(val, val.toString());
    // if (style.getDefaultStyle() == null) {
    // style.setDefaultStyle((BasicStyle) s);
    // }
    // }
    //
    // symbolizer = getElement("LineSymbolizer", rule);
    //
    // if (symbolizer != null) {
    // StrokeFillStyle s = parseLineSymbolizer(symbolizer, null);
    // s.setEnabled(true);
    // Object val = parseValues(filter);
    // map.put(val, s);
    // labelMap.put(val, val.toString());
    // if (style.getDefaultStyle() == null) {
    // style.setDefaultStyle((BasicStyle) s);
    // }
    // }
    //
    // symbolizer = getElement("PolygonSymbolizer", rule);
    //
    // if (symbolizer != null) {
    // StrokeFillStyle s = parsePolygonSymbolizer(symbolizer, null);
    // Object val = parseValues(filter);
    // map.put(val, s);
    // labelMap.put(val, val.toString());
    // if (style.getDefaultStyle() == null) {
    // style.setDefaultStyle((BasicStyle) s);
    // }
    // }
    //
    // }
    //
    // style.setAttributeValueToBasicStyleMap(map);
    // style.setEnabled(true);
    // style.setAttributeValueToLabelMap(labelMap);
    //
    // return style;
    // }

//    /**
//     * @param doc
//     * @return a list of corresponding JUMP styles
//     */
//    public static LinkedList<Style> importSLD(Document doc) {
        // LinkedList<Style> styles = new LinkedList<Style>();
        //
        // // maybe ask which feature type style to use?
        //
        // NodeList nl = doc.getElementsByTagNameNS(SLDNS, "Rule");
        //
        // LOG.debug("Found " + nl.getLength() + " rules.");
        //
        // List<Element> rules = new LinkedList<Element>();
        // List<Element> filters = new LinkedList<Element>();
        // BasicStyle basicStyle = null;
        // for (int i = 0; i < nl.getLength(); ++i) {
        // Element rule = (Element) nl.item(i);
        // Element filter = getElement("Filter", OGCNS, rule);
        // if (filter != null && getElement("PropertyIsInstanceOf", OGCNS,
        // filter) == null) {
        // rules.add(rule);
        // filters.add(filter);
        // } else {
        // LinkedList<Element> symbolizers = getElements("PointSymbolizer",
        // rule);
        // for (Element s : symbolizers) {
        // if (basicStyle == null) {
        // LinkedList<StrokeFillStyle> all = parsePointSymbolizer(s, null);
        // styles.addAll(all);
        // basicStyle = (BasicStyle) all.getFirst();
        // } else {
        // styles.addAll(parsePointSymbolizer(s, basicStyle));
        // }
        // }
        // symbolizers = getElements("LineSymbolizer", rule);
        // for (Element s : symbolizers) {
        // styles.add(basicStyle = parseLineSymbolizer(s, basicStyle));
        // }
        // symbolizers = getElements("PolygonSymbolizer", rule);
        // for (Element s : symbolizers) {
        // styles.add(basicStyle = parsePolygonSymbolizer(s, basicStyle));
        // }
        // symbolizers = getElements("TextSymbolizer", rule);
        // for (Element s : symbolizers) {
        // styles.addAll(parseTextSymbolizer(s));
        // }
        // }
        // }
        //
        // if (rules.size() > 0 && filters.size() > 0) {
        // styles.add(parseColorThemingStyle(rules, filters));
        // }
        //
        // // remove duplicates
        // LinkedList<Style> oldStyles = new LinkedList<Style>(styles);
        // styles.clear();
        // for (Style s : oldStyles) {
        // if (!styles.contains(s)) {
        // styles.add(s);
        // }
        // }
        //
        // if (LOG.isDebugEnabled()) {
        // LOG.debug("Found styles:");
        // for (Style s : styles) {
        // LOG.debug(s.getClass());
        // }
        // }
        //
        // return styles;
//        return null;
//    }

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

    // private static LinkedList<Element> getElements(String name, Element e) {
    // if (e == null) {
    // return new LinkedList<Element>();
    // }
    //
    // NodeList nl = e.getElementsByTagNameNS(SLDNS, name);
    // if (nl.getLength() == 0) {
    // return new LinkedList<Element>();
    // }
    // LinkedList<Element> elems = new LinkedList<Element>();
    // for (int i = 0; i < nl.getLength(); ++i) {
    // elems.add((Element) nl.item(i));
    // }
    // return elems;
    // }
    //
    // private static Element getElement(String name, Element e) {
    // return getElement(name, SLDNS, e);
    // }
    //
    // private static Element getElement(String name, String ns, Element e) {
    // NodeList nl = e.getElementsByTagNameNS(ns, name);
    //
    // if (nl.getLength() == 0) {
    // return null;
    // }
    //
    // return (Element) nl.item(0);
    // }

}
