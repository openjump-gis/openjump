
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

import org.xml.sax.Attributes;

import com.vividsolutions.jump.feature.AttributeType;


/**
 * This is a helper class to store information about a JCS Column for the GML Parser ({@link GMLReader}). <br>
 *
 * Also has a function for checking if an XML tag matches this column specification.
 */
public class ColumnDescription {

    static final int VALUE_IS_BODY = 1;
    static final int VALUE_IS_ATT = 2;

    String columnName; //jcs column name
    String tagName; //XML tag this is a part of
    boolean tagNeedsAttribute = false; //true if the tag containing the value has a certain attribute in it
    String attributeName; //name of this attribute
    boolean tagAttributeNeedsValue = false; // true if the tag/attribute needs a value
    String attributeValue; //     actual value of that attribute
    int valueType = VALUE_IS_BODY; //either VALUE_IS_BODY or VALUE_IS_ATT
    String valueAttribute; //which attribute the value is in (only for VALUE_IS_ATT)
    AttributeType type;

    //**constructor**/
    public ColumnDescription() {
    }

    /**
     * Sets the [JCS] type of this column
     *
     * @param t JCS type that this column will contain (null means 'STRING')
     **/
    public void setType(AttributeType t) {
        //<<TODO:DESIGN>> Shouldn't we use the Assert class to stipulate that
        //t must not be null? [Jon Aquino]
        if (t == null) {
            type = AttributeType.STRING;
        } else {
            type = t;
        }
    }

    /**
     * Returns the [JCS] type of this column
     * cf. setType()
     **/
    public AttributeType getType() {
        return type;
    }

    /**
     * Set the name of this column.
     * @param colname name of the column
     **/
    public void setColumnName(String colname) {
        columnName = colname;
    }

    /**
    * Sets the name of the XML tag that this column will be extracted from.
    * @param tagname name of the XML tag
    **/
    public void setTagName(String tagname) {
        tagName = tagname;
    }

    /**
    * Sets the name of the attribute (and its value) that the xml tag that this column will be extracted from.
     *<pre>
     *  For example, the XML '&lt;value type=name&gt; DAVE &lt;/value&gt;' would described by:
     *  setTagName('value');
     *  setTagAttribute('type','name');
     *</pre>
     *
    * @param attName name of the XML attribute name
     *@param attValue its value
    **/
    public void setTagAttribute(String attName, String attValue) {
        attributeName = attName;
        attributeValue = attValue;
        tagNeedsAttribute = true;
        tagAttributeNeedsValue = true;
    }

    /**
    * Sets the name of the attribute (with no value) that the xml tag that this column will be extracted from.
     *<PRE>
     *  For example, the XML '&lt;value name=david&gt;&lt;/value&gt;' would described by:
     *  setTagName('value');
     *  setTagAttribute('name');
     *</PRE>
     *
    * @param attName name of the XML attribute name
    **/
    public void setTagAttribute(String attName) {
        attributeName = attName;
        tagNeedsAttribute = true;
    }

    /**
     * Sets the name of the attribute that the actual column's value will be found.
     *<PRE>
      *  For example, the XML '&lt;value name=david&gt;&lt;/value&gt;' would described by:
      *  setTagName('value');
      *  setTagAttribute('name');
      *  setValueAttribute('name');
     *</PRE>
     *
     *  NOTE: not calling this function will mean to get the column's value from the BODY
     *        of the tag.
     *
     *@param attName name of the attribute that the column's value will be extracted from
     */
    public void setValueAttribute(String attName) {
        valueAttribute = attName;
        valueType = VALUE_IS_ATT;
    }

    /**
     * Given a set of XML attributes associated with an XML tag, find the index of
     * a given attribute in a case insensitive way.
     *
     * This is the case insensitive version of the standard function.  Returns -1 if
     * the attribute cannot be found
     *
     * @param atts XML attributes for a tag
     * @param att_name name of the attribute
     **/
    int lookupAttribute(Attributes atts, String att_name) {
        int t;

        for (t = 0; t < atts.getLength(); t++) {
            if (atts.getQName(t).equalsIgnoreCase(att_name)) {
                return t;
            }
        }

        return -1;
    }

    /**
     * Given an xml tag (its name and attributes), see if it matches this column description<br>
     *  If it doesnt, return 0 <br>
     *  If it does, return either VALUE_IS_BODY or VALUE_IS_ATTRIBUTE<br>
     * @param XMLtagName name of the xml tag
     * @param xmlAtts list of the xml attributes for the tag (cf. xerces or SAX)
     */
    public int match(String XMLtagName, Attributes xmlAtts) {
        int attindex;

        if (XMLtagName.compareToIgnoreCase(tagName) == 0) {
            //tags match
            if (tagNeedsAttribute) {
                //attindex = xmlAtts.getIndex(attributeName);
                attindex = lookupAttribute(xmlAtts, attributeName);

                if (attindex == -1) {
                    return 0; //doesnt have the required attribute
                }

                if (tagAttributeNeedsValue) {
                    if (xmlAtts.getValue(attindex).compareToIgnoreCase(attributeValue) != 0) {
                        return 0; // attribute doesnt have the correct value
                    }
                }
            }

            return valueType;
        }

        return 0; // not the right tag
    }
}
