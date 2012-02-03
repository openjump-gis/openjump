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
    
    /** Attribute filter includes NON GEOMETRIC attributes.*/
    public static final AttributeTypeFilter NO_GEOMETRY_FILTER
           = new AttributeTypeFilter(STRING + INTEGER + DOUBLE + DATE + OBJECT);
    
    /** Attribute filter includes NUMERIC attributes.*/
    public static final AttributeTypeFilter NUMERIC_FILTER 
           = new AttributeTypeFilter(INTEGER + DOUBLE);
           
    /** Attribute filter includes NUMERIC and STRING attributes.*/
    public static final AttributeTypeFilter NUMSTRING_FILTER 
           = new AttributeTypeFilter(INTEGER + DOUBLE + STRING);
    
    /** Attribute filter includes NUMERIC attributes.*/
    public static final AttributeTypeFilter ALL_FILTER      = new AttributeTypeFilter(63);
    
    private int filterType = 0;
    
    /** Create a new Filter filtering objects according to the filter type.*/
    public AttributeTypeFilter(int filterType) {
        this.filterType = filterType;
    }
    
    /**
     * Filter layers from a LayerManager according to AttributeType of their
     * attributes.
     * 
     * @param layerManager the Layer Manager to filter
     * @return a List of Layers
     */
    public List<Layer> filter(LayerManager layerManager) {
        List<Layer> layerList = new ArrayList<Layer>();
        for (Layer layer : (List<Layer>)layerManager.getLayers()) {
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
        List<String> attributes = new ArrayList<String>();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if ((type == AttributeType.GEOMETRY && (filterType & 1)==1) ||
                (type == AttributeType.STRING && (filterType & 2)==2) ||
                (type == AttributeType.INTEGER && (filterType & 4)==4) ||
                (type == AttributeType.DOUBLE && (filterType & 8)==8) ||
                (type == AttributeType.DATE && (filterType & 16)==16) ||
                (type == AttributeType.OBJECT && (filterType & 32)==32)) {
            
                attributes.add(schema.getAttributeName(i));
            }
        }
        return attributes;
    }
    
    public String toString() {
        if (filterType == 1) return "Geometry filter";
        if (filterType == 2) return "String filter";
        if (filterType == 4) return "Integer filter";
        if (filterType == 8) return "Double filter";
        if (filterType == 12) return "Numeric filter";
        if (filterType == 14) return "String or Numeric filter";
        if (filterType == 16) return "Date filter";
        if (filterType == 28) return "Numeric or Date filter";
        if (filterType == 30) return "String, Numeric or Date filter";
        if (filterType == 32) return "Object filter";
        if (filterType == 62) return "No Geometry filter";
        if (filterType == 63) return "All filter";
        return "\"" + filterType + "\" filter";
    }
    

}
