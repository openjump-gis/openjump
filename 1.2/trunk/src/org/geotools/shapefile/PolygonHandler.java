package org.geotools.shapefile;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.algorithm.RobustCGAlgorithms;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * Wrapper for a Shapefile polygon.
 */
public class PolygonHandler implements ShapeHandler{
    protected static CGAlgorithms cga = new RobustCGAlgorithms();
    int myShapeType;
    
    public PolygonHandler()
    {
        myShapeType = 5;
    }
    
      public PolygonHandler(int type) throws InvalidShapefileException
        {
            if  ( (type != 5) &&  (type != 15) &&  (type != 25) )
                throw new InvalidShapefileException("PolygonHandler constructor - expected type to be 5, 15, or 25.");
            
            myShapeType = type;
    }
    
    //returns true if testPoint is a point in the pointList list.
    boolean pointInList(Coordinate testPoint, Coordinate[] pointList)
    {
        int t, numpoints;
        Coordinate  p;
        
        numpoints = Array.getLength( pointList) ;
        for (t=0;t<numpoints; t++)
        {
            p = pointList[t];
            if ( (testPoint.x == p.x) && (testPoint.y == p.y) &&
                    ((testPoint.z == p.z) || (!(testPoint.z == testPoint.z))  )  //nan test; x!=x iff x is nan
                    )
            {
                return true;
            }
        }
        return false;
    }
    
    public Geometry read( EndianDataInputStream file , GeometryFactory geometryFactory, int contentLength)
    throws IOException, InvalidShapefileException
    {
    
    	int actualReadWords = 0; //actual number of words read (word = 16bits)
        
       // file.setLittleEndianMode(true);
        int shapeType = file.readIntLE();	
		actualReadWords += 2;
        
         if (shapeType ==0)
        {
             return new MultiPolygon(null,new PrecisionModel(),0); //null shape
        }
        
        if ( shapeType != myShapeType ) {
            throw new InvalidShapefileException
            ("PolygonHandler.read() - got shape type "+shapeType+" but was expecting "+myShapeType);
        }
        
        //bounds
        file.readDoubleLE();
        file.readDoubleLE();
        file.readDoubleLE();
        file.readDoubleLE();
        
		actualReadWords += 4*4;
 
        
        int partOffsets[];
        
        int numParts = file.readIntLE();
        int numPoints = file.readIntLE();       
		actualReadWords += 4;
        
        partOffsets = new int[numParts];
        
        for(int i = 0;i<numParts;i++){
            partOffsets[i]=file.readIntLE();
			actualReadWords += 2;
        }
        
        //LinearRing[] rings = new LinearRing[numParts];
        ArrayList shells = new ArrayList();
        ArrayList holes = new ArrayList();
        Coordinate[] coords = new Coordinate[numPoints];
        
        for(int t=0;t<numPoints;t++)
        {
            coords[t]= new Coordinate(file.readDoubleLE(),file.readDoubleLE());
			actualReadWords += 8;
        }
        
        if (myShapeType == 15)
        {
                //z
            file.readDoubleLE();  //zmin
            file.readDoubleLE();  //zmax
			actualReadWords += 8;
             for(int t=0;t<numPoints;t++)
            {
                coords[t].z = file.readDoubleLE();
				actualReadWords += 4;
            }
        }
      
        if (myShapeType >= 15)
        {
          //  int fullLength = 22 + (2*numParts) + (8*numPoints) + 8 + (4*numPoints)+ 8 + (4*numPoints);
          int fullLength;
          if (myShapeType == 15)
          {
          		//polyZ (with M)
			    fullLength = 22 + (2*numParts) + (8*numPoints) + 8 + (4*numPoints)+ 8 + (4*numPoints);         	
          }
          else
          {
          		//polyM (with M)
				fullLength = 22 + (2*numParts) + (8*numPoints) + 8+ (4*numPoints) ;
          }
            if (contentLength >= fullLength)
            {
                    file.readDoubleLE();  //mmin
                    file.readDoubleLE();  //mmax
					actualReadWords += 8;
                    for(int t=0;t<numPoints;t++)
                    {
                         file.readDoubleLE();
					     actualReadWords += 4;
                    }
            }            
        }
        
        
	//verify that we have read everything we need
	while (actualReadWords < contentLength)
	{
		  int junk = file.readShortBE();	
		 actualReadWords += 1;
	}
	
        
        int offset = 0;
        int start,finish,length;
        for(int part=0;part<numParts;part++){
            start = partOffsets[part];
            if(part == numParts-1){finish = numPoints;}
            else {
                finish=partOffsets[part+1];
            }
            length = finish-start;
            Coordinate points[] = new Coordinate[length];
            for(int i=0;i<length;i++){
                points[i]=coords[offset];
                offset++;
            }
    		//REVISIT: polyons with only 1 or 2 points are not polygons - geometryFactory will bomb so we skip if we find one.
            if(points.length == 0 || points.length > 3){ 
            	LinearRing ring = geometryFactory.createLinearRing(points);
            	if(CGAlgorithms.isCCW(points)){
            		holes.add(ring);
            	}
            	else{
            		shells.add(ring);
            	}
            }
    	}
        
        if ((shells.size()>1) && (holes.size()== 0)) {
        	//some shells may be CW holes - esri tolerates this
        	holes = findCWHoles(shells);  //find all rings contained in others
        	if (holes.size() > 0) {
        		shells.removeAll(holes);   
        		ArrayList ccwHoles = new ArrayList(holes.size());
        		for (int i=0; i<holes.size(); i++) {
        			ccwHoles.add( reverseRing((LinearRing) holes.get(i)) );
        		}
        		holes = ccwHoles;
        	}
        }
        
   //now we have a list of all shells and all holes
        ArrayList holesForShells = new ArrayList(shells.size());
        ArrayList holesWithoutShells = new ArrayList();
        
        for(int i=0;i<shells.size();i++){
            holesForShells.add(new ArrayList());
        }
        
        //find homes
        for(int i=0;i<holes.size();i++){
            LinearRing testRing = (LinearRing)holes.get(i);
            LinearRing minShell = null;
            Envelope minEnv = null;
            Envelope testEnv = testRing.getEnvelopeInternal();
            Coordinate testPt = testRing.getCoordinateN(0);
            LinearRing tryRing;
            for(int j=0;j<shells.size();j++){
                tryRing = (LinearRing) shells.get(j);
                Envelope tryEnv = tryRing.getEnvelopeInternal();
                if (minShell != null) minEnv = minShell.getEnvelopeInternal();
                boolean isContained = false;
                Coordinate[] coordList = tryRing.getCoordinates() ;
                
                if (tryEnv.contains(testEnv)
                        && (cga.isPointInRing(testPt,coordList ) || (pointInList(testPt, coordList)))
                   )
                    isContained = true;
                // check if this new containing ring is smaller than the current minimum ring
                if (isContained) {
                    if (minShell == null
                    || minEnv.contains(tryEnv)) {
                        minShell = tryRing;
                    }
                }
            }
            
            if (minShell == null)
            {
            	holesWithoutShells.add(testRing);
                //System.out.println("polygon found with a hole thats not inside a shell");
            }
            else
            {
              ((ArrayList)holesForShells.get(shells.indexOf(minShell))).add(testRing);
            }
        }
        
        Polygon[] polygons = new Polygon[shells.size() + holesWithoutShells.size()];
        for(int i=0;i<shells.size();i++){
            polygons[i]=geometryFactory.createPolygon((LinearRing)shells.get(i),(LinearRing[])((ArrayList)holesForShells.get(i)).toArray(new LinearRing[0]));
        }
        
        for (int i=0;i<holesWithoutShells.size();i++)
        {
            polygons[shells.size() + i]=geometryFactory.createPolygon((LinearRing)holesWithoutShells.get(i), null);        	
        }
        
        if(polygons.length==1){
            return polygons[0];
        }
        
        holesForShells = null;
        holesWithoutShells = null;
        shells = null;
        holes = null;
        //its a multi part
        

        Geometry result =  geometryFactory.createMultiPolygon(polygons);
     //   if (!(result.isValid()  ))
     //   	System.out.println("geom isnt valid");
        return result;        
    }
    
    ArrayList findCWHoles(ArrayList shells) {
        ArrayList holesCW = new ArrayList(shells.size());
        for (int i = 0; i < shells.size(); i++) {
    		LinearRing iRing = (LinearRing) shells.get(i);
    		Envelope iEnv = iRing.getEnvelopeInternal();
			Coordinate[] coordList = iRing.getCoordinates();
    		LinearRing jRing;
    		for (int j = 0; j < shells.size(); j++) {
    			if (i == j) continue;
    			jRing = (LinearRing) shells.get(j);               
    			Envelope jEnv = jRing.getEnvelopeInternal();                
        		Coordinate jPt = jRing.getCoordinateN(0);
        		Coordinate jPt2 = jRing.getCoordinateN(1);
				if ( iEnv.contains(jEnv) 
						&& (CGAlgorithms.isPointInRing(jPt, coordList) || pointInList(jPt, coordList))
						&& (CGAlgorithms.isPointInRing(jPt2, coordList) || pointInList(jPt2, coordList))) {
					if (!holesCW.contains(jRing)) holesCW.add(jRing);
				}
    		}
    	}
    	return holesCW;
    }
        
    /**
     *  reverses the order of points in lr (is CW -> CCW or CCW->CW)
     */
    LinearRing reverseRing(LinearRing lr) {
        int numPoints = lr.getNumPoints();
        Coordinate[] newCoords = new Coordinate[numPoints];

        for (int t = 0; t < numPoints; t++) {
            newCoords[t] = lr.getCoordinateN(numPoints - t - 1);
        }

        return new LinearRing(newCoords, new PrecisionModel(), 0);
    }

  
     public void write(Geometry geometry,EndianDataOutputStream file)throws IOException{
         

         
         MultiPolygon multi;
        if(geometry instanceof MultiPolygon){
            multi = (MultiPolygon)geometry;
        }
        else{
            multi = new MultiPolygon(new Polygon[]{(Polygon)geometry},geometry.getPrecisionModel(),geometry.getSRID());
        }
        //file.setLittleEndianMode(true);
        file.writeIntLE(getShapeType());
        
        Envelope box = multi.getEnvelopeInternal();
        file.writeDoubleLE(box.getMinX());
        file.writeDoubleLE(box.getMinY());
        file.writeDoubleLE(box.getMaxX());
        file.writeDoubleLE(box.getMaxY());
        
        //need to find the total number of rings and points
        int nrings=0;
        
        for (int t=0;t<multi.getNumGeometries();t++)
        {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }
        
        int u=0;
        int[] pointsPerRing = new int[nrings];
        for (int t=0;t<multi.getNumGeometries();t++)
        {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            pointsPerRing[u] = p.getExteriorRing().getNumPoints();
            u++;
            for(int v=0;v<p.getNumInteriorRing();v++)
            {
                pointsPerRing[u]  = p.getInteriorRingN(v).getNumPoints();
                u++;
            }
        }       
        
       
        int npoints = multi.getNumPoints();
        
        file.writeIntLE(nrings);
        file.writeIntLE(npoints);
        
        int count =0;
        for(int t=0;t<nrings;t++)
        {
            file.writeIntLE(count);
            count = count + pointsPerRing[t] ;
        }
        
        //write out points here!
        Coordinate[] coords = multi.getCoordinates();
        int num;
        num = Array.getLength(coords);
        for(int t=0;t<num;t++)
        {
            file.writeDoubleLE(coords[t].x);
            file.writeDoubleLE(coords[t].y);
        }       
        
        if (myShapeType == 15)
        {
                //z
            double[] zExtreame = zMinMax(multi);
            if (Double.isNaN(zExtreame[0] ))
            {
                file.writeDoubleLE(0.0);
                file.writeDoubleLE(0.0);
            }
            else
            {
                file.writeDoubleLE(zExtreame[0]);
                file.writeDoubleLE(zExtreame[1]);
            }
            for (int t=0;t<npoints; t++)
            {
                double z = coords[t].z;
                if (Double.isNaN(z))
                     file.writeDoubleLE(0.0);
                else
                     file.writeDoubleLE(z);
            }
        }
        
        if (myShapeType >=15)
        {
                //m
            file.writeDoubleLE(-10E40);
            file.writeDoubleLE(-10E40);
            for(int t=0;t<npoints;t++)
            {
                   file.writeDoubleLE(-10E40);
            }
        }
    }
    
    
    public int getShapeType(){
        return myShapeType;
    }
    public int getLength(Geometry geometry){
        
           MultiPolygon multi;
        if(geometry instanceof MultiPolygon){
            multi = (MultiPolygon)geometry;
        }
        else{
            multi = new MultiPolygon(new Polygon[]{(Polygon)geometry},geometry.getPrecisionModel(),geometry.getSRID());
        }
        
         int nrings=0;
        
        for (int t=0;t<multi.getNumGeometries();t++)
        {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }
         
         int npoints = multi.getNumPoints();
         
         if (myShapeType == 15)
         {
             return 22+(2*nrings)+8*npoints + 4*npoints+8 +4*npoints+8;
         }
         if (myShapeType==25)
         {
             return 22+(2*nrings)+8*npoints + 4*npoints+8 ;
         }
         
         
         return 22+(2*nrings)+8*npoints;
    }
    
    
      double[] zMinMax(Geometry g)
    {
        double zmin,zmax;
        boolean validZFound = false;
        Coordinate[] cs = g.getCoordinates();
        double[] result = new double[2];
        
        zmin = Double.NaN;
        zmax = Double.NaN;
        double z;
        
        for (int t=0;t<cs.length; t++)
        {
            z= cs[t].z ;
            if (!(Double.isNaN( z ) ))
            {
                if (validZFound)
                {
                    if (z < zmin)
                        zmin = z;
                    if (z > zmax)
                        zmax = z;
                }
                else
                {
                    validZFound = true;
                    zmin =  z ;
                    zmax =  z ;
                }
            }
           
        }
        
        result[0] = (zmin);
        result[1] = (zmax);
        return result;
        
    }
    
    
}

/*
 * $Log$
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
