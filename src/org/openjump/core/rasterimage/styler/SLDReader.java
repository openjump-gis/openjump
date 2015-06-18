package org.openjump.core.rasterimage.styler;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import javax.xml.parsers.ParserConfigurationException;
import org.openjump.core.rasterimage.RasterSymbology;
import org.openjump.core.rasterimage.RasterSymbology.ColorMapType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class to read an GeoServer-style SLD file into a raster symbolizer.
 * @author AdL
 */
public class SLDReader {

    /**
     * Reads the SLD file and creates a raster symbolizer.
     * @param SLDFile The SLD file to be read.
     * @return The raster symbolizer.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws Exception 
     */
    public static RasterSymbology read(File SLDFile) throws ParserConfigurationException, SAXException, IOException, Exception {
    
        DocumentBuilderFactory dbf = newInstance();
        dbf.setNamespaceAware(true);
        ColorMapType colorMapType = ColorMapType.RAMP;
        
        RasterSymbology rasterSymbolizer = null;
        
        Document doc = dbf.newDocumentBuilder().parse(SLDFile);
        NodeList nodeList = doc.getElementsByTagName("RasterSymbolizer");
        if(nodeList.getLength() == 1) {
            NodeList opacity_nl = doc.getElementsByTagName("Opacity");
            // TODO
            
            NodeList colorMap_nl = doc.getElementsByTagName("ColorMap");
            for(int me=0; me<colorMap_nl.getLength(); me++) {
                Element colorMap = (Element) colorMap_nl.item(me);
                
                /* Types: ramps, intervals, values */
                String colorMapTypeAttribute = colorMap.getAttribute("type");
                if(!colorMapTypeAttribute.equals("")) {
                    colorMapType = ColorMapType.valueOf(colorMapTypeAttribute.toUpperCase());
                }
                
                rasterSymbolizer = new RasterSymbology(colorMapType);
                
                NodeList colorMapElements_nl = colorMap.getElementsByTagName("ColorMapEntry");
                
                for(int cme=0; cme<colorMapElements_nl.getLength(); cme++) {
                    Element colorMapEntryElement = (Element) colorMapElements_nl.item(cme);
                    double quantity = Double.parseDouble(colorMapEntryElement.getAttribute("quantity"));
                    Color color = hex2Rgb(colorMapEntryElement.getAttribute("color"));
                    rasterSymbolizer.addColorMapEntry(quantity, color);
                }

            }
        }    
        
        return rasterSymbolizer;
        
    }
    
    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }
    
    
}
