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

package com.vividsolutions.wms;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
 * 
 * @deprecated unused and can probably be deleted, kept for reference only
 */
public class Parser {

  /** 
   * Creates a Parser for dealing with WMS XML.
   */
  
  public Parser() {
  }
  
  /**
   * Parses the WMT_MS_Capabilities XML from the given InputStream into
   * a Capabilities object.
   * @param service the WMService from which this MapDescriptor is derived
   * @param inStream the inputStream containing the WMT_MS_Capabilities XML to parse
   * @return the MapDescriptor object created from the specified XML InputStream
   */
  
  public Capabilities parseCapabilities( WMService service, InputStream inStream ) throws IOException {
      if ( WMService.WMS_1_1_1.equals( service.getVersion() ) 
                      || WMService.WMS_1_1_0.equals( service.getVersion() ) ){
          return parseCapabilities_1_1_1(service, inStream);
      }

      return parseCapabilities_1_0_0(service, inStream);  
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
    LinkedList<String> srsList = new LinkedList<String>();
    LinkedList<MapLayer> subLayers = new LinkedList<MapLayer>();
    BoundingBox bbox = null;
    List<MapStyle> styles = new ArrayList<MapStyle>();
    
// I think, bbox is LatLonBoundingBox.
// I need a new variable for BoundingBox.
// It must be a list because in the OGC document
// stands that Layers may have zero or more <BoundingBox> [uwe dalluege]
//    BoundingBox boundingBox = null;
    ArrayList<BoundingBox> boundingBoxList = new ArrayList<BoundingBox> ( );
    
    
    NodeList nl = layerNode.getChildNodes();

    for( int i = 0; i< nl.getLength(); i++ ) {
      Node n = nl.item( i );
      try {

        if( n.getNodeType() == Node.ELEMENT_NODE ) {
          if( n.getNodeName().equals( "Name" ) ) {
            name = ((CharacterData)n.getFirstChild()).getData();
          } else if( n.getNodeName().equals( "Title" ) ) {
            title = ((CharacterData)n.getFirstChild()).getData();
            
          } else if( n.getNodeName().equals( "SRS" ) ) {
            String srsStr = ((CharacterData)n.getFirstChild()).getData();      
            // split the srs String on spaces
            while( srsStr.length() > 0 ) {
              int ws = srsStr.indexOf( ' ' );
              if( ws > 0 ) {
                srsList.add( srsStr.substring( 0, ws ) );
                srsStr = srsStr.substring( ws + 1 );
              } else {
                if( srsStr.length() > 0 ) {
                  srsList.add( srsStr );
                  srsStr = "";
                }
              }
            }
          } else if( n.getNodeName().equals( "LatLonBoundingBox" ) ) {
              bbox = boundingBoxFromNode( n );
              boundingBoxList.add ( bbox );
            
// Check for BoundingBox [uwe dalluege]
          } else if( n.getNodeName( ).equals( "BoundingBox" ) ) {
              bbox = boundingBoxFromNode( n );
              boundingBoxList.add ( bbox );

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
    
// call the new constructor with boundingBoxList in MapLayer [uwe dalluege]
    return new MapLayer
    	( name, title, srsList, subLayers, bbox, boundingBoxList, styles );
  }
 
  /**
   * Creates a new BoundingBox object based on the DOM Node given.
   * @param n the DOM Node to create the Bounding box from, must be either a
   *          LatLonBoundingBox element or a BoundingBox element
   * @return a new BoundingBox object based on the DOM Node provided
   */
  public BoundingBox boundingBoxFromNode( Node n ) throws Exception {
    try {
      String srs = "";
      NamedNodeMap nm = n.getAttributes();

      if( n.getNodeName().equals( "LatLonBoundingBox" ) ) {
        srs = "LatLon";
      } else if( n.getNodeName().equals( "BoundingBox" ) ) {
        srs = nm.getNamedItem( "SRS" ).getNodeValue();
      } else {
          // don't bother...
//        throw new Exception( I18N.getInstance().get("com.vividsolutions.wms.Parser.not-a-latlon-boundingbox-element") );
      }
      
      // could not parse when values equal "inf"
			//	double minx = Double.parseDouble(nm.getNamedItem("minx").getNodeValue());
			//	double miny = Double.parseDouble(nm.getNamedItem("miny").getNodeValue());
			//	double maxx = Double.parseDouble(nm.getNamedItem("maxx").getNodeValue());
			//	double maxy = Double.parseDouble(nm.getNamedItem("maxy").getNodeValue());
		
      // change "inf" values with +/-"Infinity"
      double minx;
      if (nm.getNamedItem("minx").getNodeValue().equals("inf")){
			minx = Double.NEGATIVE_INFINITY;
      } else {
			minx = Double.parseDouble(nm.getNamedItem("minx").getNodeValue()); 
      }
      
      double miny;
      if (nm.getNamedItem("miny").getNodeValue().equals("inf")){
			miny = Double.NEGATIVE_INFINITY;
      } else {
			miny = Double.parseDouble(nm.getNamedItem("miny").getNodeValue()); 
      }
      double maxx;
		
      if (nm.getNamedItem("maxx").getNodeValue().equals("inf")) {
			maxx = Double.POSITIVE_INFINITY;
      } else {
			maxx = Double.parseDouble(nm.getNamedItem("maxx").getNodeValue());
      }
      
      double maxy;
      if (nm.getNamedItem("maxy").getNodeValue().equals("inf")) {
			maxy = Double.POSITIVE_INFINITY;
      } else {
			maxy = Double.parseDouble(nm.getNamedItem("maxy").getNodeValue());
      }
      
      return new BoundingBox( srs, minx, miny, maxx, maxy );
      
    } catch( Exception e ) {
      // possible NullPointerException from getNamedItem returning a null
      // also possible NumberFormatException
        e.printStackTrace();
      throw new Exception( I18N.getInstance().get("com.vividsolutions.wms.Parser.invalid-bounding-box-element-node")+": " + e.toString() );
    }    
  }
  private Capabilities parseCapabilities_1_0_0( WMService service, InputStream inStream ) throws IOException {
      MapLayer topLayer = null;
      String title = "";
      LinkedList<String> formatList = new LinkedList<String>();
      Document doc;
      
      try {
        DOMParser parser = new DOMParser();
        parser.setFeature( "http://xml.org/sax/features/validation", false );
        parser.parse( new InputSource( inStream ) );
        doc = parser.getDocument();
        // DEBUG: XMLTools.printNode( doc, "" );
      } catch( SAXException saxe ) {
        throw new IOException( saxe.toString() );
      }
      
      // get the title
      try {
        title = ((CharacterData)XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Service/Title" ).getFirstChild()).getData();
      } catch (Exception e) {
        // possible NullPointerException if there is no firstChild()
        // also possible miscast causing an Exception
      
        // [ede 2020/08] disabled to allow empty title tags as requested in bug #491
        //throw new IOException( "Maybe wrong Capabilities Version! " );
        
        Logger.warn("<Title/> unset or empty.", e);
      }
      
      // get the supported file formats
      Node formatNode = XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Capability/Request/Map/Format" );
      NodeList nl = formatNode.getChildNodes();
      for( int i=0; i < nl.getLength(); i++ ) {
        Node n = nl.item( i );
        if( n.getNodeType() == Node.ELEMENT_NODE ) {
          formatList.add( n.getNodeName() );
        }
      }
      
      // get the top layer
      topLayer = wmsLayerFromNode( XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Capability/Layer" ) );    
      
      return new Capabilities( service, title, topLayer, formatList, getInfoFormats(doc, "WMT_MS_Capabilities") );
    }
  
  //UT TODO move this into a common method (
  // private Capabilities parseCapabilities( WMService service, InputStream inStream, 
  	// String version)
  
  private Capabilities parseCapabilities_1_1_1( WMService service, InputStream inStream ) throws IOException {
      MapLayer topLayer = null;
      String title = "";
      String getMapURL, getFeatureInfoURL;
      LinkedList<String> formatList = new LinkedList<String>();
      Document doc;
      
      try {
          DOMParser parser = new DOMParser();
          parser.setFeature( "http://xml.org/sax/features/validation", false );
          parser.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
          //was throwing java.io.UTFDataFormatException: Invalid byte 2 of 3-byte UTF-8 sequence.
//          parser.parse( new InputSource( inStream ) );
          InputStreamReader ireader = new InputStreamReader( inStream );
          
          parser.parse( new InputSource( ireader ) );
          doc = parser.getDocument();
          
      } catch( SAXException saxe ) {
        throw new IOException( saxe.toString() );
      }
      
      // throw error if the xml is not a capability answer
      if ( XMLTools.simpleXPath( doc, "WMT_MS_Capabilities") == null) {
        DOMImplementationRegistry registry;
        String str = "";
        try {
          registry = DOMImplementationRegistry.newInstance();
//          DOMImplementationList list = registry.getDOMImplementationList("LS");
//          for (int i = 0; i < list.getLength(); i++) {
//            System.out.println(list.item(i));
//          }

          DOMImplementationLS impl = (DOMImplementationLS) registry
              .getDOMImplementation("LS");
          LSSerializer writer = impl.createLSSerializer();
          str = writer.writeToString(doc);
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        throw new WMSException("Unexpected answer from server. Missing node <WMT_MS_Capabilities>.", str);
      }

      // get the title
      try {
        title = ((CharacterData)XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Service/Title" ).getFirstChild()).getData();
      } catch (Exception e) {
        // eat it, not set or empty
        Logger.warn("<Title/> unset or empty.", e);
      }

      // get the supported file formats			// UT was "WMT_MS_Capabilities/Capability/Request/Map/Format"
      final Node formatNode = XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Capability/Request/GetMap" );

      NodeList nl = formatNode.getChildNodes();
      for( int i=0; i < nl.getLength(); i++ ) {
        Node n = nl.item( i );
        if( n.getNodeType() == Node.ELEMENT_NODE && "Format".equals( n.getNodeName() )) {
            formatList.add( n.getFirstChild().getNodeValue() );              
        }
      }
      
      // get the possible URLs
      String xp = "DCPType/HTTP/Get/OnlineResource";
      String xlink = "http://www.w3.org/1999/xlink";
      Element e = (Element) XMLTools.simpleXPath(formatNode, xp);
      getMapURL = e.getAttributeNS(xlink, "href");

      xp = "WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/DCPType/HTTP/Get/OnlineResource";
      e = (Element) XMLTools.simpleXPath(doc, xp);
      if (e != null) {
            getFeatureInfoURL = e.getAttributeNS(xlink, "href");
        } else {
            getFeatureInfoURL = "";
        }

      // get the top layer
      topLayer = wmsLayerFromNode( XMLTools.simpleXPath( doc, "WMT_MS_Capabilities/Capability/Layer" ) );
      
      return new Capabilities( service, title, topLayer, formatList, getInfoFormats(doc, "WMT_MS_Capabilities"), getMapURL, getFeatureInfoURL );
    }
  
    protected LinkedList<String> getInfoFormats(Document doc, String rootPath) {
        
        // get the supported infoFormats
        final Node formatNode = XMLTools.simpleXPath(doc, rootPath + "/Capability/Request/GetMap");
        NodeList nl = formatNode.getChildNodes();
        
        final Node infoFormatNode = XMLTools.simpleXPath(doc, "WMT_MS_Capabilities/Capability/Request/GetFeatureInfo");
        LinkedList<String> infoFormatList = new LinkedList<String>();
        if (infoFormatNode != null) {
            nl = infoFormatNode.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && "Format".equals(n.getNodeName())) {
                    infoFormatList.add(n.getFirstChild().getNodeValue());
                }
            }
        }
        
        return infoFormatList;
        
    }
  
}
