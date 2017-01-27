package org.openjump.core.rasterimage;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.swing.JOptionPane;

public class GridFloat {

    public GridFloat(String fltFullFileName){

        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";
        readHdr();

    }

    public GridFloat(String fltFullFileName, GridFloat gridFloat2){

        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";

        this.nCols = gridFloat2.getnCols();
        this.nRows = gridFloat2.getnRows();
        this.xllCorner = gridFloat2.getXllCorner();
        this.yllCorner = gridFloat2.getYllCorner();
        this.cellSize = gridFloat2.getCellSize();
        this.noData = gridFloat2.getNoData();
        this.byteOrder = gridFloat2.getByteOrder();

    }

    public GridFloat(String fltFullFileName, int nCols, int nRows, boolean origCorner,
            double xllOrig, double yllOrig, double cellSize, double noData, String byteOrder){

        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";

        this.nCols = nCols;
        this.nRows = nRows;
        this.origCorner = origCorner;
        if(origCorner){
            this.xllCorner = xllOrig;
            this.yllCorner = yllOrig;
        }else{
            this.xllCorner = xllOrig - 0.5*cellSize;
            this.yllCorner = yllOrig - 0.5*cellSize;
        }
        this.cellSize = cellSize;
        this.noData = noData;

        if(byteOrder.toLowerCase().equals("lsbfirst") && byteOrder.toLowerCase().equals("lsblast")){
            this.byteOrder = "LSBFIRST";
        }else{
            this.byteOrder = byteOrder;
        }
        
    }

    private int readHdr(){

        try{
            String line = null;
            BufferedReader buffRead = new BufferedReader(new FileReader(hdrFullFileName));
            while((line = buffRead.readLine()) != null){
                String[] lines = line.split(" +");
                if(lines[0].toLowerCase().equals("ncols")){
                    nCols = Integer.parseInt(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("nrows")){
                    nRows = Integer.parseInt(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("xllcorner")){
                    xllCorner = Double.parseDouble(lines[1].toString());
                    origCorner = true;
                }else if(lines[0].toLowerCase().equals("yllcorner")){
                    yllCorner = Double.parseDouble(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("xllcenter")){
                    xllCorner = Double.parseDouble(lines[1].toString());
                    origCorner = false;
                }else if(lines[0].toLowerCase().equals("yllcenter")){
                    yllCorner = Double.parseDouble(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("cellsize")){
                    cellSize = Double.parseDouble(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("nodata_value")){
                    noData = Double.parseDouble(lines[1].toString());
                }else if(lines[0].toLowerCase().equals("byteorder")){
                    byteOrder = lines[1].toString();
                }
            }
            buffRead.close();
            buffRead = null;

            if(!origCorner){
                xllCorner =- 0.5*cellSize;
                yllCorner =- 0.5*cellSize;
            }

            return 0;
        }catch(IOException Ex){
            JOptionPane.showMessageDialog(null, "Error while reading hdr file: " + Ex, "Error", JOptionPane.ERROR_MESSAGE);
            return 1;
        }

    }

    public int writeHdr(){

        try{
            File fileHeader = new File(hdrFullFileName);
            BufferedWriter buffWrite = new BufferedWriter(new FileWriter(fileHeader));

            buffWrite.write("ncols" + " " + nCols);
            buffWrite.newLine();

            buffWrite.write("nrows" + " " + nRows);
            buffWrite.newLine();

            if(origCorner){

                buffWrite.write("xllcorner" + " " + xllCorner);
                buffWrite.newLine();

                buffWrite.write("yllcorner" + " " + yllCorner);
                buffWrite.newLine();

            }else{

                buffWrite.write("xllcenter" + " " + xllCorner + 0.5*cellSize);
                buffWrite.newLine();

                buffWrite.write("yllcenter" + " " + yllCorner + 0.5*cellSize);
                buffWrite.newLine();

            }

            buffWrite.write("cellsize" + " " + cellSize);
            buffWrite.newLine();

            buffWrite.write("NODATA_value" + " " + noData);
            buffWrite.newLine();

            buffWrite.write("byteorder" + " " + byteOrder);
            buffWrite.newLine();

            buffWrite.close();
            buffWrite = null;

           return 0;
       }catch(IOException IOExc){
            JOptionPane.showMessageDialog(null, "Error while reading hdr file: " + IOExc, "Error", JOptionPane.ERROR_MESSAGE);
            return 1;
       }

    }

    public int readGrid(){

        int ret = readHdr();
        if(ret != 0) return 1;

        double valSum = 0;
        double valSumSquare = 0;
        minVal = Double.MAX_VALUE;
        maxVal = -minVal;

        try{

            File fileFlt = new File(fltFullFileName);
            FileInputStream fileInStream = new FileInputStream(fileFlt);
            FileChannel fileChannel = fileInStream.getChannel();
            long length = fileFlt.length();
            MappedByteBuffer mbb;
            mbb = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    length
                    );
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            fileChannel.close();
            fileInStream.close();

            int i = 0;
            dataArray = new float[nCols*nRows];
            for(int c=0; c<nCols*nRows; c++){
                dataArray[c] = mbb.getFloat(i);
                if(dataArray[c] != noData) {
                    valSum += dataArray[c];
                    valSumSquare += (dataArray[c] * dataArray[c]);
                    cellCount++;
                    if(dataArray[c] < minVal){minVal = dataArray[c];}
                    if(dataArray[c] > maxVal){maxVal = dataArray[c];}
                    if((int)dataArray[c] != dataArray[c]) isInteger = false;
                }
                i+=4;
            }

            meanVal = valSum / cellCount;
            stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

            mbb=null;

            // Create raster
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols, nRows, 1);
            DataBuffer db = new DataBufferFloat(dataArray, nCols*nRows);
            java.awt.Point point = new java.awt.Point();
            point.setLocation(xllCorner, yllCorner);
            raster = RasterFactory.createRaster(sampleModel, db, point);

            return 0;
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Error while reading the flt file: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
    
    }

    public int writeGrid(){

        try{

            if(raster == null){
                // Create raster
                SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols, nRows, 1);
                dataArray = new float[nCols*nRows];
                DataBuffer db = new DataBufferFloat(dataArray, nCols*nRows);
                java.awt.Point point = new java.awt.Point();
                point.setLocation(xllCorner, yllCorner);
                raster = RasterFactory.createRaster(sampleModel, db, point);
            }

            if(writeHdr() != 0) return -1;

            File fileOut = new File(fltFullFileName);
            FileOutputStream fileOutStream = new FileOutputStream(fileOut);
            FileChannel fileChannelOut = fileOutStream.getChannel();


            ByteBuffer bb = ByteBuffer.allocateDirect(nCols * 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for(int r=0; r<nRows; r++){
                for(int c=0; c<nCols; c++){
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

            return 0;

        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Error while reading flt file: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
            return 1;
        }

    }
    
    public void setFltFullFileName(String fltFullFileName){
        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";
    }

    public boolean isSpatiallyEqualTo(GridFloat gridFloat2){

        boolean isEqual = true;
        if(nCols != gridFloat2.getnCols()) isEqual = false;
        if(nRows != gridFloat2.getnRows()) isEqual = false;
        if(origCorner != gridFloat2.getOrigCorner()) isEqual = false;
        if(xllCorner != gridFloat2.getXllCorner()) isEqual = false;
        if(yllCorner != gridFloat2.getYllCorner()) isEqual = false;
        if(cellSize != gridFloat2.getCellSize()) isEqual = false;
        if(noData != gridFloat2.getNoData()) isEqual = false;
        if(!byteOrder.equals(gridFloat2.getByteOrder())) isEqual = false;

        return isEqual;

    }

    public javax.media.jai.PlanarImage getPlanarImage (){

        try{

            // Create sample model
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols, nRows, 1);

            // Create tiled image
            TiledImage tiledImage = new TiledImage(0, 0, nCols, nRows, 0, 0, sampleModel, null);

            // Create writebaleraster
            WritableRaster wraster = tiledImage.getWritableTile(0,0);

            // Set raster data
            wraster.setPixels(0, 0, nCols, nRows, dataArray);

            // Set image raster
            tiledImage.setData(wraster);


            return tiledImage;
        }catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }

    public int getnCols() {
        return nCols;
    }

    public void setnCols(int nCols) {
        this.nCols = nCols;
    }

    public int getnRows() {
        return nRows;
    }

    public void setnRows(int nRows) {
        this.nRows = nRows;
    }

    public double getXllCorner() {
        return xllCorner;
    }

    public void setXllCorner(double xllCorner) {
        this.xllCorner = xllCorner;
    }

    public double getYllCorner() {
        return yllCorner;
    }

    public void setYllCorner(double yllCorner) {
        this.yllCorner = yllCorner;
    }

    public boolean getOrigCorner(){
        return origCorner;
    }

    public void setOrigCorner(boolean origCorner){
        this.origCorner = origCorner;
    }

    public double getCellSize() {
        return cellSize;
    }

    public void setCellSize(double cellSize) {
        this.cellSize = cellSize;
    }

    public double getNoData() {
        return noData;
    }

    public void setNoData(double noData) {
        this.noData = noData;
    }

    public String getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(String byteOrder) {
        this.byteOrder = byteOrder;
    }

    public Raster getRaster(){
        return raster;
    }

    public void setRas(Raster raster){
        this.raster = raster;

        cellCount = 0;

        DataBuffer db = raster.getDataBuffer();
        for(int e=0; e<db.getSize(); e++){
            if(db.getElemFloat(e) != noData) cellCount++;
        }
    }

    public double getMinVal(){
        return minVal;
    }

    public double getMaxVal(){
        return maxVal;
    }

    public double getMeanVal(){
        return meanVal;
    }

    public double getStDevVal(){
        return stDevVal;
    }

    public long getCellCount(){
        return cellCount;
    }

    public boolean isInteger(){
        return isInteger;
    }

    private String fltFullFileName = null;
    private String hdrFullFileName = null;

    private int nCols = 0;
    private int nRows = 0;
    private double xllCorner = 0;
    private double yllCorner = 0;
//    private double xllCenter = 0;
//    private double yllCenter = 0;
    private double cellSize = 0;
    private double noData = -9999;
    private String byteOrder = "LSBFIRST";

    private boolean origCorner = true;

//    private double[][] ras = null;
    private float[] dataArray = null;
    private Raster raster = null;

    private long   cellCount = 0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = -Double.MAX_VALUE;
    private double meanVal = 0;
    private double stDevVal = 0;
    private boolean isInteger = true;

    public static final String LSBFIRST = "LSBFIRST";
    public static final String MSBFIRST = "MSBFIRST";

}
