package org.openjump.core.rasterimage;

import com.vividsolutions.jump.workbench.Logger;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.libtiff.jai.codec.XTIFF;
import org.openjump.core.ccordsys.Unit;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageInputStream;

public class TiffTags {

    /**
     * Read GeoTiff metadata with apache commons-imaging library : neat but can't parse bigtiff
     * @param tiffFile the Tiff file
     * @return the tiff image metadata
     */
    public static TiffMetadata readMetadata(File tiffFile) throws IOException, TiffReadingException, ImageReadException {
        
        Integer colCount = null;
        Integer rowCount = null;
        double noData = -3.40282346639e+038;
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
                    geoAsciiParams = geoAsciiParams.replaceAll("[\\s|_;]+", " ").trim();
                    srsInfo.setDescription(geoAsciiParams);
            }
            
        }
        
        if (tiePoint != null && pixelScale != null && colCount != null && rowCount != null){
            
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
        
        return new TiffMetadata(colCount, rowCount, pixelScale, noData, envelope, srsInfo);
        
    }

    /**
     * Read GeoTiff metadata with imageio-ext library : can read bigtiff
     * TODO not sure it is useful as reader.getImageMetadata(0) fails to read tif with
     *      wrong encoded noDataValue : is there any advantage compared with readMetadata ?
     * @param tiffFile the Tiff file
     * @return the tiff image metadata
     */
    public static TiffMetadata readMetadataWithImageIoExt(File tiffFile) throws IOException {

        Integer colCount = null;
        Integer rowCount = null;
        double noData = -3.40282346639e+038;
        Coordinate pixelOffset = null;
        Coordinate tiePoint = null;
        // use a default value, because sometimes we use tfw georeference but we need to
        // readMetadataWithImageIoExt for other tags and it should not hurt with a NPE
        Resolution pixelScale = new Resolution(1.0,1.0);
        Envelope envelope = null;
        SRSInfo srsInfo = new SRSInfo().setSource(tiffFile.getPath());

        int[] geoKeyDirectoryTag = null;
        double[] geoDoubleParams = null;
        String geoAsciiParams = null;

        //final TIFFImageReadParam param = new TIFFImageReadParam();
        // Instantiation of the file-reader
        TIFFImageReader reader = (TIFFImageReader)new TIFFImageReaderSpi().createReaderInstance();
        // Creation of the file input stream associated to the selected file
        FileImageInputStream stream0 = new FileImageInputStream(tiffFile);
        try {
            // Setting the inputstream to the reader
            reader.setInput(stream0);
            // Reading the metadata of the first image
            TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
            if (metadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH) != null)
                colCount = metadata.getTIFFField(XTIFF.TIFFTAG_IMAGE_WIDTH).getAsInt(0);
            if (metadata.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH) != null)
                rowCount = metadata.getTIFFField(XTIFF.TIFFTAG_IMAGE_LENGTH).getAsInt(0);
            if (metadata.getTIFFField(TIFFTAG_GDAL_NODATA) != null) {
                TIFFField tiffField = metadata.getTIFFField(TIFFTAG_GDAL_NODATA);
                String noDataString = "";
                if(tiffField.getType() == TIFFField.getTypeByName("Ascii")) {
                    noDataString = tiffField.getAsString(0);
                    if(noDataString.equalsIgnoreCase("NaN")) {
                        noDataString = "NaN";
                    }
                } else if(tiffField.getType() == TIFFField.getTypeByName("Byte")) {
                    noDataString = new String(tiffField.getAsBytes());
                }
                noData = Double.parseDouble(noDataString);
            }
            if (metadata.getTIFFField(GeoTiffConstants.ModelTiepointTag) != null) {
                TIFFField tiffField = metadata.getTIFFField(GeoTiffConstants.ModelTiepointTag);
                double[] tiePointValues = tiffField.getAsDoubles();
                pixelOffset = new Coordinate(tiePointValues[0], tiePointValues[1], tiePointValues[2]);
                tiePoint = new Coordinate(tiePointValues[3], tiePointValues[4], tiePointValues[5]);
            }
            if (metadata.getTIFFField(GeoTiffConstants.ModelPixelScaleTag) != null) {
                TIFFField tiffField = metadata.getTIFFField(GeoTiffConstants.ModelPixelScaleTag);
                double[] pixelSCaleValues = tiffField.getAsDoubles();
                if (pixelSCaleValues.length == 2 || pixelSCaleValues[2] == 0) {
                    pixelScale = new Resolution(pixelSCaleValues[0],pixelSCaleValues[1]);
                } else {
                    pixelScale = new Resolution(pixelSCaleValues[0],pixelSCaleValues[1],pixelSCaleValues[2]);
                }
            }
            if (metadata.getTIFFField(GeoTiffConstants.GeoKeyDirectoryTag) != null)
                geoKeyDirectoryTag = metadata.getTIFFField(GeoTiffConstants.GeoKeyDirectoryTag).getAsInts();
            if (metadata.getTIFFField(GeoTiffConstants.GeoDoubleParamsTag) != null)
                geoDoubleParams = metadata.getTIFFField(GeoTiffConstants.GeoDoubleParamsTag).getAsDoubles();
            if (metadata.getTIFFField(GeoTiffConstants.GeoAsciiParamsTag) != null) {
                geoAsciiParams = metadata.getTIFFField(GeoTiffConstants.GeoAsciiParamsTag).getAsString(0);
                geoAsciiParams = geoAsciiParams.replaceAll("[\\s|_;]+", " ").trim();
                srsInfo.setDescription(geoAsciiParams);
            }


        } catch (Exception e) {
            Logger.warn(e.getMessage(), e);
        } finally {
            // Finally, if an exception has been thrown or not, the reader
            // and the input stream are closed
            stream0.flush();
            stream0.close();

            if (reader != null) {
                reader.dispose();
            }
        }

        if (tiePoint != null && pixelScale != null && colCount != null && rowCount != null){

            Coordinate upperLeft, lowerRight;

            if (pixelOffset == null){
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

        return new TiffMetadata(colCount, rowCount, pixelScale, noData, envelope, srsInfo);

    }

    /**
     * Read GeoTiff metadata with robust imageio-ext reader able to
     * - read bigtiff file (commons-imaging and com.github.jaiimageio fail with bigtiff)
     * - read nodata value stored as byte array instead of ascii array (imageio-ext reader.getImageMetadata(fail)
     * @param tiffFile the Tiff file
     * @return the tiff image metadata
     */
    public static TiffMetadata readIIOMetadata(File tiffFile) throws IOException, TiffReadingException {

        Integer colCount = null;
        Integer rowCount = null;
        double noData = -3.40282346639e+038;
        Coordinate pixelOffset = null;
        Coordinate tiePoint = null;
        Resolution pixelScale = null;
        Envelope envelope = null;
        SRSInfo srsInfo = new SRSInfo().setSource(tiffFile.getPath());

        int[] geoKeyDirectoryTag = null;
        double[] geoDoubleParams = null;
        String geoAsciiParams = null;

        // Instantiation of it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader able to read bigtiff
        ImageReader reader = new it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader(new TIFFImageReaderSpi());
        reader.setInput(new FileImageInputStream(tiffFile));
        IIOMetadata metadata = reader.getImageMetadata(0);
        Node root = metadata.getAsTree("it_geosolutions_imageioimpl_plugins_tiff_image_1.0");
        Map<String,Object> tagsByName = new HashMap<>();
        Map<Integer,Object> tagsById = new HashMap<>();
        traverse(root.getFirstChild(), tagsByName, tagsById);
        colCount = ((int[])tagsById.get(256))[0];
        rowCount = ((int[])tagsById.get(257))[0];
        AffineTransform transform = new AffineTransform();
        if (tagsByName.get("ModelPixelScaleTag") != null && tagsByName.get("ModelTiePointTag") != null)
            transform = getTransformFromScaleAndTiePoint(
                (double[]) tagsByName.get("ModelPixelScaleTag"),
                (double[]) tagsByName.get("ModelTiePointTag")
            );
        else throw new TiffReadingException("Cannot read tiff referencing without " +
            "ModelPixelScaleTag and ModelTiePointTag");
        Point2D.Double upperLeft = new Point2D.Double();
        Point2D.Double lowerRight = new Point2D.Double();
        transform.transform(new Point2D.Double(0,0), upperLeft);
        transform.transform(new Point2D.Double(colCount,rowCount), lowerRight);
        envelope = new Envelope(new Coordinate(upperLeft.x, upperLeft.y),
                                        new Coordinate(lowerRight.x, lowerRight.y));
        pixelScale = new Resolution(transform.getScaleX(), transform.getScaleY());
        Object object = tagsById.get(42113);
        String noDataString = "NaN";
        if (object instanceof String[]) {
            noDataString = ((String[])object)[0];
        } else if (object instanceof String[]) {
            noDataString = new String((byte[])object);
        }
        if (!noDataString.toLowerCase(Locale.ROOT).equals("nan")) {
            noData = Double.parseDouble(noDataString);
        }
        srsInfo = new SRSInfo();
        srsInfo.setSource(tiffFile.getPath());
        if (tagsByName.get("GeoAsciiParams") != null)
            srsInfo.setDescription(((String[]) tagsByName.get("GeoAsciiParams"))[0]);
        if (tagsByName.get("GeoKeyDirectory") != null) {
            int[] geoKeyDirectory = (int[]) tagsByName.get("GeoKeyDirectory");
            int srid = -1;
            for (int i = 0; i < geoKeyDirectory.length; i += 4) {
                if (geoKeyDirectory[i] == 3072) srid = geoKeyDirectory[i + 3];
            }
            if (srid > 0) srsInfo.setCode("" + srid);
        }
        return new TiffMetadata(colCount, rowCount, pixelScale, noData,envelope, srsInfo);
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

    // Read Tiff fields with apache commons-imaging
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

    // Read Tiff field with apache commons-imaging
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

    // Read Tiff fields with apache commons-imaging
    public static List<TiffField> readAllFields(File tiffFile, int tagCode) throws ImageReadException, IOException {
     
        TiffImageParser parser = new TiffImageParser();
        TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
        return metadata.getAllFields();

    }
    
    public static final int FIELDTYPE_SHORT = 3;
    public static final int FIELDTYPE_LONG = 4;
    public static final int TIFFTAG_GDAL_NODATA = 42113;
    public static final int TIFFTAG_GDAL_METADATA = 42112;
    
    public static class TiffMetadata extends Metadata {

        public TiffMetadata(Integer colsCount, Integer rowsCount, Resolution resolution, Double noData,
                            Envelope envelope, SRSInfo srsInfo) {
            super(
                envelope,envelope,
                new Point(colsCount, rowsCount),
                new Point(colsCount, rowsCount),
                new Resolution(resolution.getX(), resolution.getY()),
                new Resolution(resolution.getX(), resolution.getY()),
                noData, new Stats(1));
            //this.colsCount = colsCount;
            //this.rowsCount = rowsCount;
            //this.resolution = resolution;
            //this.noData = noData;
            //this.envelope = envelope;
            this.srsInfo = srsInfo;
        }

        //public Integer getColsCount() {
        //    return colsCount;
        //}

        //public Integer getRowsCount() {
        //    return rowsCount;
        //}
        
        //public Resolution getResolution() {
        //    return resolution;
        //}
        
        //public Double getNoData() {
        //    return noData;
        //}
        
        //public Envelope getEnvelope() {
        //    return envelope;
        //}

        public boolean isGeoTiff() {
            return srsInfo != null;
        }

        public SRSInfo getSRSInfo() {
            return srsInfo;
        }
        
        //private final Integer colsCount;
        //private final Integer rowsCount;
        //private final Resolution resolution;
        //private final Double noData;
        //private final Envelope envelope;
        private final SRSInfo srsInfo;
        
    }
    
    public static class TiffReadingException extends Exception {

        public TiffReadingException() {
        }

        public TiffReadingException(String message) {
            super(message);
        }
    }

    // ----------------------------------------------------
    // Added on 2022-05-01
    // ----------------------------------------------------
    public static void traverse(Node root, Map<String, Object> tagsByName, Map<Integer, Object> tagsById) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            NamedNodeMap attributes = child.getAttributes();
            String name = null;
            int id = -1;
            Object array;
            if (attributes.getNamedItem("name") != null) {
                name = attributes.getNamedItem("name").getNodeValue();
            }
            if (attributes.getNamedItem("number") != null) {
                id = Integer.parseInt(attributes.getNamedItem("number").getNodeValue());
            }
            Node typeChild = child.getFirstChild();
            switch (typeChild.getNodeName()) {
                case "TIFFShorts":
                    array = getShortValues(typeChild);
                    break;
                case "TIFFLongs":
                    array = getLongValues(typeChild);
                    break;
                case "TIFFDoubles":
                    array = getDoubleValues(typeChild);
                    break;
                case "TIFFAsciis":
                    array = getAsciiValues(typeChild);
                    break;
                default:
                    array = null;
            }
            if (name != null) tagsByName.put(name, array);
            if (id != -1) tagsById.put(id, array);
        }
    }

    private static int[] getShortValues(Node node) {
        NodeList children = node.getChildNodes();
        int[] array = new int[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("TIFFShort"))
                array[i] = Integer.parseInt(child.getAttributes().getNamedItem("value").getNodeValue());
            else
                throw new IllegalArgumentException("Try to read " + child.getNodeName() + " with getShortValues");
        }
        return array;
    }

    private static long[] getLongValues(Node node) {
        NodeList children = node.getChildNodes();
        long[] array = new long[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("TIFFLong"))
                array[i] = Long.parseLong(child.getAttributes().getNamedItem("value").getNodeValue());
            else
                throw new IllegalArgumentException("Try to read " + child.getNodeName() + " with getLongValues");
        }
        return array;
    }

    private static double[] getDoubleValues(Node node) {
        NodeList children = node.getChildNodes();
        double[] array = new double[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("TIFFDouble"))
                array[i] = Double.parseDouble(child.getAttributes().getNamedItem("value").getNodeValue());
            else
                throw new IllegalArgumentException("Try to read " + child.getNodeName() + " with getDoubleValues");
        }
        return array;
    }

    private static String[] getAsciiValues(Node node) {
        NodeList children = node.getChildNodes();
        String[] array = new String[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("TIFFAscii"))
                array[i] = child.getAttributes().getNamedItem("value").getNodeValue();
            else
                throw new IllegalArgumentException("Try to read " + child.getNodeName() + " with getAsciiValues");
        }
        return array;
    }

    public static AffineTransform getTransformFromScaleAndTiePoint(double[] scale, double[] tiePoint) {
        // http://geotiff.maptools.org/spec/geotiff2.6.html - 2.6.3 - case 3
        // http://docs.opengeospatial.org/DRAFTS/YY-nnnrx.html
        double scaleX = scale[0];
        double scaleY = scale[1];
        double I = tiePoint[0];
        double J = tiePoint[1];
        double X = tiePoint[3];
        double Y = tiePoint[4];
        double Tx = X - I / scaleX;
        double Ty = Y + J / scaleY;
        return new AffineTransform(scaleX, 0.0, 0.0, scaleY, Tx, Ty);
    }
    
}
