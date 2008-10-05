/***********************************************
 * created on 		12.06.2006
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 
 * 
 ***********************************************/
package org.openjump.core.graph.delauneySimplexInsert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

/**
 * @author sstein
 * 
 *	Use the class to access the delauney trinagulation by L. Paul Chew
 *	Methods of the class are modified versions from DelaunayPanel.java in DelaunayAp.java.   
 */
public class DTriangulationForJTS {
    private DelaunayTriangulation dt;     // The Delaunay triangulation
    private Simplex initialTriangle;      // The large initial triangle
    private int initialSize = 10000;      // Controls size of initial triangle
    private boolean isVoronoi;            // True iff VoD instead of DT
    private double dx=0;
    private double dy=0;
    private Pnt lowerLeftPnt = null;
    public boolean debug = false;         // True iff printing info for debugging

    
    public DTriangulationForJTS(ArrayList pointList){  
        double argmaxx = 0; double argmaxy = 0;
        double argminx = 0; double argminy = 0;
        int count = 0;
        //-- calc coordinates of initial symplex
        for (Iterator iter = pointList.iterator(); iter.hasNext();) {
            Point pt = (Point) iter.next();
            if (count==0){
                argmaxx = pt.getX(); argminx = pt.getX();
                argmaxy = pt.getY(); argminy = pt.getY();
            }
            else{
                if (pt.getX() < argminx){argminx = pt.getX();}
                if (pt.getX() > argmaxx){argmaxx = pt.getX();}
                if (pt.getY() < argminy){argminy = pt.getY();}
                if (pt.getY() > argmaxy){argmaxy = pt.getY();}                
            }
            count++;
        }
        this.dx=argmaxx-argminx;
        this.dy=argmaxy-argminy;
        //-- the initial simplex must contain all points
        //-- take the bounding box, move the diagonals (sidewards) 
        //	 the meeting point will be the mirrored bbox-center on the top edge  
        this.initialTriangle = new Simplex(new Pnt[] {
    	        				new Pnt(argminx-(1.5*dx), argminy-dy), //lower left
    	        				new Pnt(argmaxx+(1.5*dx), argminy-dy), //lower right
    	        				//new Pnt(argminx+(dx/2.0), argmaxy+(dy/2.0))});	//center, top
        						new Pnt(argminx+(dx/2.0), argmaxy+1.5*dy)});	//center, top
        
        this.lowerLeftPnt = new Pnt(argminx-(1.5*dx), argminy-dy);
        this.dt = new DelaunayTriangulation(initialTriangle);
        this.addPoints(pointList);
    }
 
    /**
     * 
     * @param pointList
     * @param envelope the envelope my extend the initial point cloud and result in a larger initial simplex 
     */
    public DTriangulationForJTS(ArrayList pointList, Envelope envelope){  
        double argmaxx = 0; double argmaxy = 0;
        double argminx = 0; double argminy = 0;
        int count = 0;
        //-- calc coordinates of initial symplex
        for (Iterator iter = pointList.iterator(); iter.hasNext();) {
            Point pt = (Point) iter.next();
            if (count==0){
                argmaxx = pt.getX(); argminx = pt.getX();
                argmaxy = pt.getY(); argminy = pt.getY();
            }
            else{
                if (pt.getX() < argminx){argminx = pt.getX();}
                if (pt.getX() > argmaxx){argmaxx = pt.getX();}
                if (pt.getY() < argminy){argminy = pt.getY();}
                if (pt.getY() > argmaxy){argmaxy = pt.getY();}                
            }
            count++;
        }
        //-- do check also for the delivered envelope
        	if (envelope.getMinX() < argminx){argminx = envelope.getMinX();}
        	if (envelope.getMaxX() > argmaxx){argmaxx = envelope.getMaxX();}
        	if (envelope.getMinY() < argminy){argminy = envelope.getMinY();}
        	if (envelope.getMaxY() > argmaxy){argmaxy = envelope.getMaxY();}                        
        //--
        this.dx=argmaxx-argminx;
        this.dy=argmaxy-argminy;
        //-- the initial simplex must contain all points
        //-- take the bounding box, move the diagonals (sidewards) 
        //	 the meeting point will be the mirrored bbox-center on the top edge  
        this.initialTriangle = new Simplex(new Pnt[] {
    	        				new Pnt(argminx-(1.5*dx), argminy-dy), //lower left
    	        				new Pnt(argmaxx+(1.5*dx), argminy-dy), //lower right
    	        				//new Pnt(argminx+(dx/2.0), argmaxy+(dy/2.0))});	//center, top
        						new Pnt(argminx+(dx/2.0), argmaxy+1.5*dy)});	//center, top
        
        this.lowerLeftPnt = new Pnt(argminx-(1.5*dx), argminy-dy);
        this.dt = new DelaunayTriangulation(initialTriangle);
        this.addPoints(pointList);
    }
    
    public void addPoints(ArrayList pointList){
        for (Iterator iter = pointList.iterator(); iter.hasNext();) {
            try{
                Geometry element = (Geometry) iter.next();
                if (element instanceof Point){
                    Point jtsPt = (Point)element;
	                Pnt point = new Pnt(jtsPt.getX(), jtsPt.getY());
	                dt.delaunayPlace(point);
                }
                else{
                    if (debug) System.out.println("no point geometry");
                }
            }
            catch(Exception e){
                if (debug) System.out.println("no geometry");
            }
        }
    }
    
    
    public void addPoint(double x, double y){
        Pnt point = new Pnt(x, y);
        if (debug) System.out.println("Click " + point);
        dt.delaunayPlace(point);
    }

    /**
     * Draw all the Delaunay edges.
     * @return Arraylist with LineString geometries.
     */
    public ArrayList drawAllDelaunay() {
        // Loop through all the edges of the DT (each is done twice)
        GeometryFactory gf = new GeometryFactory();
        ArrayList lines = new ArrayList();
        for (Iterator it = dt.iterator(); it.hasNext();) {
            Simplex triangle = (Simplex) it.next();
            for (Iterator otherIt = triangle.facets().iterator(); otherIt.hasNext();) {
                Set facet = (Set) otherIt.next();
                Pnt[] endpoint = (Pnt[]) facet.toArray(new Pnt[2]);
                Coordinate[] coords = new Coordinate[2];  
                coords[0] = new Coordinate(endpoint[0].coord(0), endpoint[0].coord(1));
                coords[1] = new Coordinate(endpoint[1].coord(0), endpoint[1].coord(1));
                LineString ls = gf.createLineString(coords);
                lines.add(ls);
            }
        }
        return lines;
    }
    
    /**
     * Draw all the Voronoi edges.
     * @return Arraylist with LineString geometries. 
     */
    public ArrayList drawAllVoronoi () {
        GeometryFactory gf = new GeometryFactory();
        ArrayList lines = new ArrayList();
        // Loop through all the edges of the DT (each is done twice)
        for (Iterator it = dt.iterator(); it.hasNext();) {
            Simplex triangle = (Simplex) it.next();
            for (Iterator otherIt = dt.neighbors(triangle).iterator(); otherIt.hasNext();) {
                Simplex other = (Simplex) otherIt.next();
                Pnt p = Pnt.circumcenter((Pnt[]) triangle.toArray(new Pnt[0]));
                Pnt q = Pnt.circumcenter((Pnt[]) other.toArray(new Pnt[0]));
                Coordinate[] coords = new Coordinate[2];  
                coords[0] = new Coordinate(p.coord(0), p.coord(1));
                coords[1] = new Coordinate(q.coord(0), q.coord(1));
                LineString ls = gf.createLineString(coords);
                lines.add(ls);
            }
        }
        return lines;
    }
    
    /**
     * Draw all the sites (i.e., the input points) of the DT.
     * @return Arraylist with point geometries.      
     */
    public ArrayList drawAllSites () {
        // Loop through all sites of the DT
        // Each is done several times, about 6 times each on average; this
        // can be proved via Euler's formula.
        GeometryFactory gf = new GeometryFactory();
        ArrayList points = new ArrayList();        
        for (Iterator it = dt.iterator(); it.hasNext();) {
            for (Iterator otherIt = ((Simplex) it.next()).iterator(); otherIt.hasNext();){
                Pnt pt = (Pnt) otherIt.next();
                Coordinate coord = new Coordinate(pt.coord(0), pt.coord(1));
                Point jtsPt = gf.createPoint(coord);  
                points.add(jtsPt);
            }
        }
        return points;
    }
    
    /**
     * Draw all the empty circles (one for each triangle) of the DT.
     * @return Arraylist with polygon geometries.   
     */
    public ArrayList drawAllCircles () {
        // Loop through all triangles of the DT
        GeometryFactory gf = new GeometryFactory();
        ArrayList circles = new ArrayList();
        loop: for (Iterator it = dt.iterator(); it.hasNext();) {
            Simplex triangle = (Simplex) it.next();
            for (Iterator otherIt = initialTriangle.iterator(); otherIt.hasNext();) {
                Pnt p = (Pnt) otherIt.next();
                if (triangle.contains(p)) continue loop;
            }
            Pnt c = Pnt.circumcenter((Pnt[]) triangle.toArray(new Pnt[0]));
            double radius = c.subtract((Pnt) triangle.iterator().next()).magnitude();
            Coordinate coord = new Coordinate(c.coord(0), c.coord(1));
            Point jtsPt = gf.createPoint(coord);  
            circles.add(jtsPt.buffer(radius));
        }
    	return circles;
    }
    public DelaunayTriangulation getDelaunayTriangulation() {
        return dt;
    }
    
    /**
     * 
     * @return the corner points of the initial simplex which is divided
     * 		   into smaller simplexes by the iterative insertion of the point dataset
     */
    public ArrayList getInitialSimmplexAsJTSPoints(){
        GeometryFactory gf = new GeometryFactory();
        ArrayList points = new ArrayList();        
        
        for (Iterator otherIt = this.initialTriangle.iterator(); otherIt.hasNext();){
            Pnt pt = (Pnt) otherIt.next();
            Coordinate coord = new Coordinate(pt.coord(0), pt.coord(1));
            Point jtsPt = gf.createPoint(coord);  
            points.add(jtsPt);
        }                   
        return points;        
    }
    
    /**
     * the size of the box has been empirically defined to get "undistorted" 
     * outer thiessen polygons
     * @return a bounding box necesseray to create the final thiessenpolygons
     */
    public Geometry getThiessenBoundingBox(){
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(this.lowerLeftPnt.coord(0)+1*this.dx, this.lowerLeftPnt.coord(1)+0.5*this.dy); //lowerleft
        coords[1] = new Coordinate(this.lowerLeftPnt.coord(0)+3*this.dx, this.lowerLeftPnt.coord(1)+0.5*this.dy); //lowerright
        coords[2] = new Coordinate(this.lowerLeftPnt.coord(0)+3*this.dx, this.lowerLeftPnt.coord(1)+2.5*this.dy); //topright
        coords[3] = new Coordinate(this.lowerLeftPnt.coord(0)+1*this.dx, this.lowerLeftPnt.coord(1)+2.5*this.dy); //topleft
        //-- to close linestring
        coords[4] = new Coordinate(this.lowerLeftPnt.coord(0)+1*this.dx, this.lowerLeftPnt.coord(1)+0.5*dy); //lowerleft        
        LinearRing lr = gf.createLinearRing(coords);
        Geometry bbox = gf.createPolygon(lr, null);
        return bbox;
    }
    
    /**
     * Method returns thiessen polygons within a empirically enlarged 
     * bounding box around the point set. Therefore the voronoi edges are
     * polygonized and the intersecting voronoi polygons with the bounding box
     * are calculated. These intersecting thiessen polygons (in the bounding box) are returned.<p>
     * Note: "thiesen" and "voronoi" is exchangeable. 
     * @return a list of Polygons
     */
    public ArrayList getThiessenPolys(){        
		//-- do union of all edges and use the polygonizer to create polygons from it
        if (debug) System.out.println("get voronoi egdes");
	    ArrayList edges = this.drawAllVoronoi(); 	
        if (debug) System.out.println("merge voronoi egdes to multiLineString");	    
		Geometry mls = (Geometry)edges.get(0);				
		for (int i = 1; i < edges.size(); i++) {
            Geometry line = (Geometry)edges.get(i);
            mls = mls.union(line); 
        }
        if (debug) System.out.println("polygonize");		
		Polygonizer poly = new Polygonizer(); 
		poly.add(mls);
		Collection polys = poly.getPolygons();
		//-- get final polygons in bounding box (=intersection polygons with the bbox)
		Geometry bbox= this.getThiessenBoundingBox();
        if (debug) System.out.println("get intersections and final polys..");		
        ArrayList finalPolys = new ArrayList();
		for (Iterator iter = polys.iterator(); iter.hasNext();) {
            Geometry candPoly = (Geometry) iter.next();
            Geometry intersection = bbox.intersection(candPoly);
            if( intersection != null){
                if (intersection.getArea() > 0){
                    finalPolys.add(intersection);
                }
            }
        }        
        return finalPolys;
    }
    
}
