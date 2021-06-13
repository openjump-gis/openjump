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

// Changed by Uwe Dalluege, uwe.dalluege@rzcn.haw-hamburg.de
// to differ between LatLonBoundingBox and BoundingBox
// 2005-08-09
// Completely refactored by Michael Michaud on 2013-04-13

package com.vividsolutions.wms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.wms.util.XMLTools;


/**
 * Pulls WMS objects out of the XML
 * @author Chris Hodgson chodgson@refractions.net
 * @author Uwe Dalluege uwe.dalluege@rzcn.haw-hamburg.de
 * @author Michael Michaud m.michael.michaud@orange.fr
 */
public abstract class AbstractParser implements IParser {

   
   /** 
    * Creates a Parser for dealing with WMS XML.
    */
    public AbstractParser() {}
    
  
   /**
    * Parses the WMT_MS_Capabilities XML from the given InputStream into
    * a Capabilities object.
    * @param service the WMService from which this MapDescriptor is derived
    * @param inStream the inputStream containing the WMT_MS_Capabilities XML to parse
    * @return the MapDescriptor object created from the specified XML InputStream
    */
    public Capabilities parseCapabilities(WMService service, InputStream inStream) throws IOException {
        Document doc;
        try {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://xml.org/sax/features/validation", false);
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parser.parse(new InputSource(inStream));
            doc = parser.getDocument();
            checkCapabilities(doc);
        } catch(SAXException saxe) {
            throw new IOException(saxe.toString());
        }
        return parseCapabilities(service, doc);
    }
    
    abstract protected String getRootPath();
    
    protected void checkCapabilities(Document doc) throws IOException {
        
        if (XMLTools.simpleXPath(doc, getRootPath()) == null) {
            DOMImplementationRegistry registry;
            String str = "";
            try {
                registry = DOMImplementationRegistry.newInstance();
                DOMImplementationLS impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
                LSSerializer writer = impl.createLSSerializer();
                str = writer.writeToString(doc);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new WMSException("Missing root node <" + getRootPath() + "> : you probably use a wrong URL or a wrong version of WMS", str);
        }
        
    }
    
    
    abstract protected Capabilities parseCapabilities(WMService service, Document doc) throws IOException;
    
    protected String getTitlePath() {
        return getRootPath() + "/Service/Title";
    }
    
    protected String getTitle(Document doc) throws IOException {
        String title = "";
        Node titleNode = XMLTools.simpleXPath(doc, getTitlePath());
        if (titleNode != null) {
            // TODO check why we have to get titleNode child
            Node firstTitle = titleNode.getFirstChild();
            if (firstTitle != null) {
                title = ((CharacterData)firstTitle).getData();
            } else {
                // [ede 2020/08] disabled to allow empty service title tags as requested in bug #491
                Logger.warn("service <Title/> unset or empty.");
            }
        } else {
            // [ede 2020/08] disabled to allow empty service title tags as requested in bug #491
            Logger.warn("service <Title/> unset or empty.");
        }
        return title;
    }

    // path is common for wms 1.1 and more (overriden by WMS 1.0 which followed
    // a different path
    protected LinkedList<String> getFormatList(Document doc) throws IOException {
        
        final LinkedList<String> formatList = new LinkedList<>();
        final Node formatNode = XMLTools.simpleXPath(doc,
                getRootPath() + "/Capability/Request/GetMap");
        if (formatNode != null) {
            final NodeList nl = formatNode.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && "Format".equals(n.getNodeName())) {
                    String format = n.getFirstChild().getNodeValue();
                    if (format.matches("^image/(png|jpeg|gif).*")) {
                        formatList.add(format);
                    }
                }
            }
        } else {
            Logger.warn("Element '/Capability/Request/GetMap' not found");
        }
        
        return formatList;
    }
    
    protected LinkedList<String> getInfoFormats(Document doc) {
        
        // get the supported infoFormats
        //final Node formatNode = XMLTools.simpleXPath(doc, getRootPath() + "/Capability/Request/GetMap");

        final Node infoFormatNode = XMLTools.simpleXPath(doc, "WMT_MS_Capabilities/Capability/Request/GetFeatureInfo");
        LinkedList<String> infoFormatList = new LinkedList<>();
        if (infoFormatNode != null) {
            NodeList nl = infoFormatNode.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && "Format".equals(n.getNodeName())) {
                    infoFormatList.add(n.getFirstChild().getNodeValue());
                }
            }
        }
        
        return infoFormatList;
        
    }
    
   /**
    * Traverses the DOM tree underneath the specified Node and generates
    * a corresponding WMSLayer object tree. The returned WMSLayer will be set to 
    * have the specified parent.
    * @param layerNode a DOM Node which is a <layer> XML element
    * @return a WMSLayer with complete subLayer tree that corresponds
    *         to the DOM Node provided
    */
    public MapLayer wmsLayerFromNode( Node layerNode ) {
        String name = null;
        String title = null;
        LinkedList<String> srsList = new LinkedList<>();
        LinkedList<MapLayer> subLayers = new LinkedList<>();
        BoundingBox geographicBBox = null;
        ArrayList<BoundingBox> boundingBoxList = new ArrayList<> ( );
        List<MapStyle> styles = new ArrayList<>();
        boolean firstBoundingBox = true;
    
        NodeList nl = layerNode.getChildNodes();

        for( int i = 0; i < nl.getLength(); i++ ) {
            Node n = nl.item( i );
            try {
                if( n.getNodeType() == Node.ELEMENT_NODE ) {

                    if( n.getNodeName().equals( "Name" ) ) {
                        name = ((CharacterData)n.getFirstChild()).getData();
                    } else if( n.getNodeName().equals( "Title" ) ) {
                        title = ((CharacterData)n.getFirstChild()).getData();
                    } else if( n.getNodeName().equals( getSRSName() ) ) {
                        addSRSNode(n, srsList);
                    } else if( n.getNodeName().equals( "LatLonBoundingBox" ) ) {
                        geographicBBox = latLonBoundingBoxFromNode( n );
                        boundingBoxList.add ( geographicBBox );
                        boundingBoxList.add ( new BoundingBox("Geographics", geographicBBox.getEnvelope()) );
                    } else if( n.getNodeName().equals( "BoundingBox" ) ) {
                        // first boundingBox comes first in the list
                        if (firstBoundingBox) {
                            boundingBoxList.add(0, boundingBoxFromNode(n));
                            firstBoundingBox = false;
                        } else boundingBoxList.add(boundingBoxFromNode(n));
                    } else if( n.getNodeName().equals( "EX_GeographicBoundingBox" ) ) {
                        geographicBBox = exGeographicBoundingBoxFromNode( n );
                        boundingBoxList.add ( geographicBBox );
                        boundingBoxList.add ( new BoundingBox("Geographics", geographicBBox.getEnvelope()) );
                    } else if( n.getNodeName().equals( "Layer" ) ) {
                        subLayers.add( wmsLayerFromNode( n ) );
                    } else if (n.getNodeName().equals("Style")) { 
                        String styleName = ""; 
                        String titleName = ""; 
                        String legendFormat = ""; 
                        String url = ""; 
                        int h=0,w=0;
                        NodeList nodeStyle = n.getChildNodes();
                        for( int k = 0; k < nodeStyle.getLength(); k++ ) {
                            Node n1 = nodeStyle.item(k);
                            if (n1.getNodeName().equals("Name")) { 
                                styleName = ((CharacterData) n1.getFirstChild()).getData();
                            } else if (n1.getNodeName().equals("Title") & n1.hasChildNodes()) { 
                                titleName = ((CharacterData) n1.getFirstChild()).getData();
                            } else if (n1.getNodeName().equals("LegendURL")) { 
                                try {
                                    h=Integer.parseInt(n1.getAttributes().getNamedItem("height").getNodeValue());
                                    w=Integer.parseInt(n1.getAttributes().getNamedItem("width").getNodeValue());
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    throw new Exception(e.toString());
                                }
                                NodeList nodelegend = n1.getChildNodes();
                                for( int k1 = 0; k1 < nodelegend.getLength(); k1++ ) {
                                    Node n2 = nodelegend.item(k1);
                                    if (n2.getNodeName().equals("Format")) { 
                                        legendFormat =
                                            ((CharacterData) n2.getFirstChild()).getData();
                                    } else if (n2.getNodeName().equals("OnlineResource")) { 
                                        url =
                                            n2.getAttributes()
                                                .getNamedItem("xlink:href").getNodeValue(); 
                                    }
                                }

                            }
                        }
                        styles.add(new MapStyle(styleName, titleName, url, legendFormat,w,h));
                    }
                }
            } catch( Exception e ) {
                e.printStackTrace();
                Logger.error( "Exception caught in wmsLayerFromNode(): " + e.toString() );
            }
        }
        if (title == null) title = name + " (untitled)";
        // call the new constructor with boundingBoxList in MapLayer [uwe dalluege]
        return new MapLayer(name, title, srsList, subLayers, geographicBBox, boundingBoxList, styles);
    }
    
    protected void addSRSNode(Node n, List<String> srsList) {
        String srsString = ((CharacterData)n.getFirstChild()).getData();
        String[] tokens = srsString.split("\\s+");
        srsList.addAll(Arrays.asList(tokens));
    }
    
    protected BoundingBox boundingBoxFromNode(Node n) throws Exception {
        try {
            NamedNodeMap nm = n.getAttributes();           
            String srs = nm.getNamedItem(getSRSName()).getNodeValue();
            double minx = getCoord("minx", nm);
			      double miny = getCoord("miny", nm);
			      double maxx = getCoord("maxx", nm);
			      double maxy = getCoord("maxy", nm);
			      return new BoundingBox(srs, minx, miny, maxx, maxy);
        } catch( Exception e ) {
            // possible NullPointerException from getNamedItem returning a null
            // also possible NumberFormatException
            e.printStackTrace();
            throw new Exception( I18N.getInstance().get("com.vividsolutions.wms.Parser.invalid-bounding-box-element-node")+": " + e.toString() );
        }   
    }
    
    protected BoundingBox latLonBoundingBoxFromNode(Node n) throws Exception {
        try {
            NamedNodeMap nm = n.getAttributes();
            String srs = "EPSG:4326";
            double minx = getCoord("minx", nm);
			      double miny = getCoord("miny", nm);
			      double maxx = getCoord("maxx", nm);
			      double maxy = getCoord("maxy", nm);
			      return new BoundingBox(srs, minx, miny, maxx, maxy);
        } catch( Exception e ) {
            e.printStackTrace();
            throw new Exception( I18N.getInstance().get("com.vividsolutions.wms.Parser.invalid-bounding-box-element-node")+": " + e.toString() );
        }   
    }
    
    public BoundingBox exGeographicBoundingBoxFromNode(Node n) throws Exception {
        try {
            String srs = "EPSG:4326";
            double minx = 0.0;
            double miny = 0.0;
            double maxx = 0.0;
            double maxy = 0.0;
            NodeList childNodes = n.getChildNodes();
            for( int i = 0; i < childNodes.getLength(); i++ ) {
                Node childNode = childNodes.item( i );
                if( childNode.getNodeType() == Node.ELEMENT_NODE ) {
                    switch (childNode.getNodeName()) {
                        case "westBoundLongitude":
                            minx = getCoord(childNode.getTextContent().trim());
                            break;
                        case "eastBoundLongitude":
                            maxx = getCoord(childNode.getTextContent().trim());
                            break;
                        case "southBoundLatitude":
                            miny = getCoord(childNode.getTextContent().trim());
                            break;
                        case "northBoundLatitude":
                            maxy = getCoord(childNode.getTextContent().trim());
                            break;
                        default:
                    }
                }
            }
			      return new BoundingBox(srs, minx, miny, maxx, maxy);
        } catch( Exception e ) {
            e.printStackTrace();
            throw new Exception( I18N.getInstance().get("com.vividsolutions.wms.Parser.invalid-bounding-box-element-node")+": " + e.toString() );
        }
    }

    /**
     * Coordinates in attributes minx, miny, maxx, maxy
     */
    public double getCoord(String name, NamedNodeMap nm) throws Exception {
        return getCoord(nm.getNamedItem(name).getNodeValue());
    }

    /**
     * Coordinates in subelements westBoundLongitude, southBoundLongitude...
     */
    public double getCoord(String text) throws Exception {
        if (text.equals("inf")) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.parseDouble(text);
        }
    }

    abstract protected String getSRSName();
  
}
