package org.openjump.core.rasterimage;

import com.vividsolutions.jts.geom.Coordinate;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
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

        ras = new double[nCols+2][nRows+2];
        for(int r=0; r<nRows+2; r++){
            for(int c=0; c<nCols+2; c++){
                ras[c][r] = noData;
            }
        }

    }

    public GridFloat(String fltFullFileName, GridFloat gridFloat2){

        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";

        this.setHeaderEqualTo(gridFloat2);
        ras = new double[nCols+2][nRows+2];
        for(int r=0; r<nRows+2; r++){
            for(int c=0; c<nCols+2; c++){
                ras[c][r] = noData;
            }
        }

    }

    public GridFloat(String fltFullFileName, int nCols, int nRows, boolean origCorner, double xllOrig, double yllOrig, double cellSize, double noData, String byteOrder){

        this.fltFullFileName = fltFullFileName;
        hdrFullFileName = fltFullFileName.substring(0, fltFullFileName.lastIndexOf(".")) + ".hdr";

        this.nCols = nCols;
        this.nRows = nRows;
        this.origCorner = origCorner;
        if(origCorner){
            this.xllCorner = xllOrig;
            this.yllCorner = yllOrig;
        }else{
            this.xllCenter = xllOrig;
            this.yllCenter = yllOrig;
        }
        this.cellSize = cellSize;
        this.noData = noData;

        if(byteOrder.toLowerCase().equals("lsbfirst") && byteOrder.toLowerCase().equals("lsblast")){
            this.byteOrder = "LSBFIRST";
        }else{
            this.byteOrder = byteOrder;
        }

        ras = new double[nCols+2][nRows+2];
        for(int r=0; r<nRows+2; r++){
            for(int c=0; c<nCols+2; c++){
                ras[c][r] = noData;
            }
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
                    xllCenter = Double.parseDouble(lines[1].toString());
                    origCorner = false;
                }else if(lines[0].toLowerCase().equals("yllcenter")){
                    yllCenter = Double.parseDouble(lines[1].toString());
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

                buffWrite.write("xllcenter" + " " + xllCenter);
                buffWrite.newLine();

                buffWrite.write("yllcenter" + " " + yllCenter);
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
            for(int r=1; r<=nRows; r++){
                for(int c=1; c<=nCols; c++){
                    ras[c][r] = mbb.getFloat(i);
                    if(ras[c][r] != noData) {
                        valSum += ras[c][r];
                        valSumSquare += (ras[c][r] * ras[c][r]);
                        cellCount++;
                        if(ras[c][r] < minVal){minVal = ras[c][r];}
                        if(ras[c][r] > maxVal){maxVal = ras[c][r];}
                        if((int)ras[c][r] != ras[c][r]) isInteger = false;
                    }
                    i = i + 4;
                }
            }

            meanVal = valSum / cellCount;
            stDevVal = Math.sqrt(valSumSquare/cellCount - meanVal*meanVal);

            mbb=null;
            return 0;
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Error while reading the flt file: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
    
    }

    public int writeGrid(){

        try{

            if(ras == null){
                ras = new double[nCols+2][nRows+2];
            }

            if(writeHdr() != 0) return -1;

            File fileOut = new File(fltFullFileName);
            FileOutputStream fileOutStream = new FileOutputStream(fileOut);
            FileChannel fileChannelOut = fileOutStream.getChannel();


            ByteBuffer bb = ByteBuffer.allocateDirect(nCols * 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for(int r=1; r<=nRows; r++){
                for(int c=1; c<=nCols; c++){
                    if(bb.hasRemaining()){

                        bb.putFloat((float)ras[c][r]);
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

    public double readCellVal(Coordinate coord){

        try{

            if(coord.x - xllCorner < 0 || coord.y - yllCorner <0 ||
                    coord.x >= xllCorner + nCols * cellSize || coord.y >= yllCorner + nRows * cellSize){
                return noData;
            }

            int col = (int)((coord.x - xllCorner) / cellSize) + 1;
            int row = (int)((coord.y - yllCorner) / cellSize) + 1;
            row = nRows - row + 1;

            long offset = ((row - 1) * nCols + col - 1) * 4;

            File fileFlt = new File(fltFullFileName);
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

            return (double)mbb.getFloat();

        }catch(Exception ex){
            System.out.println(ex);
            return noData;
        }

    }

    public void toOrigCenter(){

        if(origCorner){
            xllCenter = xllCorner + 0.5 * cellSize;
            yllCenter = yllCorner + 0.5 * cellSize;
            origCorner = false;
        }
        
    }

    public void toOrigCorner(){

        if(!origCorner){
            xllCorner = xllCenter - 0.5 * cellSize;
            yllCorner = yllCenter - 0.5 * cellSize;
            origCorner = true;
        }

    }

    public void setHeaderEqualTo(GridFloat gridFlt){

        this.nCols = gridFlt.getnCols();
        this.nRows = gridFlt.getnRows();
        if(origCorner){
            this.xllCorner = gridFlt.getXllCorner();
            this.yllCorner = gridFlt.getYllCorner();
        }else{
            this.xllCenter = gridFlt.getXllCenter();
            this.yllCenter = gridFlt.getYllCenter();
        }
        this.cellSize = gridFlt.getCellSize();
        this.noData = gridFlt.getNoData();
        this.byteOrder = gridFlt.getByteOrder();

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
        if(origCorner){
            if(xllCorner != gridFloat2.getXllCorner()) isEqual = false;
            if(yllCorner != gridFloat2.getYllCorner()) isEqual = false;
        }else{
            if(xllCorner != gridFloat2.getXllCorner()) isEqual = false;
            if(yllCorner != gridFloat2.getYllCorner()) isEqual = false;
        }
        if(cellSize != gridFloat2.getCellSize()) isEqual = false;
        if(noData != gridFloat2.getNoData()) isEqual = false;
        if(!byteOrder.equals(gridFloat2.getByteOrder())) isEqual = false;

        return isEqual;

    }

    public javax.media.jai.PlanarImage getPlanarImage (){

        try{

            // Data array
            float[] dataArray = new float[nCols*nRows];
            int aPos = 0;
            for(int r=1; r<=nRows; r++){
                for(int c=1; c<=nCols; c++){
                    dataArray[aPos] = (float)ras[c][r];
                    aPos++;
                }
            }

            // Create sample model
            SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols, nRows, 1);

            // Create tiled image
            TiledImage tiledImage = new TiledImage(0,0,nCols,nRows,0,0,sampleModel,null);

            // Create writebaleraster
            WritableRaster raster = tiledImage.getWritableTile(0,0);

            // Set raster data
            raster.setPixels(0, 0, nCols, nRows, dataArray);

            // Set image raster
            tiledImage.setData(raster);


            return tiledImage;
        }catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }


      private ColorModel generateColorModel() {
        // Generate 16-color model
        byte[] r = new byte[16];
        byte[] g = new byte[16];
        byte[] b = new byte[16];

        r[0] = 0; g[0] = 0; b[0] = 0;
        r[1] = 0; g[1] = 0; b[1] = (byte)192;
        r[2] = 0; g[2] = 0; b[2] = (byte)255;
        r[3] = 0; g[3] = (byte)192; b[3] = 0;
        r[4] = 0; g[4] = (byte)255; b[4] = 0;
        r[5] = 0; g[5] = (byte)192; b[5] = (byte)192;
        r[6] = 0; g[6] = (byte)255; b[6] = (byte)255;
        r[7] = (byte)192; g[7] = 0; b[7] = 0;
        r[8] = (byte)255; g[8] = 0; b[8] = 0;
        r[9] = (byte)192; g[9] = 0; b[9] = (byte)192;
        r[10] = (byte)255; g[10] = 0; b[10] = (byte)255;
        r[11] = (byte)192; g[11] = (byte)192; b[11] = 0;
        r[12] = (byte)255; g[12] = (byte)255; b[12] = 0;
        r[13] = (byte)80; g[13] = (byte)80; b[13] = (byte)80;
        r[14] = (byte)192; g[14] = (byte)192; b[14] = (byte)192;
        r[15] = (byte)255; g[15] = (byte)255; b[15] = (byte)255;

        ColorModel colorModel = new IndexColorModel(4, 16, r, g, b);
        return colorModel;
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

    public double getXllCenter() {
        return xllCenter;
    }

    public void setXllCenter(double xllCenter) {
        this.xllCenter = xllCenter;
    }

    public double getXllCorner() {
        return xllCorner;
    }

    public void setXllCorner(double xllCorner) {
        this.xllCorner = xllCorner;
    }

    public double getYllCenter() {
        return yllCenter;
    }

    public void setYllCenter(double yllCenter) {
        this.yllCenter = yllCenter;
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

    public double[][] getRas(){
        return ras;
    }

    public void setRas(double[][] ras){
        this.ras = ras;

        cellCount = 0;
        for(int r=1; r<=nRows; r++){
            for(int c=1; c<=nCols; c++){
                if(ras[c][r] != noData){
                    cellCount++;
                }
            }
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
    private double xllCenter = 0;
    private double yllCenter = 0;
    private double cellSize = 0;
    private double noData = -9999;
    private String byteOrder = "LSBFIRST";

    private boolean origCorner = true;

    private double[][] ras = null;

    private long   cellCount = 0;
    private double minVal = Double.MAX_VALUE;
    private double maxVal = -Double.MAX_VALUE;
    private double meanVal = 0;
    private double stDevVal = 0;
    private boolean isInteger = true;

    public static final String LSBFIRST = "LSBFIRST";
    public static final String MSBFIRST = "MSBFIRST";

}
