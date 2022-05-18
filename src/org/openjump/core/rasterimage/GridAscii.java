package org.openjump.core.rasterimage;

import org.locationtech.jts.geom.Coordinate;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

public class GridAscii extends Grid {

    private static final String lineFeed = System.getProperty("line.separator");

    private float[] dataArray = null;
    private boolean llCellCorner = true;
    private boolean isInteger = true;
    private Integer decimalPlaces = 0;

    public GridAscii(String fileName) throws IOException {
        super(fileName);
    }

    //public GridAscii(String fileName, int cols, int rows, Coordinate ll, double res, double noData) {
    //    super(fileName, new Grid.Header(cols,rows,ll, new Resolution(res,res), noData));
    //}

    public GridAscii(String fileName, GridAscii ascii) {
        super(fileName, ascii);
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

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public float[] getFloatArray() {
        return dataArray;
    }

    //public GridAscii(String ascFullFileName) throws IOException{
    //    this.ascFullFileName = ascFullFileName;
    //    readHeader();
    //}


    //public GridAscii(String ascFullFileName, GridAscii gridAscii2){
//
    //    this.ascFullFileName = ascFullFileName;
//
    //    this.nCols = gridAscii2.getnCols();
    //    this.nRows = gridAscii2.getnRows();
    //    this.xllCorner = gridAscii2.getXllCorner();
    //    this.yllCorner = gridAscii2.getYllCorner();
    //    this.cellSize = gridAscii2.getCellSize();
    //    this.noData = gridAscii2.getNoData();
//
    //}

    //public GridAscii(String ascFullFileName, int nCols, int nRows, boolean origCorner,
    //        double xllOrig, double yllOrig, double cellSize, double noData){
//
    //    this.ascFullFileName = ascFullFileName;
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
    //    this.cellSize = new Resolution(cellSize, cellSize);
    //    this.noData = noData;
//
    //}

    @Override
    protected void readHeader() throws IOException{

        BufferedReader buffRead = new BufferedReader(new FileReader(getFileName()));

        String line;
        String[] lines;

        String[] header = new String[6];
        //Boolean origCorner = null;

        for(int l=0; l<6; l++){

            line = buffRead.readLine();
            lines = line.split(" +");

            if(lines[0].trim().equalsIgnoreCase("ncols")){
                header[0] = lines[1];
            }

            if(lines[0].trim().equalsIgnoreCase("nrows")){
                header[1] = lines[1];
            }

            if(lines[0].trim().equalsIgnoreCase("xllcorner")){
                header[2] = lines[1];
                llCellCorner = true;
            }
            if(lines[0].trim().equalsIgnoreCase("yllcorner")){
                header[3] = lines[1];
                llCellCorner = true;
            }
            if(lines[0].trim().equalsIgnoreCase("xllcenter")){
                header[2] = lines[1];
                llCellCorner = false;
            }
            if(lines[0].trim().equalsIgnoreCase("yllcenter")){
                header[3] = lines[1];
                llCellCorner = false;
            }

            if(lines[0].trim().equalsIgnoreCase("cellsize")){
                header[4] = lines[1];
            }
            if(lines[0].trim().equalsIgnoreCase("nodata_value")){
                header[5] = lines[1];
            }
        }

        buffRead.close();

        int nCols = Integer.parseInt(header[0]);
        int nRows = Integer.parseInt(header[1]);
        double xllCorner = Double.parseDouble(header[2]);
        double yllCorner = Double.parseDouble(header[3]);
        double cellSize = Double.parseDouble(header[4]);
        double noData = Double.parseDouble(header[5]);

        // From corner to center, if needed
        if(!llCellCorner){
            xllCorner = xllCorner + 0.5 * cellSize;
            yllCorner = yllCorner + 0.5 * cellSize;
        }
        setHeader(new Grid.Header(nCols, nRows,
            new Coordinate(xllCorner, yllCorner),
            new Resolution(cellSize, cellSize), noData)
        );
    }

    public void readGrid(Rectangle subset) throws IOException{

        readHeader();

        double valSum = 0;
        double valSumSquare = 0;
        double minVal = Double.MAX_VALUE;
        double maxVal = -minVal;

        BufferedReader buffRead = new BufferedReader(new FileReader(getFileName()));

        // Skip header
        for(int l=0; l<=5; l++){
            buffRead.readLine();
        }

        // Read remaining part of grids
        String dtmLine;
        String[] columns;

        int cell = 0;
        int cellCount = 0;
        
        int startCol = 0;
        int endCol = getColNumber();

        if(subset == null) {
            dataArray = new float[getColNumber() * getRowNumber()];
        } else {
            dataArray = new float[subset.width * subset.height];
            startCol = subset.x;
            endCol = subset.x + subset.width;
        }
            
        int row = 0;
        while((dtmLine = buffRead.readLine()) != null){
            
            if(subset != null) {
                if(row < subset.y || row >= subset.y + subset.height) {
                    row++;
                    continue;
                }
            }
            
            dtmLine = dtmLine.trim();
            columns = dtmLine.split(" +");
            
            for (int c=startCol; c<endCol; c++) {

                dataArray[cell] = Float.parseFloat(columns[c]);
                if(dataArray[cell] != getNoDataValue()) {
                    valSum += dataArray[cell];
                    valSumSquare += (dataArray[cell] * dataArray[cell]);
                    cellCount++;
                    if(dataArray[cell] < minVal){minVal = dataArray[cell];}
                    if(dataArray[cell] > maxVal){maxVal = dataArray[cell];}
                    if((int)dataArray[cell] != dataArray[cell]) isInteger = false;
                }
                cell++;
            }
            
            row++;
        }
        buffRead.close();

        double meanVal = valSum / cellCount;
        double stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

        // Create raster
        SampleModel sampleModel;
        if(subset == null) {
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, getColNumber(), getRowNumber(), 1);
        } else {
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, subset.width, subset.height, 1);
        }
        DataBuffer db = new DataBufferFloat(dataArray, dataArray.length / 4);
        java.awt.Point point = new java.awt.Point();
        point.setLocation(0, 0);
        raster = WritableRaster.createWritableRaster(sampleModel, db, point);
        statistics = new BasicStatistics(
            getRowNumber()*getColNumber(),minVal, maxVal, meanVal, stDevVal);
        
    }

    public void writeGrid() throws Exception{

        // Write header
        FileWriter fileWriter = new FileWriter(getFileName());
        BufferedWriter buffw = new BufferedWriter(fileWriter);

        writeHeader(buffw);

        // Write data ------------------------------------------------------
        NumberFormat numberFormat = null;
        if(decimalPlaces != null && decimalPlaces > 0) {
            String pattern = "0.";
            for(int p=0; p<decimalPlaces; p++) {
                pattern = pattern.concat("0");
            }
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
            numberFormat = new DecimalFormat(pattern, decimalFormatSymbols);
        }
        
        for(int r=0; r<getRowNumber(); r++){
            StringBuilder sb = new StringBuilder();
            for(int c=0; c<getColNumber(); c++){
                
                if(numberFormat == null){
                    sb.append(" ").append(raster.getSampleFloat(c, r, 0));
                } else {
                    sb.append(" ").append(numberFormat.format(raster.getSampleFloat(c, r, 0)));
                }

            }
            buffw.write(sb + lineFeed);
        }

        buffw.close();
        fileWriter.close();
        
    }
    
    public void writeHeader(BufferedWriter bufferedWriter) throws Exception {
        
        String line;
        line = "ncols " + getColNumber();
        bufferedWriter.write(line + lineFeed);

        line = "nrows " + getRowNumber();
        bufferedWriter.write(line + lineFeed);

        if(llCellCorner){
            line = "xllcorner " + (getLlCorner().x);
            bufferedWriter.write(line + lineFeed);

            line = "yllcorner " + (getLlCorner().y);
            bufferedWriter.write(line + lineFeed);
        } else {
            line = "xllcenter " + (getLlCorner().x + 0.5 * getResolution().getX());
            bufferedWriter.write(line + lineFeed);

            line = "yllcenter " + (getLlCorner().y + 0.5 * getResolution().getY());
            bufferedWriter.write(line + lineFeed);
        }

        line = "cellsize " + getResolution().getX();
        bufferedWriter.write(line + lineFeed);

        line = "nodata_value " + getNoDataValue();
        bufferedWriter.write(line + lineFeed);

    }
    
    //public void setHeaderEqualTo(GridAscii gridAscii){
//
    //    this.nCols = gridAscii.getnCols();
    //    this.nRows = gridAscii.getnRows();
    //    this.xllCorner = gridAscii.getXllCorner();
    //    this.yllCorner = gridAscii.getYllCorner();
    //    this.cellSize = gridAscii.getCellSizeX();
    //    this.noData = gridAscii.getNoData();
    //    this.origCorner = gridAscii.origCorner;
    //
    //}

    //public boolean isSpatiallyEqualTo(GridAscii gridAscii2){
    //
    //    return getHeader().equals(gridAscii2.getHeader());
    //    //boolean isEqual = true;
    //    //if(getColNumber() != gridAscii2.getColNumber()) isEqual = false;
    //    //if(getRowNumber() != gridAscii2.getRowNumber()) isEqual = false;
    //    //if(!getLlCorner().equals(gridAscii2.getLlCorner())) isEqual = false;
    //    //if(getResolution().getX() != gridAscii2.getResolution().getX()) isEqual = false;
    //    //if(noData != gridAscii2.getNoData()) isEqual = false;
    //    //return isEqual;
    //}

    public BufferedImage getBufferedImage() throws IOException{
        SampleModel sm = raster.getSampleModel();
        ColorModel colorModel = PlanarImage.createColorModel(sm);
        return new BufferedImage(colorModel, WritableRaster.createWritableRaster(
            raster.getSampleModel(), raster.getDataBuffer(), new Point(0, 0)),
            false, null);
    }

    public double readCellValue(int col, int row) throws IOException {
        
        BufferedReader buffRead = new BufferedReader(new FileReader(getFileName()));
        
        // Skip header
        for(int r=0; r<6; r++) {
            buffRead.readLine();
        }
        
        // Skip rows
        for(int r=0; r<row; r++) {
            buffRead.readLine();
        }
        
        String[] cellVals = buffRead.readLine().trim().split(" +");
        
        return Double.parseDouble(cellVals[col]);

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

    //public Resolution getCellSize() {
    //    return new Point2D.Double(cellSize, cellSize);
    //}

    //public void setCellSize(double cellSize) {
    //    this.cellSize = new Resolution(cellSize,cellSize);
    //}

    //public double getNoData() {
    //    return noData;
    //}

    //public void setNoData(double noData) {
    //    this.noData = noData;
    //}

    //public Raster getRaster(){
    //    return raster;
    //}

    //public void setRas(Raster raster){
    //    this.raster = raster;
    //
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

    //public float[] getFloatArray() {
    //    return dataArray;
    //}

    //public void setDecimalPlaces(Integer decimalPlaces) {
    //    this.decimalPlaces = decimalPlaces;
    //}
    
    //private String ascFullFileName = null;

    //private boolean origCorner = false;
    //private int nCols = 0;
    //private int nRows = 0;
    //private double xllCorner = 0;
    //private double yllCorner = 0;
    //private Resolution cellSize = new Resolution(0,0);
    //private double noData = -9999;

    //private Raster raster = null;

    //private long   cellCount = 0;
    //private double minVal = Double.MAX_VALUE;
    //private double maxVal = -Double.MAX_VALUE;
    //private double meanVal = 0;
    //private double stDevVal = 0;


}
