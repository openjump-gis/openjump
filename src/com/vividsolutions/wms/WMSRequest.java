package com.vividsolutions.wms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public interface WMSRequest {

  public WMService getService();
  
  public void setWMSVersion( String wmsVersion );
  
  public URL getURL() throws MalformedURLException, KeyManagementException, NoSuchAlgorithmException;
  
  public URLConnection getConnection() throws IOException, KeyManagementException, NoSuchAlgorithmException;
}
