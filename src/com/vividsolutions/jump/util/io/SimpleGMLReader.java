
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

package com.vividsolutions.jump.util.io;

import java.io.*;
import java.util.List;

import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.io.*;


/**
 * Provides an easy way to read spatial data from a GML document. Attributes
 * are not read. Simply pass in a Reader on the GML, and the names of the
 * various tags. A List of Geometries will be returned.
 */
public class SimpleGMLReader {
    public SimpleGMLReader() {
    }

    /**
     * @param gml a Reader on an XML document containing GML
     * @param collectionElement the name of the feature-collection tag
     * @param featureElement the name of the feature tag
     * @param geometryElement the name of the geometry tag
     * @return a List of Geometries
     */
    public List toGeometries(Reader gml, String collectionElement,
        String featureElement, String geometryElement)
        throws Exception {
        GMLInputTemplate template = template(collectionElement, featureElement,
                geometryElement);
        GMLReader gmlReader = new GMLReader();
        gmlReader.setInputTemplate(template);

        return FeatureUtil.toGeometries(gmlReader.read(gml).getFeatures());
    }

    private GMLInputTemplate template(String collectionElement,
        String featureElement, String geometryElement)
        throws IOException, ParseException {
        String s = "";
        s += "<?xml version='1.0' encoding='UTF-8'?>";
        s += "<JCSGMLInputTemplate>";
        s += ("<CollectionElement>" + collectionElement +
        "</CollectionElement>");
        s += ("<FeatureElement>" + featureElement + "</FeatureElement>");
        s += ("<GeometryElement>" + geometryElement + "</GeometryElement>");
        s += "<ColumnDefinitions></ColumnDefinitions>"; //no attributes read
        s += "</JCSGMLInputTemplate>";

        GMLInputTemplate template = new GMLInputTemplate();
        StringReader sr = new StringReader(s);

        try {
            template.load(sr);
        } finally {
            sr.close();
        }

        return template;
    }

    /**
     * @param gml
     * @see #toGeometries(Reader, String, String, String)
     */
    public List toGeometries(String gml, String collectionElement,
        String featureElement, String geometryElement)
        throws Exception {
        StringReader r = new StringReader(gml);

        try {
            return toGeometries(r, collectionElement, featureElement,
                geometryElement);
        } finally {
            r.close();
        }
    }

    /**
     * Reads a GML file that is in FME format.
     * @return the contents of the file, including both spatial and attribute data
     */
    public FeatureCollection readFMEFile(File file) throws Exception {
        FMEGMLReader fmeGMLReader = new FMEGMLReader();
        FileReader fileReader = new FileReader(file);
        GMLInputTemplate inputTemplate;

        try {
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            try {
                inputTemplate = fmeGMLReader.getGMLInputTemplate(bufferedReader,
                        file.getPath());
            } finally {
                bufferedReader.close();
            }
        } finally {
            fileReader.close();
        }

        GMLReader gmlReader = new GMLReader();
        gmlReader.setInputTemplate(inputTemplate);

        FeatureCollection fc;
        fileReader = new FileReader(file);

        try {
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            try {
                fc = gmlReader.read(bufferedReader);
            } finally {
                bufferedReader.close();
            }
        } finally {
            fileReader.close();
        }

        return fc;
    }
}
