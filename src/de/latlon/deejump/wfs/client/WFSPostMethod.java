package de.latlon.deejump.wfs.client;

import org.apache.commons.httpclient.methods.PostMethod;

public class WFSPostMethod extends PostMethod implements WFSMethod{

  private String uri;
  
  public WFSPostMethod(String uri) {
    super(uri);
    this.uri = uri;
  }

  @Override
  public String getUri() {
    return this.uri;
  }



}
