/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.client;

import static org.deegree.ogcbase.CommonNamespaces.getNamespaceContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.deegree.datatypes.QualifiedName;
import org.deegree.framework.xml.DOMPrinter;
import org.deegree.framework.xml.NamespaceContext;
import org.deegree.framework.xml.XMLParsingException;
import org.deegree.framework.xml.XMLTools;
import org.deegree.ogcwebservices.getcapabilities.DCPType;
import org.deegree.ogcwebservices.getcapabilities.HTTP;
import org.deegree.ogcwebservices.getcapabilities.InvalidCapabilitiesException;
import org.deegree.ogcwebservices.wfs.capabilities.WFSCapabilities;
import org.deegree.ogcwebservices.wfs.capabilities.WFSFeatureType;
import org.openjump.util.UriUtil;
import org.xml.sax.SAXException;

import com.vividsolutions.jump.workbench.Logger;

import de.latlon.deejump.wfs.DeeJUMPException;
import de.latlon.deejump.wfs.auth.UserData;
import de.latlon.deejump.wfs.deegree2mods.WFSCapabilitiesDocument;

/**
 * This class represents a WFService. It handles connection with the server behind the given URL. It also caches Feature
 * Information such as which propertis belong to a FeatueType
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class WFServiceWrapper_1_1_0 extends AbstractWFSWrapper {

    private static final NamespaceContext nsContext = getNamespaceContext();

    private WFSCapabilities wfsCapabilities;

    private String[] featureTypes;

    private String getFeatureUrl;

    private String capsString;

    /**
     * @param logins
     * @param wfsURL
     * @throws DeeJUMPException
     */
    public WFServiceWrapper_1_1_0( UserData logins, String wfsURL ) throws DeeJUMPException {
        super( logins, wfsURL );
        init();
    }

    private void init()
                            throws DeeJUMPException {

        WFSCapabilitiesDocument wfsCapsDoc = new de.latlon.deejump.wfs.deegree2mods.WFSCapabilitiesDocument();
        try {
            String caps = getCapabilitiesURL();
            Logger.debug( "Sending capabilities request: " + caps );
            wfsCapsDoc.load( new URL( caps ) );
        } catch ( MalformedURLException e ) {
            Logger.error( "Invalid URL", e );
            throw new DeeJUMPException( e );
        } catch ( IOException e ) {
            Logger.error( "IOException when opening: " + getCapabilitiesURL(), e );
            throw new DeeJUMPException( e );
        } catch ( SAXException e ) {
            throw new DeeJUMPException( e );
        }

        String n = wfsCapsDoc.getRootElement().getLocalName();
        if ( n.indexOf( "Exception" ) != -1 ) {
            String msg = null;
            try {
                msg = XMLTools.getNodeAsString( wfsCapsDoc.getRootElement(), "//*[local-name(.) = 'ExceptionText']",
                                                nsContext, null );
            } catch ( XMLParsingException e ) {
                Logger.debug( "Stack trace while trying to parse a service exception:", e );
            }
            if ( msg != null ) {
                throw new DeeJUMPException( "The WFS responded with an error: '" + msg + "'" );
            }
            throw new DeeJUMPException( "An exception occurred while accessing the server: "
                                        + DOMPrinter.nodeToString( wfsCapsDoc.getRootElement(), "UTF-8" ) );
        }

        capsString = DOMPrinter.nodeToString( wfsCapsDoc.getRootElement(), "" );

        try {
            wfsCapabilities = (WFSCapabilities) wfsCapsDoc.parseCapabilities();
        } catch ( InvalidCapabilitiesException e ) {
            Logger.error( "Could not initialize WFS capabilities", e );
            throw new DeeJUMPException( e );
        }
    }

    private synchronized String[] extractFeatureTypes() {

        String[] fts = null;

        WFSFeatureType[] featTypes = wfsCapabilities.getFeatureTypeList().getFeatureTypes();
        ftNameToWfsFT = new HashMap<String, WFSFeatureType>();
        fts = new String[featTypes.length];
        for ( int i = 0; i < fts.length; i++ ) {
            QualifiedName qn = featTypes[i].getName();
            fts[i] = qn.getLocalName();
            // well, putting prefix + : + simple name
            // should consider to put simple name only!
            /*
             * ftNameToWfsFT.put( qn.getPrefix() + ":" + qn.getLocalName(), featTypes[i] );
             */
            ftNameToWfsFT.put( qn.getLocalName(), featTypes[i] );
        }

        return fts;
    }

    @Override
    public String[] getFeatureTypes() {
        if ( featureTypes == null ) {
            featureTypes = extractFeatureTypes();
        }
        return featureTypes;
    }

    @Override
    public String getGetFeatureURL() {

        org.deegree.ogcwebservices.getcapabilities.Operation[] ops = this.wfsCapabilities.getOperationsMetadata().getOperations();
        getFeatureUrl = null;

        for ( int i = 0; i < ops.length && getFeatureUrl == null; i++ ) {

            if ( ops[i].getName().equals( "GetFeature" ) ) {
                DCPType[] dcps = ops[i].getDCPs();
                if ( dcps.length > 0 ) {
                    getFeatureUrl = ( (HTTP) dcps[0].getProtocol() ).getPostOnlineResources()[0].toString();
                }
            }
        }

        if ( getFeatureUrl == null ) {
            throw new RuntimeException( "Service does not have a GetFeature operation accessible by HTTP POST." );
        }

        if (logins != null && !logins.isEmpty())
          getFeatureUrl = UriUtil.urlAddCredentials(getFeatureUrl, logins.getUsername(),
              logins.getPassword());
        
        return getFeatureUrl;
    }

    @Override
    public String getCapabilitesAsString() {
        return this.capsString;
    }

    @Override
    public String getServiceVersion() {
        return "1.1.0";
    }

    @Override
    protected String createDescribeFTOnlineResource() {
        org.deegree.ogcwebservices.getcapabilities.Operation[] ops = this.wfsCapabilities.getOperationsMetadata().getOperations();
        String descrFtUrl = null;
        for ( int i = 0; i < ops.length && descrFtUrl == null; i++ ) {

            if ( ops[i].getName().equals( "DescribeFeatureType" ) ) {
                DCPType[] dcps = ops[i].getDCPs();
                if ( dcps.length > 0 ) {
                    descrFtUrl = ( (HTTP) dcps[0].getProtocol() ).getGetOnlineResources()[0].toString();
                }

                if ( descrFtUrl == null ) {
                    descrFtUrl = ( (HTTP) dcps[0].getProtocol() ).getPostOnlineResources()[0].toString();
                }
            }
        }
        return descrFtUrl;
    }

}
