package de.latlon.deejump.wfs.deegree2mods;

import java.io.*;
import java.net.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.deegree.enterprise.*;
import org.xml.sax.*;

import de.latlon.deejump.wfs.DeeJUMPException;
import de.latlon.deejump.wfs.client.*;

public class WFSCapabilitiesDocument extends org.deegree.ogcwebservices.wfs.capabilities.WFSCapabilitiesDocument {

  /**
   * this override is necessary to have this method use the WFSHttpClient
   * which holds some general settings like the socket timeout
   */
  public void load( URL url )
      throws IOException, SAXException {
    if (url == null) {
      throw new IllegalArgumentException("The given url may not be null");
    }

    try {
      String urlString = url.toExternalForm();
      load(WFSClientHelper.createResponseStreamfromWFS(urlString, null),
          urlString);
    } catch (DeeJUMPException e) {
      throw new IOException(e);
    }
  }
}
