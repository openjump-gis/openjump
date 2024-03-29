
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

package com.vividsolutions.jump.tools;

import java.util.*;

import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.feature.*;


public class AttributeMapping {

    private List<String> aAttributeNames;
    private List<String> bAttributeNames;
    private List<String> aNewAttributeNames;
    private List<String> bNewAttributeNames;
    private FeatureSchema aSchema;
    private FeatureSchema bSchema;

    protected AttributeMapping() {
        //for testing [Jon Aquino]
    }

    /**
     * Constructs an AttributeMapping that will transfer all the attributes from
     * two feature collections A and B. If A and B have attributes with the same
     * name, they will be postfixed with _1 and _2 respectively. Case sensitive.
     * If you only wish to map attributes for one schema, simply pass in a
     * new FeatureSchema for the other schema.
     * @param a schema for first feature collection from which to transfer attributes
     * @param b schema for second feature collection from which to transfer attributes
     */
    public AttributeMapping(FeatureSchema a, FeatureSchema b) {
        init(a, nonSpatialAttributeNames(a, null, null),
            nonSpatialAttributeNames(a, b, "_1"), b,
            nonSpatialAttributeNames(b, null, null),
            nonSpatialAttributeNames(b, a, "_2"));
    }

    /**
     * Constructs an AttributeMapping.
     * @param aSchema metadata for feature-collection A
     * @param aAttributeNames non-spatial feature-collection-A attributes to transfer
     * @param aNewAttributeNames corresponding names in the feature collection receiving the attributes
     * @param bSchema metadata for feature-collection B
     * @param bAttributeNames non-spatial feature-collection-B attributes to transfer
     * @param bNewAttributeNames corresponding names in the feature collection receiving the attributes
     */
    public AttributeMapping(FeatureSchema aSchema, List<String> aAttributeNames,
        List<String> aNewAttributeNames, FeatureSchema bSchema, List<String> bAttributeNames,
        List<String> bNewAttributeNames) {
        init(aSchema, aAttributeNames, aNewAttributeNames, bSchema,
            bAttributeNames, bNewAttributeNames);
    }

    private List<String> nonSpatialAttributeNames(FeatureSchema schema,
        FeatureSchema other, String postfix) {
        List<String> attributeNames = new ArrayList<>();

        for (int i = 0; i < schema.getAttributeCount(); i++) {
            if (schema.getAttributeType(i) == AttributeType.GEOMETRY) {
                continue;
            }

            String attributeName = schema.getAttributeName(i);

            if ((other != null) && other.hasAttribute(attributeName)) {
                attributeName += postfix;
            }

            attributeNames.add(attributeName);
        }

        return attributeNames;
    }

    private void init(FeatureSchema aSchema, List<String> aAttributeNames,
        List<String> aNewAttributeNames, FeatureSchema bSchema, List<String> bAttributeNames,
        List<String> bNewAttributeNames) {
        Assert.isTrue(isDisjoint(aNewAttributeNames, bNewAttributeNames));
        Assert.isTrue(aAttributeNames.size() == aNewAttributeNames.size());
        Assert.isTrue(bAttributeNames.size() == bNewAttributeNames.size());
        this.aSchema = aSchema;
        this.bSchema = bSchema;
        this.aAttributeNames = new ArrayList<>(aAttributeNames);
        this.bAttributeNames = new ArrayList<>(bAttributeNames);
        this.aNewAttributeNames = new ArrayList<>(aNewAttributeNames);
        this.bNewAttributeNames = new ArrayList<>(bNewAttributeNames);
    }

    /**
     * Returns a new FeatureSchema with the destination attributes of the mapping
     * and a spatial attribute with the given name
     * @param geometryName name to assign to the spatial attribute
     */
    public CombinedSchema createSchema(String geometryName) {
        CombinedSchema newSchema = new CombinedSchema();
        addAttributes(newSchema, aSchema, aAttributeNames, aNewAttributeNames, newSchema.aNewToOldAttributeIndexMap);
        newSchema.lastNewAttributeIndexForA = newSchema.getAttributeCount() - 1;
        addAttributes(newSchema, bSchema, bAttributeNames, bNewAttributeNames, newSchema.bNewToOldAttributeIndexMap);
        newSchema.addAttribute(geometryName, AttributeType.GEOMETRY);

        return newSchema;
    }
    
    public static class CombinedSchema extends FeatureSchema {
        private static final long serialVersionUID = -8627306219650589202L;
        private Map<Integer,Integer> aNewToOldAttributeIndexMap = new HashMap<>();
        private Map<Integer,Integer> bNewToOldAttributeIndexMap = new HashMap<>();
        public int toAOldAttributeIndex(int newAttributeIndex) {
            return aNewToOldAttributeIndexMap.get(newAttributeIndex);
        }
        public int toBOldAttributeIndex(int newAttributeIndex) {
            return bNewToOldAttributeIndexMap.get(newAttributeIndex);
        }
        private int lastNewAttributeIndexForA;
        public boolean isFromA(int newAttributeIndex) {
            return newAttributeIndex <= lastNewAttributeIndexForA;
        }
    }

    private void addAttributes(FeatureSchema newSchema,
                FeatureSchema sourceSchema, List<String> attributeNames,
                List<String> newAttributeNames, Map<Integer,Integer> newToOldAttributeIndexMap) {
        for (int i = 0; i < attributeNames.size(); i++) {
            String attributeName = attributeNames.get(i);
            String newAttributeName = newAttributeNames.get(i);
            AttributeType type = sourceSchema.getAttributeType(attributeName);

            if (type == AttributeType.GEOMETRY) {
                continue;
            }

            newSchema.addAttribute(newAttributeName, type);
            newToOldAttributeIndexMap.put(newSchema.getAttributeCount()-1, i);
        }
    }

    protected boolean isDisjoint(Collection<String> a, Collection<String> b) {
        HashSet<String> c = new HashSet<>();
        c.addAll(a);
        c.addAll(b);

        return c.size() == (a.size() + b.size());
    }

    /**
     * Transfers attributes (not the geometry) from two features to a third
     * feature, using the mappings specified in the constructor. The third feature's
     * schema must be able to accomodate the attributes being transferred.
     * @param aFeature a feature from feature-collection A
     * @param bFeature a feature from feature-collection B (can be null)
     * @param cFeature the feature to transfer the A and B attributes to
     */
    public void transferAttributes(Feature aFeature, Feature bFeature, Feature cFeature) {
    	//-- [sstein: 27Mar2008] added check to avoid errors
    	if ((aFeature != null) && (cFeature != null)){
	        transferAttributes(aFeature, cFeature, aAttributeNames, aNewAttributeNames);
    	}
    	if ((bFeature != null) && (cFeature != null)){
	        transferAttributes(bFeature, cFeature, bAttributeNames, bNewAttributeNames);
    	}
    }

    private void transferAttributes(Feature source, Feature dest,
        List<String> attributeNames, List<String> newAttributeNames) {
        for (int i = 0; i < attributeNames.size(); i++) {
            String attributeName = attributeNames.get(i);
            String newAttributeName = newAttributeNames.get(i);
            Assert.isTrue(source.getSchema().getAttributeType(attributeName) != AttributeType.GEOMETRY);
            dest.setAttribute(newAttributeName, source.getAttribute(attributeName));
        }
    }
}
