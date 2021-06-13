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

import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.AssertionFailedException;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystem;

import java.io.Serializable;
import java.util.*;

/**
 * Metadata for a FeatureCollection: attribute names and types.
 * @see FeatureCollection
 */
public class FeatureSchema implements Cloneable, Serializable {

    private static final long serialVersionUID = -8627306219650589202L;

    protected CoordinateSystem coordinateSystem = CoordinateSystem.UNSPECIFIED;
    protected Map<String,Integer> attributeNameToIndexMap = new HashMap<>();
    protected int geometryIndex = -1;
    protected int externalPKIndex = -1;    // [mmichaud 2013-07-21] database id used in client-server environment
    protected int attributeCount = 0;
    protected List<String> attributeNames = new ArrayList<>();
    protected List<AttributeType> attributeTypes = new ArrayList<>();
    protected List<Boolean> attributeReadOnly = new ArrayList<>();
    // [mmichaud 2012-10-13] add Operation capability for dynamic attributes 
    protected ArrayList<Operation> operations = new ArrayList<>();

    public FeatureSchema() {
    }
    
    public FeatureSchema( FeatureSchema fsIn ) {
      cloneFromTo(fsIn, this);
    }
    
  /**
   * a deepcopy routine to copy one FeatureSchema values into another,
   * preferrable freshly created
   * 
   * @param fsIn input FeatureSchema
   * @param fsOut output FeatureSchema
   */
    public static void cloneFromTo( FeatureSchema fsIn, FeatureSchema fsOut ) {
      for (int i = 0; i < fsIn.attributeCount; i++) {
        AttributeType at = fsIn.attributeTypes.get(i);
        String aname = fsIn.attributeNames.get(i);
        fsOut.addAttribute(aname,at);
        fsOut.setAttributeReadOnly(i, fsIn.isAttributeReadOnly(i));
        fsOut.setOperation(i, fsIn.getOperation(i));
      }
      fsOut.setCoordinateSystem(fsIn.coordinateSystem);
    }
    
    /**
     * Creates a deep copy of this FeatureSchema.
     *
     * Warning : FeatureSchema.clone() does not follow general contract of clone
     * (which recommends using super.clone) but makes a deep copy of the original
     * FeatureSchema using the constructor.
     */
    public FeatureSchema clone() {
        FeatureSchema fsOut = new FeatureSchema();
        cloneFromTo(this, fsOut);
        return fsOut;
    }

    /**
     * Returns the zero-based index of the attribute with the given name
     * (case-sensitive)
     * @throws  IllegalArgumentException  if attributeName is unrecognized
     */
    public int getAttributeIndex(String attributeName) {
        //<<TODO:RECONSIDER>> Attribute names are currently case sensitive.
        //I wonder whether or not this is desirable. [Jon Aquino]
        Integer index = attributeNameToIndexMap.get(attributeName);
        if (index == null) {
            throw new IllegalArgumentException(
                I18N.getInstance().get("feature.FeatureSchema.unrecognized-attribute-name")+" " + attributeName);
        }
        return index;
    }

    /**
     * Returns whether this FeatureSchema has an attribute with this name
     * @param attributeName the name to look up
     * @return whether this FeatureSchema has an attribute with this name
     */
    public boolean hasAttribute(String attributeName) {
        return attributeNameToIndexMap.get(attributeName) != null;
    }

    /**
	 * Returns the attribute index of the Geometry, or -1 if there is no
	 * Geometry attribute
	 */
    public int getGeometryIndex() {
        return geometryIndex;
    }

    /**
     * Returns the (case-sensitive) name of the attribute at the given zero-based index.
     */    
    public String getAttributeName(int attributeIndex) {
        return attributeNames.get(attributeIndex);
    }

    /**
     * Returns whether the attribute at the given zero-based index is a string,
     * integer, double, etc.
     */
    public AttributeType getAttributeType(int attributeIndex) {
        return attributeTypes.get(attributeIndex);
    }

    /**
     * Returns whether the attribute with the given name (case-sensitive) is a string,
     * integer, double, etc.
     */    
    public AttributeType getAttributeType(String attributeName) {
        return getAttributeType(getAttributeIndex(attributeName));
    }

    /**
     * Returns the total number of spatial and non-spatial attributes in this
     * FeatureSchema. There are 0 or 1 spatial attributes and 0 or more
     * non-spatial attributes.
     */
    public int getAttributeCount() {
        return attributeCount;
    }

    /**
     * Returns an unmodifiable list containing all attribute names.
     * Method added to facilitate foreach iteration over attributes
     * @return an unmodifiable list of attribute names
     */
    public List<String> getAttributeNames() {
       return Collections.unmodifiableList(attributeNames);
    }

    /**
     * Adds an attribute with the given case-sensitive name. 
     * @throws AssertionFailedException if a second Geometry is being added
     */
    public void addAttribute(String attributeName, AttributeType attributeType) {
        if (AttributeType.GEOMETRY == attributeType) {
            //Assert.isTrue(geometryIndex == -1);
            if (geometryIndex != -1) {
                throw new IllegalArgumentException(
                        I18N.getInstance().get("feature.FeatureSchema.only-one-geometry-is-authorized"));
            }
            geometryIndex = attributeCount;
        }
        //Assert.isTrue(!attributeNames.contains(attributeName));
        if (attributeNames.contains(attributeName)) {
            throw new IllegalArgumentException(
                    I18N.getInstance().get("feature.FeatureSchema.attribute-already-exists", attributeName));
        }
        attributeNames.add(attributeName);
        attributeNameToIndexMap.put(attributeName, attributeCount);
        attributeTypes.add(attributeType);
        // default to current implementation - all attributes are editable (not readonly)
        attributeReadOnly.add(false);
        operations.add(null);
        attributeCount++;
    }

    /**
     * Add a dynamic attribute to this FeatureSchema
     * A dynamic attribute is a readOnly attribute which is dynamically
     * evaluated on demand.
     * @since 1.6
     */
    public void addDynamicAttribute(String attributeName, 
                AttributeType attributeType, Operation operation) {
        Assert.isTrue(attributeType != AttributeType.GEOMETRY);
        attributeNames.add(attributeName);
        attributeNameToIndexMap.put(attributeName, attributeCount);
        attributeTypes.add(attributeType);
        attributeReadOnly.add(true);
        operations.add(operation);
        attributeCount++;
    
    }

    /**
     * Returns whether the two FeatureSchemas have the same attribute names
     * with the same types and in the same order.
     */
    public boolean equals(Object other) {
        return other instanceof FeatureSchema && this.equals(other, false);
    }

    /**
     * Returns whether the two FeatureSchemas have the same attribute names
     * with the same types and (optionally) in the same order.
     * WARNING : be aware that equals method compare neither isReadOnly attribute 
     * nor operation. Not sure if it must be added. 
     */
    public boolean equals(Object other, boolean orderMatters) {
        if (!(other instanceof FeatureSchema)) {
            return false;
        }
        FeatureSchema otherFeatureSchema = (FeatureSchema) other;
        if (attributeNames.size() != otherFeatureSchema.attributeNames.size()) {
            return false;
        }
        for (int i = 0; i < attributeNames.size(); i++) {
            String attributeName = attributeNames.get(i);
            if (!otherFeatureSchema.attributeNames.contains(attributeName)) {
                return false;
            }
            if (orderMatters
                && !otherFeatureSchema.attributeNames.get(i).equals(attributeName)) {
                return false;
            }
            if (getAttributeType(attributeName)
                != otherFeatureSchema.getAttributeType(attributeName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the CoordinateSystem associated with this FeatureSchema, but does
     * not perform any reprojection.
     * @return this FeatureSchema
     */
	public FeatureSchema setCoordinateSystem(CoordinateSystem coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
		return this;
	}

    /**
     * @see #setCoordinateSystem(CoordinateSystem)
     */
	public CoordinateSystem getCoordinateSystem() {
		return coordinateSystem;
	}

	/**
	 * Returns the "readonly" status of the attribute specified by the
	 * attributeIndex.<br>
	 * <br>
	 * A return result of <tt>TRUE</tt> means the a user will not be able to
	 * edit the attribute in the layer's attribute table, even though the
	 * layer's "editable" flag has been set to <tt>TRUE</tt>
	 * 
	 * @param attributeIndex The index of the attribute in question.
	 * @return <tt>TRUE</tt> if the specified attribute has been previously set
	 *         as readonly.
	 * @see #setAttributeReadOnly(int, boolean)
	 */
	public boolean isAttributeReadOnly(int attributeIndex) {
		return attributeReadOnly.get(attributeIndex);
	}

	/**
	 * Sets the "readonly" status of the attribute specified by the
	 * attributeIndex. <br>
	 * <br>
	 * Some schemas (like those that represent database tables) can have
	 * attributes that should not be modified (like primary keys). Setting such
	 * an attribute as readonly means a user will not be able to edit the
	 * attribute in the layer's attribute table, even though the layer's
	 * "editable" flag has been set to <tt>TRUE</tt>
	 * 
	 * @param attributeIndex The index of the attribute to set
	 * @param isReadOnly A flag that indicates whether the specified attribute should
	 *            be considered "readonly".
	 * @see #isAttributeReadOnly(int)
	 */
	public void setAttributeReadOnly(int attributeIndex, boolean isReadOnly) {
		attributeReadOnly.set(attributeIndex, isReadOnly);
	}
	
    /**
	 * Returns true if an attribute must be computed on the fly by an Operation.
	 * @param attributeIndex The index of the attribute in question.
	 * @return <tt>TRUE</tt> if the specified attribute is dynamically computed.
	 */
	 public boolean isOperation(int attributeIndex) {
	     return attributeIndex >= 0 && operations.get(attributeIndex) != null;
	 }
	 
	/**
	 * Set the Operation in charge of computing this attribute.
	 * @param attributeIndex index of the attribute to compute.
	 * @param operation operation in charge of the evaluation.
	 */
	 public void setOperation(int attributeIndex, Operation operation) {
	     operations.set(attributeIndex, operation);
	 }
	 
	/**
	 * Get the operation in charge of the attribute value evaluation.
	 * @param attributeIndex index of the attribute.
	 */
	 public Operation getOperation(int attributeIndex) {
	     return operations.get(attributeIndex);
	 }

    /**
     * Returns the attribute index of the externalId attribute, or -1 if there is no
     * externalId.
     */
    public int getExternalPrimaryKeyIndex() {
        return externalPKIndex;
    }

    /**
     * Sets the primary key to be the attribute at position index.
     * @param index index of the external primary key
     */
    public void setExternalPrimaryKeyIndex(int index) {
        assert index < getAttributeCount();
        AttributeType attributeType = this.getAttributeType(index);
        if (attributeType == AttributeType.INTEGER ||
                attributeType == AttributeType.LONG ||
                attributeType == AttributeType.STRING ||
                attributeType == AttributeType.OBJECT) {
            this.externalPKIndex = index;
            setAttributeReadOnly(index, true);
        } else {
            throw new IllegalArgumentException("Primary Key must be of type String, Integer or Object");
        }
    }

    /**
     * Remove the primary key from this schema definition.
     */
    public void removeExternalPrimaryKey() {
        this.externalPKIndex = -1;
    }

    /**
     * Add an attribute containing an external identifier.
     * This attribute is read-only fo OpenJUMP. It is the responsability of the external
     * datastore to write in this attribute.
     * @param attributeName name of the external id
     * @param attributeType type of the external id
     * @throws IllegalArgumentException if the attributeType of the id is not one of
     * Integer, String or Object (for Long)
     */
    public void addExternalPrimaryKey(String attributeName, AttributeType attributeType) {
        if (attributeType == AttributeType.INTEGER ||
                attributeType == AttributeType.STRING ||
                attributeType == AttributeType.OBJECT) {
            addAttribute(attributeName, attributeType);
            setAttributeReadOnly(getAttributeIndex(attributeName), true);
        } else {
            throw new IllegalArgumentException("Primary Key must be of type String, Integer or Object");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FeatureSchema (").append(getCoordinateSystem()).append(")");
        for (int i = 0 ; i < getAttributeCount() ; i++) {
            if (geometryIndex==i) sb.append("\n\tGeometry: ");
            else if (externalPKIndex==i) sb.append("\n\tExternalId: ");
            else sb.append("\n\t");
            sb.append(getAttributeName(i)).append(" ").append(attributeTypes.get(i));
            if (operations.get(i) != null) sb.append(" [operation]");
            else if (attributeReadOnly.get(i)) sb.append(" [read only]");
        }
        return sb.toString();
    }

}
