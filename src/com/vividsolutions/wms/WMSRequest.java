package com.vividsolutions.wms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public interface WMSRequest {

  WMService getService();
  
  void setWMSVersion( String wmsVersion );
  
  URL getURL() throws MalformedURLException;
  
  URLConnection getConnection() throws IOException;
}
