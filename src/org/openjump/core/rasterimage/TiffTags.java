package org.openjump.core.rasterimage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.libtiff.jai.codec.XTIFF;
import org.openjump.core.ccordsys.Unit;
import org.openjump.core.ccordsys.utils.SRSInfo;

public class TiffTags {
    
    public static TiffMetadata readMetadata(File tiffFile) throws IOException, TiffReadingException, ImageReadException {
        
        Integer colCount = null;
        Integer rowCount = null;
        Double noData = -3.40282346639e+038;
        Coordinate pixelOffset = null;
        Coordinate tiePoint = null;
        Resolution pixelScale = null;
        Envelope envelope = null;
        SRSInfo srsInfo = new SRSInfo().setSource(tiffFile.getPath());
        
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        List<TiffField> tiffFields = metadata.getAllFields();
        int[] geoKeyDirectoryTag = null;
        double[] geoDoubleParams = null;
        String geoAsciiParams = null;
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
                case GeoTiffConstants.GeoKeyDirectoryTag:
                    geoKeyDirectoryTag = tiffField.getIntArrayValue();
                    break;
                case GeoTiffConstants.GeoDoubleParamsTag:
                    geoDoubleParams = tiffField.getDoubleArrayValue();
                    break;
                case GeoTiffConstants.GeoAsciiParamsTag:
                    geoAsciiParams = tiffField.getStringValue();
                    geoAsciiParams = geoAsciiParams.replaceAll("[\\s\\|_;]+", " ").trim();
                    srsInfo.setDescription(geoAsciiParams);
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

        if (geoKeyDirectoryTag != null && geoKeyDirectoryTag.length >= 4) {
            readGeoKeys(geoKeyDirectoryTag, geoDoubleParams, geoAsciiParams, srsInfo);
            srsInfo.complete();
        } else {
            srsInfo = null;
        }
        
        return new TiffTags().new TiffMetadata(colCount, rowCount, pixelScale, noData, envelope, srsInfo);
        
    }


    private static void readGeoKeys(int[] geoKeys, double[] geoDoubleParams, String geoAsciiParams, SRSInfo srsInfo) {
        int numberOfKeys = geoKeys[3];
        for (int i = 1 ; i <= numberOfKeys ; i++) {
            int keyID = geoKeys[i*4];
            int location = geoKeys[i*4+1];
            int count = geoKeys[i*4+2];
            int offset = geoKeys[i*4+3];
            Object value;
            switch(keyID) {
                case GeoTiffConstants.GTModelTypeGeoKey:
                    int coordSystemType = offset;
                    // default unit for geographic CRS
                    if (coordSystemType == GeoTiffConstants.ModelTypeGeographic)
                        srsInfo.setUnit(Unit.DEGREE);
                    // default unit for projected CRS
                    else if (coordSystemType == GeoTiffConstants.ModelTypeProjected)
                        srsInfo.setUnit(Unit.METRE);
                    break;
                //case GeoTiffConstants.GTRasterTypeGeoKey:
                //    srsInfo.rasterType = offset;
                //    break;
                case GeoTiffConstants.GTCitationGeoKey:
                    break;
                case GeoTiffConstants.GeographicTypeGeoKey:  // 2048
                case GeoTiffConstants.ProjectedCSTypeGeoKey: // 3072
                    value = getGeoValue(location, count, offset, geoDoubleParams, geoAsciiParams);
                    if (value instanceof String)
                        srsInfo.setDescription((String)value);
                    else if (value instanceof Integer) {
                        if ((Integer)value < 32767) {
                            srsInfo.setRegistry(SRSInfo.Registry.EPSG).setCode(value.toString());
                        } else if ((Integer)value == 32767) {
                            srsInfo.setRegistry(SRSInfo.Registry.SRID).setCode(SRSInfo.USERDEFINED);
                        }
                    }
                    break;
                case GeoTiffConstants.GeogCitationGeoKey:
                    break;
                case GeoTiffConstants.PCSCitationGeoKey:
                    break;
                case GeoTiffConstants.VerticalCSTypeGeoKey:
                    break;
                case GeoTiffConstants.GeogLinearUnitsGeoKey:
                case GeoTiffConstants.GeogAngularUnitsGeoKey:
                case GeoTiffConstants.ProjLinearUnitsGeoKey:
                    srsInfo.setUnit(Unit.find(Integer.toString(offset)));
            }
        }
    }

    private static Object getGeoValue(int location, int count, int offset,
                                      double[] geoDoubleParams, String geoAsciiParams) {
        if (location == 0) return offset;
        else if (location == GeoTiffConstants.GeoDoubleParamsTag) {
            if (count == 0) return new double[0];
            double[] dd = new double[count];
            System.arraycopy(geoDoubleParams, offset, dd, 0, count);
            return dd;
        }
        else if (location == GeoTiffConstants.GeoAsciiParamsTag) {
            if (geoAsciiParams == null || geoAsciiParams.length() == 0) return "";
            String substring = geoAsciiParams.substring(offset, offset + count);
            return substring.endsWith("|") ? substring.substring(0, substring.length()-1) : substring;
        }
        else return "";
    }
    
    static TiffField readField(File tiffFile, int tagCode) throws ImageReadException, IOException {
     
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

        public TiffMetadata(Integer colsCount, Integer rowsCount, Resolution resolution, Double noData,
                            Envelope envelope, SRSInfo srsInfo) {
            this.colsCount = colsCount;
            this.rowsCount = rowsCount;
            this.resolution = resolution;
            this.noData = noData;
            this.envelope = envelope;
            this.srsInfo = srsInfo;
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

        public boolean isGeoTiff() {
            return srsInfo != null;
        }

        public SRSInfo getSRSInfo() {
            return srsInfo;
        }
        
        private Integer colsCount;
        private Integer rowsCount;
        private Resolution resolution;
        private Double noData;
        private Envelope envelope;
        private SRSInfo srsInfo;
        
    }
    
    public class TiffReadingException extends Exception {

        public TiffReadingException() {
        }

        public TiffReadingException(String message) {
            super(message);
        }
    }
    
}
