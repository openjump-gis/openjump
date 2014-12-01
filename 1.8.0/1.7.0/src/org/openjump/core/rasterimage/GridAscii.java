package org.openjump.core.rasterimage;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.FileReader;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

public class GridAscii {

    public GridAscii(String ascFullFileName){
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

    public final int readHeader(){
        try{

            BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));

            String line = null;
            String[] lines = null;

            int nDecimalsXll = 0;
            int nDecimalsYll = 0;
            int nDecimalsCellSize = 0;

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

                    nDecimalsXll = lines[1].length() - lines[1].lastIndexOf(".") - 1;
                }
                if(lines[0].trim().toLowerCase().equals("yllcorner")){
                    header[3] = lines[1];
                    origCorner = true;

                    nDecimalsYll = lines[1].length() - lines[1].lastIndexOf(".") - 1;
                }
                if(lines[0].trim().toLowerCase().equals("xllcenter")){
                    header[2] = lines[1];
                    origCorner = false;

                    nDecimalsXll = lines[1].length() - lines[1].lastIndexOf(".") - 1;
                }
                if(lines[0].trim().toLowerCase().equals("yllcenter")){
                    header[3] = lines[1];
                    origCorner = false;

                    nDecimalsYll = lines[1].length() - lines[1].lastIndexOf(".") - 1;
                }

                if(lines[0].trim().toLowerCase().equals("cellsize")){
                    header[4] = lines[1];

                    nDecimalsCellSize = lines[1].length() - lines[1].lastIndexOf(".") - 1;
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

            return 0;

        }catch(Exception ex){
            return 1;
        }

    }

    public int readGrid(){

        int ret = readHeader();
        if(ret != 0) return 1;

        double valSum = 0;
        double valSumSquare = 0;
        minVal = Double.MAX_VALUE;
        maxVal = -minVal;

        try{

            BufferedReader buffRead = new BufferedReader(new FileReader(ascFullFileName));

            // Skip header
            for(int l=0; l<=5; l++){
                buffRead.readLine();
            }

            // Read remaining part of grids
            String dtmLine = null;
            String[] dtmLines = null;

            int col = 0;
            int row = 0;

            // Read DTM
            int line = 0;
            int cell = 0;
            cellCount = 0;
            dataArray = new double[nCols*nRows];
            while((dtmLine = buffRead.readLine()) != null){
                dtmLine = dtmLine.trim();
                dtmLines = dtmLine.split(" +");
                for(int c=0; c<dtmLines.length; c++){
//                    row = (cell/nCols);
//                    col = cell - (row * nCols) + 1;
                    dataArray[cell] = Double.parseDouble(dtmLines[c]);
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
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols, nRows, 1);
            DataBuffer db = new DataBufferDouble(dataArray, nCols*nRows);
            java.awt.Point point = new java.awt.Point();
            point.setLocation(xllCorner, yllCorner);
            raster = RasterFactory.createRaster(sampleModel, db, point);

            return 0;
        }catch(Exception ex){
            return 1;
        }

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



    private String ascFullFileName = null;

    private boolean origCorner = false;
    private int nCols = 0;
    private int nRows = 0;
    private double xllCorner = 0;
    private double yllCorner = 0;
    private double cellSize = 0;
    private double noData = -9999;

    private double[] dataArray = null;
    private Raster raster = null;

    private long   cellCount = 0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = -Double.MAX_VALUE;
    private double meanVal = 0;
    private double stDevVal = 0;
    private boolean isInteger = true;

}
