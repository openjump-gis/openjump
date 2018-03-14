package it.betastudio.adbtoolbox.libs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.JOptionPane;


/**
 *
 * @author Beta Studio
 */
public class DxfExport {

    public DxfExport(){

        dxfOut = new String();

    }

    public void writeHeader(double minX, double minY, double maxX, double maxY){

        dxfOut = "0";
        appString("SECTION");
        appString(2);
        appString("HEADER");
        appString(9);
        appString("$ACADVER");
        appString(1);
        appString("AC1009");

        appString(9);
        appString("$EXTMIN");
        appString(10);
        appString(minX);
        appString(20);
        appString(minY);
        appString(30);
        appString(0);
        appString(9);
        appString("$EXTMAX");
        appString(10);
        appString(maxX);
        appString(20);
        appString(maxY);
        appString(30);
        appString(0);
        
        appString(9);
        appString("$LIMMIN");
        appString(10);
        appString(minX);
        appString(20);
        appString(minY);
        appString(9);
        appString("$LIMMAX");
        appString(10);
        appString(maxX);
        appString(20);
        appString(maxY);
//        appString(9);
//        appString("$CLAYER");
//        appString(8);
//        appString(layerName);
        appString(0);
        appString("ENDSEC");

    }

    public void writeStartSec(){

        appString(0);
        appString("SECTION");

    }

    public void writeEndSec(){

        appString(0);
        appString("ENDSEC");

    }

    public void writeTablesStart(){
        
        appString(2);
        appString("TABLES");        
    
    }
    
    public void writeTableStart(){
    
        appString(0);
        appString("TABLE");       
        
    }

    public void writeTableEnd(){
        
        appString(0);
        appString("ENDTAB");      
    
    }
    
    public void writeLayersStart(){

        appString(2);
        appString("LAYER");
        appString(70);
        appString(1);

    }

    public void writeLayer(String layName, int colourNr){

        appString(0);
        appString("LAYER");
        appString(2);
        appString(layName);
        appString(62);
        appString(colourNr);
        appString(70);
        appString(0);
        appString(6);
        appString("CONTINUOUS");

    }
    
    public void writeVPort(double centerX, double centerY, double minX, double minY, double maxX, double maxY){

        appString(2);
        appString("VPORT");
        appString(5);
        appString(8);
        appString(100);
        appString("AcDbSymbolTable");
        appString(70);
        appString(2);
        appString(0);
        appString("VPORT");
        appString(5);
        appString("4A");
        appString(100);
        appString("AcDbSymbolTableRecord");
        appString(100);
        appString("AcDbViewportTableRecord");
        appString(2);
        appString("*Active");
        appString(70);
        appString(0);
        appString(10);
        appString(minX);
        appString(20);
        appString(minY);
        appString(11);
        appString(maxX);
        appString(21);
        appString(maxY);
        appString(12);
        appString(centerX);
        appString(22);
        appString(centerY);
        appString(13);
        appString(0.0);
        appString(23);
        appString(0.0);
        appString(14);
        appString(10.0);
        appString(24);
        appString(10.0);
        appString(15);
        appString(10.0);
        appString(25);
        appString(10.0);
        appString(16);
        appString(0.0);
        appString(26);
        appString(0.0);
        appString(36);
        appString(1.0);
        appString(17);
        appString(0.0);
        appString(27);
        appString(0.0);
        appString(37);
        appString(0.0);
        appString(40);
        appString(1009.022556390977);
        appString(41);
        appString(1.783132530120481);
        appString(42);
        appString(50.0);
        appString(43);
        appString(0.0);
        appString(44);
        appString(0.0);
        appString(50);
        appString(0.0);
        appString(51);
        appString(0.0);
        appString(71);
        appString(0);
        appString(72);
        appString(100);
        appString(73);
        appString(1);
        appString(74);
        appString(3);
        appString(75);
        appString(0);
        appString(76);
        appString(0);
        appString(77);
        appString(0);
        appString(78);
        appString(0);

    }    

    public void writeEntStart(){
        
        appString(2);
        appString("ENTITIES");

    }

    public void writeAppId(){

        appString(2);
        appString("APPID");
        appString(5);
        appString(9);
        appString(100);
        appString("AcDbSymbolTable");
        appString(70);
        appString(1);
        appString(0);
        appString("APPID");
        appString(5);
        appString(12);
        appString(100);
        appString("AcDbSymbolTableRecord");
        appString(100);
        appString("AcDbRegAppTableRecord");
        appString(2);
        appString("ACAD");
        appString(70);
        appString(0);
        
    }

    public void writeEnding() {

        // Ending
        appString(0);
        appString("ENDSEC");
        appString(0);
        appString("EOF");

    }

    public void writeLine(String layName, double p1x, double p1y, double p2x, double p2y){

        appString("0");
        appString("LINE");
        appString(8);
        appString(layName);
        appString("10");
        appString(p1x);
        appString("20");
        appString(p1y);
        appString("11");
        appString(p2x);
        appString("21");
        appString(p2y);

    }

    public void writePolyline(String layName, double[][] vertices){

        appString(0);
        appString("POLYLINE");
        appString(8);
        appString(layName);
        appString(62);
        appString(1);
        appString(66);
        appString(1);

        for(int v=0; v<vertices.length; v++){
            appString(0);
            appString("VERTEX");
            appString(8);
            appString(layName);
            appString(10);  // X value
            appString(vertices[v][0]);
            appString(20);  // Y value
            appString(vertices[v][1]);
//            appString(30);  // Z value
//            appString(vertices[v][2]);
            appString(70);  // Vertex flag
            appString(4);  // Vertex flags:
        }

        appString("0");
        appString("SEQEND");

    }

    public void writeLwPolyLine(double[][] vertices){



    }

    public void writeText(String layName, double alignPoint1x, double alignPoint1y, double alignPoint1z, double alignPoint2x, double alignPoint2y, double alignPoint2z, int textHight, double textRotation, int horizJust, int vertAlign, String text){

        appString(0);
        appString("TEXT");
        appString(8);
        appString(layName);
        appString(10);            // Alignment point x
        appString(alignPoint1x);
        appString(20);            // Alignment point y
        appString(alignPoint1y);
        appString(30);            // Alignment point z
        appString(alignPoint1z);
        appString(40);            // Text hight
        appString(textHight);
        appString(50);            // Text rotation
        appString(textRotation);
        appString(1);             // Text
        appString(text);

        if(horizJust != 0 || vertAlign != 0){
            appString(11);            // Alignment point x
            appString(alignPoint2x);
            appString(21);            // Alignment point y
            appString(alignPoint2y);
            appString(31);            // Alignment point z
            appString(alignPoint2z);
            appString(72);
            appString(horizJust);
            appString(73);
            appString(vertAlign);

        }

    }

    private void appString(String appEnd){
        dxfOut = dxfOut + lineFeed + appEnd;
    }

    private void appString(int appEnd){
        dxfOut = dxfOut + lineFeed + Integer.toString(appEnd);
    }

    private void appString(double appEnd){
        dxfOut = dxfOut + lineFeed + Double.toString(appEnd);
    }

    public int exportDxf(String dxfFullFileName){

        try{
            BufferedWriter buffWrite = new BufferedWriter(new FileWriter(new File(dxfFullFileName)));
            buffWrite.write(dxfOut, 0, dxfOut.length());
            buffWrite.close();
            return 0;
        }catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Errore durante la scrittura del DXF: " + ex, "Errore", JOptionPane.ERROR_MESSAGE);
            return 1;
        }

    }

    private String dxfOut = null;
    private String lineFeed = System.getProperty("line.separator");

}
