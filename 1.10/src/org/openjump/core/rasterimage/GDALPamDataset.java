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
import org.w3c.dom.NodeList;
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
    
    public void writeStatistics(File auxXmlFile, Stats stats)
            throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SAXException, IOException {
        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc;
        
        Element pamDatasetElement;
        NodeList pamRasterBandNodeList;
        
        // Try to read the xml file
        if(auxXmlFile.isFile()) {
            try {
                doc = docBuilder.parse(auxXmlFile);
            } catch(SAXException | IOException ex) {
                doc = docBuilder.newDocument();
            }
        } else {
            doc = docBuilder.newDocument();
        }
            
        // Check if PAMDataset element exists and, if not, create it
        String pamDatasetTagName = "PAMDataset";
        pamDatasetElement = (Element) doc.getElementsByTagName(pamDatasetTagName).item(0);
        if(pamDatasetElement == null) {
            pamDatasetElement = doc.createElement(pamDatasetTagName);
        }
        
        String pamRasterBandTagName = "PAMRasterBand";
        String bandAttribute = "band";
        String metadataElementName = "Metadata";
        
        pamRasterBandNodeList = pamDatasetElement.getElementsByTagName(pamRasterBandTagName);
        if(pamRasterBandNodeList != null && pamRasterBandNodeList.getLength() > 0) {
            for(int b=0; b<pamRasterBandNodeList.getLength(); b++) {
                Element pamRasterBandElement = (Element) pamRasterBandNodeList.item(b);
                int bandNr = Integer.parseInt(pamRasterBandElement.getAttribute(bandAttribute));
                
                if(bandNr == b+1) {
                
                    Element metadataElement = (Element) pamRasterBandElement.getElementsByTagName(metadataElementName).item(0);
                    metadataElement = updateMetadataElement(doc, metadataElement, stats, band);
                
                    pamRasterBandElement.appendChild(metadataElement);
                    pamDatasetElement.appendChild(pamRasterBandElement);
                    
                }
            }            
        } else {
            for(int b=0; b<stats.getBandCount(); b++) {
                
                Element pamRasterBandElement = doc.createElement(pamRasterBandTagName);
                Attr attr = doc.createAttribute(bandAttribute);
                attr.setValue(Integer.toString(b+1));
                pamRasterBandElement.setAttributeNode(attr);
                
                Element metadataElement = doc.createElement(metadataElementName);
                metadataElement = updateMetadataElement(doc, metadataElement, stats, band);
                pamRasterBandElement.appendChild(metadataElement);
                pamDatasetElement.appendChild(pamRasterBandElement);
            }
            
            doc.appendChild(pamDatasetElement);
        }
 
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(auxXmlFile);
        transformer.transform(source, result);
        
    }
    
    private Element updateMetadataElement(Document doc, Element metadataElement, Stats stats, int band) {
        
        Element mdi = doc.createElement("MDI");
        mdi.setAttribute("key", "STATISTICS_MINIMUM");
        mdi.setTextContent(Double.toString(stats.getMin(band)));
        metadataElement.appendChild(mdi);

        mdi = doc.createElement("MDI");
        mdi.setAttribute("key", "STATISTICS_MAXIMUM");
        mdi.setTextContent(Double.toString(stats.getMax(band)));
        metadataElement.appendChild(mdi);

        mdi = doc.createElement("MDI");
        mdi.setAttribute("key", "STATISTICS_MEAN");
        mdi.setTextContent(Double.toString(stats.getMean(band)));
        metadataElement.appendChild(mdi);

        mdi = doc.createElement("MDI");
        mdi.setAttribute("key", "STATISTICS_STDDEV");
        mdi.setTextContent(Double.toString(stats.getStdDev(band)));
        metadataElement.appendChild(mdi);
        
        return metadataElement;
        
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
                    switch (attributes.getValue(a).toUpperCase()) {
                        case "STATISTICS_MINIMUM":
                            attributeValue = "STATISTICS_MINIMUM";
                            break;
                        case "STATISTICS_MAXIMUM":
                            attributeValue = "STATISTICS_MAXIMUM";
                            break;
                        case "STATISTICS_MEAN":
                            attributeValue = "STATISTICS_MEAN";
                            break;
                        case "STATISTICS_STDDEV":
                            attributeValue = "STATISTICS_STDDEV";
                            break;
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
            switch (attributeValue.toUpperCase()) {
                case "STATISTICS_MINIMUM":
                    min_l.add(Double.parseDouble(tmpValue));
                    break;
                case "STATISTICS_MAXIMUM":
                    max_l.add(Double.parseDouble(tmpValue));
                    break;
                case "STATISTICS_MEAN":
                    mean_l.add(Double.parseDouble(tmpValue));
                    break;
                case "STATISTICS_STDDEV":
                    stdDev_l.add(Double.parseDouble(tmpValue));
                    break;
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
    private final List<Double> min_l;
    private final List<Double> max_l;
    private List<Double> mean_l = null;
    private List<Double> stdDev_l = null;
    
    
}
