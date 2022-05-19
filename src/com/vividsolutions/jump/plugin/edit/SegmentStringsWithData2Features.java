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
package com.vividsolutions.jump.plugin.edit;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.noding.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.linemerge.LineSequencer;
import com.vividsolutions.jump.feature.Feature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for noded Geometry reconstruction. It has two public
 * methods :
 * <ul>
 * <li>One to build a Geometry from a set of noded SegmentString carrying their
 * {@link SegmentStringData}</li>
 * <li>The second to build a full map containing Features and their 
 * corresponding SegmentString's Map</li>
 * </ul>
 *
 * @author Micha&euml;l Michaud
 */
public class SegmentStringsWithData2Features {

    private SegmentStringsWithData2Features() {}
    
    /**
     * Build a new noded geometry from the source Geometry and a structured
     * map of its SegmentStrings.
     * @param source the source geometry
     * @param nodedSegmentStrings the hierarchical map of noded segment strings
     * @param interpolate_z whether the z of SegmentString ends must be 
     *        interpolated or not
     * @param interpolated_z_dp is the number of decimal digits to keep in
     *        interpolated z values
     */
    public static Geometry buildGeometry(Geometry source, 
            Map<Integer,Map<Integer,List<SegmentString>>> nodedSegmentStrings,
            boolean interpolate_z, int interpolated_z_dp, GeometryFactory gf) {

        // Use the same factory as source
        Geometry[] finalComponents = new Geometry[nodedSegmentStrings.size()];

        // For each component
        for (int i = 0 ; i < finalComponents.length ; i++) {
            Geometry sourceComponent = source.getGeometryN(i);
            Map<Integer,List<SegmentString>> lines = nodedSegmentStrings.get(i);
            
            // LineString component processing
            if (sourceComponent instanceof LineString) {
                // Merge SegmentStrings to create the new noded Geometry
                finalComponents[i] = merge(lines.get(0), gf, false);
                // Restore z values from the source geometry where possible
                restoreZ(lines.get(0), (LineString)sourceComponent, gf);
                // Interpolate z values for new nodes
                if (interpolate_z) {
                    interpolate(lines.get(0), (LineString)finalComponents[i], interpolated_z_dp);
                }
            }
            
            // Polygonal component processing
            else if (sourceComponent instanceof Polygon) {
                // Merge SegmentStrings to create the new noded exterior ring
                LinearRing exteriorRing = (LinearRing)merge(lines.get(0), gf, true);
                // Restore z from source geometry where possible
                restoreZ(lines.get(0), ((Polygon)sourceComponent).getExteriorRing(), gf);
                // Interpolate z values for new nodes
                if (interpolate_z) {
                    interpolate(lines.get(0), exteriorRing, interpolated_z_dp);
                }
                // Now process holes the same way
                List<LinearRing> holes = new ArrayList<>();
                for (int j = 0 ; j < lines.size()-1 ; j++) {
                    LinearRing hole = (LinearRing)merge(lines.get(j+1), gf, true);
                    restoreZ(lines.get(j+1), ((Polygon)sourceComponent).getInteriorRingN(j), gf);
                    if (hole.isEmpty()) continue;
                    holes.add(hole);
                    if (interpolate_z) {
                        interpolate(lines.get(j+1), hole, interpolated_z_dp);
                    }
                }
                finalComponents[i] = source.getFactory().createPolygon(exteriorRing, holes.toArray(new LinearRing[holes.size()]));
            }
        }
        return source.getFactory().buildGeometry(Arrays.asList(finalComponents));
    }
    
    /** 
     * Merge a List of connected SegmentStrings and return either a LineString 
     * or a LinearRing.
     */
    private static LineString merge(List<SegmentString> list,
                                    GeometryFactory gf, boolean close) {
        LineMerger lineMerger = new LineMerger();
        for (SegmentString ss : list) {
            if (ss.size()>1) lineMerger.add(gf.createLineString(ss.getCoordinates()));
        }
        Collection lineStrings = lineMerger.getMergedLineStrings();
        // If SegmentStrings are part of a non-simple (auto-intersecting) 
        // LineString, the result of the merge operation will be a set of
        // unordered linestrings.
        // Ordering them with the LineSequencer makes it possible to re-build a
        // single continuous LineString. However, there is no guarantee that
        // the resulting LineString describes the original one in the same order
        LineSequencer sequencer = new LineSequencer();
        sequencer.add(lineStrings);
        LineString ls = gf.createLineString(sequencer.getSequencedLineStrings().getCoordinates()); 
        if (close) {
            CoordinateList coords = new CoordinateList(ls.getCoordinates());
            coords.closeRing();
            if (ls.getNumPoints() < 4) {
                return gf.createLinearRing(new Coordinate[0]);
            }
            return gf.createLinearRing(coords.toCoordinateArray());
        }
        return ls;
    }
    
    /**
     * For SegmentString's end points equal to a source geometry point
     * set the end point z to the source geometry z.
     * Otherwise, set it to NaN (this second operation is important to avoid
     * transferring a z from a feature to the other).
     */
    private static void restoreZ(List<SegmentString> list, LineString g, GeometryFactory gf) {
        Map<Coordinate,Coordinate> map = new HashMap<>();
        for (Coordinate c : g.getCoordinates()) {
            c = (Coordinate)c.clone();
            gf.getPrecisionModel().makePrecise(c);
            map.put(c,c);
        }
        for (SegmentString ss : list) {
            Coordinate[] cc = ss.getCoordinates();
            Coordinate c;
            if (null != (c = map.get(cc[0]))) {
                cc[0].z = c.z;
            }
            else cc[0].z = Double.NaN;
            if (null != (c = map.get(cc[cc.length-1]))) {
                cc[cc.length-1].z = c.z;
            }
            else cc[cc.length-1].z = Double.NaN;
        }
    }
    
    private static void interpolate(List<SegmentString> list, LineString g, int interpolated_z_dp) {
        for (SegmentString ss : list) {
            Coordinate[] cc = ss.getCoordinates();
            if (Double.isNaN(cc[0].z)) {
                cc[0].z = interpolate(cc[0], g, interpolated_z_dp);
            }
            if (Double.isNaN(cc[cc.length-1].z)) {
                cc[cc.length-1].z = interpolate(cc[cc.length-1], g, interpolated_z_dp);
            }
        }
    }
    
    private static double round(double d, int dp) {
        double scale = 1.0;
        for (int i = 0 ; i < dp ; i++) scale *= 10.0;
        return Math.rint(d*scale)/scale;
    }

    private static double[] nextPointWithZ(int index, LineString line) {
        double dist = 0.0;
        Coordinate[] cc = line.getCoordinates();
        for (int i = index+1 ; i < cc.length ; i++) {
            dist += cc[i].distance(cc[i - 1]);
            if (!Double.isNaN(cc[i].z)) return new double[] {cc[i].z, dist};
        }
        if (line.isClosed()) {
            for (int i = 1 ; i < index ; i++) {
                dist += cc[i].distance(cc[i - 1]);
                if (!Double.isNaN(cc[i].z)) return new double[] {cc[i].z, dist};
            }
        }
        return null;
    }

    private static double[] previousPointWithZ(int index, LineString line) {
        double dist = 0.0;
        Coordinate[] cc = line.getCoordinates();
        for (int i = index-1 ; i >= 0 ; i--) {
            dist += cc[i].distance(cc[i + 1]);
            if (!Double.isNaN(cc[i].z)) return new double[] {cc[i].z, dist};
        }
        if (line.isClosed()) {
            for (int i = cc.length-2 ; i > index ; i--) {
                dist += cc[i].distance(cc[i + 1]);
                if (!Double.isNaN(cc[i].z)) return new double[] {cc[i].z, dist};
            }
        }
        return null;
    }

    /**
     * Interpolate the z of coordinate c between coordinates
     * having prev and next indices in the line Coordinate array.
     */
    private static double interpolate(Coordinate c, LineString line, int dp) {
        //int prevIndex = -1;
        int index = -1;
        //int nextIndex = -1;
        for (int i = 0 ; i < line.getNumPoints() ; i++) {
            if (line.getCoordinateN(i).equals(c)) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            double[] previous = previousPointWithZ(index, line);
            double[] next = nextPointWithZ(index, line);
            if (previous != null && next != null) {
                double pZ = previous[0];
                double nZ = next[0];
                double pD = previous[1];
                double nD = next[1];
                double interpolated = pZ + (nZ-pZ)*(pD/(pD+nD));
                return round(interpolated, dp);
            }
        }
        return c.z;
        /*
        Coordinate[] cc = line.getCoordinates();
        Coordinate[][] beforeafter = split(c, line);
        Coordinate[] before = beforeafter[0];
        Coordinate[] after = beforeafter[1];
        if (before.length > 0 && after.length > 0) {
            double distBefore = 0.0;
            double distAfter = 0.0;
            for (int i = 0 ; i < before.length ; i++) {

            }
        }
        for (int i = 0 ; i < cc.length ; i++) {
            if (c.equals(cc[i]) && !Double.isNaN(cc[i].z)) {return cc[i].z;}
            // index of c in line
            else if (index==-1 && c.equals(cc[i])) {index = i;}
            // pass points having NaN values
            else if (Double.isNaN(cc[i].z)) {}
            // if c index has still not be found and current cc[i] is not NaN,
            // current cc[i] is the last previous point
            else if (index==-1) {prevIndex = i;}
            // if cc[i] is neither a previous point neither the current point and it is not NaN
            // set it as the next point and quit
            else {
                nextIndex = i;
                if (prevIndex != -1)
                    break;
            }
            if (prevIndex == -1 && nextIndex != -1 && line instanceof LinearRing) {
                prevIndex = i;
            }
        }
        if (prevIndex > -1 && nextIndex > -1) {
            return round(interpolate(index, prevIndex, nextIndex, cc), dp);
        }
        else {
            return c.z;
        }
        */
    }
    
    /**
     * Interpolate the z of coordinate having indice c between coordinates
     * having prev and next indices in cc coordinate array.
     */
    private static double interpolate(int c, int prev, int next, Coordinate[] cc) {
        double dBefor = 0.0;
        double dAfter = 0.0;
        if (prev > next) {
            for (int i = prev ; i < cc.length-1 ; i++) dBefor += cc[i].distance(cc[i+1]);
            for (int i = 0 ; i < c ; i++) dBefor += cc[i].distance(cc[i+1]);
        }
        else {
            for (int i = prev; i < c; i++) dBefor += cc[i].distance(cc[i + 1]);
        }
        for (int i = c ; i < next ; i++) dAfter += cc[i].distance(cc[i+1]);
        return cc[prev].z + (cc[next].z-cc[prev].z) * (dBefor/(dBefor+dAfter));
    }
    
    /**
     * Creates a hierarchical structure containing all edges
     * derived from Geometry components and linear elements as SegmentStrings.
     */
    public static Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> 
                    getFeature2SegmentStringTreeMap(Collection nodedSubstring) {
        // Cr�ation de geomStructureMap, une table regroupant les SegmentStrings
        // r�sultant du traitement en fonction du feature, du component, 
        // (GeometryCollection) et de l'�l�ment lin�aire d'origine.
        // geomStructureMap     : 1 Feature --> N components
        // componentMap         : 1 component --> N linearElements
        // segmentString List   : 1 linearElement --> N SegmentStringWithRef
        Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap = 
            new HashMap<>();
        for (Object line : nodedSubstring) {
            SegmentString ss = (SegmentString)line;
            SegmentStringData metadata = (SegmentStringData)ss.getData();
            Feature feature = metadata.getFeature();
            int component = metadata.getComponent();
            int linearElement = metadata.getLinearElement();
            // R�cup�rer la table pour ce Feature
            Map<Integer,Map<Integer,List<SegmentString>>> components = geomStructureMap.get(feature);
            // Ce Feature n'a pas encore d'entr�e
            if (components == null) {
                components = new HashMap<>(1);
                Map<Integer,List<SegmentString>> linearElements = new HashMap<>(1);
                List<SegmentString> ssl = new ArrayList<>(2);
                ssl.add(ss);
                linearElements.put(linearElement, ssl);
                components.put(component, linearElements);
                geomStructureMap.put(feature, components);
            }
            else {
                Map<Integer,List<SegmentString>> linearElements = components.get(component);
                if (linearElements == null) {
                    linearElements = new HashMap<>(1);
                    List<SegmentString> ssl = new ArrayList<>(2);
                    ssl.add(ss);
                    linearElements.put(linearElement, ssl);
                    components.put(component, linearElements);
                }
                else {
                    List<SegmentString> ssl = linearElements.get(linearElement);
                    if (ssl == null) {
                        ssl = new ArrayList<>(2);
                    }
                    ssl.add(ss);
                    linearElements.put(linearElement, ssl);
                }
            }
        }
        return geomStructureMap;
    }
        
}
