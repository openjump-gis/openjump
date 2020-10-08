package org.openjump.core.rasterimage.algorithms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridCell;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridRasterWrapper;
import org.openjump.core.ui.util.LayerableUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

 
/**
 * 
 * @author Giuseppe Aruta
 * a class to port two methods to rasterize a FeatureCollection
 * - from Sextante (https://joinup.ec.europa.eu/solution/sextante/about)
 * - from AdbToolbox (http://www.pcn.minambiente.it/mattm/adb-toolbox/)
 */
 


public class RasterizeAlgorithm {

	private static Double noData = -99999.0D;
	private static Double cellSize;
	private static double dValue;
	private static int m_iNX;
	private static int m_iNY;
	private static GridExtent m_Extent;
    private static WritableRaster raster;
    private static  FeatureCollection featureCollection;
    private static String attrName;
    private static double[][] values;
    private static Envelope env;
     
    public RasterizeAlgorithm(Envelope limitEnvelope, FeatureCollection fCollection, 
    		    		String attributeName, double CellSize) throws OutOfMemoryError, Exception  {
    	   featureCollection = fCollection; 
    	   env=limitEnvelope;
		   attrName=attributeName;
    	   cellSize=CellSize;
		   m_Extent= new GridExtent();
	       m_Extent.setCellSize(CellSize, CellSize);
	       double minX = limitEnvelope.getMinX();
	       double minY = limitEnvelope.getMinY();
	       double maxX = limitEnvelope.getMaxX();
	       double maxY = limitEnvelope.getMaxY();
		   m_Extent.setXRange(minX, maxX);
		   m_Extent.setYRange(minY, maxY);
		   m_iNX = m_Extent.getNX();
		   m_iNY = m_Extent.getNY(); 
	       values= new double[m_iNX][m_iNY];  
		   for (int x = 0; x < m_iNX; x++){
	 			for (int y = 0; y < m_iNY; y++){
	 				values[x][y]=noData;
           }
	 	  } 
      }
    
    
    //[Giuseppe Aruta 2020-10-4] used to methods to rasterize:
    // a) From AdbToolbox. Quite efficient with different types of geometries but slow
    // b) From Sextante, almost faster but still not working with ponts and linestrings
    // The code below simplefies the access to the the method
    /**
     * Convert a feature collection to a raster according to a numeric attribute, a limit envelope
     * and a cell size.  Feature are first selected by the envelope and merged according 
     * the chosen  attribute, then converted to a grid
     */
    public void process() throws OutOfMemoryError, Exception {
     
    	 FeatureCollection fc2 = getFeaturesOverlappingEnvelope();
	     FeatureCollection fc3 =  unionByAttributeValue(fc2);
    	 if (!LayerableUtil.isPolygonalLayer(fc3)) {
  	       RasterizeAlgorithm.RasterizeAdbToolbox(fc3);
  	      } else   {
  	    	   RasterizeAlgorithm.RasterizeSextante(fc3);  
  	      } 
    	 
    }
    
    /**
     * Method to save results to a TIFF file
     * @param file
     * @throws IOException
     */
    
    public void saveToFile(File file) throws IOException {
    	Envelope    m_Envelope = new Envelope(m_Extent.getXMin(),  m_Extent.getXMax(),m_Extent.getYMin(),  m_Extent.getYMax() );
 	    RasterImageIO rasterImageIO = new RasterImageIO();
 	    rasterImageIO.writeImage(file, raster, m_Envelope,
 	          rasterImageIO.new CellSizeXY(cellSize, cellSize), noData); 
    }
    
    /**
     * Rasterize a FeatureCollection using AdBToolboxframework
     * @throws Exception
     */
	   
	  
     private static  void RasterizeAdbToolbox(FeatureCollection fCollection) throws OutOfMemoryError, Exception  {
		   final Iterator<?> iter = fCollection.iterator();
           while (iter.hasNext()) {
            Feature feature = (Feature) iter.next();
            try {
	            dValue = Double.parseDouble(feature.getAttribute(attrName).toString());
	           } catch (Exception e) {
	        	 dValue = noData;
	           }
		    double[][] rasterized = new double[m_iNX][m_iNY];
 		    rasterized	= rasterize(feature.getGeometry());
			for (int r = 1; r < m_iNX; r++) {
				for (int c = 1; c < m_iNY; c++) {
					if (rasterized[r][c] == 1) {
						values[r][c] = dValue;
					}
				}
			  }
			}
		raster =  GridRasterWrapper.matrixToRaster(values); 
	   }
	   
     
     /**
      * Converts World coordinates to grid coordinates
      * @param Coordinate coordinate
      * @param double CellSize
      * @return
      */
     
	   private static java.awt.Point fromCoordToCell(Coordinate coordinate, double CellSize){
	        int x = (int)Math.floor((coordinate.x  - m_Extent.getXMin()) / CellSize);
	       // int y = (int)Math.floor((m_Extent.getYMax()-coord.y) / CellSize);
	        int y = (int)Math.floor((coordinate.y-m_Extent.getYMin()) / CellSize);
	        return new java.awt.Point(x, y);
	        
	    }
	   
	  
	   
	   /**
	    * Method Rasterize a FeatureCollection using Sextante framework
	    * @throws Exception 
	    * @throws OutOfMemoryError 
	    */
	   private static void RasterizeSextante(FeatureCollection fCollection ) throws OutOfMemoryError, Exception  {
		   
		  raster = GridRasterWrapper.matrixToRaster(values);
	 	  final Coordinate[] coords = new Coordinate[5];
	      coords[0] = new Coordinate(m_Extent.getXMin(), m_Extent.getYMin());
	      coords[1] = new Coordinate(m_Extent.getXMin(), m_Extent.getYMax());
	      coords[2] = new Coordinate(m_Extent.getXMax(), m_Extent.getYMax());
	      coords[3] = new Coordinate(m_Extent.getXMax(), m_Extent.getYMin());
	      coords[4] = new Coordinate(m_Extent.getXMin(), m_Extent.getYMin());
	      final GeometryFactory gf = new GeometryFactory();
	      final LinearRing ring = gf.createLinearRing(coords); 
	      final Polygon extent =   gf.createPolygon(ring, null);
	      List<Feature> inputC = fCollection.getFeatures();
	      FeatureSchema schema = fCollection.getFeatureSchema();
	      FeatureDataset inputFC = new FeatureDataset(inputC, schema);
	      for (Iterator<Feature> it = inputFC.iterator() ; it.hasNext() ; ) {
	            Feature f = it.next();
	            try {
	            dValue = Double.parseDouble(f.getAttribute(attrName).toString());
	           } catch (Exception e) {
	        	 dValue = noData;
	           }
	         final Geometry geometry = f.getGeometry();
	         if (geometry.intersects(extent)) { 
	        	doGeometry(geometry);
	         }
	     }
	   }

	 /**
	  * Gets java.awt.image.WritableRaster
	  * @return java.awt.image.WritableRaster
	  */
	   public WritableRaster getRaster() {
		   return raster;
	   }
	   
	   
	  
	   
	   
	  private static void doPolygon(final Geometry geom) {
          final GeometryFactory gf = new GeometryFactory();
	      for (int i = 0; i < geom.getNumGeometries(); i++) {
	         final Polygon poly = (Polygon) geom.getGeometryN(i);
	         LinearRing lr = gf.createLinearRing(poly.getExteriorRing().getCoordinates());
	         Polygon part = gf.createPolygon(lr, null);
	         doPolygonPart(part,  false);
	         for (int j = 0; j < poly.getNumInteriorRing(); j++) {
	            lr = gf.createLinearRing(poly.getInteriorRingN(j).getCoordinates());
	            part = gf.createPolygon(lr, null);
	            doPolygonPart(part, true);
	         }
	      }
       }


	   private static void doPolygonPart(final Polygon geom,
	                              final boolean bIsHole) {
		  boolean bFill;
	      boolean bCrossing[];
	      int x, y, ix, xStart, xStop, iPoint;
	      double yPos;;
	      Coordinate pLeft, pRight, pa, pb;
	      final Coordinate p = new Coordinate();
	      bCrossing = new boolean[m_iNX];
          final Envelope extent = geom.getEnvelopeInternal();
	      xStart = (int) ((extent.getMinX()- m_Extent.getXMin()) / m_Extent.getCellSize().x) - 1;
	      if (xStart < 0) {
	         xStart = 0;
	      }
          xStop = (int) ((extent.getMaxX()- m_Extent.getXMin()) / m_Extent.getCellSize().x) + 1;
	      if (xStop >= m_iNX) {
	         xStop = m_iNX - 1;
	      }
         final Coordinate[] points = geom.getCoordinates();
         for (y = 0, yPos = m_Extent.getYMax(); y < m_iNY; y++, yPos -= m_Extent.getCellSize().y
	    		  ) {
	         if ((yPos >= extent.getMinY()) && (yPos <= extent.getMaxY())) {
	            Arrays.fill(bCrossing, false);
	            pLeft = new Coordinate(m_Extent.getXMin() - 1.0, yPos);
	            pRight = new Coordinate(m_Extent.getXMax() + 1.0, yPos);
                pb = points[points.length - 1];
	            for (iPoint = 0; iPoint < points.length; iPoint++) {
	               pa = pb;
	               pb = points[iPoint];
	               if ((((pa.y <= yPos) && (yPos < pb.y)) || ((pa.y > yPos) && (yPos >= pb.y)))) {
	                  getCrossing(p, pa, pb, pLeft, pRight);
	                  ix = (int) ((p.x - m_Extent.getXMin()) / m_Extent.getCellSize().x + 1.0);
	                  if (ix < 0) {
	                     ix = 0;
	                  }
	                  else if (ix >= m_iNX) {
	                     ix = m_iNX - 1;
	                  }
	                  bCrossing[ix] = !bCrossing[ix];
	               }
	            }
	            for (x = xStart, bFill = false; x <= xStop; x++) {
	               if (bCrossing[x]) {
	                  bFill = !bFill;
	               }
	               if (bFill) {
	                  final double dPrevValue =raster.getSampleDouble(x, y,0);// sRasterLayer.getCellValueAsDouble(x, y);
	                  if (bIsHole) {
	                     if (dPrevValue == dValue) {
	                    	 raster.setSample(x, y, 0, noData);
	                     }
	                  }
	                  else {
	                     if (dPrevValue == noData) {
	                    	 raster.setSample(x, y, 0, dValue);
	                     }
	                  }
	               }
	            }
	         }
	      }
	   }

   
	   private static void doLine(final Geometry geom) {
                for (int i = 0; i < geom.getNumGeometries(); i++) {
                final Geometry part = geom.getGeometryN(i);
                doLineString(part);
               }
             }

	   private static void doLineString(final Geometry geom) {
            int i;
            double x, y, x2, y2;
            final Coordinate[] coords = geom.getCoordinates();
            for (i = 0; i < coords.length - 1; i++) {
            	x = coords[i].x;
            	y = coords[i].y;
            	x2 = coords[i + 1].x;
            	y2 = coords[i + 1].y;
            	writeSegment(x, y, x2, y2);
            }
	   }

	   private static void writeSegment(double x,
                     double y,
                     final double x2,
                     final double y2) {
		   double dx, dy, d, n;
		   GridCell cell;
		   dx = Math.abs(x2 - x);
		   dy = Math.abs(y2 - y);
		   if ((dx > 0.0) || (dy > 0.0)) {
			   if (dx > dy) {
				   dx /= cellSize;
				   n = dx;
				   dy /= dx;
				   dx = cellSize;
			   }
			   else {
				   dy /= cellSize;
				   n = dy;
				   dx /= dy;
				   dy = cellSize;
			   }
			   if (x2 < x) {
				   dx = -dx;
			   }
			   if (y2 < y) {
				   dy = -dy;
			   }
			   for (d = 0.0; d <= n; d++, x += dx, y += dy) {
				   if (m_Extent.contains(x, y)) {
					  //   final GridCell cell = m_Extent.getGridCoordsFromWorldCoords(coord.x, coord.y);
					  // java.awt.Point cell1 = fromCoordToCell(new Coordinate(x,y), cellSize, m_Extent);
					   cell = m_Extent.getGridCoordsFromWorldCoords(x, y);
					   //System.out.println(cell.getX() + " " + cell.getY());
				   raster.setSample(cell.getX(), cell.getY(), 0,dValue);
				   }
			   }
		   }
	   }


	   private static void doPoint(final Geometry geometry) {
		   final Coordinate coord = geometry.getCoordinate();
	    // final GridCell cell = m_Extent.getGridCoordsFromWorldCoords(coord.x, coord.y);
		    java.awt.Point cell = fromCoordToCell(coord, cellSize);
		   raster.setSample(cell.x, cell.y, 0,dValue);
	   }
 

	   private static boolean getCrossing(final Coordinate crossing,
	                               final Coordinate a1,
	                               final Coordinate a2,
	                               final Coordinate b1,
	                               final Coordinate b2) {
	      double lambda, div, a_dx, a_dy, b_dx, b_dy;
	      a_dx = a2.x - a1.x;
	      a_dy = a2.y - a1.y;
	      b_dx = b2.x - b1.x;
	      b_dy = b2.y - b1.y;
          if ((div = a_dx * b_dy - b_dx * a_dy) != 0.0) {
	         lambda = ((b1.x - a1.x) * b_dy - b_dx * (b1.y - a1.y)) / div;
	         crossing.x = a1.x + lambda * a_dx;
	         crossing.y = a1.y + lambda * a_dy;
	         return true;
          }
          return false;
	   }
	
	   
	   private static void doGeometry (Geometry geometry) {
			 if (geometry.getGeometryType().equals("Point") || geometry.getGeometryType().equals("MultiPoint")) {
	         	  doPoint(geometry);
	         } else if (geometry.getGeometryType().equals("LineString") || geometry.getGeometryType().equals("MultiLineString")) {
	         	  doLine(geometry);
	         } else if (geometry.getGeometryType().equals("Polygon") || geometry.getGeometryType().equals("MultiPolygon")){
	         	doPolygon(geometry);
	         }	else if (geometry instanceof GeometryCollection) {
	         	  for (int j = 0; j < geometry.getNumGeometries(); j++) {
	         		  Geometry geometry2 = geometry.getGeometryN(j);
	         		  if (geometry2.getGeometryType().equals("Point") || geometry2.getGeometryType().equals("MultiPoint")) {
	 	            	  doPoint(geometry2);
	 	            } else if (geometry2.getGeometryType().equals("LineString") || geometry2.getGeometryType().equals("MultiLineString")) {
	 	            	  doLine(geometry2);
	 	            } else if (geometry2.getGeometryType().equals("Polygon") || geometry2.getGeometryType().equals("MultiPolygon")){
	 	            	doPolygon(geometry2);
	 	            	}
	         		  }
	         	  }
	 	      }
	 
	
	 
	 private static FeatureCollection unionByAttributeValue(FeatureCollection featureCollection) throws Exception {
		  FeatureDataset outputFC = new FeatureDataset(featureCollection.getFeatureSchema());
	        
		    Map<Object, FeatureCollection> map = new HashMap<Object, FeatureCollection>();
		    Iterator<Feature> itFeat= featureCollection.getFeatures().iterator();
		     while (itFeat.hasNext()) {
		    	 Feature feature = itFeat.next();
		    	 Object key = feature.getAttribute(attrName);
	             if (!map.containsKey(key)) {
	                 FeatureCollection fd = new FeatureDataset(featureCollection.getFeatureSchema());
	                 fd.add(feature);
	                 map.put(key, fd);
	             }  else {
	                 map.get(key).add(feature);
	             } 
		     }
	
	        Iterator<Object>  iter = map.keySet().iterator();
	        while (iter.hasNext()) {
	        	   Object key = iter.next();
	                FeatureCollection fca = map.get(key);
	                if (fca.size() > 0) {
	                  Feature feature = union(fca);
	                  feature.setAttribute(attrName, key);
	                  outputFC.add(feature);
	                //  tree_map.put(feature.getID(), key.toString());
	                }
	        }
	        
	      
	        return  outputFC;
	     }
	 

	     private static Feature union(FeatureCollection fc) {
	    	 GeometryFactory factory = new  GeometryFactory();
	         Collection<Geometry> geometries  = new ArrayList<Geometry>();
	         for (Feature f :  fc.getFeatures()) {
	             Geometry g = f.getGeometry();
	             geometries.add(g);
	         }
	         Geometry unioned = UnaryUnionOp.union(geometries);
	          FeatureSchema schema = fc.getFeatureSchema();
	         Feature feature = new BasicFeature(schema);
	         if (geometries.size()==0) {
	             feature.setGeometry(factory.createGeometryCollection(new Geometry[]{}));
	         }
	         else {
	             feature.setGeometry(unioned);
	         }
	         return feature;
	     }
	    
	      
	     
  private static FeatureCollection getFeaturesOverlappingEnvelope() throws Exception {
	  		Collection<Feature> inputC = featureCollection.getFeatures();
 	        FeatureSchema schema1 = featureCollection.getFeatureSchema();
	        FeatureDataset inputFC = new FeatureDataset(inputC, schema1);  	 
	         FeatureSchema schema = new FeatureSchema();
	    	 schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
	    	 schema.addAttribute(attrName, AttributeType.DOUBLE);
	    	 FeatureDataset outputFC = new FeatureDataset(schema);
		     GeometryFactory factory = new GeometryFactory();
	    	 Geometry geom = factory.toGeometry(env);
	    	 for (Feature f : inputFC.getFeatures()) {
	             Geometry g = f.getGeometry();
	             if (!geom.disjoint(g)){
	           outputFC.add(f);}
	         }
	 	 return outputFC;
	     }
 
  
  
  private static double[][] rasterize(Geometry geom)
          throws Exception, OutOfMemoryError {
     double[][] grid = new double[m_iNX+2][m_iNY+2];
      for(int row = 0; row <= m_iNY; row++){
          for(int col = 0; col <= m_iNX; col++){
              grid[col][row] = 0;
          }
      }
      BufferedImage bimage = new BufferedImage(m_iNX, m_iNY, BufferedImage.TYPE_INT_ARGB);
      bimage.setAccelerationPriority(1.0f);
      Graphics2D graphics = bimage.createGraphics();
      Coordinate[] coord;
      int[] coordGridX;
      int[] coordGridY;
      Color color = new Color(100);
      graphics.setPaint(color);
      graphics.setPaintMode();
      for(int g=0; g<geom.getNumGeometries(); g++){
          // Check if polygons has holes
          if(geom.getGeometryN(g).getGeometryType().equals("Polygon")){
              Polygon polygon = (Polygon) geom.getGeometryN(g);
              java.awt.geom.Area awtArea;
              if(polygon.getNumInteriorRing() > 0){
                  // Holes found
                  // Exterior ring
                  coord = polygon.getExteriorRing().getCoordinates();
                  coordGridX = new int[coord.length];
                  coordGridY = new int[coord.length];
                  // From geographic coords to image coords
                  for(int p=0; p<coord.length; p++){
                      java.awt.Point point = fromCoordToCell(coord[p], cellSize);
                      coordGridX[p] = point.x;
                      coordGridY[p] = point.y;
                  }
                  java.awt.Polygon awtPolygon = new java.awt.Polygon(coordGridX, coordGridY, coord.length);
                  awtArea = new java.awt.geom.Area(awtPolygon);
                  // Subtract inner rings
                  for(int ir=0; ir<polygon.getNumInteriorRing(); ir++){
                      coord = polygon.getInteriorRingN(ir).getCoordinates();
                      coordGridX = new int[coord.length];
                      coordGridY = new int[coord.length];
                      // From geographic coords to image coords
                      for(int p=0; p<coord.length; p++){
                          java.awt.Point point = fromCoordToCell(coord[p], cellSize);
                          coordGridX[p] = point.x;
                          coordGridY[p] = point.y;
                      }
                      awtPolygon = new java.awt.Polygon(coordGridX, coordGridY, coord.length);
                      java.awt.geom.Area awtArea2 = new java.awt.geom.Area(awtPolygon);
                      awtArea.subtract(awtArea2);
                  }
              }else{
                  coord = polygon.getCoordinates();
                  coordGridX = new int[coord.length];
                  coordGridY = new int[coord.length];
                  // From geographic coords to image coords
                  for(int p=0; p<coord.length; p++){
                      java.awt.Point point = fromCoordToCell(coord[p], cellSize);
                      coordGridX[p] = point.x;
                      coordGridY[p] = point.y;
                  }
                  java.awt.Polygon awtPolygon = new java.awt.Polygon(coordGridX, coordGridY, coord.length);
                  awtArea = new java.awt.geom.Area(awtPolygon);
              }

              graphics.setPaint(color);
              graphics.setPaintMode();
              graphics.draw(awtArea);
              graphics.fill(awtArea);
          }else{
              coord = geom.getGeometryN(g).getCoordinates();
              coordGridX = new int[coord.length];
              coordGridY = new int[coord.length];

              // From geographic coords to image coords
              for(int p=0; p<coord.length; p++){
                  java.awt.Point point = fromCoordToCell(coord[p], cellSize);
                  coordGridX[p] = point.x;
                  coordGridY[p] = point.y;
              }
              if(geom.getGeometryN(g).getGeometryType().equals("LineString") || geom.getGeometryN(g).getGeometryType().equals("MultiLineString")){
                  graphics.setPaint(color);
                  graphics.setPaintMode();
                  graphics.drawPolyline(coordGridX, coordGridY, coord.length);
              }else if(geom.getGeometryN(g).getGeometryType().equals("Point") || geom.getGeometryN(g).getGeometryType().equals("MultiPoint")){
                  graphics.setPaint(color);
                  graphics.setPaintMode();
                  graphics.fillRect(coordGridX[0], coordGridY[0], 1, 1);
              }
          }
       }
      for(int r=0; r<m_iNY; r++){
          for(int c=0; c<m_iNX; c++){
              if(bimage.getRGB(c, r) != 0 && bimage.getRGB(c, r) != -1){
                 
                  grid[c+1][m_iNY-r] = 1;
              }
          }
      }        
      return grid;
  }
}

