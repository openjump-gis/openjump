package org.geotools.shapefile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;
import com.vividsolutions.jump.workbench.Logger;

/**
 * This class represents an ESRI Shape file.<p>
 * You construct it with a file name, and later
 * you can read the file's properties, i.e. Sizes, Types, and the data itself.<p>
 * Copyright 1998 by James Macgill. <p>
 *
 * Version 1.0beta1.1 (added construct with inputstream)
 * 1.0beta1.2 (made Shape type constants public 18/Aug/98)
 *
 * This class supports the Shape file as set out in :-<br>
 * <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf"><b>"ESRI(r) Shapefile - A Technical Description"</b><br>
 * <i>'An ESRI White Paper . May 1997'</i></a><p>
 *
 * This code is coverd by the LGPL.
 *
 * <a href="mailto:j.macgill@geog.leeds.ac.uk">Mail the Author</a>
 */
public class Shapefile  {

    static final int    SHAPEFILE_ID = 9994;
    static final int    VERSION = 1000;
    
    public static final int NULL        =  0;
    public static final int POINT       =  1;
    public static final int POINTZ      = 11;
    public static final int POINTM      = 21;
    public static final int ARC         =  3;
    public static final int ARCM        = 23;
    public static final int ARCZ        = 13;
    public static final int POLYGON     =  5;
    public static final int POLYGONM    = 25;
    public static final int POLYGONZ    = 15;
    public static final int MULTIPOINT  =  8;
    public static final int MULTIPOINTM = 28;
    public static final int MULTIPOINTZ = 18;
    public static final int MULTIPATCH  = 31;
    public static final int UNDEFINED   = -1;
    //Types 2,4,6,7 and 9 were undefined at time or writeing
    
    private URL baseURL;
    private InputStream shpInputStream;
    private int errors;
    
    /**
     * Creates and initialises a shapefile from a url
     * @param url The url of the shapefile
     */
    public Shapefile(URL url) {
        baseURL=url;
        shpInputStream= null;
        try {
            URLConnection uc = baseURL.openConnection();
            // a 16 kb buffer may be up to 20% faster than the default 2 kb buffer
            shpInputStream = new BufferedInputStream(uc.getInputStream(), 16*1024);
        }
        catch (Exception e){
            Logger.error(e);
        }
    }
    
    public Shapefile(InputStream is) {
        shpInputStream = is;
    }
    
    public void close() {
        try {
            shpInputStream.close();
    	}
    	catch (IOException ex){
            Logger.error(ex);
        }
    }
    
    private EndianDataInputStream getInputStream() throws IOException {
        if (shpInputStream == null) {
            throw new IOException("Couldn't make a connection to the URL: " + baseURL);
        }
        return new EndianDataInputStream(shpInputStream);
    }
    
    private EndianDataOutputStream getOutputStream() throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(baseURL.getFile()));
        return new EndianDataOutputStream(out);
    }
        
    
    /**
     * Initialises a shapefile from disk.
     * Use Shapefile(String) if you don't want to use LEDataInputStream directly (recommended)
     * @param geometryFactory the geometry factory to use to read the shapes
     */
    public GeometryCollection read(GeometryFactory geometryFactory) throws Exception {

        ArrayList<Geometry> list = new ArrayList<>();
        int pos = 0;
        try (EndianDataInputStream file = getInputStream()) {

            ShapefileHeader mainHeader = new ShapefileHeader(file);
            if(mainHeader.getVersion() != VERSION){
                Logger.warn(String.format("Unknown shapefile version (%s) : try to read anyway", mainHeader.getVersion()));
            }
            pos += 50;

            Geometry body;

            int type = mainHeader.getShapeType();
            ShapeHandler handler = getShapeHandler(type);
            if(handler==null) throw new ShapeTypeNotSupportedException("Unsuported shape type: " + type);

            errors = 0;
            int count = 1;

            while(true){
                int recordNumber=file.readIntBE(); pos+=2;
                if (recordNumber != count) {
                    Logger.warn("wrong record number (" + recordNumber + ")");
                    continue;
                }
                int contentLength=file.readIntBE(); pos+=2;
                if (contentLength <= 0) {
                    Logger.warn("found a negative content length (" + contentLength + ")");
                    continue;
                }
                try{
                    body = handler.read(file,geometryFactory,contentLength);
                    Logger.debug("" + recordNumber + " : from " + (pos-4) + " for " + contentLength + " (" + body.getNumPoints() + " pts)");
                    pos += contentLength;
                    list.add(body);
                    count++;
                    if (body.getUserData() != null) errors++;
                } catch(Exception e) {
                    Logger.warn("Error processing record " +recordNumber + " : " + e.getMessage(), e);
                    errors++;
                }
            }
        }
        catch(EOFException e) {}

        return geometryFactory.createGeometryCollection((Geometry[])list.toArray(new Geometry[]{}));
    }
    
    /**
     * Get the number of errors found after a read.
     */
     public int getErrorNumber() {return errors;}
    
    /**
     * Saves a shapefile to an output stream.
     * @param geometries geometry collection to write
     * @param ShapeFileDimension shapefile dimension (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     */
    public  void write(GeometryCollection geometries, int ShapeFileDimension) throws Exception {
        try(EndianDataOutputStream file = getOutputStream()) {
            ShapefileHeader mainHeader = new ShapefileHeader(geometries, ShapeFileDimension);
            mainHeader.write(file);
            int pos = 50; // header length in WORDS

            int numShapes = geometries.getNumGeometries();
            Geometry body;
            ShapeHandler handler;

            if (geometries.getNumGeometries() == 0) {
                handler = new PointHandler(); //default
            } else {
                handler = Shapefile.getShapeHandler(geometries.getGeometryN(0), ShapeFileDimension);
            }

            for (int i = 0; i < numShapes; i++) {
                body = geometries.getGeometryN(i);
                file.writeIntBE(i + 1);
                file.writeIntBE(handler.getLength(body));
                pos += 4; // length of header in WORDS
                handler.write(body, file);
                pos += handler.getLength(body); // length of shape in WORDS
            }
            file.flush();
        }
    }
  
  
    //ShapeFileDimension =>    2=x,y ; 3=x,y,m ; 4=x,y,z,m
    /**
     * Saves a shapefile index (shx) to an output stream.
     * @param geometries geometry collection to write
     * @param file file to write to
     * @param ShapeFileDimension shapefile dimension (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     */
    public synchronized void writeIndex(GeometryCollection geometries,
                                        EndianDataOutputStream file,
                                        int ShapeFileDimension) throws Exception {
        Geometry geom;    
        
        ShapeHandler handler ;
        int nrecords = geometries.getNumGeometries();
        ShapefileHeader mainHeader = new ShapefileHeader(geometries,ShapeFileDimension);
        
        if (geometries.getNumGeometries() == 0) {
            handler = new PointHandler(); //default
        }
        else {
            handler = Shapefile.getShapeHandler(geometries.getGeometryN(0), ShapeFileDimension);
        }
        
        mainHeader.writeToIndex(file);
        int pos = 50;
        int len;
       
        for(int i=0 ; i<nrecords ; i++){
            geom = geometries.getGeometryN(i);
            len = handler.getLength(geom);
            file.writeIntBE(pos);
            file.writeIntBE(len);
            pos = pos+len+4;
        }
        file.flush();
        file.close();
    }
   
    
    /**
     * Returns a string describing the shape type.
     * @param index An int coresponding to the shape type to be described
     * @return A string describing the shape type
     */
    public static String getShapeTypeDescription(int index){
        switch(index){
            case(NULL):return ("Null Shape");
            case(POINT):return ("Point");
            case(POINTZ):return ("PointZ");
            case(POINTM):return ("PointM");
            case(ARC):return ("PolyLine");
            case(ARCM):return ("PolyLineM");
            case(ARCZ):return ("PolyLineZ");
            case(POLYGON):return ("Polygon");
            case(POLYGONM):return ("PolygonM");
            case(POLYGONZ):return ("PolygonZ");
            case(MULTIPOINT):return ("MultiPoint");
            case(MULTIPOINTM):return ("MultiPointM");
            case(MULTIPOINTZ):return ("MultiPointZ");
            default:return ("Undefined"); 
        }
    }
    
    public static ShapeHandler getShapeHandler(Geometry geom, int ShapeFileDimension) throws Exception {
        return getShapeHandler(getShapeType(geom, ShapeFileDimension));
    }
    
    public static ShapeHandler getShapeHandler(int type) throws Exception {
        switch(type){
            case Shapefile.NULL: return new NullShapeHandler();
            case Shapefile.POINT: return new PointHandler();
            case Shapefile.POINTZ: return new PointHandler(Shapefile.POINTZ);
            case Shapefile.POINTM: return new PointHandler(Shapefile.POINTM);
            case Shapefile.POLYGON: return new PolygonHandler();
            case Shapefile.POLYGONM: return new PolygonHandler(Shapefile.POLYGONM);
            case Shapefile.POLYGONZ: return new PolygonHandler(Shapefile.POLYGONZ);
            case Shapefile.ARC: return new MultiLineHandler();
            case Shapefile.ARCM: return new MultiLineHandler(Shapefile.ARCM);
            case Shapefile.ARCZ: return new MultiLineHandler(Shapefile.ARCZ);
            case Shapefile.MULTIPOINT: return new MultiPointHandler();
            case Shapefile.MULTIPOINTM: return new MultiPointHandler(Shapefile.MULTIPOINTM);
            case Shapefile.MULTIPOINTZ: return new MultiPointHandler(Shapefile.MULTIPOINTZ);
        }
        return null;
    }
    
    /**
     * Returns the Shape Type corresponding to geometry geom of dimension
     * ShapeFileDimension.
     * @param geom the geom
     * @param ShapeFileDimension the dimension of the geom (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     * @return A int representing the Shape Type
     */
    public static int getShapeType(Geometry geom, int ShapeFileDimension) throws ShapefileException {
        
        if ((ShapeFileDimension !=2) && (ShapeFileDimension !=3) && (ShapeFileDimension !=4)) {
            throw new ShapefileException(
                "invalid ShapeFileDimension for getShapeType - expected 2,3,or 4 but got "
                + ShapeFileDimension + "  (2=x,y ; 3=x,y,m ; 4=x,y,z,m)"
            );
        }
        
        if(geom instanceof Point) {
            switch (ShapeFileDimension) {
                case 2: return Shapefile.POINT;
                case 3: return Shapefile.POINTM;
                case 4: return Shapefile.POINTZ;    
            }
        }
        
        if(geom instanceof MultiPoint) {
            switch (ShapeFileDimension) {
                case 2: return Shapefile.MULTIPOINT;
                case 3: return Shapefile.MULTIPOINTM;
                case 4: return Shapefile.MULTIPOINTZ;    
            }
        }
        
        if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) {
            switch (ShapeFileDimension) {
                case 2: return Shapefile.POLYGON;
                case 3: return Shapefile.POLYGONM;
                case 4: return Shapefile.POLYGONZ;    
            }
        }
        
        if ((geom instanceof LineString) || (geom instanceof MultiLineString)) {
            switch (ShapeFileDimension) {
                case 2: return Shapefile.ARC;
                case 3: return Shapefile.ARCM;
                case 4: return Shapefile.ARCZ;    
            }
        }
        
        if ((geom instanceof GeometryCollection) && (geom.isEmpty())) {
            return Shapefile.NULL;
        }
        
        return Shapefile.UNDEFINED;
    }


    /**
     * The purpose of this new reader [mmichaud 2015-04-11] is to read a shapefile using the shx
     * index file.
     * While the legacy reader #read(GeometryFactory geometryFactory) read the shapefile sequentially
     * and don't need the shx index file, this new parser read the shx file and access the shp file
     * with a RandomAccessReader.
     * Because the shapefile may come from a compressed input stream, the method first write the
     * shapefile in a temporary file.
     * @param geometryFactory geometry factory to use to build geometries
     * @param is shx input stream
     * @return a GeometryCollection containing all the shapes.
     */
    public synchronized GeometryCollection readFromIndex(GeometryFactory geometryFactory, InputStream is)
            throws Exception {

        // Flush shapefile inputStream to a temporary file, because inputStream
        // may come from a zipped archive, and we want to access data in Random mode
        File tmpShp = File.createTempFile("tmpshp", ".shp");
        ArrayList<Geometry> list = new ArrayList<>();
        try (BufferedInputStream bis= new BufferedInputStream(shpInputStream, 4096);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpShp), 4096);
             RandomAccessFile raf = new RandomAccessFile(tmpShp, "r");
             EndianDataInputStream shx = new EndianDataInputStream(is)) {
            int nb;
            byte[] bytes = new byte[4096];
            while (-1 != (nb = bis.read(bytes))) {
                bos.write(bytes, 0, nb);
            }
            bos.flush();
            // read shapefile header
            bytes = new byte[100];
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            raf.getChannel().read(bb);

            EndianDataInputStream shp = new EndianDataInputStream(new ByteArrayInputStream(bytes));
            ShapefileHeader shpMainHeader = new ShapefileHeader(shp);
            if (shpMainHeader.getVersion() != VERSION) {
                Logger.warn(String.format("Unknown shp version (%s) : try to read anyway", shpMainHeader.getVersion()));
            }

            ShapefileHeader shxMainHeader = new ShapefileHeader(shx);
            if (shxMainHeader.getVersion() != VERSION) {
                Logger.warn(String.format("Unknown shx version (%s) : try to read anyway", shxMainHeader.getVersion()));
            }

            Geometry body;
            int type = shpMainHeader.getShapeType();
            ShapeHandler handler = getShapeHandler(type);
            if(handler==null) throw new ShapeTypeNotSupportedException("Unsupported shape type:" + type);

            int recordNumber = 0;
            while (true) {
                long offset = shx.readIntBE() & 0x00000000ffffffffL;
                int length = shx.readIntBE();
                recordNumber++;
                try{
                    bytes = new byte[length*2];
                    bb = ByteBuffer.wrap(bytes);
                    raf.getChannel().read(bb, offset*2 + 8);
                    shp = new EndianDataInputStream(new ByteArrayInputStream(bytes));
                    body = handler.read(shp, geometryFactory, length);
                    Logger.debug("" + recordNumber + " : from " + offset + " for " + length + " (" + body.getNumPoints() + " pts)");
                    list.add(body);
                    if (body.getUserData() != null) errors++;
                } catch(Exception e) {
                    Logger.warn("Error processing record " + recordNumber + ": " + e.getMessage(), e);
                    Logger.warn("an empty Geometry has been returned");
                    list.add(handler.getEmptyGeometry(geometryFactory));
                    errors++;
                }
            }

        }
        catch (EOFException e) {}
        finally {
            if (tmpShp.exists()) {
                if (!tmpShp.delete()) {
                    Logger.warn(tmpShp + " could not be deleted");
                }
            }
        }

        return geometryFactory.createGeometryCollection(list.toArray(new Geometry[]{}));
    }
}

