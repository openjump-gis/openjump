package org.openjump.core.rasterimage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.libtiff.jai.codec.XTIFF;

public class TiffTags {
    
    public static TiffMetadata readMetadata(File tiffFile) throws FileNotFoundException, IOException, TiffReadingException, ImageReadException {
        
        Integer colCount = null;
        Integer rowCount = null;
        Double noData = -3.40282346639e+038;
        Coordinate pixelOffset = null;
        Coordinate tiePoint = null;
        Resolution pixelScale = null;
        Envelope envelope = null;
        
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        List<TiffField> tiffFields = metadata.getAllFields();
        for(TiffField tiffField : tiffFields) {
            
            switch(tiffField.getTag()) {
                case XTIFF.TIFFTAG_IMAGE_WIDTH:
                    colCount = tiffField.getIntValue();
                    break;
                case XTIFF.TIFFTAG_IMAGE_LENGTH:
                    rowCount = tiffField.getIntValue();
                    break;
                case TIFFTAG_GDAL_NODATA:
                    String noDataString = "";
                    if(tiffField.getFieldType() == FieldType.ASCII) {
                        noDataString = tiffField.getStringValue();
                        if(noDataString.equalsIgnoreCase("NaN")) {
                            noDataString = "NaN";
                        }                    
                    } else if(tiffField.getFieldType() == FieldType.BYTE) {
                        noDataString = new String(tiffField.getByteArrayValue());
                    }
                    noData = Double.parseDouble(noDataString);
                    break;
                case GeoTiffConstants.ModelTiepointTag:
                    double[] tiePointValues = tiffField.getDoubleArrayValue();
                    pixelOffset = new Coordinate(tiePointValues[0], tiePointValues[1], tiePointValues[2]);     
                    tiePoint = new Coordinate(tiePointValues[3], tiePointValues[4], tiePointValues[5]);
                    break;
                case GeoTiffConstants.ModelPixelScaleTag:
                    double[] pixelSCaleValues = tiffField.getDoubleArrayValue();
                    if (pixelSCaleValues.length == 2 || pixelSCaleValues[2] == 0) {
                        pixelScale = new Resolution(pixelSCaleValues[0],pixelSCaleValues[1]);
                    } else {
                        pixelScale = new Resolution(pixelSCaleValues[0],pixelSCaleValues[1],pixelSCaleValues[2]);
                    }
                    break;
            }
            
        }
        
        if (tiePoint != null && pixelScale != null){
            
            Coordinate upperLeft, lowerRight;
                  
            if (pixelOffset==null){
                upperLeft = tiePoint;
            } else {
                upperLeft = new Coordinate( tiePoint.x - (pixelOffset.x * pixelScale.getX()), tiePoint.y - (pixelOffset.y * pixelScale.getY()));
            }

            lowerRight = new Coordinate( upperLeft.x + (colCount * pixelScale.getX()), upperLeft.y - (rowCount * pixelScale.getY()));
            envelope = new Envelope(upperLeft, lowerRight);
                  
        }
        
        return new TiffTags().new TiffMetadata(colCount, rowCount, pixelScale, noData, envelope);
        
    }
    
    public static TiffField readField(File tiffFile, int tagCode) throws ImageReadException, IOException {
     
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        List<TiffField> tiffFields = metadata.getAllFields();
        for(TiffField tiffField : tiffFields) {
            if(tiffField.getTag() == tagCode) {                
                return tiffField;
            }
        }
        return null;
        
    }
    
    public static TiffField readField(File tiffFile, String tagName) throws ImageReadException, IOException {
     
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        List<TiffField> tiffFields = metadata.getAllFields();
        for(TiffField tiffField : tiffFields) {
            if(tiffField.getTagName().equalsIgnoreCase(tagName)) {                
                return tiffField;
            }
        }
        return null;
        
    }
    
    public static List<TiffField> readAllFields(File tiffFile, int tagCode) throws ImageReadException, IOException {
     
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        List<TiffField> tiffFields = metadata.getAllFields();
        return tiffFields;
        
    }
    
    public static final int FIELDTYPE_SHORT = 3;
    public static final int FIELDTYPE_LONG = 4;
    public static final int TIFFTAG_GDAL_NODATA = 42113;
    public static final int TIFFTAG_GDAL_METADATA = 42112;
    
    public class TiffMetadata {

        public TiffMetadata(Integer colsCount, Integer rowsCount, Resolution resolution, Double noData, Envelope envelope) {
            this.colsCount = colsCount;
            this.rowsCount = rowsCount;
            this.resolution = resolution;
            this.noData = noData;
            this.envelope = envelope;
        }

        public Integer getColsCount() {
            return colsCount;
        }

        public Integer getRowsCount() {
            return rowsCount;
        }
        
        public Resolution getResolution() {
            return resolution;
        }
        
        public Double getNoData() {
            return noData;
        }
        
        public Envelope getEnvelope() {
            return envelope;
        }
        
        private Integer colsCount;
        private Integer rowsCount;
        private Resolution resolution;
        private Double noData;
        private Envelope envelope;
        
    }
    
    public class TiffReadingException extends Exception {

        public TiffReadingException() {
        }

        public TiffReadingException(String message) {
            super(message);
        }
    }
    
}
