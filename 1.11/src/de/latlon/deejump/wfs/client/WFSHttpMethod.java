package de.latlon.deejump.wfs.client;

import org.apache.commons.httpclient.HttpMethod;

public interface WFSHttpMethod extends HttpMethod{

  /**
   * we need a way to retrieve the underlying url later eg. to fetch auth info
   * @return
   */
  public String getWfsUri();

}
