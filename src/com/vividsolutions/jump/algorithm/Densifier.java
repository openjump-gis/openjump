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

package com.vividsolutions.jump.algorithm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;


/**
 * Inser vertices every x units along linear components of a Geometry.
 *
 * @author Micha&euml; Michaud
 */
public class Densifier {

    private Geometry geom;
    private GeometryFactory factory;
    private double maxLength = 1.0;

    /**
     * A Densifier to insert vertices every maxLength units along geom.
     *
     * @param geom the geometry to densify
     * @param maxLength the maximum length between two vertices
     */
    public Densifier(Geometry geom, double maxLength) {
        this.geom = geom;
        this.factory = geom.getFactory();
        this.maxLength = maxLength;
    }

    /**
     * Static method to densify a geometry by inserting vertices every
     * maxLength distance along linear components.
     *
     * @param geom the geometry to densify
     * @param maxLength the maximum length between two vertices
     */
    public static Geometry densify(Geometry geom, double maxLength) {
        Densifier densifier = new Densifier(geom, maxLength);
        return densifier.densify();
    }
    
    private Geometry densify() {
        if (geom.isEmpty() || geom.getDimension() == 0) {
            return geom;
        }
        List<Geometry> list = new ArrayList<>();
        for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
            list.add(densify(geom.getGeometryN(i)));
        }
        return factory.buildGeometry(list);
    }
    
    private Geometry densify(Geometry geometry) {
        if (geometry.getDimension() == 1) {
            return densify((LineString)geometry);
        }
        else if (geometry.getDimension() == 2) {
            return densify((Polygon)geometry);
        }
        else {
            return geometry;
        }
    }
    
    private Polygon densify(Polygon polygon) {
        LinearRing exteriorRing = densify((LinearRing)polygon.getExteriorRing());
        LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
        for (int i = 0 ; i < holes.length ; i++) {
            holes[i] = densify((LinearRing)polygon.getInteriorRingN(i));
        }
        return factory.createPolygon(exteriorRing, holes);
    }
    
    private LineString densify(LineString line) {
        CoordinateSequence sequence = line.getCoordinateSequence();
        CoordinateList list = new CoordinateList();
        for (int i = 0 ; i < sequence.size() ; i++) {
            densify(sequence, i, list);
        }
        list.add(sequence.getCoordinate(sequence.size()-1));
        return factory.createLineString(list.toCoordinateArray());
    }
    
    private LinearRing densify(LinearRing line) {
        CoordinateSequence sequence = line.getCoordinateSequence();
        CoordinateList list = new CoordinateList();
        for (int i = 0 ; i < sequence.size() ; i++) {
            densify(sequence, i, list);
        }
        list.add(sequence.getCoordinate(sequence.size()-1));
        return factory.createLinearRing(list.toCoordinateArray());
    }

    private void densify(CoordinateSequence seq, int index, 
                                            CoordinateList coordinateList) {
        if (index == 0) return;
        
        Coordinate p0 = seq.getCoordinate(index - 1);
        Coordinate p1 = seq.getCoordinate(index);
        
        double dx = (p1.x - p0.x);
        double dy = (p1.y - p0.y);
        double dz = (p1.z - p0.z);
        double frac = Math.sqrt(dx*dx+dy*dy)/maxLength;
        dx = dx/frac;
        dy = dy/frac;
        dz = dz/frac;
        // *0.9999 to avoid to add a point too close to next point
        int nbSegments = (int)(frac+0.9999);
        
        for (int i = 0; i < nbSegments; i++) {
            double x = p0.x + i*dx;
            double y = p0.y + i*dy;
            double z = p0.z + i*dz;
            Coordinate pt = new Coordinate(x, y, z);
            coordinateList.add(pt);
        } 
    }
  
}
