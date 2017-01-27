package com.vividsolutions.jump.workbench.imagery;

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
import java.util.Map;
import java.util.WeakHashMap;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class ImageryLayerDataset {
    public static final String ATTR_GEOMETRY = "GEOMETRY";
    public static final String ATTR_FILE = "IMAGEFILE";
    public static final String ATTR_FORMAT = "IMAGEFORMAT";
    public static final String ATTR_ERROR = "IMAGEERROR";
    public static final String ATTR_TYPE = "IMAGETYPE";
    public static final String ATTR_FACTORY = "IMAGEFACT";


    public static FeatureSchema SCHEMA = new FeatureSchema() {
        {
            addAttribute(ATTR_GEOMETRY, AttributeType.GEOMETRY);
            addAttribute(ATTR_FILE, AttributeType.STRING);
            addAttribute(ATTR_FORMAT, AttributeType.STRING);
            addAttribute(ATTR_FACTORY, AttributeType.STRING);
            addAttribute(ATTR_ERROR, AttributeType.STRING);
            addAttribute(ATTR_TYPE, AttributeType.STRING);
        }
    };

    public static FeatureSchema getSchema() {
        return SCHEMA;
    }

    public void createImage(Feature feature) {
        String factoryClassPath = (String) feature.getString(ATTR_FACTORY);
        String imageFilePath = (String) feature.getString(ATTR_FILE);
        GeometryFactory geometryFactory = new GeometryFactory();

        try {
            ReferencedImageFactory imageFactory = ( ReferencedImageFactory )
                        Class.forName( factoryClassPath ).newInstance();
            ReferencedImage referencedImage = imageFactory.createImage(imageFilePath);

            featureToReferencedImageMap.put(feature, referencedImage);
            Envelope env = referencedImage.getEnvelope();
            Geometry boundingBox = geometryFactory.toGeometry(env);
            feature.setGeometry(boundingBox);
            
            feature.setAttribute(ATTR_TYPE,referencedImage.getType());
        } catch (Exception e) {
            feature.setAttribute(ATTR_ERROR, e.toString());
            e.printStackTrace();
        }
    }

    public ReferencedImage referencedImage(Feature feature) {
        if (!(feature.getString(ATTR_ERROR) == null
                || feature.getString(ATTR_ERROR).equals(""))) {
            return null;
        }
        if (!featureToReferencedImageMap.containsKey(feature)) {
            createImage(feature);
        }
        // Will be null if an exception occurs [Jon Aquino 2005-04-12]
        return (ReferencedImage) featureToReferencedImageMap.get(feature);
    }

    private Map featureToReferencedImageMap = new WeakHashMap();
}