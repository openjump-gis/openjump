package de.latlon.deejump.wfs.client;

import org.apache.commons.httpclient.methods.PostMethod;

public class WFSPostMethod extends PostMethod implements WFSHttpMethod{

  private String uri;
  
  public WFSPostMethod(String uri) {
    super(uri);
    this.uri = uri;
  }

  @Override
  public String getWfsUri() {
    return this.uri;
  }



}
