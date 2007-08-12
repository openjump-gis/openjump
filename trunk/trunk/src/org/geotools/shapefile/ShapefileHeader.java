/*
 * Header.java
 *
 * Created on February 12, 2002, 3:29 PM
 */

package org.geotools.shapefile;

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 *
 * @author  jamesm
 */
public class ShapefileHeader{
    private final static boolean DEBUG=false;
    private int fileCode = -1;
    public int fileLength = -1;
    private int indexLength = -1;
    private int version = -1;
    private int shapeType = -1;
    //private double[] bounds = new double[4];
    private Envelope bounds;
    // added by Michaël MICHAUD on 4 nov. 2004 in order to handle shapefile 3D
    // the right way (zmin and z max may be used by arcgis data translator when
    // transforming shapefiles to geodatabase)
    private double zmin = 0.0;
    private double zmax = 0.0;
    
    public ShapefileHeader(EndianDataInputStream file) throws IOException {
      //  file.setLittleEndianMode(false);
        fileCode = file.readIntBE();
       // if(DEBUG)System.out.println("Sfh->Filecode "+fileCode);
        if ( fileCode != Shapefile.SHAPEFILE_ID )
            System.err.println("Sfh->WARNING filecode "+fileCode+" not a match for documented shapefile code "+Shapefile.SHAPEFILE_ID);
        
        for(int i=0;i<5;i++){
            int tmp = file.readIntBE();
           // if(DEBUG)System.out.println("Sfh->blank "+tmp);
        }
        fileLength = file.readIntBE();
        
      //  file.setLittleEndianMode(true);
        version=file.readIntLE();
        shapeType=file.readIntLE();
       
        //read in and for now ignore the bounding box
        for(int i = 0;i<4;i++){
            file.readDoubleLE();
        }
        
        //skip remaining unused bytes
       // file.setLittleEndianMode(false);//well they may not be unused forever...
        file.skipBytes(32);
    }
    
    public ShapefileHeader(GeometryCollection geometries,int dims) throws Exception
    {
        ShapeHandler handle;
        if (geometries.getNumGeometries() == 0)
        {
            handle = new PointHandler(); //default
        }
        else
        {
               handle = Shapefile.getShapeHandler(geometries.getGeometryN(0),dims);
        }
        int numShapes = geometries.getNumGeometries();
        shapeType = handle.getShapeType();
        // added by Michaël MICHAUD on 4 nov. 2004
        boolean zvalues = false;
        if (shapeType==11 || shapeType==13 || shapeType==15 || shapeType==18) {
            zvalues = true;
            zmin = Double.MAX_VALUE;
            zmax = Double.MIN_VALUE;
        }
        version = Shapefile.VERSION;
        fileCode = Shapefile.SHAPEFILE_ID;
        bounds = geometries.getEnvelopeInternal();
        fileLength = 0;
        for(int i=0;i<numShapes;i++){
            Geometry g = geometries.getGeometryN(i);
            fileLength+=handle.getLength(g);
            fileLength+=4;//for each header
            // added by Michaël MICHAUD on 4 nov. 2004
            if (zvalues) {
                Coordinate[] cc = g.getCoordinates();
                for (int j = 0 ; j < cc.length ; j++) {
                    if (Double.isNaN(cc[j].z)) continue;
                    if (cc[j].z < zmin) zmin = cc[j].z;
                    if (cc[j].z > zmax) zmax = cc[j].z;
                }
            }
        }
        fileLength+=50;//space used by this, the main header
        indexLength = 50+(4*numShapes);
    }
    
    public void setFileLength(int fileLength){
        this.fileLength = fileLength;
    }
    
 
    
    public void write(EndianDataOutputStream file)throws IOException {
        int pos = 0;
       // file.setLittleEndianMode(false);
        file.writeIntBE(fileCode);
        pos+=4;
        for(int i=0;i<5;i++){
            file.writeIntBE(0);//Skip unused part of header
            pos+=4;
        }
        file.writeIntBE(fileLength);
        pos+=4;
        //file.setLittleEndianMode(true);
        file.writeIntLE(version);
        pos+=4;
        file.writeIntLE(shapeType);
        pos+=4;
        //write the bounding box
        file.writeDoubleLE(bounds.getMinX());
        file.writeDoubleLE(bounds.getMinY());
        file.writeDoubleLE(bounds.getMaxX());
        file.writeDoubleLE(bounds.getMaxY());
        pos+=8*4;
        
        // added by Michaël MICHAUD on 4 nov. 2004
        file.writeDoubleLE(zmin);
        file.writeDoubleLE(zmax);
        pos+=8*2;
        //skip remaining unused bytes
        //file.setLittleEndianMode(false);//well they may not be unused forever...
        //for(int i=0;i<2;i++){
        file.writeDoubleLE(0.0);
        file.writeDoubleLE(0.0);//Skip unused part of header
        pos+=8;
        //}
        
        if(DEBUG)System.out.println("Sfh->Position "+pos);
    }
    
    public void writeToIndex(EndianDataOutputStream file)throws IOException {
        int pos = 0;
        //file.setLittleEndianMode(false);
        file.writeIntBE(fileCode);
        pos+=4;
        for(int i=0;i<5;i++){
            file.writeIntBE(0);//Skip unused part of header
            pos+=4;
        }
        file.writeIntBE(indexLength);
        pos+=4;
       // file.setLittleEndianMode(true);
        file.writeIntLE(version);
        pos+=4;
        file.writeIntLE(shapeType);
        pos+=4;
        //write the bounding box
        pos+=8;
         file.writeDoubleLE(bounds.getMinX() );
         pos+=8;
         file.writeDoubleLE(bounds.getMinY() );
         pos+=8;
         file.writeDoubleLE(bounds.getMaxX() );
         pos+=8;
         file.writeDoubleLE(bounds.getMaxY() );
         /*
        for(int i = 0;i<4;i++){
            pos+=8;
            file.writeDouble(bounds[i]);
        }*/
        
        //skip remaining unused bytes
        //file.setLittleEndianMode(false);//well they may not be unused forever...
        for(int i=0;i<4;i++){
            file.writeDoubleLE(0.0);//Skip unused part of header
            pos+=8;
        }
        if(DEBUG)System.out.println("Sfh->Index Position "+pos);
    }
    
    public int getShapeType(){
        return shapeType;
    }
    
    public int getVersion(){
        return version;
    }
    
    public Envelope getBounds(){
        return bounds;
    }
    
    public String toString()  {
        String res = new String("Sf-->type "+fileCode+" size "+fileLength+" version "+ version + " Shape Type "+shapeType);
        return res;
    }
}
