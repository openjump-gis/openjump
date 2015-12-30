package com.vividsolutions.wms;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import net.iharder.Base64;

abstract public class AbstractWMSRequest implements WMSRequest {

  protected WMService service;
  protected String version = WMService.WMS_1_1_1;

  protected AbstractWMSRequest(WMService service) {
    this.service = service;
  }

  /**
   * Gets the WMService that this object will make requests from.
   * 
   * @return the WMService that this object will make requests from
   */
  public WMService getService() {
    return service;
  }

  /**
   * reset WMS version for requests to the underlying wms service
   * 
   * @param ver
   */
  public void setWMSVersion(String ver) {
    this.version = ver;
  }

  /**
   * must be implemented according to the specific needs
   * 
   * @return URL
   * @throws MalformedURLException
   */
  abstract public URL getURL() throws MalformedURLException;

  /**
   * unified way to create a url connection, may be overwritten and modified
   * 
   * @return
   * @throws IOException
   */
  protected URLConnection prepareConnection() throws IOException {
    URL requestUrl = getURL();
    URLConnection con = requestUrl.openConnection();
    con.setConnectTimeout(WMService.TIMEOUT_OPEN);
    con.setReadTimeout(WMService.TIMEOUT_READ);
    
    // add this service's auth info
    String userinfo = requestUrl.getUserInfo();
    if (userinfo != null) {
      con.setRequestProperty("Authorization",
          "Basic " + Base64.encodeBytes(requestUrl.getUserInfo().getBytes()));
    }
    con.setRequestProperty("Host", requestUrl.getHost());

    return con;
  }

  /**
   * for implementations seeking to work with the connection to retrieve headers
   * or such
   * 
   * @Override
   */
  public URLConnection getConnection() throws IOException {
    return prepareConnection();
  }

  /**
   * Connect to the service and get an Image of the map.
   * 
   * @return the retrieved map Image
   */
  public Image getImage() throws IOException {
    URLConnection con = getConnection();

    boolean isImage = false;
    // System.out.println(requestUrl);
    for (Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {

      String key = entry.getKey() != null ? entry.getKey() : "";
      String value = null;
      try {
        value = entry.getValue().get(0).toString();
      } catch (Exception e) {
      }

      // System.out.println(key + "/" + value);

      if (key.matches("^(?i)Content-Type$") && value.matches("^(?i)image/.*"))
        isImage = true;
    }

    if (isImage)
      return ImageIO.read(con.getInputStream());

    readConnection(con);
    return null;
  }

  protected void readConnection(URLConnection con) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        con.getInputStream()));
    StringBuilder result = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      result.append(line);
    }
    System.out.println(result.toString());
  }

}
