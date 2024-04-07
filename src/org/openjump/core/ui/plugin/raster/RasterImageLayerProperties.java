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
import org.apache.commons.imaging.Imaging;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayer.RasterDataNotFoundException;

import org.locationtech.jts.geom.Envelope;

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
	     * @param layer a RasterImageLayer
	     * @return the size of the file containing the image
	     */
	    public static long getFileSizeBytes(RasterImageLayer layer) {
	        File rfile = new File(layer.getImageFileName());
	        return rfile.length();
	    }

	    /**
	     * Return the extension of the file as String
	     * @param layer a RasterImageLayer
	     * @return the file extension as String
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
	     * @param layer a RasterImageLayer
	     * @return the number of bands as a String
	     */
	    public static String numBands(RasterImageLayer layer) {
	        int bands = layer.getNumBands();
	        return String.valueOf(bands);
	    }
	    /**
	     * Gets data type as String
	     * @param raster a java.awt.image.Raster
	     * @return the data type as a String
	     * @throws IOException never
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
	     * @param raster a java.awt.image.Raster
	     * @param file the File containing the image
	     * @return the color depth as a String
	     * @throws IOException if an IOException occurs during file I/O
	     */
	    public static String getColorDepth(Raster raster, File file) throws IOException {
	    	 BufferedImage buf;
	    	 String components ="(Cannot read num componets)";
	    	 ColorModel cm;
	    	 int colordepth;
	    	 try {
	    		 SampleModel sm = raster.getSampleModel();
	    		 cm = PlanarImage.createColorModel(sm);
	    		 colordepth = cm.getNumColorComponents();
	    	 } catch (Exception e) {
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
	     * Gets BPP (bits per pixel), first check into the raster then into the file
	     * using Commons Imaging
	     * @param raster the java.awt.image.Raster
	     * @param file the File containing the image
	     * @return the number of bits per pixel as a String
	     * @throws IOException if an IOException occurs during file I/O
	     */
	    public static  String getDPI(Raster raster, File file) throws IOException {
	    	 int pixelSize;
	    	 String pixelSizeS="";
	    	 try {
	         SampleModel sm = raster.getSampleModel();
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
	      * @param raster the java.awt.image.Raster
	      * @param env image Envelope
	      * @return pixel width as a double
	      * @throws IOException never
	      */
	    public static double cellSizeX(Raster raster, Envelope env) throws IOException {
	        return env.getWidth() / raster.getWidth();
	    }

	    
	    /**
	      * Gets the cell size along Y as double
	      * @param raster the java.awt.image.Raster
	      * @param env the Envelope
	      * @return pixel height as a double
	      * @throws IOException never
	      */
	    public static  double cellSizeY(Raster raster, Envelope env) throws IOException {
	        return env.getHeight() / raster.getHeight();
	    }

	    /**
	     * Gets the number of column in a Raster as int
	     * @param raster the java.awt.image.Raster
	     * @return the number of columns
	     * @throws IOException never
	     */
	    public static  int getNumColumns(Raster raster) throws IOException {
	        return raster.getWidth();
	    }

	    /**
	     * Gets number of rows as int
	     * @param raster the java.awt.image.Raster
	     * @return the number of rows
	     * @throws IOException never
	     */
	    public static  int getNumRows(Raster raster) throws IOException {
	        return raster.getHeight();
	    }

	    /**
	     * Count the number of cells in a Raster of a specific double value and 
	     * return as int.
	     * @param raster the java.awt.image.Raster
	     * @param nodata the value meaning "no data"
	     * @return the number of nodata cells
	     * @throws IOException if an IOException occurs during I/O
			 * @throws RasterDataNotFoundException if an error occurs accessing raster data
	     */
	    public static int getNodataCellNumber(Raster raster, double nodata)
	            throws IOException, RasterDataNotFoundException {
	        int counter = 0;

	        int nx = raster.getWidth();
	        int ny = raster.getHeight();
	        for (int y = 0; y < ny; y++) {
	            for (int x = 0; x < nx; x++) {
	                double value = raster.getSampleDouble(x, y, 0);
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
			 * @param raster the java.awt.image.Raster
			 * @param nodata the value meaning "no data"
			 * @return the number of valid cells (different from no data)
			 * @throws IOException if an IOException occurs during I/O
			 * @throws RasterDataNotFoundException if an error occurs accessing raster data
	     */
	    public static  int getValidCellsNumber(Raster raster, double nodata)
	            throws IOException, RasterDataNotFoundException {

	        return raster.getWidth() * raster.getHeight()
	                - getNodataCellNumber(raster, nodata);
	    }
	
}
