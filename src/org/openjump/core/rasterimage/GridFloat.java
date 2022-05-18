package org.openjump.core.rasterimage;

import org.locationtech.jts.geom.Coordinate;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

public class GridFloat extends Grid {

    public static final String LSBFIRST = "LSBFIRST";
    public static final String MSBFIRST = "MSBFIRST";

    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private float[] dataArray = null;
    private boolean isInteger = true;
    private boolean llCellCorner = true;

    public GridFloat(String fileName) throws IOException{
        super(fileName);
    }

    private String getHdr() {
        return getFileName().substring(0, getFileName().lastIndexOf(".")) + ".hdr";
    }

    //public GridFloat(String fileName, int cols, int rows, Coordinate ll,
    //                 double res, double noData, ByteOrder byteOrder) throws IOException{
    //    super(fileName, new Grid.Header(cols,rows,ll, new Resolution(res,res), noData));
    //    this.byteOrder = byteOrder;
    //    hdrFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".hdr";
    //    readHdr();
    //}

    public GridFloat(String fileName, GridFloat gridFloat) throws IOException{
        super(fileName, gridFloat);
    }

    //public float[] getDataArray() {
    //    return dataArray;
    //}

    public boolean isLlCellCorner() {
        return llCellCorner;
    }

    public boolean isInteger(){
        return isInteger;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public float[] getFloatArray() {
        return dataArray;
    }

    //public GridFloat(String fltFullFileName, GridFloat gridFloat2){
//
    //    this.fltFullFileName = fltFullFileName;
    //    hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";
//
    //    this.nCols = gridFloat2.getnCols();
    //    this.nRows = gridFloat2.getnRows();
    //    this.xllCorner = gridFloat2.getXllCorner();
    //    this.yllCorner = gridFloat2.getYllCorner();
    //    this.cellSize = gridFloat2.getCellSizeX();
    //    this.noData = gridFloat2.getNoData();
    //    this.byteOrder = gridFloat2.getByteOrder();
//
    //}

    //public GridFloat(String fltFullFileName, int nCols, int nRows, boolean origCorner,
    //        double xllOrig, double yllOrig, double cellSize, double noData, ByteOrder byteOrder){
//
    //    this.fltFullFileName = fltFullFileName;
    //    hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";
//
    //    this.nCols = nCols;
    //    this.nRows = nRows;
    //    this.origCorner = origCorner;
    //    if(origCorner){
    //        this.xllCorner = xllOrig;
    //        this.yllCorner = yllOrig;
    //    }else{
    //        this.xllCorner = xllOrig - 0.5*cellSize;
    //        this.yllCorner = yllOrig - 0.5*cellSize;
    //    }
    //    this.cellSize = cellSize;
    //    this.noData = noData;
//
    //    this.byteOrder = byteOrder;
    //
//  //      if(byteOrder.toLowerCase().equals("lsbfirst") && byteOrder.toLowerCase().equals("lsblast")){
//  //          this.byteOrder = "LSBFIRST";
//  //      }else{
//  //          this.byteOrder = byteOrder;
//  //      }
    //
    //}

    @Override
    protected void readHeader() throws IOException{

        String line;
        BufferedReader buffRead = new BufferedReader(new FileReader(getHdr()));
        int nCols = 0, nRows = 0;
        double xllCorner = 0.0, yllCorner = 0.0;
        double cellSize = 0.0, noData = -9999.0;
        boolean llCellCorner = true;
        ByteOrder byteOrder = null;
        while((line = buffRead.readLine()) != null){
            String[] lines = line.split(" +");
            if(lines[0].equalsIgnoreCase("ncols")){
                nCols = Integer.parseInt(lines[1]);
            }else if(lines[0].equalsIgnoreCase("nrows")){
                nRows = Integer.parseInt(lines[1]);
            }else if(lines[0].equalsIgnoreCase("xllcorner")){
                xllCorner = Double.parseDouble(lines[1]);
                llCellCorner = true;
            }else if(lines[0].equalsIgnoreCase("yllcorner")){
                yllCorner = Double.parseDouble(lines[1]);
            }else if(lines[0].equalsIgnoreCase("xllcenter")){
                xllCorner = Double.parseDouble(lines[1]);
                llCellCorner = false;
            }else if(lines[0].equalsIgnoreCase("yllcenter")){
                yllCorner = Double.parseDouble(lines[1]);
            }else if(lines[0].equalsIgnoreCase("cellsize")){
                cellSize = Double.parseDouble(lines[1]);
            }else if(lines[0].equalsIgnoreCase("nodata_value")){
                noData = Double.parseDouble(lines[1]);
            }else if(lines[0].equalsIgnoreCase("byteorder")){
                if(lines[1].equalsIgnoreCase(MSBFIRST)) {
                    byteOrder = ByteOrder.BIG_ENDIAN;
                } else if(lines[1].equalsIgnoreCase(LSBFIRST)) {
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                }
            }
        }
        buffRead.close();

        if(!llCellCorner){
            xllCorner =- 0.5*cellSize;
            yllCorner =- 0.5*cellSize;
        }

        Grid.Header header = new Grid.Header(nCols, nRows, new Coordinate(xllCorner, yllCorner),
            new Resolution(cellSize,cellSize), noData);
        setHeader(header);
        this.byteOrder = byteOrder;
    }

    public void writeHdr() throws IOException{

        File fileHeader = new File(getFileName());
        FileWriter fileWriter = new FileWriter(fileHeader);
        BufferedWriter buffWrite = new BufferedWriter(fileWriter);

        buffWrite.write("ncols" + " " + getColNumber());
        buffWrite.newLine();

        buffWrite.write("nrows" + " " + getRowNumber());
        buffWrite.newLine();

        if(llCellCorner){

            buffWrite.write("xllcorner" + " " + getLlCorner().getX());
            buffWrite.newLine();

            buffWrite.write("yllcorner" + " " + getLlCorner().getY());
            buffWrite.newLine();

        }else{

            buffWrite.write("xllcenter" + " " + getLlCorner().getX() + 0.5*getResolution().getX());
            buffWrite.newLine();

            buffWrite.write("yllcenter" + " " + getLlCorner().getY() + 0.5*getResolution().getY());
            buffWrite.newLine();

        }

        buffWrite.write("cellsize" + " " + getResolution().getX());
        buffWrite.newLine();

        buffWrite.write("NODATA_value" + " " + getNoDataValue());
        buffWrite.newLine();

        buffWrite.write("byteorder" + " " + byteOrder);
        buffWrite.newLine();

        buffWrite.close();
        fileWriter.close();

    }

    public void readGrid(Rectangle subset) throws IOException{

        readHeader();

        double valSum = 0;
        double valSumSquare = 0;
        double minVal = Double.MAX_VALUE;
        double maxVal = -minVal;

        File fileFlt = new File(getFileName());
        FileInputStream fileInStream = new FileInputStream(fileFlt);
        FileChannel fileChannel = fileInStream.getChannel();
        int cellCount = 0;
        if(subset == null) {
        
            dataArray = new float[getColNumber()*getRowNumber()];
            
            long length = fileFlt.length();
            MappedByteBuffer mbb;
            mbb = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    length
                    );
            mbb.order(byteOrder);

            int i = 0;

            for(int p=0; p<getColNumber()*getRowNumber(); p++){
                dataArray[p] = mbb.getFloat(i);
                if(dataArray[p] != getNoDataValue()) {
                    valSum += dataArray[p];
                    valSumSquare += (dataArray[p] * dataArray[p]);
                    cellCount++;
                    if(dataArray[p] < minVal){minVal = dataArray[p];}
                    if(dataArray[p] > maxVal){maxVal = dataArray[p];}
                    if((int)dataArray[p] != dataArray[p]) isInteger = false;
                }
                i+=4;
            }

            double meanVal = valSum / cellCount;
            double stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

        } else {
            
            dataArray = new float[subset.width*subset.height];
            
            long length = subset.width * 4;
            MappedByteBuffer mbb;
            
            for(int r=0; r<subset.height; r++) {
                long position = (subset.y + r) * getColNumber() * 4 + subset.x * 4;
                mbb = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    position,
                    length
                    );
                mbb.order(byteOrder);
                
                for(int c=0; c<subset.width; c++){
                    int p = r*subset.width + c;
                    dataArray[p] = mbb.getFloat();
                    if(dataArray[p] != getNoDataValue()) {
                        valSum += dataArray[p];
                        valSumSquare += (dataArray[p] * dataArray[p]);
                        cellCount++;
                        if(dataArray[p] < minVal){minVal = dataArray[p];}
                        if(dataArray[p] > maxVal){maxVal = dataArray[p];}
                        if((int)dataArray[p] != dataArray[p]) isInteger = false;
                    }
                }
                
            }
            
        }

        fileChannel.close();
        fileInStream.close();
        
        // Create raster
        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT,
            getColNumber(), getRowNumber(), 1);
        DataBuffer db = new DataBufferFloat(dataArray, dataArray.length / 4);
        java.awt.Point point = new java.awt.Point();
        point.setLocation(0, 0);
        raster = WritableRaster.createWritableRaster(sampleModel, db, point);
    
    }

    public void writeGrid() throws IOException{

        if(raster == null){
            // Create raster
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(
                DataBuffer.TYPE_DOUBLE, getColNumber(), getRowNumber(), 1);
            dataArray = new float[getColNumber()*getRowNumber()];
            DataBuffer db = new DataBufferFloat(dataArray, getColNumber()*getRowNumber());
            java.awt.Point point = new java.awt.Point();
            point.setLocation(getLlCorner().getX(), getLlCorner().getY());
            raster = RasterFactory.createRaster(sampleModel, db, point).createCompatibleWritableRaster();
        }

        writeHdr();

        File fileOut = new File(getFileName());
        FileOutputStream fileOutStream = new FileOutputStream(fileOut);
        FileChannel fileChannelOut = fileOutStream.getChannel();


        ByteBuffer bb = ByteBuffer.allocateDirect(getColNumber() * 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        for(int r=0; r<getRowNumber(); r++){
            for(int c=0; c<getColNumber(); c++){
                if(bb.hasRemaining()){
                    bb.putFloat(raster.getSampleFloat(c, r, 0));
                }else{
                    c--;
                    bb.compact();
                    fileChannelOut.write(bb);
                    bb.clear();
                }
            }
        }

        bb.compact();
        fileChannelOut.write(bb);
        bb.clear();
        fileChannelOut.close();
        fileOutStream.close();

    }
    
    //public void setFltFullFileName(String fltFullFileName){
    //    this.fltFullFileName = fltFullFileName;
    //    hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";
    //}

    //public boolean isSpatiallyEqualTo(GridFloat gridFloat2){
//
    //    boolean isEqual = true;
    //    if(nCols != gridFloat2.getnCols()) isEqual = false;
    //    if(nRows != gridFloat2.getnRows()) isEqual = false;
    //    if(origCorner != gridFloat2.getOrigCorner()) isEqual = false;
    //    if(xllCorner != gridFloat2.getXllCorner()) isEqual = false;
    //    if(yllCorner != gridFloat2.getYllCorner()) isEqual = false;
    //    if(cellSize != gridFloat2.getCellSizeX()) isEqual = false;
    //    if(noData != gridFloat2.getNoData()) isEqual = false;
    //    if(!byteOrder.equals(gridFloat2.getByteOrder())) isEqual = false;
//
    //    return isEqual;
//
    //}

    public BufferedImage getBufferedImage (){

        SampleModel sm = raster.getSampleModel();
        ColorModel colorModel = PlanarImage.createColorModel(sm);
        BufferedImage image = new BufferedImage(colorModel,
            WritableRaster.createWritableRaster(sm, raster.getDataBuffer(),
                new Point(0,0)), false, null);
        return image;

    }

    public double readCellVal(Integer col, Integer row) throws FileNotFoundException, IOException{

        long offset = (row * getColNumber() + col) * 4;

        File fileFlt = new File(getFileName());
        FileInputStream fileInStream = new FileInputStream(fileFlt);
        FileChannel fileChannel = fileInStream.getChannel();
        long length = 4;
        MappedByteBuffer mbb;
        mbb = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                offset,
                length
                );
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.close();
        fileInStream.close();

        return mbb.getFloat();

    }
    
    //public int getnCols() {
    //    return nCols;
    //}

    //public void setnCols(int nCols) {
    //    this.nCols = nCols;
    //}

    //public int getnRows() {
    //    return nRows;
    //}

    //public void setnRows(int nRows) {
    //    this.nRows = nRows;
    //}

    //public double getXllCorner() {
    //    return xllCorner;
    //}

    //public void setXllCorner(double xllCorner) {
    //    this.xllCorner = xllCorner;
    //}

    //public double getYllCorner() {
    //    return yllCorner;
    //}

    //public void setYllCorner(double yllCorner) {
    //    this.yllCorner = yllCorner;
    //}

    //public boolean getOrigCorner(){
    //    return origCorner;
    //}

    //public void setOrigCorner(boolean origCorner){
    //    this.origCorner = origCorner;
    //}

    //public Point2D.Double getCellSize() {
    //    return new Point2D.Double(cellSize, cellSize);
    //}

    //public double getCellSizeX() {
    //    return cellSize;
    //}

    //public double getCellSizeY() {
    //    return cellSize;
    //}


    //public void setCellSize(double cellSize) {
    //    this.cellSize = cellSize;
    //}

    //public double getNoData() {
    //    return noData;
    //}

    //public void setNoData(double noData) {
    //    this.noData = noData;
    //}

    //public ByteOrder getByteOrder() {
    //    return byteOrder;
    //}

    //public void setByteOrder(ByteOrder byteOrder) {
    //    this.byteOrder = byteOrder;
    //}

    @Override
    public Raster getRaster(){
        return raster;
    }

    //public void setRas(Raster raster){
    //    this.raster = raster;
//
    //    cellCount = 0;
//
    //    DataBuffer db = raster.getDataBuffer();
    //    for(int e=0; e<db.getSize(); e++){
    //        if(db.getElemDouble(e) != noData) cellCount++;
    //    }
    //}

    //public double getMinVal(){
    //    return minVal;
    //}

    //public double getMaxVal(){
    //    return maxVal;
    //}

    //public double getMeanVal(){
    //    return meanVal;
    //}

    //public double getStDevVal(){
    //    return stDevVal;
    //}

    //public long getCellCount(){
    //    return cellCount;
    //}

    //public boolean isInteger(){
    //    return isInteger;
    //}
    
    //public float[] getFloatArray() {
    //    return dataArray;
    //}

    //private String fltFullFileName = null;
    //private String hdrFullFileName = null;

    //private int nCols = 0;
    //private int nRows = 0;
    //private double xllCorner = 0;
    //private double yllCorner = 0;
    //private double cellSize = 0;
    //private double noData = -9999;


    //private boolean origCorner = true;

//    private double[][] ras = null;
    //private float[] dataArray = null;
    //private Raster raster = null;

    //private long   cellCount = 0;
    //private double minVal = Double.MAX_VALUE;
    //private double maxVal = -Double.MAX_VALUE;
    //private double meanVal = 0;
    //private double stDevVal = 0;
    //private boolean isInteger = true;

}
