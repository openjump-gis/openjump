package org.openjump.core.rasterimage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.READ;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.libtiff.jai.codec.XTIFF;

public class TiffTags {
    
    public enum FieldType {
        BYTE, ASCII, SHORT, LONG, RATIONAL,
        SBYTE, UNDEFINED, SSHORT,
        SLONG, SRATIONAL, FLOAT, DOUBLE
    }
    
    private static final Map<Integer, FieldType> fieldCodeToType;
    private static final Map<FieldType, Integer> fieldTypeToBytesCount;
    
    static {
        
        fieldCodeToType = new HashMap<Integer, FieldType>();
        fieldCodeToType.put(1, FieldType.BYTE);
        fieldCodeToType.put(2, FieldType.ASCII);
        fieldCodeToType.put(3, FieldType.SHORT);
        fieldCodeToType.put(4, FieldType.LONG);
        fieldCodeToType.put(5, FieldType.RATIONAL);
        fieldCodeToType.put(6, FieldType.SBYTE);
        fieldCodeToType.put(7, FieldType.UNDEFINED);
        fieldCodeToType.put(8, FieldType.SSHORT);
        fieldCodeToType.put(9, FieldType.SLONG);
        fieldCodeToType.put(10, FieldType.SRATIONAL);
        fieldCodeToType.put(11, FieldType.FLOAT);
        fieldCodeToType.put(12, FieldType.DOUBLE);
        
        fieldTypeToBytesCount = new EnumMap<FieldType, Integer>(FieldType.class);
        fieldTypeToBytesCount.put(FieldType.BYTE, 1);
        fieldTypeToBytesCount.put(FieldType.ASCII, 1);
        fieldTypeToBytesCount.put(FieldType.SHORT, 2);
        fieldTypeToBytesCount.put(FieldType.LONG, 4);
        fieldTypeToBytesCount.put(FieldType.RATIONAL, 8);
        fieldTypeToBytesCount.put(FieldType.SBYTE, 1);
        fieldTypeToBytesCount.put(FieldType.UNDEFINED, 1);
        fieldTypeToBytesCount.put(FieldType.SSHORT, 2);
        fieldTypeToBytesCount.put(FieldType.SLONG, 4);
        fieldTypeToBytesCount.put(FieldType.SRATIONAL, 64);
        fieldTypeToBytesCount.put(FieldType.FLOAT, 4);
        fieldTypeToBytesCount.put(FieldType.DOUBLE, 8);
        
    }
    
    public static TiffMetadata readTags(File file) throws FileNotFoundException, IOException, TiffReadingException {
        
        FileChannel fc = (FileChannel.open(file.toPath(), READ));
        
        byte endianness1 = readByte(fc);
        byte endianness2 = readByte(fc);
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        
        if(endianness1 == 73 && endianness2 == 73) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else if(endianness1 == 77 && endianness2 == 77) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else {
            throw new TiffTags().new TiffReadingException("Something wrong with endianness.");
        }
        
        int fourtyTwo = readUnsignedShort(fc, byteOrder);
        
        if(fourtyTwo != 42) {
            throw new TiffTags().new TiffReadingException("Wrong TIFF number. 42 expected but found: " + fourtyTwo);
        }
        
        int offset = readInt(fc, byteOrder);        
        fc.position(offset);
        
        int directoriesCount = readUnsignedShort(fc, byteOrder);
        
        Integer colCount = null;
        Integer rowCount = null;
        Double noData = -3.40282346639e+038;;
        Coordinate pixelOffset = null;
        Coordinate tiePoint = null;
        Resolution pixelScale = null;
        Envelope envelope = null;
        
        for(int d=0; d<directoriesCount; d++) {
        
            int tag = readUnsignedShort(fc, byteOrder);
            int fieldTypeCode = readUnsignedShort(fc, byteOrder);
            FieldType fieldType = fieldCodeToType.get(fieldTypeCode);
            int valuesCount = readInt(fc, byteOrder);
            int valueOffset = readInt(fc, byteOrder);
            int valueBytes = valuesCount * fieldTypeToBytesCount.get(fieldType);
            
            // Columns
            if(tag == XTIFF.TIFFTAG_IMAGE_WIDTH) {
                colCount = valueOffset;
                continue;
            }
            
            // Rows
            if(tag == XTIFF.TIFFTAG_IMAGE_LENGTH) {
                rowCount = valueOffset;
                continue;
            }
            
            // Cell size
//            if(tag == GeoTiffConstants.ModelPixelScaleTag) {
//                cellSize = readDouble(fc, byteOrder, valueOffset);
//                continue;
//            }
            
            // No data
            if(tag == TIFFTAG_GDAL_NODATA) {
                
                if(valueBytes <= 4) {
                    fc.position(fc.position() - 4);
                    
                    byte[] noDataBytes = new byte[valuesCount];
                    for(int p=0; p<valuesCount; p++) {

                        noDataBytes[p] = readByte(fc);

                    }

                    String noDataS = new String(noDataBytes);
                    
                    try {
                        noData = Double.parseDouble(noDataS);
                    } catch (NumberFormatException ex) {
                        // Standard ESRI nodata value
                        noData = -3.40282346639e+038;
                    }
                        
                } else {
                    
                    fc.position(valueOffset);

                    byte[] noDataBytes = new byte[valuesCount];
                    for(int p=0; p<valuesCount; p++) {

                        noDataBytes[p] = readByte(fc);

                    }

                    String noDataS = new String(noDataBytes);
                    noData = Double.parseDouble(noDataS);
                    
                }
                continue;
            }
            
            // Tie point
            if(tag == GeoTiffConstants.ModelTiepointTag) {
                
                double[] values = new double[valuesCount];
                for(int v=0; v<values.length; v++) {
                    values[v] = readDouble(fc, byteOrder, valueOffset + v * fieldTypeToBytesCount.get(fieldType));
                }

                pixelOffset = new Coordinate(values[0], values[1], values[2]);     
                tiePoint = new Coordinate(values[3], values[4], values[5]);       
                
                continue;
            }        
        
            if(tag == GeoTiffConstants.ModelPixelScaleTag) {
                
                double[] values = new double[valuesCount];
                for(int v=0; v<values.length; v++) {
                    values[v] = readDouble(fc, byteOrder, valueOffset + v * fieldTypeToBytesCount.get(fieldType));
                }
                
                if (values.length == 2 || values[2] == 0) {
                    pixelScale = new Resolution(values[0],values[1]);
                } else {
                    pixelScale = new Resolution(values[0],values[1],values[2]);
                }
                continue;
            }
            
            // Read to skip
//            int bytesToSkip = valuesCount * fieldTypeToBytesCount.get(fieldType);
//            fc.position(fc.position() + bytesToSkip);
//            if(bytesToSkip <= 4) {
//                for(int b=0; b<bytesToSkip; b++) {
//                    readByte(fc);
//                }
//            }
        }
        
        if (tiePoint != null && pixelScale != null){
            
            Coordinate upperLeft, lowerRight;
                  
            if (pixelOffset==null){
                upperLeft = tiePoint;
            } else {
                upperLeft = new Coordinate( tiePoint.x - (pixelOffset.x * pixelScale.getX()), tiePoint.y - (pixelOffset.y * pixelScale.getY()));
            }

            lowerRight = new Coordinate( upperLeft.x + (colCount * pixelScale.getX()), upperLeft.y - (rowCount * pixelScale.getY()));

            //logger.printDebug("upperLeft: " + upperLeft);
            //logger.printDebug("lowerRight: " + lowerRight);

            envelope = new Envelope(upperLeft, lowerRight);
                  
        }
        
        
        return new TiffTags().new TiffMetadata(colCount, rowCount, pixelScale, noData, envelope);
        
    }

    private static byte readByte(FileChannel fc) throws IOException {
    
        ByteBuffer bb = ByteBuffer.allocate(1);
        fc.read(bb);
    
        return bb.get(0); 
        
    }
    
    private static short readShort(FileChannel fc, ByteOrder byteOrder) throws IOException {
        
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(byteOrder);
        fc.read(bb);
        return bb.getShort(0);
        
    }
    
    private static int readUnsignedShort(FileChannel fc, ByteOrder byteOrder) throws IOException {
        
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(byteOrder);
        fc.read(bb);
        return bb.getShort(0) & 0xffff;
        
    }
    
    private static int readInt(FileChannel fc, ByteOrder byteOrder) throws IOException {
        
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(byteOrder);
        fc.read(bb);
        return bb.getInt(0);
        
    }
    
    private static double readDouble(FileChannel fc, ByteOrder byteOrder, long valueOffset) throws IOException {
        
        long oldPos = fc.position();
        
        fc.position(valueOffset);
        
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(byteOrder);
        fc.read(bb);
        
        fc.position(oldPos);
        
        return bb.getDouble(0);
        
    }
    
    private static char readChar(FileChannel fc, ByteOrder byteOrder) throws IOException {
        
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.order(byteOrder);
        fc.read(bb);
        return bb.getChar(0);
        
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
