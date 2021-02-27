/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/exse/
 lat/lon Fitzke/Fretter/Poth GbR
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon Fitzke/Fretter/Poth GbR
 Meckenheimer Allee 176
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Jens Fitzke
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: jens.fitzke@uni-bonn.de

 
 ---------------------------------------------------------------------------*/
package de.latlon.deejump.plugin.style;

import static de.latlon.deejump.plugin.style.BitmapVertexStyle.getUpdatedSVGImage;
import static javax.imageio.ImageIO.write;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicFillPattern;
import com.vividsolutions.jump.workbench.ui.renderer.style.WKTFillPattern;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei </a>
 * 
 */
public class XSLUtility {

    /**
     * @param colorNode an XML Node containing an hexadecimal color
     * @return the hex color
     */
    public static String toHexColor(Node colorNode) {
        String value = "#000000";
        if (colorNode == null)
            return value;

        try {// FIXME no good to grab 1st child and then node val
            if (colorNode.getFirstChild() == null)
                return value;
            String nodeVal = colorNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split(", ");
            StringBuffer sb = new StringBuffer(100);
            sb.append("#");
            for (int i = 0; i < components.length - 1; i++) {

                String uglyHack = Integer.toHexString(Integer.parseInt(components[i]));
                uglyHack = uglyHack.length() == 1 ? "0" + uglyHack : uglyHack;
                sb.append(uglyHack);

            }

            value = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    /**
     * @param node an XML Node containing a Color alpha value
     * @return the alpha value
     */
    public static String toAlphaValue(Node node) {
        String value = "1";

        if (node == null || node.getFirstChild() == null) {
            return value;
        }

        try {// FIXME no good to grab 1st child than node val
            String nodeVal = node.getFirstChild().getNodeValue();

            value = String.valueOf(Double.parseDouble(nodeVal) / 255d);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // don't care about number formating, just trim the string
        if (value.length() > 6) {
            value = value.substring(0, 5);
        }
        return value;
    }

    /**
     * @param node an XML Node containing a font family name
     * @return the family name
     */
    public static String toFontFamily(Node node) {
        String value = "Dialog";

        try {// FIXME no good to grab 1st child than node val
            String nodeVal = node.getFirstChild().getNodeValue();
            String[] components = nodeVal.split(", ");
            value = components[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * @param fontNode an XML Node containing a font style
     * @return the font style
     */
    public static String toFontStyle(Node fontNode) {
        // bold not supported in SLD?
        final String[] styles = { "normal", "normal", "italic" };

        String value = styles[0];
        try {// FIXME no good to grab 1st child than node val
            String nodeVal = fontNode.getFirstChild().getNodeValue();
            String[] components = nodeVal.split(", ");

            // cheap, cheap, cheap
            value = styles[Integer.parseInt(components[1])];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * @param vertexStyleNode a vertex style XML Node
     * @return the vertex style name
     */
    public static String toWellKnowName(Node vertexStyleNode) {
        if (vertexStyleNode == null) {
            return "";
        }

        String value = "square";
        try {

            NamedNodeMap atts = vertexStyleNode.getAttributes();
            String nodeVal = atts.getNamedItem("class").getNodeValue();

            if (nodeVal.indexOf("Square") > -1) {
                // already there
            } else if (nodeVal.indexOf("Circle") > -1) {
                value = "circle";
            } else if (nodeVal.indexOf("Cross") > -1) {
                value = "cross";
            } else if (nodeVal.indexOf("Star") > -1) {
                value = "star";
            } else if (nodeVal.indexOf("Triangle") > -1) {
                value = "triangle";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * @param filename file containing the image
     * @param fill node containing the fill property (svg case)
     * @param stroke node containing the stroke property (svg case)
     * @param size size of the image
     * @return a URL to the image (svg images will be colored and saved as
     *         image)
     * @throws IOException if an IOException occurs
     */
    public static String getImageURL(String filename, Node fill, Node stroke, int size) throws IOException {
        if (filename.toLowerCase().endsWith(".svg")) {
            File file = File.createTempFile("ojp", "pti.png");

            write(getUpdatedSVGImage(filename, toHexColor(stroke), toHexColor(fill), size), "png", file);

            return file.toURI().toURL().toExternalForm();
        }

        return fileToURL(filename);
    }

    /**
     * @param filename file name
     * @return the url
     */
    public static String fileToURL(String filename) {
        File f = new File(filename);

        try {
            return f.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            return filename;
        }
    }

    /**
     * @param node XML Node to format
     * @return the new string
     * TODO : what is it for ? why ss[0] is doubled when there is no comma, what happens if there are several commas
     */
    public static String replaceComma(Node node) {
        if (node.getFirstChild().getTextContent() == null) {
            return "";
        }

        String[] ss = node.getFirstChild().getTextContent().split(",");

        if (ss.length == 1) {
            return ss[0] + " " + ss[0];
        }

        return ss[0] + " " + ss[1];
    }

    /**
     * @param icon icon name
     * @return the url string
     */
    public static String getIconURL(String icon) {
        return IconLoader.class.getResource(icon).toExternalForm();
    }

    /**
     * @param width width of the WKTFillPattern
     * @param extent extent of the WKTFillPattern
     * @param pattern name of the pattern
     * @param color color code
     * @return the image url
     * @throws IOException if an IOException occurs
     */
    public static String createPatternImage(int width, int extent, String pattern, String color) throws IOException {
        File file = File.createTempFile("ojp", "pti.png");
        WKTFillPattern pat = new WKTFillPattern(width, extent, pattern);
        Blackboard b = pat.getProperties();
        Color c = Color.decode(color);
        b.put(BasicFillPattern.COLOR_KEY, c);
        BufferedImage img = pat.createImage(pat.getProperties());
        ImageIO.write(img, "png", file);
        return file.toURI().toURL().toExternalForm();
    }

}
