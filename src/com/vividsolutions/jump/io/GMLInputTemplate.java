
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

package com.vividsolutions.jump.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.FlexibleDateParser;


/**
 * Reads an XML file that starts with a 'JCSGMLInputTemplate'. <br>
 * Will abort read at the end of the 'JCSGMLInputTemplate' tag. <br>
 * Constructs a description of the Columns and geometry tag so the <br>
 * actual GML parser ({@link GMLReader}) will know what to do with different tags.
 *<br><Br>
 *This is a SAX Handler.
 */
public class GMLInputTemplate extends DefaultHandler {

    boolean loaded = false;
    String collectionTag;
    String featureTag;
    ArrayList<ColumnDescription> columnDefinitions = new ArrayList<>(); //list of type ColumnDescription

    private XMLReader xr;
    private String tagBody = "";
    private ArrayList<String> geometryElements = new ArrayList<>(20); //shouldnt need more than 20, but will auto-expand bigger
    private String streamName;
    private boolean havecollectionTag = false;
    private boolean havefeatureTag = false;
    private boolean havegeometryElement = false;

    //for the jcs column definition
    private int columnDef_valueType = 0; // 0 - undef, 1 = body, 2 = attribute
    private String columnDef_valueAttribute = ""; // name of the attribute the value is in
    private String columnDef_tagName = ""; // tag this is a part of
    private int columnDef_tagType = 0; // 0 - undef, 1=tag only, 2 = attribute, 3 = att & value
    private String columnDef_tagAttribute = "";
    private String columnDef_tagValue = "";
    private String columnDef_columnName = "";
    private AttributeType columnDef_type = null;
    private String lastStartTag_uri;
    private String lastStartTag_name;
    private String lastStartTag_qName;
    private Attributes lastStartTag_atts;

    /**
     * constructor - makes a new org.apache.xerces.parser and makes this class be the SAX
     *  content and error handler.
     */
    public GMLInputTemplate() {
        super();
        xr = new org.apache.xerces.parsers.SAXParser();
        xr.setContentHandler(this);
        xr.setErrorHandler(this);
    }

    /**
     * Returns the column name for the 'index'th column.
     *@param index 0=first
     */
    public String columnName(int index) throws ParseException {
        if (loaded) {
            return columnDefinitions.get(index).columnName;
        } else {
            throw new ParseException(
                "requested columnName w/o loading the template");
        }
    }

    /**
     * Converts this GMLInputTemplate to a feature schema.
     */
    public FeatureSchema toFeatureSchema() throws ParseException {
        if (!(loaded)) {
            throw new ParseException(
                "requested toFeatureSchema w/o loading the template");
        }

        FeatureSchema fcmd = new FeatureSchema();

        fcmd.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        for (ColumnDescription columnDescription : columnDefinitions) {
            fcmd.addAttribute(columnDescription.columnName, columnDescription.getType());
        }

        return (fcmd);
    }

    /**
     * Function to help the GMLParser - is this tag name the Geometry Element tag name?
     *@param tag an XML tag name
     **/
    public boolean isGeometryElement(String tag) {
        int t;
        String s;

        for (t = 0; t < geometryElements.size(); t++) {
            s = geometryElements.get(t);

            if (s.equalsIgnoreCase(tag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * previous method to parse a file
     * 
     * @deprecated use load(InputStream is, String readerName) instead
     */
    public void load( Reader r ) throws ParseException, IOException{
      load(r, "Unknown Stream");
    }
    
    /**
     * Helper function - load a GMLInputTemplate file with the stream name "Unknown Stream"
     */
    public void load(InputStream is) throws ParseException, IOException {
        load(is, "Unknown Stream");
    }

    /**
     * Main function - load in an XML file. <br>
     * Error handling/reporting also done here.
     *@param o inputStream where to read the XML file from
     *@param readerName name of the stream for error reporting
     */
    public void load(Object o, String readerName)
        throws ParseException, IOException {
        //myReader = new LineNumberReader(r);
        streamName = readerName; // for error reporting

        try {
          if (o instanceof Reader)
            xr.parse(new InputSource((Reader)o));
          else if (o instanceof InputStream)
            xr.parse(new InputSource((InputStream)o));
          else
            throw new IOException("neither InputStream or Reader given");
        } catch (EndOfParseException e) {
            // This is not really an error
        } catch (SAXParseException e) {
            throw new ParseException(e.getMessage() + " (Is this really a GML file?)  Last Opened Tag: " +
                lastStartTag_qName, streamName + " - " + e.getPublicId() + " (" + e.getSystemId() +
                ") ", e.getLineNumber(), e.getColumnNumber());
        } catch (SAXException e) {
            throw new ParseException(e.getMessage() + "  Last Opened Tag: " +
                lastStartTag_qName, streamName, -1, 0);
        }

        loaded = (havecollectionTag) && (havefeatureTag) &&
            (havegeometryElement);

        if (!(loaded)) {
            String miss;
            miss = "";

            if (!(havecollectionTag)) {
                miss = miss + "Missing CollectionElement.  ";
            }

            if (!(havefeatureTag)) {
                miss = miss + "Missing FeatureElement.  ";
            }

            if (!(havegeometryElement)) {
                miss = miss + "Missing GeometryElement.  ";
            }

            throw new ParseException("Failed to load the GML Input Template.  " +
                miss);
        }
    }

    /**
     * Get the name of the FeatureCollectionElement tag
     */
    public String getFeatureCollectionElementName() throws ParseException {
        if (loaded) {
            return collectionTag;
        } else {
            throw new ParseException(
                "requested FeatureCollectionElementName w/o loading the template");
        }
    }

    /**
     * Get the name of the FeatureElement tag
     */
    public String getFeatureElementName() throws ParseException {
        if (loaded) {
            return featureTag;
        } else {
            throw new ParseException(
                "requested FeatureCollectionElementName w/o loading the template");
        }
    }

    /**
     * Given a tag name and its XML attributes, find the index of the column it belongs to.<br>
     * Returns -1 if it doesnt match any of the columns.
     *@param XMLtagName the tag name found in the xml
     *@param xmlAtts the attributes associated with the xml
     */
    public int match(String XMLtagName, Attributes xmlAtts)
        throws ParseException {
        if (loaded) {
            for (int t = 0; t < columnDefinitions.size(); t++) {
                if ((columnDefinitions.get(t)).match(XMLtagName, xmlAtts) != 0) {
                    return t;
                }
            }

            return -1;
        }

        throw new ParseException("requested match() w/o loading the template");
    }

    /**
     * Given a ColumnDescription index, the XML tagBody, and the tag's attributes, return the
     * actual value (it could be an attribute or the tag's body).  You probably got the index
     * from the match() function.
     *
     * @param index index number of the column description
     * @param tagBody value of the XML tag body
     * @param xmlAtts key/values of the XML tag's attributes
     */
    public Object getColumnValue(int index, String tagBody, Attributes xmlAtts)
            throws ParseException {
        String val;
        ColumnDescription cd;

        if (!(loaded)) {
            throw new ParseException(
                "requested getColumnValue w/o loading the template");
        }

        cd = columnDefinitions.get(index);

        if (cd.valueType == ColumnDescription.VALUE_IS_BODY) {
            val = tagBody;
        } else {
            val = xmlAtts.getValue(cd.valueAttribute);
        }

        //have the value as a string, make it an object
        if (cd.type == AttributeType.STRING ||
                cd.type == AttributeType.VARCHAR ||
                cd.type == AttributeType.LONGVARCHAR ||
                cd.type == AttributeType.CHAR ||
                cd.type == AttributeType.TEXT) {
            return val;
        }

        if (cd.type == AttributeType.INTEGER ||
                cd.type == AttributeType.SMALLINT ||
                cd.type == AttributeType.TINYINT) {
            try {
                //Was Long, but JUMP expects AttributeType.INTEGER to hold Integers.
                //e.g. open JML file then save as Shapefile => get ClassCastException.
                //Dave Blasby says there was a reason for changing it to Long, but
                //can't remember -- suspects there were datasets whose INTEGER
                //values didn't fit in an Integer. [Jon Aquino 1/13/2004]
                
                //Compromise -- try Long if Integer fails. Some other parts of JUMP
                //won't like it (exceptions), but it's better than null. Actually I don't like
                //this null business -- future: warn the user. [Jon Aquino 1/13/2004]
                return Integer.parseInt(val);
            } catch (Exception e) {
                return null;
            }
        }

        if (cd.type == AttributeType.LONG || cd.type == AttributeType.BIGINT) {
            try {
                return Long.parseLong(val);
            } catch (Exception e) {
                return null;
            }
        }

        if (cd.type == AttributeType.DOUBLE ||
                cd.type == AttributeType.REAL ||
                cd.type == AttributeType.FLOAT ||
                cd.type == AttributeType.NUMERIC ||
                cd.type == AttributeType.DECIMAL ||
                cd.type == AttributeType.BIGDECIMAL) {
            try {
                return new Double(val);
            } catch (Exception e) {
                return null;
            }
        }
        
        //Adding date support. Can we throw an exception if an exception
        //occurs or if the type is unrecognized? [Jon Aquino]
        if (cd.type == AttributeType.DATE ||
                cd.type == AttributeType.TIMESTAMP ||
                cd.type == AttributeType.TIME) {
            try {
                return dateParser.parse(val, false);
            } catch (Exception e) {
                return null;
            }
        }

        if (cd.type == AttributeType.BOOLEAN || cd.type == AttributeType.BIT) {
            try {
                return Boolean.parseBoolean(val);
            } catch (Exception e) {
                return null;
            }
        }

        if (cd.type == AttributeType.OBJECT)
        {
        	return val; // the GML file has text in it and we want to convert it to an "object"
        	            // just return a String since we dont know anything else about it!
        }

        return null; //unknown type
    }
    
    private FlexibleDateParser dateParser = new FlexibleDateParser();

    ////////////////////////////////////////////////////////////////////
    // Error handlers.
    ////////////////////////////////////////////////////////////////////
    public void warning(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception)
        throws SAXException {
        throw exception;
    }

    ////////////////////////////////////////////////////////////////////
    // Event handlers.
    ////////////////////////////////////////////////////////////////////

    /**
     * SAX startDocument handler - null
     */
    public void startDocument() {
        //System.out.println("Start document");
    }

    /**
     * SAX endDocument handler - null
     */
    public void endDocument() {
        //System.out.println("End document");
    }

    /**
     * SAX startElement handler  <br>
     * Basically just records the tag name and its attributes since all the
     * smarts are in the endElement handler.
     */
    public void startElement(String uri, String name, String qName,
        Attributes atts) throws SAXException {
        try {
            tagBody = "";

            if (qName.equals("column")) {
                //reset these values!
                columnDef_tagName = ""; // tag this is a part of
                columnDef_tagType = 0; // 0 - undef, 1=tag only, 2 = attribute, 3 = att & value
                columnDef_tagAttribute = "";
                columnDef_tagValue = "";

                columnDef_valueType = 0; // 0 - undef, 1 = body, 2 = attribute
                columnDef_valueAttribute = ""; // name of the attribute the value is in

                columnDef_columnName = "";
                columnDef_type = null;
            }

            lastStartTag_uri = uri;
            lastStartTag_name = name;
            lastStartTag_qName = qName;
            lastStartTag_atts = atts;
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }

    /**
     * Helper function - get attribute in a case insensitive manner.
     * returns index or -1 if not found.
     *@param atts the attributes for the xml tag (from SAX)
     *@param att_name the name of the attribute to search for
     */
    private int lookupAttribute(Attributes atts, String att_name) {
        int t;

        for (t = 0; t < atts.getLength(); t++) {
            if (atts.getQName(t).equalsIgnoreCase(att_name)) {
                return t;
            }
        }

        return -1;
    }

    /**
     *  SAX endElement handler - the main working function <br>
     *  <br>
     *  handles the following tags in the appropriate manner: <br>
     *  GeometryElement : sets the name of the document's geometry tag <bR>
     *  CollectionElement : sets the name of the document's collection tag<br>
     *  FeatureElement : sets the name of the document's feature tag<br>
     *  type : sets a column type (to be used when a column ends) <br>
     * valueelement : sets information about what element a column is associated with <br>
     * valuelocation : set information about where a column's value is stored in the document <br>
     * column : takes the accumlated information about a column and constructs a ColumnDescription object <bR>
     */
    public void endElement(String uri, String name, String qName)
            throws SAXException {
        try {
            if (qName.equalsIgnoreCase("JCSGMLInputTemplate")) {
                throw new EndOfParseException("Finished parsing input template");
            }

            if (qName.equalsIgnoreCase("type")) {
                String t;
                t = tagBody.toUpperCase();
                t = t.trim();

                try {
                    columnDef_type = com.vividsolutions.jump.feature.AttributeType.toAttributeType(t);
                } catch (IllegalArgumentException e) {
                    //Hmm...we're just eating the exception here. Perhaps we should
                    //allow the exception to propagate up to the caller. [Jon Aquino]
                    columnDef_type = null;
                }
            }

            if (qName.equalsIgnoreCase("GeometryElement")) {
                tagBody = tagBody.trim();
                geometryElements.add(tagBody);
                havegeometryElement = true;

                return;
            }

            if (qName.equalsIgnoreCase("CollectionElement")) {
                tagBody = tagBody.trim();
                collectionTag = tagBody;
                havecollectionTag = true;

                return;
            }

            if (qName.equalsIgnoreCase("FeatureElement")) {
                tagBody = tagBody.trim();
                featureTag = tagBody;
                havefeatureTag = true;

                return;
            }

            if (qName.equalsIgnoreCase("name")) {
                columnDef_columnName = tagBody.trim();
            }

            if (qName.equalsIgnoreCase("valueelement")) {
                int attindex;

                columnDef_tagType = 1;

                //attindex = lastStartTag_atts.getIndex("elementname");
                attindex = lookupAttribute(lastStartTag_atts, "elementname");

                if (attindex == -1) {
                    throw new SAXException(
                        "column definition has 'valueelement' tag without 'elementname' attribute");
                }

                columnDef_tagName = lastStartTag_atts.getValue(attindex);

                attindex = lookupAttribute(lastStartTag_atts, "attributename");

                if (attindex != -1) {
                    columnDef_tagAttribute = lastStartTag_atts.getValue(attindex);
                    columnDef_tagType = 2;

                    attindex = lookupAttribute(lastStartTag_atts,
                            "attributevalue");

                    if (attindex != -1) {
                        columnDef_tagValue = lastStartTag_atts.getValue(attindex);
                        columnDef_tagType = 3;
                    }
                }
            }

            if (qName.equalsIgnoreCase("valuelocation")) {
                int attindex;

                attindex = lookupAttribute(lastStartTag_atts, "position");

                if (attindex == -1) {
                    throw new SAXException(
                        "column definition has 'valuelocation' tag without 'position' attribute");
                }

                if (lastStartTag_atts.getValue(attindex).equalsIgnoreCase("body")) {
                    columnDef_valueType = 1;
                } else {
                    attindex = lookupAttribute(lastStartTag_atts, "attributename");
                    columnDef_valueType = 2;

                    if (attindex == -1) {
                        throw new SAXException(
                            "column definition has 'valuelocation' tag, attribute type, but no 'attributename' attribute");
                    }

                    columnDef_valueAttribute = lastStartTag_atts.getValue(attindex);
                }
            }

            if (qName.equalsIgnoreCase("column")) {
                //commit column entry
                if (columnDef_tagName.equalsIgnoreCase("")) {
                    throw new SAXException(
                        "column Definition didnt include tag name ('<name>...</name>')");
                }

                if (columnDef_tagType == 0) {
                    throw new SAXException(
                        "column Definition didnt include 'valueelement' ");
                }

                if (columnDef_valueType == 0) {
                    throw new SAXException(
                        "column Definition didnt have a 'valuelocation'");
                }

                //we're okay
                ColumnDescription colDes;

                colDes = new ColumnDescription();
                colDes.setColumnName(columnDef_columnName);

                if (colDes.columnName.compareTo("GEOMETRY") == 0) {
                    throw new ParseException(
                        "Cannot have a column named GEOMETRY!");
                }

                if (columnDef_valueType == 2) {//auto set for #1=body
                    colDes.setValueAttribute(columnDef_valueAttribute); //not the body
                }

                colDes.setTagName(columnDef_tagName);

                if (columnDef_tagType == 3) {//1=simple
                    colDes.setTagAttribute(columnDef_tagAttribute,
                        columnDef_tagValue);
                }

                if (columnDef_tagType == 2) {
                    colDes.setTagAttribute(columnDef_tagAttribute);
                }

                colDes.setType(columnDef_type);
                columnDefinitions.add(colDes); //remember this
            }
        } catch (EndOfParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }

    /**
     * SAX handler for characters - just store and accumulate for later use
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        try {
            String part;
            part = new String(ch, start, length);
            tagBody = tagBody + part;
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }
}
