package com.vividsolutions.wms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public interface WMSRequest {

  public WMService getService();
  
  public void setWMSVersion( String wmsVersion );
  
  public URL getURL() throws MalformedURLException;
  
  public URLConnection getConnection() throws IOException;
}
