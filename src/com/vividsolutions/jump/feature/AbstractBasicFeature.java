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
package com.vividsolutions.jump.feature;
import java.io.Serializable;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.Assert;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the Feature interface. Subclasses need
 * implement only the four remaining Feature methods: #getAttribute,
 * #setAttribute, #getAttributes, #setAttributes
 */
public abstract class AbstractBasicFeature implements Feature, Serializable {

    private static final long serialVersionUID = 4215477286292970800L;
    private FeatureSchema schema;
    private int id;

    // [mmichaud 2012-10-13] userData idea is taken from the GeoAPI interfaces,
    // and is used for dynamic attributes calculation to avoid circular references.
    // Access methods are not yet exposed in Feature interface.
    private Map<Object,Object> userData;

    /**
     * A low-level accessor that is not normally used.
     */
    public void setSchema(FeatureSchema schema) {
        this.schema = schema;
    }

    /**
     * Creates a new Feature based on the given metadata.
     *
     *@param  featureSchema  the metadata containing information on each column
     */
    public AbstractBasicFeature(FeatureSchema featureSchema) {
        id = FeatureUtil.nextID();
        this.schema = featureSchema;
    }

    /**
     * Returns a number that uniquely identifies this feature. This number is not
     * persistent.
     * @return n, where this feature is the nth Feature created by this application
     */
    public int getID() {
        return id;
    }

    /**
     * Sets the specified attribute.
     *
     *@param  attributeName  the name of the attribute to set
     *@param  newAttribute   the new attribute
     */
    public void setAttribute(String attributeName, Object newAttribute) {
        setAttribute(schema.getAttributeIndex(attributeName), newAttribute);
    }

    /**
     *  Convenience method for setting the spatial attribute. JUMP Workbench
     * PlugIns and CursorTools should not use this method directly, but should use an
     * EditTransaction, so that the proper events are fired.
     *
     *@param  geometry  the new spatial attribute
     */
    public void setGeometry(Geometry geometry) {
        setAttribute(schema.getGeometryIndex(), geometry);
    }

    /**
     * Returns the specified attribute.
     * Throws an ArrayOutOfBoundException if attributeName does not exists.
     *@param  name  the name of the attribute to get
     *@return the attribute
     */
    public Object getAttribute(String name) {
        return getAttribute(schema.getAttributeIndex(name));
    }

    /**
     * Returns a String representation of the attribute at the given index.
     * If the attribute at the given index is null, the method returns null.
     *
     *@param  attributeIndex  the array index of the attribute
     *@return                 a String representation of the attribute.
     */
    public String getString(int attributeIndex) {
        // return (String) attributes[attributeIndex];
        //Dave B changed this so you can convert Integers->Strings
        //Automatic conversion of integers to strings is a bit hack-like.
        //Instead one should do #getAttribute followed by #toString.
        //#getString should be strict: it should throw an Exception if it is used
        //on a non-String attribute. [Jon Aquino]
        Object result = getAttribute(attributeIndex);
        //We used to eat ArrayOutOfBoundsExceptions here. I've removed this behaviour
        //because ArrayOutOfBoundsExceptions are bugs and should be exposed. [Jon Aquino]        
        //Is it valid for an attribute to be null? If not, we should put an
        //Assert here [Jon Aquino]
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    /**
     * Returns a integer attribute.
     *
     *@param  attributeIndex the index of the attribute to retrieve
     *@return                the integer attribute with the given name
     */
    public int getInteger(int attributeIndex) {
        return ((Integer) getAttribute(attributeIndex)).intValue();
    }

    /**
     *  Returns a double attribute.
     *
     *@param  attributeIndex the index of the attribute to retrieve
     *@return                the double attribute with the given name
     */
    public double getDouble(int attributeIndex) {
        return ((Double) getAttribute(attributeIndex)).doubleValue();
    }

    //<<TODO:DOC>>Update JavaDoc -- the attribute need not be a String [Jon Aquino]
    /**
     * Returns a String representation of the attribute at the given index.
     * If the attribute at the given index is null, the method returns null.
     *
     *@param  attributeName  the name of the attribute to retrieve
     *@return                the String attribute with the given name
     */
    public String getString(String attributeName) {
        return getString(schema.getAttributeIndex(attributeName));
    }

    /**
     * Convenience method for returning the spatial attribute.
     *
     *@return    the feature's spatial attribute
     */
    public Geometry getGeometry() {
        return (Geometry) getAttribute(schema.getGeometryIndex());
    }

    /**
     *  Returns the feature's metadata
     *
     *@return    the metadata describing the names and types of the attributes
     */
    public FeatureSchema getSchema() {
        return schema;
    }

    /**
     * Clones this Feature. Geometry and PrimaryKey will also be cloned.
     *
     * Warning : clone method does not follow general contract of clone (which
     * recommends using super.clone) but makes a deep copy of the Object using
     * BasicFeature constructor.
     *
     * @return a new Feature with the same attributes as this Feature
     */
    public Feature clone() {
        return clone(true);
    }

    /**
     * Clones this Feature.
     * @param deep whether or not to clone the geometry
     * @return a new Feature with the same attributes as this Feature
     */
    public Feature clone(boolean deep) {
        return clone(this, deep, true);
    }

    /**
     * Clones this Feature.
     * @param deep whether or not to clone the geometry
     * @param copyPK whether or not to copy external PK attribute if exists
     * @return a new Feature with the same attributes as this Feature
     */
    public Feature clone(boolean deep, boolean copyPK) {
        return clone(this, deep, copyPK);
    }

    /**
     * Util static method used to create a new BasicFeature from a feature.
     *
     * @param feature the feature to be cloned
     * @param deep if deep, the geometry is cloned.
     * @param copyPK if copyPK is true and a PK is defined, the PK is copied
     *               otherwise, the PK is set to null.
     * @return a new BasicFeature
     */
    public static BasicFeature clone(Feature feature, boolean deep, boolean copyPK) {
        BasicFeature clone = new BasicFeature(feature.getSchema());
        for (int i = 0; i < feature.getSchema().getAttributeCount(); i++) {
            if (feature.getSchema().getAttributeType(i) == AttributeType.GEOMETRY) {
                clone.setAttribute(i, deep ? feature.getGeometry().copy() : feature.getGeometry());
            } else if (feature.getSchema().getExternalPrimaryKeyIndex() == i) {
                if (copyPK) clone.setAttribute(i, feature.getAttribute(i));
            } else {
                clone.setAttribute(i, feature.getAttribute(i));
            }
        }
        return clone;        
    }

    public int compareTo(Object o) {
        return compare(this, (Feature)o);
    }

    /**
     * Static method to compare two features. The method uses feature ID to
     * compare them in a first time, and if equals, it uses the feature hashcode.
     * @param a the first feature to be compared
     * @param b the second feature to be compared
     * @return a positive integer if a > b, a negative integer if a < b and 0
     * if a and b have same ID and same hashcode.
     */
    public static int compare(Feature a, Feature b) {
        int geometryComparison = a.getGeometry().compareTo((b).getGeometry());
        if (geometryComparison != 0) { return geometryComparison; }
        if (a == b) { return 0; }
        //The features do not refer to the same object, so try to return something consistent. [Jon Aquino]
        if (a.getID() != b.getID()) { return a.getID() - b.getID(); }
        //The ID is hosed. Last gasp: hope the hash codes are different. [Jon Aquino]
        if (a.hashCode() != b.hashCode()) { return a.hashCode() - b.hashCode(); }
        Assert.shouldNeverReachHere();
        return -1;        
    }
    
    /**
     * Sets a new value in userData replacing the old one for this key.
     */
    public void setUserData(Object key, Object value) {
        if (userData == null) userData = new HashMap<Object,Object>();
        userData.put(key, value);
    }
    
    /**
     * Gets the userData value for this key.
     */
    public Object getUserData(Object key) {
        return userData == null ? null : userData.get(key);
    }
    
    /**
     * Removes the userData value for this key.
     */
    public void removeUserData(Object key) {
        if (userData != null) userData.remove(key);
    }
    
    /**
     * Remove all the userData keys and nullify the userData map itself.
     */
    public void removeAllUserData(Object key) {
        if (userData != null) userData.clear();
        userData = null;
    }
}
