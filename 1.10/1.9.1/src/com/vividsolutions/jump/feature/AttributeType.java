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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Types of attributes
 * <p>
 * </p>
 * 
 * @author
 * @since 1.0
 */
public class AttributeType implements Serializable {
  private static final long serialVersionUID = -8443945292593638566L;

  // [SBCALVO - 27/10/2008] Changed from HashMap to LinkedMap to iterate over
  // the values preserving adding order Must be initialized before the
  // AttributeType constants because AttributeType constructor uses
  // nameToAttributeTypeMap
  private static Map nameToAttributeTypeMap = new LinkedHashMap();

  // String attributes
  public final static AttributeType STRING = new AttributeType(
      "STRING", String.class); //$NON-NLS-1$
  public final static AttributeType CHAR = new AttributeType(
      "CHAR", String.class); //$NON-NLS-1$
  public final static AttributeType VARCHAR = new AttributeType(
      "VARCHAR", String.class); //$NON-NLS-1$
  public final static AttributeType LONGVARCHAR = new AttributeType(
      "LONGVARCHAR", String.class); //$NON-NLS-1$
  public final static AttributeType TEXT = new AttributeType(
      "TEXT", String.class); //$NON-NLS-1$

  // Boolean attributes
  public final static AttributeType BOOLEAN = new AttributeType(
      "BOOLEAN", Boolean.class); //$NON-NLS-1$
  public final static AttributeType BIT = new AttributeType(
      "BIT", Boolean.class); //$NON-NLS-1$    

  // Short attributes
  public final static AttributeType SMALLINT = new AttributeType(
      "SMALLINT", Short.class); //$NON-NLS-1$
  public final static AttributeType TINYINT = new AttributeType(
      "TINYINT", Short.class); //$NON-NLS-1$

  // Integer attributes
  public final static AttributeType INTEGER = new AttributeType(
      "INTEGER", Integer.class); //$NON-NLS-1$

  // Long attributes
  public final static AttributeType LONG = new AttributeType("LONG", Long.class); //$NON-NLS-1$
  public final static AttributeType BIGINT = new AttributeType(
      "BIGINT", Long.class); //$NON-NLS-1$

  // BigDecimal attributes
  public final static AttributeType DECIMAL = new AttributeType(
      "DECIMAL", BigDecimal.class); //$NON-NLS-1$
  public final static AttributeType NUMERIC = new AttributeType(
      "NUMERIC", BigDecimal.class); //$NON-NLS-1$
  public final static AttributeType BIGDECIMAL = new AttributeType(
      "BIGDECIMAL", BigDecimal.class); //$NON-NLS-1$

  // Float attributes
  public final static AttributeType FLOAT = new AttributeType(
      "FLOAT", Float.class); //$NON-NLS-1$

  // Double attributes
  public final static AttributeType DOUBLE = new AttributeType(
      "DOUBLE", Double.class); //$NON-NLS-1$
  public final static AttributeType REAL = new AttributeType(
      "REAL", Double.class); //$NON-NLS-1$

  // Date attributes
  public final static AttributeType DATE = new AttributeType(
      "DATE", java.sql.Date.class); //$NON-NLS-1$

  // Time attributes
  public final static AttributeType TIME = new AttributeType(
      "TIME", java.sql.Time.class); //$NON-NLS-1$

  // Timestamp attributes
  public final static AttributeType TIMESTAMP = new AttributeType(
      "TIMESTAMP", java.sql.Timestamp.class); //$NON-NLS-1$

  // Geometry attributes
  public final static AttributeType GEOMETRY = new AttributeType(
      "GEOMETRY", Geometry.class); //$NON-NLS-1$

  // Other attribute types
  public final static AttributeType OBJECT = new AttributeType(
      "OBJECT", Object.class); //$NON-NLS-1$

  /** Attribute type name */
  private String name;

  /** Attribute type java class mapping */
  private Class<?> javaClass;

  /**
   * @return
   */
  public static Collection<AttributeType> allTypes() {
    return nameToAttributeTypeMap.values();
  }

  /**
   * @return
   */
  public static Collection<AttributeType> basicTypes() {
    List<AttributeType> basicTypes = new ArrayList<AttributeType>();
    basicTypes.add(GEOMETRY);
    basicTypes.add(STRING);
    basicTypes.add(INTEGER);
    basicTypes.add(LONG);
    //basicTypes.add(FLOAT);
    basicTypes.add(DOUBLE);
    basicTypes.add(DATE);
    //basicTypes.add(TIMESTAMP);
    basicTypes.add(BOOLEAN);
    basicTypes.add(OBJECT);
    return basicTypes;
  }

  /**
   * @param name
   * @param javaClass
   */
  private AttributeType(String name, Class<?> javaClass) {
    this.name = name;
    this.javaClass = javaClass;
    nameToAttributeTypeMap.put(name, this);
  }

  /**
   * @param name
   */
  public AttributeType(String name) {
    this.name = name;
    this.javaClass = ((AttributeType) nameToAttributeTypeMap.get(name))
        .toJavaClass();
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Converts a type name to an AttributeType.
   * 
   * @param name
   *          the name of the AttributeType to retrieve
   * @return the corresponding AttributeType
   */
  public final static AttributeType toAttributeType(String name) {
    AttributeType type = (AttributeType) nameToAttributeTypeMap.get(name);

    if (type == null) {
      throw new IllegalArgumentException();
    }

    return type;
  }

  /**
   * @return
   */
  public Class<?> toJavaClass() {
    return javaClass;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !obj.getClass().equals(AttributeType.class)) {
      return false;
    }
    AttributeType tipo = (AttributeType) obj;

    // [SBCALVO: 18/09/2008] Cambiado la comparacion de tipo de clase a nombre
    // (revisar)
    return name.equals(tipo.getName());
  }

  /**
   * Gets the attribute type related with a given class
   * 
   * @param javaClass
   * @return
   */
  public static AttributeType toAttributeType(Class<?> javaClass) {
    // The order is given by the adding order
    for (Iterator<AttributeType> i = allTypes().iterator(); i.hasNext();) {
      AttributeType type = i.next();
      if (type.toJavaClass() == javaClass) {
        return type;
      }
    }
    return null;
  }

  /**
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if two attribute types are compatible.
   * 
   * @param attrType1
   * @param attrType2
   * @return
   */
  public static boolean areCompatibleTypes(AttributeType attrType1,
      AttributeType attrType2) {
    // Primero miramos si son el mismo o se traducen a la misma clase
    boolean compatible = attrType1.equals(attrType2)
        || attrType1.toJavaClass().equals(attrType2.toJavaClass());

    if (!compatible) {
      // Comprobamos que no siendo el mismo son compatibles
      Class<?> javaClass1 = attrType1.toJavaClass();
      Class<?> javaClass2 = attrType2.toJavaClass();
      if ((Number.class.isAssignableFrom(javaClass1) && Number.class
          .isAssignableFrom(javaClass2))
          || ((javaClass1.equals(String.class) && javaClass2
              .equals(Boolean.class)))
          || (javaClass1.equals(Boolean.class) && javaClass2
              .equals(String.class))
          || (Number.class.isAssignableFrom(javaClass1) && javaClass2
              .equals(Boolean.class))
          || (javaClass1.equals(Boolean.class) && (Number.class
              .isAssignableFrom(javaClass2)))) {
        compatible = true;
      }
    }
    return compatible;

  }

  /**
   * Checks if an attribute type is numeric
   * 
   * @param type
   *          Tipo de atributo
   * @return
   */
  public static boolean isNumeric(AttributeType type) {
    return Number.class.isAssignableFrom(type.toJavaClass());
  }

  /**
   * Checks if an attribute type is numeric without any decimal
   * 
   * @param type
   * @return
   */
  public static boolean isNumericWithoutDecimal(AttributeType type) {
    return Long.class.isAssignableFrom(type.toJavaClass());
  }

  /**
   * Checks if an attribute type is a date
   * 
   * @param type
   *          Tipo de atributo
   * @return
   */
  public static boolean isDate(AttributeType type) {
    return Date.class.isAssignableFrom(type.toJavaClass());
  }

  /**
   * Checks if an attribute type is assignable to a String
   * 
   * @param type
   *          Tipo de atributo
   * @return
   */
  public static boolean isString(AttributeType type) {
    return String.class.isAssignableFrom(type.toJavaClass());
  }

  /**
   * Checks if an attribute type is assignable to a boolean
   * 
   * @param type
   *          Tipo de atributo
   * @return
   */
  public static boolean isBoolean(AttributeType type) {
    return Boolean.class.isAssignableFrom(type.toJavaClass());
  }
}