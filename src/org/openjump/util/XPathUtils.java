//$HeadURL$
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

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static org.apache.log4j.Logger.getLogger;

import java.util.LinkedList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>XPathUtils</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class XPathUtils {

    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    private static final Logger LOG = getLogger(XPathUtils.class);

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return an int, possibly parsed from a double value
     * @throws XPathExpressionException
     */
    public static int getInt(String xpath, Element e, NamespaceContext nscontext) throws XPathExpressionException {
        // sick casts
        double res = getDouble(xpath, e, nscontext);
        return (int) res;
    }

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return a double, possibly converted from an integer value
     * @throws XPathExpressionException
     */
    public static double getDouble(String xpath, Element e, NamespaceContext nscontext) throws XPathExpressionException {
        XPATH.setNamespaceContext(nscontext);

        Object res = XPATH.evaluate(xpath, e, NUMBER);

        if (LOG.isDebugEnabled()) {
            LOG.debug("XPath expression " + xpath + " yielded " + res);
        }

        if (res instanceof Double) {
            return (Double) res;
        }
        return (Integer) res;
    }

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return a list of matching nodes, or an empty list if none match
     * @throws XPathExpressionException
     */
    public static LinkedList<Node> getNodes(String xpath, Element e, NamespaceContext nscontext)
            throws XPathExpressionException {
        XPATH.setNamespaceContext(nscontext);

        NodeList nl = (NodeList) XPATH.evaluate(xpath, e, NODESET);

        LOG.debug("XPath expression " + xpath + " yielded " + nl.getLength() + " nodes.");

        LinkedList<Node> list = new LinkedList<Node>();

        for (int i = 0; i < nl.getLength(); ++i) {
            list.add(nl.item(i));
        }

        return list;
    }

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return a list of matching elements, or an empty list if none match
     * @throws XPathExpressionException
     */
    public static LinkedList<Element> getElements(String xpath, Element e, NamespaceContext nscontext)
            throws XPathExpressionException {
        LinkedList<Node> nodes = getNodes(xpath, e, nscontext);
        LinkedList<Element> list = new LinkedList<Element>();

        for (Node n : nodes) {
            list.add((Element) n);
        }

        return list;
    }

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return a matching node, or null if none match
     * @throws XPathExpressionException
     */
    public static Node getNode(String xpath, Element e, NamespaceContext nscontext) throws XPathExpressionException {
        XPATH.setNamespaceContext(nscontext);

        Node n = (Node) XPATH.evaluate(xpath, e, NODE);

        LOG.debug("XPath expression " + xpath + " yielded " + (n == null ? "nothing." : "a node."));

        return n;
    }

    /**
     * @param xpath
     * @param e
     * @param nscontext
     * @return a matching element, or null if none matches
     * @throws XPathExpressionException
     */
    public static Element getElement(String xpath, Element e, NamespaceContext nscontext)
            throws XPathExpressionException {
        return (Element) getNode(xpath, e, nscontext);
    }

}
