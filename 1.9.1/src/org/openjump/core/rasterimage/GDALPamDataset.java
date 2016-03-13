package org.openjump.core.rasterimage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author AdL
 */
public class GDALPamDataset extends DefaultHandler {

    public GDALPamDataset() {
        this.min_l = new ArrayList<Double>();
        this.max_l = new ArrayList<Double>();
        this.mean_l = new ArrayList<Double>();
        this.stdDev_l = new ArrayList<Double>();
    }
    
    public Stats readStatistics(File auxXmlFile) throws ParserConfigurationException, SAXException, IOException  {
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(auxXmlFile, this);

        if(min_l.isEmpty() || max_l.isEmpty() || mean_l.isEmpty() || stdDev_l.isEmpty()) {
            throw new IOException("Error while reading xml file: some statistcs non found");
        }
        
        Stats stats = new Stats(bandCount);
        for(int b=0; b<bandCount; b++) {
            stats.setStatsForBand(b, min_l.get(b), max_l.get(b), mean_l.get(b), stdDev_l.get(b));
        }
        return stats;
        
    }
    
    public void writeStatistics(File auxXmlFile, Stats stats) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("PAMDataset");
        
        for(int b=0; b<stats.getBandCount(); b++) {
        
            Element pamRasterBand = doc.createElement("PAMRasterBand");            

            Attr attr = doc.createAttribute("band");
            attr.setValue(Integer.toString(b+1));
            pamRasterBand.setAttributeNode(attr);

            Element metadata = doc.createElement("Metadata");

            Element mdi = doc.createElement("MDI");
            mdi.setAttribute("key", "STATISTICS_MINIMUM");
            mdi.setTextContent(Double.toString(stats.getMin(b)));
            metadata.appendChild(mdi);

            mdi = doc.createElement("MDI");
            mdi.setAttribute("key", "STATISTICS_MAXIMUM");
            mdi.setTextContent(Double.toString(stats.getMax(b)));
            metadata.appendChild(mdi);

            mdi = doc.createElement("MDI");
            mdi.setAttribute("key", "STATISTICS_MEAN");
            mdi.setTextContent(Double.toString(stats.getMean(b)));
            metadata.appendChild(mdi);

            mdi = doc.createElement("MDI");
            mdi.setAttribute("key", "STATISTICS_STDDEV");
            mdi.setTextContent(Double.toString(stats.getStdDev(b)));
            metadata.appendChild(mdi);
            
            pamRasterBand.appendChild(metadata);
            rootElement.appendChild(pamRasterBand);
        }
        
        doc.appendChild(rootElement);
        
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(auxXmlFile);
        transformer.transform(source, result);
        
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        attributeValue = null;
        if(localName.toUpperCase().equals("PAMRASTERBAND")) {
            for(int a=0; a<attributes.getLength(); a++) {
                if(attributes.getQName(a).toLowerCase().equals("band")) {
                    attributeQName = "band";
                    attributeValue = attributes.getValue(a);
                    band = Integer.parseInt(attributeValue);
                    bandCount = Math.max(band, bandCount);
                }
            }
        }
        
        if(localName.toUpperCase().equals("MDI")) {
            for(int a=0; a<attributes.getLength(); a++) {
                if(attributes.getQName(a).toLowerCase().equals("key")) {
                    attributeQName = "key";
                    if(attributes.getValue(a).toUpperCase().equals("STATISTICS_MINIMUM")) {
                        attributeValue = "STATISTICS_MINIMUM";
                    } else if(attributes.getValue(a).toUpperCase().equals("STATISTICS_MAXIMUM")) {
                        attributeValue = "STATISTICS_MAXIMUM";
                    } else if(attributes.getValue(a).toUpperCase().equals("STATISTICS_MEAN")) {
                        attributeValue = "STATISTICS_MEAN";
                    } else if(attributes.getValue(a).toUpperCase().equals("STATISTICS_STDDEV")) {
                        attributeValue = "STATISTICS_STDDEV";
                    }
                }
            }
        }
        
    }    
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        
        if(attributeValue == null || tmpValue.trim().equals("")) return;
        
        if(attributeQName.toLowerCase().equals("key")) {
            if(attributeValue.toUpperCase().equals("STATISTICS_MINIMUM")) {
                min_l.add(Double.parseDouble(tmpValue));
            } else if(attributeValue.toUpperCase().equals("STATISTICS_MAXIMUM")) {
                max_l.add(Double.parseDouble(tmpValue));
            } else if(attributeValue.toUpperCase().equals("STATISTICS_MEAN")) {
                mean_l.add(Double.parseDouble(tmpValue));
            } else if(attributeValue.toUpperCase().equals("STATISTICS_STDDEV")) {
                stdDev_l.add(Double.parseDouble(tmpValue));
            }
        }
        
    }

    @Override
    public void characters(char[] ac, int i, int j) throws SAXException {
        tmpValue = new String(ac, i, j);
    }

    
    private String tmpValue;
    private String attributeQName;
    private String attributeValue;
    private int bandCount = 0;
    private int band = 0;
    private List<Double> min_l;
    private List<Double> max_l;
    private List<Double> mean_l = null;
    private List<Double> stdDev_l = null;
    
    
}
