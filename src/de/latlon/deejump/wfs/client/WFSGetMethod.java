package de.latlon.deejump.wfs.client;

import org.apache.commons.httpclient.methods.GetMethod;

public class WFSGetMethod extends GetMethod implements WFSHttpMethod{

  private String uri;
  
  public WFSGetMethod(String uri) {
    super(uri);
    this.uri = uri;
  }

  @Override
  public String getWfsUri() {
    return this.uri;
  }



}
