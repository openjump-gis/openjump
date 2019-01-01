/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.ui;

import de.latlon.deejump.wfs.data.JUMPFeatureFactory;

/**
 * <code>WFSOptions</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1438 $, $Date: 2008-05-29 15:00:02 +0200 (Do, 29 Mai 2008) $
 */
public class WFSOptions {

    private String[] outputFormats;

    private String selectedOutputFormat;

    private String[] protocols;

    private String selectedProtocol;

    /**
     * @param maxFeatures
     * @param outputFormats
     * @param protocols
     */
    public WFSOptions( int maxFeatures, String[] outputFormats, String[] protocols ) {
        setMaxFeatures( maxFeatures );
        setOutputFormats( outputFormats );
        setProtocols( protocols );
    }

    /**
     * 
     */
    public WFSOptions() {
        this( JUMPFeatureFactory.getMaxFeatures(), new String[] { "GML2", "text/xml; subtype=gml/3.1.1" }, new String[] { "GET", "POST" } );
    }

    /**
     * @return the max features setting to be used in the request
     */
    public int getMaxFeatures() {
        return JUMPFeatureFactory.getMaxFeatures();
    }

    /**
     * @param maxFeatures
     */
    public void setMaxFeatures( int maxFeatures ) {
        if ( maxFeatures < 1 ) {
            throw new IllegalArgumentException( "maxFeatures must be a number greater than 1." );
        }
        JUMPFeatureFactory.setMaxFeatures(maxFeatures);
    }

    /**
     * @return the list of output formats
     */
    public String[] getOutputFormats() {

        return outputFormats;
    }

    /**
     * @param outputFormats
     */
    public void setOutputFormats( String[] outputFormats ) {
        if ( outputFormats == null || outputFormats.length == 0 ) {
            throw new IllegalArgumentException( "outputFormats cannot be null or have zero length" );
        }
        this.outputFormats = outputFormats;
        // the selected is the first one in the list
        setSelectedOutputFormat( this.outputFormats[0] );
    }

    /**
     * @return the list of protocols (unused?)
     */
    public String[] getProtocols() {
        return protocols;
    }

    /**
     * @param protocols
     */
    public void setProtocols( String[] protocols ) {
        if ( protocols == null || protocols.length == 0 ) {
            throw new IllegalArgumentException( "outputFormats cannot be null or have zero length" );
        }
        this.protocols = protocols;
        // the selected is the first one in the list
        setSelectedProtocol( this.protocols[0] );

    }

    /**
     * @return the selected output format
     */
    public String getSelectedOutputFormat() {
        return selectedOutputFormat;
    }

    /**
     * @param selectedOutputFormat
     */
    public void setSelectedOutputFormat( String selectedOutputFormat ) {
        if ( selectedOutputFormat == null ) {
            throw new IllegalArgumentException( "selectedOutputFormat cannot be null or have zero length" );
        }
        this.selectedOutputFormat = selectedOutputFormat;
    }

    /**
     * @return the selected protocol (unused?)
     */
    public String getSelectedProtocol() {
        return selectedProtocol;
    }

    /**
     * @param selectedProtocol
     */
    public void setSelectedProtocol( String selectedProtocol ) {
        if ( selectedProtocol == null ) {
            throw new IllegalArgumentException( "selectedProtocol cannot be null or have zero length" );
        }
        this.selectedProtocol = selectedProtocol;
    }

}
