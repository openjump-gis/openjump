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
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.deegree.framework.log.ILogger;
import org.deegree.framework.log.LoggerFactory;
import org.deegree.framework.util.CharsetUtils;

import com.vividsolutions.jump.workbench.Logger;

import de.latlon.deejump.wfs.DeeJUMPException;

/**
 * Does the posting and getting of requests/reponses for the WFSPanel.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1438 $, $Date: 2008-05-29 15:00:02 +0200 (Do, 29 Mai
 *          2008) $
 */
public class WFSClientHelper {

  /**
   * convenience method, big datasets tend to flood your memory
   * 
   * @param serverUrl
   * @param request
   * @return the response as String
   * @throws DeeJUMPException
   */
  public static String createResponseStringfromWFS(String serverUrl,
      String request) throws DeeJUMPException {

    PushbackInputStream pbis = new PushbackInputStream(
        createResponseStreamfromWFS(serverUrl, request), 1024);

    try {
      String encoding = readEncoding(pbis);
      return IOUtils.toString(pbis, encoding);
    } catch (IOException e) {
      throw new DeeJUMPException( e);
    }
  }

  /**
   * creates and InputStream for memory efficient handling
   * 
   * if postData == null, a get request is executed
   * this method supports gzip content encoding
   * 
   * @param serverUrl
   * @param postData
   * @return
   * @throws DeeJUMPException
   */
  public static InputStream createResponseStreamfromWFS(String serverUrl,
      String postData) throws DeeJUMPException {
    Logger.debug("WFS GetFeature: " + serverUrl + " -> " + postData);
    //System.out.println(serverUrl);

    HttpClient httpclient = new WFSHttpClient();

    try {
      WFSHttpMethod method;
      if (postData != null) {
        WFSPostMethod pm = new WFSPostMethod(serverUrl);
        pm.setRequestEntity(new StringRequestEntity(postData, "text/xml",
            "UTF-8"));
        method = pm;
      } else {
        method = new WFSGetMethod(serverUrl);
      }

      method.addRequestHeader("Accept-Encoding", "gzip");

      // WebUtils.enableProxyUsage(httpclient, new URL(serverUrl));
      httpclient.executeMethod(method);

      Header encHeader = method.getResponseHeader("Content-Encoding");
      InputStream is = method.getResponseBodyAsStream();
      if (encHeader != null) {
        String encValue = encHeader.getValue();
        if (encValue != null && encValue.toLowerCase().equals("gzip")) {
          is = new GZIPInputStream(is);
        }
      }

      return is;
    } catch (Exception e) {
      throw new DeeJUMPException(e);
    }
  }

  /**
   * reads the encoding of a XML document from its header. If no header
   * available <code>CharsetUtils.getSystemCharset()</code> will be returned
   * 
   * @param pbis
   * @return encoding of a XML document
   * @throws IOException
   */
  public static String readEncoding(PushbackInputStream pbis)
      throws IOException {
    byte[] b = new byte[80];
    String s = "";
    int rd = 0;

    LinkedList<byte[]> bs = new LinkedList<byte[]>();
    LinkedList<Integer> rds = new LinkedList<Integer>();
    while (rd < 80) {
      rds.addFirst(pbis.read(b));
      if (rds.peek() == -1) {
        rds.poll();
        break;
      }
      rd += rds.peek();
      s += new String(b, 0, rds.peek()).toLowerCase();
      bs.addFirst(b);
      b = new byte[80];
    }

    String encoding = CharsetUtils.getSystemCharset();
    if (s.indexOf("?>") > -1) {
      int p = s.indexOf("encoding=");
      if (p > -1) {
        StringBuffer sb = new StringBuffer();
        int k = p + 1 + "encoding=".length();
        while (s.charAt(k) != '"' && s.charAt(k) != '\'') {
          sb.append(s.charAt(k++));
        }
        encoding = sb.toString();
      }
    }
    while (!bs.isEmpty()) {
      pbis.unread(bs.poll(), 0, rds.poll());
    }

    return encoding;
  }

}
