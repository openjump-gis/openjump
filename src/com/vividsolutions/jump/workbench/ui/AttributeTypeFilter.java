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
package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

import java.util.ArrayList;
import java.util.List;

/**
 *  Filter to select attributes or layers based on their attribute types.
 */
public class AttributeTypeFilter {
    
    public static final int GEOMETRY     = 1;
    public static final int STRING       = 2;
    public static final int INTEGER      = 4;
    public static final int DOUBLE       = 8;
    public static final int DATE         = 16;
    public static final int OBJECT       = 32;
    public static final int BOOLEAN      = 64;
    public static final int LONG         = 128;

    
    /** Attribute filter includes GEOMETRY attributes.*/
    public static final AttributeTypeFilter GEOMETRY_FILTER = new AttributeTypeFilter(GEOMETRY);
    
    /** Attribute filter includes STRING attributes.*/
    public static final AttributeTypeFilter STRING_FILTER   = new AttributeTypeFilter(STRING);
    
    /** Attribute filter includes INTEGER attributes.*/
    public static final AttributeTypeFilter INTEGER_FILTER  = new AttributeTypeFilter(INTEGER);
    
    /** Attribute filter includes DOUBLE attributes.*/
    public static final AttributeTypeFilter DOUBLE_FILTER   = new AttributeTypeFilter(DOUBLE);
    
    /** Attribute filter includes DATE attributes.*/
    public static final AttributeTypeFilter DATE_FILTER     = new AttributeTypeFilter(DATE);
    
    /** Attribute filter includes OBJECT attributes.*/
    public static final AttributeTypeFilter OBJECT_FILTER   = new AttributeTypeFilter(OBJECT);

    /** Attribute filter includes BOOLEAN attributes.*/
    public static final AttributeTypeFilter BOOLEAN_FILTER  = new AttributeTypeFilter(BOOLEAN);

    /** Attribute filter includes LONG attributes.*/
    public static final AttributeTypeFilter LONG_FILTER     = new AttributeTypeFilter(LONG);
    
    /** Attribute filter includes NON GEOMETRIC attributes.*/
    public static final AttributeTypeFilter NO_GEOMETRY_FILTER
           = new AttributeTypeFilter(STRING + INTEGER + DOUBLE + DATE + OBJECT + BOOLEAN + LONG);
    
    /** Attribute filter includes NUMERIC attributes.*/
    public static final AttributeTypeFilter NUMERIC_FILTER 
           = new AttributeTypeFilter(INTEGER + LONG + DOUBLE);
           
    /** Attribute filter includes NUMERIC and STRING attributes.*/
    public static final AttributeTypeFilter NUMSTRING_FILTER 
           = new AttributeTypeFilter(INTEGER + LONG + DOUBLE + STRING);
    
    /** Attribute filter includes NUMERIC attributes.*/
    public static final AttributeTypeFilter ALL_FILTER      = new AttributeTypeFilter(255);
    
    private int filterType = 0;

    
    /** Create a new Filter filtering objects according to the filter type.*/
    public AttributeTypeFilter(int filterType) {
        this.filterType = filterType;
    }

    /** Inverse the filter.*/
    public AttributeTypeFilter getInverseFilter() {
        return new AttributeTypeFilter(~filterType);
    }
    
    /**
     * Filter layers from a LayerManager according to AttributeType of their
     * attributes.
     * 
     * @param layerManager the Layer Manager to filter
     * @return a List of Layers
     */
    public List<Layer> filter(LayerManager layerManager) {
        List<Layer> layerList = new ArrayList<>();
        for (Layer layer : layerManager.getLayers()) {
            FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            if (filter(schema).size() > 0) layerList.add(layer);
        }
        return layerList;
    }
    
    /**
     * Filter attributes of a FeatureSchema according to their AttributeType.
     * 
     * @param schema the FeatureSchema to Filter
     * @return a List of attributes matching the filter criteria
     */
    public List<String> filter(FeatureSchema schema) {
        List<String> attributes = new ArrayList<>();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if ((type == AttributeType.GEOMETRY && (filterType & 1)==1) ||
                (type == AttributeType.STRING && (filterType & 2)==2) ||
                (type == AttributeType.INTEGER && (filterType & 4)==4) ||
                (type == AttributeType.DOUBLE && (filterType & 8)==8) ||
                (type == AttributeType.DATE && (filterType & 16)==16) ||
                (type == AttributeType.OBJECT && (filterType & 32)==32) ||
                (type == AttributeType.BOOLEAN && (filterType & 64)==64) ||
                (type == AttributeType.LONG && (filterType & 128)==128)    ) {
            
                attributes.add(schema.getAttributeName(i));
            }
        }
        return attributes;
    }

    public List<String> filter(Layer layer) {
        return filter(layer.getFeatureCollectionWrapper().getFeatureSchema());
    }
    
    public String toString() {
        if (filterType == GEOMETRY) return "Geometry filter";
        if (filterType == STRING)   return "String filter";
        if (filterType == INTEGER)  return "Integer filter";
        if (filterType == DOUBLE)   return "Double filter";
        if (filterType == DATE)     return "Date filter";
        if (filterType == OBJECT)   return "Object filter";
        if (filterType == BOOLEAN)  return "Boolean filter";
        if (filterType == LONG)     return "Long filter";
        if (filterType == NUMERIC_FILTER.filterType) return "Numeric filter";
        if (filterType == NUMSTRING_FILTER.filterType) return "String or Numeric filter";
        if (filterType == NO_GEOMETRY_FILTER.filterType) return "No Geometry filter";
        if (filterType == ALL_FILTER.filterType) return "All filter";
        return "\"" + filterType + "\" filter";
    }
    

}
