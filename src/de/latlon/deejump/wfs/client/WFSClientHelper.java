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

import java.io.*;
import java.net.*;
import java.util.LinkedList;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.io.IOUtils;
import org.deegree.enterprise.*;
import org.deegree.framework.log.*;
import org.deegree.framework.util.CharsetUtils;

import de.latlon.deejump.wfs.*;

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

        HttpClient httpclient = new WFSHttpClient();

        PostMethod httppost = new PostMethod( serverUrl );
        httppost.setRequestEntity( new StringRequestEntity( request ) );

        try {
            WebUtils.enableProxyUsage( httpclient, new URL(serverUrl) );
            httpclient.executeMethod( httppost );
            PushbackInputStream pbis = new PushbackInputStream( httppost.getResponseBodyAsStream(), 1024 );
            String encoding = readEncoding( pbis );

            return IOUtils.toString(pbis, encoding);

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

    /**
     * reads the encoding of a XML document from its header. If no header available
     * <code>CharsetUtils.getSystemCharset()</code> will be returned
     * 
     * @param pbis
     * @return encoding of a XML document
     * @throws IOException
     */
    private static String readEncoding( PushbackInputStream pbis )
                            throws IOException {
        byte[] b = new byte[80];
        String s = "";
        int rd = 0;

        LinkedList<byte[]> bs = new LinkedList<byte[]>();
        LinkedList<Integer> rds = new LinkedList<Integer>();
        while ( rd < 80 ) {
            rds.addFirst( pbis.read( b ) );
            if ( rds.peek() == -1 ) {
                rds.poll();
                break;
            }
            rd += rds.peek();
            s += new String( b, 0, rds.peek() ).toLowerCase();
            bs.addFirst( b );
            b = new byte[80];
        }

        String encoding = CharsetUtils.getSystemCharset();
        if ( s.indexOf( "?>" ) > -1 ) {
            int p = s.indexOf( "encoding=" );
            if ( p > -1 ) {
                StringBuffer sb = new StringBuffer();
                int k = p + 1 + "encoding=".length();
                while ( s.charAt( k ) != '"' && s.charAt( k ) != '\'' ) {
                    sb.append( s.charAt( k++ ) );
                }
                encoding = sb.toString();
            }
        }
        while ( !bs.isEmpty() ) {
            pbis.unread( bs.poll(), 0, rds.poll() );
        }

        return encoding;
    }
    
}
