package de.latlon.deejump.wfs.deegree2mods;

import java.io.IOException;
import java.net.URL;

import org.xml.sax.SAXException;

import de.latlon.deejump.wfs.DeeJUMPException;
import de.latlon.deejump.wfs.client.WFSClientHelper;

public class GMLSchemaDocument extends
    org.deegree.model.feature.schema.GMLSchemaDocument {

  private static final long serialVersionUID = 42459124798674598L;

  /**
   * this override is necessary to have this method use the WFSHttpClient which
   * holds some general settings like the socket timeout
   */
  @Override
  public void load(URL url) throws IOException, SAXException {
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