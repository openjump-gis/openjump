/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openjump.core.rasterimage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author deluca
 */
public class GDALInternalMetadata extends DefaultHandler {
    
    public GDALInternalMetadata() {
        minVals_l = new ArrayList<Double>();
        maxVals_l = new ArrayList<Double>();
        meanVals_l = new ArrayList<Double>();
        stdDevVals_l = new ArrayList<Double>();
    }
    
    public Stats readStatistics(String statsTag) throws ParserConfigurationException, SAXException, IOException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        //XMLReader reader = parser.getXMLReader();

        parser.parse(new ByteArrayInputStream(statsTag.getBytes("UTF-8")), this);
        
        sampleNr++;
        Stats stats = new Stats(sampleNr);
        for(int b=0; b<sampleNr; b++) {
            stats.setStatsForBand(b, minVals_l.get(b), maxVals_l.get(b), meanVals_l.get(b), stdDevVals_l.get(b));
        }
        return stats;
        
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName); //To change body of generated methods, choose Tools | Templates.
    
        if(item == null) {
            return;
        }
        
        if(item.equals(STATISTICS_MINIMUM)) {
            Double value = Double.parseDouble(tmpValue);
            minVals_l.add(value);
        } else if(item.equals(STATISTICS_MAXIMUM)) {
            Double value = Double.parseDouble(tmpValue);
            maxVals_l.add(value);
        } else if(item.equals(STATISTICS_MEAN)) {
            Double value = Double.parseDouble(tmpValue);
            meanVals_l.add(value);
        } else if(item.equals(STATISTICS_STDDEV)) {
            Double value = Double.parseDouble(tmpValue);
            stdDevVals_l.add(value);
        }
        item = null;
        
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes); //To change body of generated methods, choose Tools | Templates.
    
        if(localName.equalsIgnoreCase("Item")) { 
            for(int a=0; a<attributes.getLength(); a++) {
                if(attributes.getQName(a).equalsIgnoreCase(itemSample)) {
                    int newSampleNr = Integer.parseInt(attributes.getValue(a));
                    if(newSampleNr > sampleNr) {
                        sampleNr = newSampleNr;
                    }
                } else if(attributes.getQName(a).equalsIgnoreCase(itemName)) {
                    item = attributes.getValue(a);
                }
            } 
        } 
        
    }

    @Override
    public void characters(char[] ac, int i, int j) throws SAXException {
        tmpValue = new String(ac, i, j);
        System.out.println(tmpValue);
    }
    
    private String tmpValue;
    private final List<Double> minVals_l;
    private final List<Double> maxVals_l;
    private final List<Double> meanVals_l;
    private final List<Double> stdDevVals_l;
    private int sampleNr;
    
    private static final String itemName = "name";
    private static final String itemSample = "sample";
    
    private static String item;
    private static final String STATISTICS_MAXIMUM = "STATISTICS_MAXIMUM";
    private static final String STATISTICS_MINIMUM = "STATISTICS_MINIMUM";
    private static final String STATISTICS_MEAN = "STATISTICS_MEAN";
    private static final String STATISTICS_STDDEV = "STATISTICS_STDDEV";
    
}
