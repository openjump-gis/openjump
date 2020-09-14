package org.openjump.core.ui.plugin.raster;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;

import com.vividsolutions.jts.geom.Envelope;

public class RasterImageLayerProperties {

	 
	  private static int datatype;
	   private static String type;

	   private static final String[] Q = new String[] { "", "KB", "MB", "GB",
	            "TB", "PB", "EB" };

	   
	   /**
	    * Converts bytes to multiple (Kilo, Mega, Giga-bytes according to the
	    * dimension of the file. Return as String
	    * @param bytes as long
	    * @return java.lang.String
	    */
	    
	    public static String getFileSizeMegaBytes(long bytes) {
	        for (int i = 6; i > 0; i--) {
	            double step = Math.pow(1024, i);
	            if (bytes > step)
	                return String.format("%3.1f %s", bytes / step, Q[i]);
	        }
	        return Long.toString(bytes);
	    }

	    /**
	     * Gets dimension of RasterImageLayer in long
	     * @param org.openjump.core.rasterimage.RasterImageLayer
	     * @return long
	     */
	    
	    public static long getFileSizeBytes(RasterImageLayer layer) {
	        File rfile = new File(layer.getImageFileName());
	        return rfile.length();
	    }

	    /**
	     * Return the extension of the file as String
	     * @param org.openjump.core.rasterimage.RasterImageLayer
	     * @return java.lang.String
	     */
	    
	    public static String getFileExtension(RasterImageLayer layer) {
	        File f = new File(layer.getImageFileName());
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');
	        if (i > 0 && i < s.length() - 1) {
	            ext = s.substring(i + 1).toUpperCase();
	        }
	        return ext;
	    }

	    /**
	     * Gets the number of bands from A RasterImageLayer as String
	     * @param org.openjump.core.rasterimage.RasterImageLayer
	     * @return java.lang.String
	     */
	    
	    public static String numBands(RasterImageLayer layer) {
	        int bands = layer.getNumBands();
	        return String.valueOf(bands);
	    }
	    /**
	     * Gets data type as String
	     * @param java.awt.image.Raster
	     * @return java.lang.String
	     * @throws IOException
	     */
	     
	    public static String getDataType(Raster raster) throws IOException {
	        SampleModel sm = raster.getSampleModel();
	        datatype = sm.getDataType();
	        switch (datatype) {
	        case DataBuffer.TYPE_BYTE: {
	            type = "byte";
	            break;
	        }
	        case DataBuffer.TYPE_SHORT: {
	            type = "short";
	            break;
	        }
	        case DataBuffer.TYPE_USHORT: {
	            type = "ushort";
	            break;
	        }
	        case DataBuffer.TYPE_INT: {
	            type = "int";
	            break;
	        }
	        case DataBuffer.TYPE_FLOAT: {
	            type = "float";
	            break;
	        }
	        case DataBuffer.TYPE_DOUBLE: {
	            type = "double";
	            break;
	        }
	        case DataBuffer.TYPE_UNDEFINED: {
	            type = "undefined";
	            break;
	        }
	        }
	        return type;
	    }
	    /**
	     * Gets color depth as String
	     * It first check into the Raster
	     * otherwise uses first Commons Imaging then ImageIO
	     * to check into the file
	     * @param java.awt.image.Raster
	     * @param java.io.File
	     * @return java.lang.String
	     * @throws IOException
	     * @throws ImageReadException
	     */
	    
	    public static String getColorDepth(Raster r, File file) throws IOException, ImageReadException {
	    	 BufferedImage buf = null;
	    	 String components ="(Cannot read num componets)";
	    	 ColorModel cm;
	    	 int colordepth;
	    	 try {
	    		 SampleModel sm = r.getSampleModel();
	            cm = PlanarImage.createColorModel(sm);
	            colordepth = cm.getNumColorComponents(); 
	    	 }catch (Exception e) {
	    		 try {
	    		 buf =  Imaging.getBufferedImage(file);
	             cm  =buf.getColorModel();
	             colordepth = cm.getNumColorComponents();
	    	 } catch(ArrayIndexOutOfBoundsException ex) {
	    		 buf = ImageIO.read(file);
	   		   cm =buf.getColorModel();
	   	   	   colordepth = cm.getNumColorComponents();
	    		 }
	    	 }
	    	 components=String.valueOf(colordepth);
	    	 return components + " bpp";
	    	 
	     }
	   
	    /**
	     * Gets DPI, first check into the raster then into the file
	     * using Commons Imaging
	     * @param java.awt.image.Raster
	     * @param java.io.File
	     * @return java.lang.String
	     * @throws IOException
	     * @throws ImageReadException
	     */
	     
	    public static  String getDPI(Raster r, File file) throws IOException, ImageReadException {
	    	 int pixelSize;
	    	 String pixelSizeS="";
	    	 try {
	         SampleModel sm = r.getSampleModel();
	         ColorModel cm = PlanarImage.createColorModel(sm);
	         pixelSize = cm.getPixelSize();
	         }catch (Exception e) {
	        	 final ImageInfo imageInfo = Imaging.getImageInfo(file);
	        	 pixelSize = imageInfo.getBitsPerPixel();
	         }
	    	 try {
	    		 pixelSizeS=String.valueOf(pixelSize);
	    	 } catch (Exception ex){
	    		 pixelSizeS="Not recognized";
	    	 }
	         return pixelSizeS;
	     } 
	     
	     /**
	      * Gets the cell size along X as Double
	      * @param java.awt.image.Raster
	      * @param com.vividsolutions.jts.geom.Envelope
	      * @return double
	      * @throws IOException
	      */
	    
	    public static  double cellSizeX(Raster r, Envelope env) throws IOException {
	        return env.getWidth() / r.getWidth();
	    }

	    
	    /**
	      * Gets the cell size along Y as double
	      * @param java.awt.image.Raster
	      * @param com.vividsolutions.jts.geom.Envelope
	      * @return double
	      * @throws IOException
	      */
	    public static  double cellSizeY(Raster r, Envelope env) throws IOException {
	        return env.getHeight() / r.getHeight();
	    }

	    /**
	     * Gets the number of column in a Raster as int
	     * @param java.awt.image.Raster
	     * @return int
	     * @throws IOException
	     */
	    public static  int getNumColumns(Raster r) throws IOException {
	        return r.getWidth();
	    }

	    /**
	     * Gets number of rows as int
	     * @param java.awt.image.Raster
	     * @return int
	     * @throws IOException
	     */
	    public static  int getNumRows(Raster r) throws IOException {
	        return r.getHeight();
	    }

	    /**
	     * Count the number of cells in a Raster of a specific double value and 
	     * return as int.
	     * @param java.awt.image.Raster
	     * @param double value
	     * @return int
	     * @throws IOException
	     */

	    public static  int getNodataCellNumber(Raster ras, double nodata)
	            throws IOException, RasterDataNotFoundException {
	        int counter = 0;

	        int nx = ras.getWidth();
	        int ny = ras.getHeight();
	        for (int y = 0; y < ny; y++) {
	            for (int x = 0; x < nx; x++) {
	                double value = ras.getSampleDouble(x, y, 0);
	                if (value == nodata)
	                    counter++;
	            }
	        }
	        return counter;
	    }

	    
	    /**
	     * Count the number of cells in a Raster excluding 
	     * the ones of a specific double value and 
	     * return as int.
	     * @param java.awt.image.Raster
	     * @param double value
	     * @return int
	     * @throws IOException
	     */
	    
	    public static  int getValidCellsNumber(Raster raster, double nodata)
	            throws IOException, RasterDataNotFoundException {

	        return raster.getWidth() * raster.getHeight()
	                - getNodataCellNumber(raster, nodata);
	    }
	
	
}
