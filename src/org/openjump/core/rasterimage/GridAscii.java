package org.openjump.core.rasterimage;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

public class GridAscii {

    public GridAscii(String ascFullFileName) throws IOException{
        this.ascFullFileName = ascFullFileName;
        readHeader();
    }


    public GridAscii(String ascFullFileName, GridAscii gridAscii2){

        this.ascFullFileName = ascFullFileName;

        this.nCols = gridAscii2.getnCols();
        this.nRows = gridAscii2.getnRows();
        this.xllCorner = gridAscii2.getXllCorner();
        this.yllCorner = gridAscii2.getYllCorner();
        this.cellSize = gridAscii2.getCellSize();
        this.noData = gridAscii2.getNoData();

    }

    public GridAscii(String ascFullFileName, int nCols, int nRows, boolean origCorner,
            double xllOrig, double yllOrig, double cellSize, double noData, String byteOrder){

        this.ascFullFileName = ascFullFileName;

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

    }

    public final void readHeader() throws FileNotFoundException, IOException{

        BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));

        String line;
        String[] lines;

        String[] header = new String[6];

        for(int l=0; l<6; l++){

            line = buffRead.readLine();
            lines = line.split(" +");

            if(lines[0].trim().toLowerCase().equals("ncols")){
                header[0] = lines[1];
            }

            if(lines[0].trim().toLowerCase().equals("nrows")){
                header[1] = lines[1];
            }

            if(lines[0].trim().toLowerCase().equals("xllcorner")){
                header[2] = lines[1];
                origCorner = true;
            }
            if(lines[0].trim().toLowerCase().equals("yllcorner")){
                header[3] = lines[1];
                origCorner = true;
            }
            if(lines[0].trim().toLowerCase().equals("xllcenter")){
                header[2] = lines[1];
                origCorner = false;
            }
            if(lines[0].trim().toLowerCase().equals("yllcenter")){
                header[3] = lines[1];
                origCorner = false;
            }

            if(lines[0].trim().toLowerCase().equals("cellsize")){
                header[4] = lines[1];
            }
            if(lines[0].trim().toLowerCase().equals("nodata_value")){
                header[5] = lines[1];
            }
        }

        buffRead.close();

        nCols = Integer.parseInt(header[0]);
        nRows = Integer.parseInt(header[1]);
        xllCorner = Double.parseDouble(header[2]);
        yllCorner = Double.parseDouble(header[3]);
        cellSize = Double.parseDouble(header[4]);
        noData = Double.parseDouble(header[5]);


        // From corner to center, if needed
        if(!origCorner){
            xllCorner = xllCorner + 0.5 * cellSize;
            yllCorner = yllCorner + 0.5 * cellSize;
        }

    }

    public void readGrid() throws FileNotFoundException, IOException{

        readHeader();

        double valSum = 0;
        double valSumSquare = 0;
        minVal = Double.MAX_VALUE;
        maxVal = -minVal;


        BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));

        // Skip header
        for(int l=0; l<=5; l++){
            buffRead.readLine();
        }

        // Read remaining part of grids
        String dtmLine;
        String[] dtmLines;

        int col = 0;
        int row = 0;

        // Read DTM
        int line = 0;
        int cell = 0;
        cellCount = 0;
        dataArray = new float[nCols*nRows];
        while((dtmLine = buffRead.readLine()) != null){
            dtmLine = dtmLine.trim();
            dtmLines = dtmLine.split(" +");
            for(int c=0; c<dtmLines.length; c++){
//                    row = (cell/nCols);
//                    col = cell - (row * nCols) + 1;
                dataArray[cell] = Float.parseFloat(dtmLines[c]);
//                    ras[col][row] = Double.parseDouble(dtmLines[c]);
                if(dataArray[cell] != noData) {
                    valSum += dataArray[cell];
                    valSumSquare += (dataArray[cell] * dataArray[cell]);
                    cellCount++;
                    if(dataArray[cell] < minVal){minVal = dataArray[cell];}
                    if(dataArray[cell] > maxVal){maxVal = dataArray[cell];}
                    if((int)dataArray[cell] != dataArray[cell]) isInteger = false;
                }
                cell++;
            }
            line++;
        }
        buffRead.close();

        meanVal = valSum / cellCount;
        stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

        // Create raster
        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, nCols, nRows, 1);
        DataBuffer db = new DataBufferFloat(dataArray, nCols*nRows);
        java.awt.Point point = new java.awt.Point();
        point.setLocation(0, 0);
        raster = WritableRaster.createWritableRaster(sampleModel, db, point);
        
    }

    public void setHeaderEqualTo(GridAscii gridAscii){

        this.nCols = gridAscii.getnCols();
        this.nRows = gridAscii.getnRows();
        this.xllCorner = gridAscii.getXllCorner();
        this.yllCorner = gridAscii.getYllCorner();
        this.cellSize = gridAscii.getCellSize();
        this.noData = gridAscii.getNoData();
        this.origCorner = gridAscii.origCorner;
        
    }

    public boolean isSpatiallyEqualTo(GridAscii gridAscii2){

        boolean isEqual = true;
        if(nCols != gridAscii2.getnCols()) isEqual = false;
        if(nRows != gridAscii2.getnRows()) isEqual = false;
        if(origCorner != gridAscii2.getOrigCorner()) isEqual = false;
        if(xllCorner != gridAscii2.getXllCorner()) isEqual = false;
        if(yllCorner != gridAscii2.getYllCorner()) isEqual = false;
        if(cellSize != gridAscii2.getCellSize()) isEqual = false;
        if(noData != gridAscii2.getNoData()) isEqual = false;

        return isEqual;

    }

    public BufferedImage getBufferedImage() throws IOException{

        SampleModel sm = raster.getSampleModel();
        ColorModel colorModel = PlanarImage.createColorModel(sm);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        return image;
        
//        // Create sample model
//        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, nCols, nRows, 1);
//
//        // Create tiled image
//        TiledImage tiledImage = new TiledImage(0, 0, nCols, nRows, 0, 0, sampleModel, null);
//
//        // Create writebaleraster
//        WritableRaster wraster = tiledImage.getWritableTile(0,0);
//
//        // Set raster data
//        wraster.setPixels(0, 0, nCols, nRows, ((DataBufferDouble)readGrid().getDataBuffer()).getData());
//
//        // Set image raster
//        tiledImage.setData(wraster);
//
//
//        return tiledImage;
    }

    public double readCellValue(int col, int row) throws FileNotFoundException, IOException {
        
        BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));
        
        // Skip rows
        for(int r=0; r<row; r++) {
            buffRead.readLine();
        }
        
        String[] cellVals = buffRead.readLine().trim().split(" +");
        
        return Double.parseDouble(cellVals[col]);
        
        
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

    public Raster getRaster(){
        return raster;
    }

//    public void setRas(Raster raster){
//        this.raster = raster;
//
//        cellCount = 0;
//
//        DataBuffer db = raster.getDataBuffer();
//        for(int e=0; e<db.getSize(); e++){
//            if(db.getElemDouble(e) != noData) cellCount++;
//        }
//    }

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

    public float[] getFloatArray() {
        return dataArray;
    }

    private String ascFullFileName = null;

    private boolean origCorner = false;
    private int nCols = 0;
    private int nRows = 0;
    private double xllCorner = 0;
    private double yllCorner = 0;
    private double cellSize = 0;
    private double noData = -9999;

    private float[] dataArray = null;
    private WritableRaster raster = null;

    private long   cellCount = 0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = -Double.MAX_VALUE;
    private double meanVal = 0;
    private double stDevVal = 0;
    private boolean isInteger = true;

}
