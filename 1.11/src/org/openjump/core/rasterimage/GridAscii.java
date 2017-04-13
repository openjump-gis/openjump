package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.Rectangle;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
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
            double xllOrig, double yllOrig, double cellSize, double noData){

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

    public void readGrid(Rectangle subset) throws FileNotFoundException, IOException{

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
        String[] columns;

        int cell = 0;
        cellCount = 0;
        
        int startCol = 0;
        int endCol = nCols;

        if(subset == null) {
            dataArray = new float[nCols*nRows];
        } else {
            dataArray = new float[subset.width*subset.height];
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
            
            row++;
        }
        buffRead.close();

        meanVal = valSum / cellCount;
        stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

        // Create raster
        SampleModel sampleModel;
        if(subset == null) {
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, nCols, nRows, 1);
        } else {
            sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, subset.width, subset.height, 1);
        }
        DataBuffer db = new DataBufferFloat(dataArray, dataArray.length / 4);
        java.awt.Point point = new java.awt.Point();
        point.setLocation(0, 0);
        raster = WritableRaster.createWritableRaster(sampleModel, db, point);
        
    }

    public void writeGrid() throws IOException, Exception{

        // Write header
        FileWriter fileWriter = new FileWriter(new File(ascFullFileName));
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
        
        for(int r=0; r<nRows; r++){
            StringBuffer sb = new StringBuffer();
            for(int c=0; c<nCols; c++){
                
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
        line = "ncols " + Integer.toString(nCols);
        bufferedWriter.write(line + lineFeed);

        line = "nrows " + Integer.toString(nRows);
        bufferedWriter.write(line + lineFeed);

        if(origCorner){
            line = "xllcorner " + (xllCorner);
            bufferedWriter.write(line + lineFeed);

            line = "yllcorner " + (yllCorner);
            bufferedWriter.write(line + lineFeed);
        }else{
            line = "xllcenter " + (xllCorner + 0.5 * cellSize);
            bufferedWriter.write(line + lineFeed);

            line = "yllcenter " + (yllCorner + 0.5 * cellSize);
            bufferedWriter.write(line + lineFeed);
        }

        line = "cellsize " + cellSize;
        bufferedWriter.write(line + lineFeed);

        line = "nodata_value " + Double.toString(noData);
        bufferedWriter.write(line + lineFeed);

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
        BufferedImage image = new BufferedImage(colorModel, WritableRaster.createWritableRaster(raster.getSampleModel(), raster.getDataBuffer(), new Point(0, 0)), false, null);
        return image;

    }

    public double readCellValue(int col, int row) throws FileNotFoundException, IOException {
        
        BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));
        
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

    public void setRas(Raster raster){
        this.raster = raster;
        
        
        cellCount = 0;

        DataBuffer db = raster.getDataBuffer();
        for(int e=0; e<db.getSize(); e++){
            if(db.getElemDouble(e) != noData) cellCount++;
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

    public float[] getFloatArray() {
        return dataArray;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
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
    private Raster raster = null;

    private long   cellCount = 0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = -Double.MAX_VALUE;
    private double meanVal = 0;
    private double stDevVal = 0;
    private boolean isInteger = true;
    private Integer decimalPlaces = 0;
    
    private final String lineFeed = System.getProperty("line.separator");

}
