package org.openjump.core.rasterimage.styler;

//import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import org.openjump.core.rasterimage.RasterSymbology;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class to read a GeoServer-style SLD file into a raster symbolizer.
 * @author AdL
 */
public class SLDHandler {

    /**
     * Reads the SLD file and creates a raster symbolizer.
     * @param SLDFile The SLD file to be read.
     * @return The raster symbolizer.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws Exception 
     */
    public static RasterSymbology read(File SLDFile)
            throws ParserConfigurationException, SAXException, IOException, Exception {
    
        DocumentBuilderFactory dbf = newInstance();
        dbf.setNamespaceAware(true);
        String colorMapType = RasterSymbology.TYPE_RAMP;
        
        RasterSymbology rasterSymbology = null;
        
        Document doc = dbf.newDocumentBuilder().parse(SLDFile);
        NodeList rasterSymb_nl = doc.getElementsByTagName("RasterSymbolizer");
        if(rasterSymb_nl.getLength() == 1) {
            
            Element rasterSymb_el = (Element) rasterSymb_nl.item(0);
            
            // Type
            NodeList type_nl = rasterSymb_el.getElementsByTagName("Type");
            if(type_nl.getLength() == 1) {
                Element type_el = (Element) type_nl.item(0);
                String type = type_el.getTextContent();
                colorMapType = type.toUpperCase();
            }
            
            rasterSymbology = new RasterSymbology(colorMapType);
            
            // Opacity
            NodeList opacity_nl = rasterSymb_el.getElementsByTagName("Opacity");
            if(opacity_nl.getLength() == 1) {
                Element opacity_el = (Element) opacity_nl.item(0);
                double opacity = Double.parseDouble(opacity_el.getTextContent());
                rasterSymbology.setTransparency(1. - opacity);
            }          
      
            NodeList colorMap_nl = doc.getElementsByTagName("ColorMap");
            for(int me=0; me<colorMap_nl.getLength(); me++) {
                Element colorMap = (Element) colorMap_nl.item(me);                
                NodeList colorMapElements_nl = colorMap.getElementsByTagName("ColorMapEntry");
                
                for(int cme=0; cme<colorMapElements_nl.getLength(); cme++) {
                    Element colorMapEntryElement = (Element) colorMapElements_nl.item(cme);
                    double quantity = Double.parseDouble(colorMapEntryElement.getAttribute("quantity"));
                    Color color = hex2Rgb(colorMapEntryElement.getAttribute("Color"));
                    rasterSymbology.addColorMapEntry(quantity, color);
                }
            }
        }    
        
        return rasterSymbology;
        
    }
    
    public static void write(RasterSymbology symbology, String symbologyName, File sldFile) throws IOException, XMLStreamException, TransformerConfigurationException {
        
        XMLOutputFactory outFactory =  XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(sldFile));
        
        writer.writeStartDocument();
        writer.writeStartElement("StyledLayerDescriptor");
        writer.writeAttribute("xmlns", "http://www.opengis.net/sld");
        writer.writeAttribute("xmlns:ogc", "http://www.opengis.net/ogc");
        writer.writeAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        writer.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        writer.writeAttribute("version", "1.0.0");
        writer.writeAttribute("xsi:schemaLocation", "http://www.opengis.net/sld StyledLayerDescriptor.xsd");
        
        writer.writeStartElement("NamedLayer");
        writer.writeStartElement("Name");
        if(symbologyName != null) {
            writer.writeCharacters(symbologyName);
        }
        writer.writeStartElement("UserStyle");
        writer.writeStartElement("Title");
        writer.writeCharacters("OpenJUMP Raster Style");
        writer.writeStartElement("FeatureTypeStyle");
        writer.writeStartElement("Rule");
        writer.writeStartElement("RasterSymbolizer");
        
        writer.writeStartElement("Type");
        writer.writeCharacters(symbology.getColorMapType());
        writer.writeEndElement();
        
        writer.writeStartElement("Opacity");
        writer.writeCharacters(Double.toString(1. - symbology.getTransparency()));
        writer.writeEndElement();
        
        writer.writeStartElement("ColorMap");
        
        for(Map.Entry<Double,Color> colorMapEntry : symbology.getColorMapEntries_tm().entrySet()) {
            
            writer.writeStartElement("ColorMapEntry");
            writer.writeAttribute("Color", SLDHandler.rgb2Hex(colorMapEntry.getValue()));
            writer.writeAttribute("quantity", colorMapEntry.getKey().toString());
            writer.writeEndElement();
            
        }
        
        writer.writeEndElement(); // ColorMap
        
        writer.writeEndElement(); // RasterSymbolizer
        writer.writeEndElement(); // Rule
        writer.writeEndElement(); // FeatureTypeStyle
        writer.writeEndElement(); // Title
        writer.writeEndElement(); // UserStyle
        writer.writeEndElement(); // Name
        writer.writeEndElement(); // NamedLayer
        writer.writeEndElement(); // StyledLayerDescriptor
        
        writer.writeEndDocument();
        
        writer.flush();
        writer.close();        
        
    }
    
    public static Color hex2Rgb(String colorStr) {
        
        if(colorStr.equals("")) {
            return null;
        }
        
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }
    
    public static String rgb2Hex(Color rgbColor) {
        
        if(rgbColor == null) {
            return "";
        }
        
        return SLDHandler.rgb2Hex(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue());
        
    }
    
    public static String rgb2Hex(int r, int g, int b) {
        
        return String.format("#%02x%02x%02x", r, g, b);
        
    }
    
}
