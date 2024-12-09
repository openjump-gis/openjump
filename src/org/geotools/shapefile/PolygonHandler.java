package org.geotools.shapefile;

import com.vividsolutions.jump.workbench.Logger;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;
import org.locationtech.jts.index.strtree.STRtree;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper for a Shapefile Polygon.
 */
public class PolygonHandler implements ShapeHandler {

    int myShapeType;

    public PolygonHandler() {
        myShapeType = 5;
    }
    
    public PolygonHandler(int type) throws InvalidShapefileException {
        if ((type != 5) && (type != 15) && (type != 25)) {
            throw new InvalidShapefileException("PolygonHandler constructor - expected type to be 5, 15, or 25.");
        }
        myShapeType = type;
    }
    
    public Geometry read(EndianDataInputStream file ,
                         GeometryFactory geometryFactory,
                         int contentLength) throws IOException, InvalidShapefileException {
    
        int actualReadWords = 0; //actual number of 16 bits words read
        Geometry geom;

        int shapeType = file.readIntLE();
        actualReadWords += 2;
        
        if (shapeType == 0) {
             geom = geometryFactory.createMultiPolygon(new Polygon[0]); //null shape
        }
        
        else if ( shapeType != myShapeType ) {
            throw new InvalidShapefileException(
                "PolygonHandler.read() - got shape type " + shapeType + " but was expecting " + myShapeType
            );
        }
        
        else {
            
            //bounds
            file.readDoubleLE();
            file.readDoubleLE();
            file.readDoubleLE();
            file.readDoubleLE();
            actualReadWords += 4*4;
            
            int[] partOffsets;
            
            int numParts = file.readIntLE();
            int numPoints = file.readIntLE();
            actualReadWords += 4;
            
            partOffsets = new int[numParts];
            
            for(int i = 0 ; i<numParts ; i++) {
                partOffsets[i]=file.readIntLE();
                actualReadWords += 2;
            }

            Coordinate[] coords = new Coordinate[numPoints];
            
            for(int t=0 ; t<numPoints ; t++) {
                coords[t]= new Coordinate(file.readDoubleLE(),file.readDoubleLE());
                actualReadWords += 8;
            }
            
            if (myShapeType == 15) {  // PolygonZ
                file.readDoubleLE();  //zmin
                file.readDoubleLE();  //zmax
                actualReadWords += 8;
                 for(int t=0 ; t<numPoints ; t++) {
                    coords[t].z = file.readDoubleLE();
                    actualReadWords += 4;
                }
            }
            
            if (myShapeType >= 15) {      // PolygonM or PolygonZ
                int fullLength;
                if (myShapeType == 15) {  //polyZ (with M)
                    fullLength = 22 + (2*numParts) + (8*numPoints) + 8 + (4*numPoints)+ 8 + (4*numPoints);
                }
                else {                    //polyM (with M)
                    fullLength = 22 + (2*numParts) + (8*numPoints) + 8+ (4*numPoints) ;
                }
                if (contentLength >= fullLength) {
                    file.readDoubleLE();  //mmin
                    file.readDoubleLE();  //mmax
                    actualReadWords += 8;
                    for(int t=0 ; t<numPoints ; t++) {
                         file.readDoubleLE();
                         actualReadWords += 4;
                    }
                }
            }

            // Build rings from coordinates list and parts indices
            List<LinearRing> rings = new ArrayList<>();
            int offset = 0;
            int start, finish, length;
            for (int part = 0; part < numParts; part++) {
                start = partOffsets[part];
                if (part == numParts - 1) {
                    finish = numPoints;
                } else {
                    finish = partOffsets[part + 1];
                }
                length = finish - start;
                Coordinate[] points = new Coordinate[length];
                for (int i = 0; i < length; i++) {
                    points[i] = coords[offset];
                    offset++;
                }
                // Create valid linear rings (and log invalid sequence of coordinates)
                if (points.length != 1) {
                    if ((points.length == 0 || points.length > 3) && points[0].equals(points[points.length - 1])) {
                        LinearRing ring = geometryFactory.createLinearRing(points);
                        rings.add(ring);
                    } else {
                        Logger.warn("Wrong ring for a Polygon: " + Arrays.toString(points));
                    }
                } else {
                    Logger.warn("Wrong ring for a Polygon: " + Arrays.toString(points));
                }
            }

            geom = polygonsFromRings(rings, geometryFactory);

        }
        //verify that we have read everything we need
        while (actualReadWords < contentLength) {
            int junk = file.readShortBE();
            actualReadWords += 1;
        }
        return geom;
    }

    private Geometry polygonsFromRings(List<LinearRing> rings, GeometryFactory geometryFactory) {
      if (rings.isEmpty()) {
        return geometryFactory.createPolygon();
      } else if (rings.size() == 1) {
        return geometryFactory.createPolygon(rings.get(0));
      } else {
        // Add metadata and sort rings by ascending area
        LinearRing[] ringArray = new LinearRing[rings.size()];
        for (int i = 0; i < rings.size(); i++) {
          ringArray[i] = rings.get(i);
          ringArray[i].setUserData(new MD(ringArray[i]));
        }
        Arrays.parallelSort(ringArray, Comparator.comparingDouble(r -> ((MD) r.getUserData()).area));
        for (int i = 0; i < ringArray.length; i++) {
          ((MD) ringArray[i].getUserData()).index = i;
        }

        // Indexing rings
        STRtree index = new STRtree();
        for (LinearRing r : ringArray) {
          index.insert(r.getEnvelopeInternal(), r);
        }
        index.build();

        // Main (and longest) process to determine which ring is a shell and which one is a hole
        // Loop through rings in increasing area order
        // and find the smallest LinearRing entirely enclosing it
        for (LinearRing r : ringArray) {
          List<LinearRing> candidates = index.query(r.getEnvelopeInternal());
          // LinearRings intersecting r, with a larger area and a bbox enclosing r bbox
          candidates = candidates.stream()
              .filter(a -> ((MD) a.getUserData()).index > ((MD) r.getUserData()).index)
              .filter(a -> a.getEnvelopeInternal().contains(r.getEnvelopeInternal()))
              .collect(Collectors.toList());
          // Sort candidates to find the single enclosing one with the smallest area
          candidates.sort(Comparator.comparingDouble(a -> ((MD) a.getUserData()).area));
          for (LinearRing candidate : candidates) {
            if (PointLocation.isInRing(((MD) r.getUserData()).c, candidate.getCoordinates())) {
              ((MD) r.getUserData()).parent = ((MD) candidate.getUserData()).index;
              ((MD) candidate.getUserData()).children.add(((MD) r.getUserData()).index);
              break;
            }
          }
        }

        // Producing polygons
        List<Polygon> polygons = new ArrayList<>();
        for (LinearRing r : ringArray) {
          MD md = (MD) r.getUserData();
          if (isShell(ringArray, md.index)) {
            if (Orientation.isCCW(r.getCoordinates())) {
              Logger.warn("This CCW ring seems to be a shell : " + r);
            }
            List<LinearRing> holes = md.children.stream().map(i -> ringArray[i]).collect(Collectors.toList());
            polygons.add(r.getFactory().createPolygon(r, holes.toArray(new LinearRing[0])));
            ringArray[md.index] = null;
            for (LinearRing h : holes) ringArray[((MD) h.getUserData()).index] = null;
          }
        }
        for (LinearRing r : ringArray) {
          if (r != null) {
            Logger.warn("This ring is undefined, we eep it as a shell : " + r);
            polygons.add(r.getFactory().createPolygon(r));
          }
        }

        return geometryFactory.buildGeometry(polygons);
      }
    }

    /**
    * LinearRing Metadata used during tests
    */
    static class MD {
        int index = -1;
        double area;
        Coordinate c;
        int parent = -1;
        List<Integer>children = new ArrayList<>();
        public MD(LinearRing ring) {
            Polygon p = ring.getFactory().createPolygon(ring);
            this.area = p.getArea();
            this.c = p.getInteriorPoint().getCoordinate();
        }
        public String toString() {
            return index + ": {area=" + this.area + ", parent=" + this.parent + ",c=" + this.c + "}";
        }
    }

    private boolean isShell(LinearRing[] array, int index) {
      MD md = (MD)array[index].getUserData();
      int ancestors = 0;
      if (md != null) {
        while (md.parent != -1) {
          ancestors++;
          md = (MD)array[md.parent].getUserData();
        }
      }
      return ancestors % 2 == 0;
    }
    
//   /**
//    * Finds a object in a list using == instead of equals.
//    * Should be much faster than indexof
//    */
//    private static int findIndex(List<?> list, Object o) {
//        for (int i = 0, n = list.size(); i < n; i++) {
//            if (list.get(i) == o) return i;
//        }
//        return -1;
//    }

    public void write(Geometry geometry, EndianDataOutputStream file) throws IOException{

        if (geometry.isEmpty()) {
            file.writeIntLE(0);
            return;
        }
        
        MultiPolygon multi;
        if(geometry instanceof MultiPolygon) {
            multi = (MultiPolygon)geometry;
        }
        else {
            multi = geometry.getFactory().createMultiPolygon(new Polygon[]{(Polygon)geometry});
        }
        
        file.writeIntLE(getShapeType());
        
        Envelope box = multi.getEnvelopeInternal();
        file.writeDoubleLE(box.getMinX());
        file.writeDoubleLE(box.getMinY());
        file.writeDoubleLE(box.getMaxX());
        file.writeDoubleLE(box.getMaxY());
        
        //need to find the total number of rings and points
        int nrings=0;
        for (int t=0 ; t<multi.getNumGeometries() ; t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }

        int u=0;
        int[] pointsPerRing = new int[nrings];
        for (int t=0 ; t<multi.getNumGeometries() ; t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            pointsPerRing[u] = p.getExteriorRing().getNumPoints();
            u++;
            for(int v=0 ; v<p.getNumInteriorRing() ; v++) {
                pointsPerRing[u]  = p.getInteriorRingN(v).getNumPoints();
                u++;
            }
        }

        int npoints = multi.getNumPoints();

        file.writeIntLE(nrings);
        file.writeIntLE(npoints);

        int count =0;
        for(int t=0 ; t<nrings ; t++) {
            file.writeIntLE(count);
            count = count + pointsPerRing[t] ;
        }

        //write out points here!
        Coordinate[] coords = multi.getCoordinates();
        int num;
        num = Array.getLength(coords);
        for(int t=0 ; t<num ; t++) {
            file.writeDoubleLE(coords[t].x);
            file.writeDoubleLE(coords[t].y);
        }

        if (myShapeType == 15) {  //z
            double[] zExtreame = zMinMax(multi);
            if (Double.isNaN(zExtreame[0] )) {
                file.writeDoubleLE(0.0);
                file.writeDoubleLE(0.0);
            }
            else {
                file.writeDoubleLE(zExtreame[0]);
                file.writeDoubleLE(zExtreame[1]);
            }
            for (int t=0 ; t<npoints ; t++) {
                double z = coords[t].z;
                if (Double.isNaN(z))
                     file.writeDoubleLE(0.0);
                else
                     file.writeDoubleLE(z);
            }
        }

        if (myShapeType >= 15) {  //m
            file.writeDoubleLE(-10E40);
            file.writeDoubleLE(-10E40);
            for(int t=0 ; t<npoints ; t++) {
                file.writeDoubleLE(-10E40);
            }
        }
    }

    public int getShapeType() {
        return myShapeType;
    }

    public int getLength(Geometry geometry) {
        
        if (geometry.isEmpty())     return 2;
        
        MultiPolygon multi;
        if(geometry instanceof MultiPolygon) {
            multi = (MultiPolygon)geometry;
        }
        else {
            multi = geometry.getFactory().createMultiPolygon(new Polygon[]{(Polygon)geometry});
        }
        int nrings=0;
        for (int t=0 ; t<multi.getNumGeometries() ; t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }
        int npoints = multi.getNumPoints();
        if (myShapeType == 15) {
             return 22 + (2*nrings) + 8*npoints + 4*npoints + 8 + 4*npoints + 8;
        }
        if (myShapeType==25) {
            return 22 + (2*nrings) + 8*npoints + 4*npoints + 8 ;
        }
        return 22 + (2*nrings) + 8*npoints;
    }

     double[] zMinMax(Geometry g) {
         
        double zmin = Double.NaN;
        double zmax = Double.NaN;
        boolean validZFound = false;
        Coordinate[] cs = g.getCoordinates();
        double z;

        for (Coordinate c : cs) {
            z= c.z;
            if (!(Double.isNaN( z ))) {
                if (validZFound) {
                    if (z < zmin) zmin = z;
                    if (z > zmax) zmax = z;
                }
                else {
                    validZFound = true;
                    zmin = z ;
                    zmax = z ;
                }
            }
        }
        return new double[]{zmin, zmax};
    }
    
    /**
     * Return a empty geometry.
     */
     public Geometry getEmptyGeometry(GeometryFactory factory) {
         return factory.createMultiPolygon(new Polygon[0]);
     }
     
}

/*
 * $Log$
 *
 * Revision 1.8 2024/11/16 michaudm
 * Improve polygon construction. Speedup may me more than 10x in the case of polygons with
 * a lot of holes (tens of thousands)
 *
 * Revision 1.7  2009/05/10 michaudm
 * Fix a bug in findCWHoles. Could create a 'outer hole' because the test to
 * check if a ring contains another ring was a quick and dirty test.
 *
 * Revision 1.6  2008/04/22 20:55:36  beckerl
 * Restored the original inline code in read() and added the CW hole detection.  The new geotools routines always created Multipolygons.
 *
 * Revision 1.3  2007/01/03 22:43:17  rlittlefield
 * changed so that the holesWithoutShells array initialized to zero length
 *
 * Revision 1.2  2007/01/03 16:48:43  rlittlefield
 * modified code so that holes without shells are not excluded
 *
 * Revision 1.1  2006/11/28 22:30:57  beckerl
 * First SkyJUMP commit.  Prior version numbers lost.
 *
 * Revision 1.1  2006/02/28 22:42:14  ashsdesigner
 * Initial commit of larry's jump/org Eclipse project folder
 *
 * Revision 1.5  2003/09/23 17:15:26  dblasby
 * *** empty log message ***
 *
 * Revision 1.4  2003/07/25 18:49:15  dblasby
 * Allow "extra" data after the content.  Fixes the ICI shapefile bug.
 *
 * Revision 1.3  2003/02/04 02:10:37  jaquino
 * Feature: EditWMSQuery dialog
 *
 * Revision 1.2  2003/01/22 18:31:05  jaquino
 * Enh: Make About Box configurable
 *
 * Revision 1.2  2002/09/09 20:46:22  dblasby
 * Removed LEDatastream refs and replaced with EndianData[in/out]putstream
 *
 * Revision 1.1  2002/08/27 21:04:58  dblasby
 * orginal
 *
 * Revision 1.3  2002/03/05 10:51:01  andyt
 * removed use of factory from write method
 *
 * Revision 1.2  2002/03/05 10:23:59  jmacgill
 * made sure geometries were created using the factory methods
 *
 * Revision 1.1  2002/02/28 00:38:50  jmacgill
 * Renamed files to more intuitve names
 *
 * Revision 1.4  2002/02/13 00:23:53  jmacgill
 * First semi working JTS version of Shapefile code
 *
 * Revision 1.3  2002/02/11 18:44:22  jmacgill
 * replaced geometry constructions with calls to geometryFactory.createX methods
 *
 * Revision 1.2  2002/02/11 18:28:41  jmacgill
 * rewrote to have static read and write methods
 *
 * Revision 1.1  2002/02/11 16:54:43  jmacgill
 * added shapefile code and directories
 *
 */
