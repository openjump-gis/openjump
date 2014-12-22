//$Header$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2006 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de

 ---------------------------------------------------------------------------*/

package de.latlon.deejump.wfs.client;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.deegree.framework.log.ILogger;
import org.deegree.framework.log.LoggerFactory;

import de.latlon.deejump.wfs.DeeJUMPException;

/**
 * Does the posting and getting of requests/reponses for the WFSPanel.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1438 $, $Date: 2008-05-29 15:00:02 +0200 (Do, 29 Mai 2008) $
 */
public class WFSClientHelper {

    private static ILogger LOG = LoggerFactory.getLogger( WFSClientHelper.class );

    /**
     * @param serverUrl
     * @param request
     * @return the response as String
     * @throws DeeJUMPException
     */
    public static String createResponsefromWFS( String serverUrl, String request )
                            throws DeeJUMPException {
        LOG.logDebug( "WFS GetFeature: " + serverUrl + " -> " + request );

        HttpClient httpclient = new HttpClient();

        String proxyUser = System.getProperty( "proxyUser" );
        String proxyPasswd = System.getProperty( "proxyPassword" );
        String proxyHost = System.getProperty( "proxyHost" );
        String port = System.getProperty( "proxyPort" );

        int proxyPort = 80;
        if ( port != null ) {
            try {
                proxyPort = Integer.valueOf( port ).intValue();
            } catch ( Exception e ) {
                e.printStackTrace();
                LOG.logDebug( "Cannot convert port into an integer: " + port );
            }
        }

        LOG.logDebug( "Proxy settings: host='" + proxyHost + "' port='" + proxyPort + "' " + " user='" + proxyUser
                      + "' pw='" + proxyPasswd + "'" );

        if ( proxyHost != null ) {
            httpclient.getHostConfiguration().setProxy( proxyHost, proxyPort );

            if ( proxyUser != null ) {
                httpclient.getState().setCredentials( new AuthScope( proxyHost, proxyPort ),
                                                      new UsernamePasswordCredentials( proxyUser, proxyPasswd ) );
            }
        }

        PostMethod httppost = new PostMethod( serverUrl );
        httppost.setRequestEntity( new StringRequestEntity( request ) );

        try {
            httpclient.executeMethod( httppost );
            return httppost.getResponseBodyAsString();
        } catch ( HttpException e ) {
            String mesg = "Error opening connection with " + serverUrl;
            LOG.logError( mesg, e );
            throw new DeeJUMPException( mesg, e );
        } catch ( IOException e ) {
            String mesg = "Error opening connection with " + serverUrl;
            LOG.logError( mesg, e );
            throw new DeeJUMPException( mesg, e );
        }

    }

}
