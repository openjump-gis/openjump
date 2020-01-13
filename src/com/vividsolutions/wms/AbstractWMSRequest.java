package com.vividsolutions.wms;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.openjump.util.URLConnectionProvider;

import com.vividsolutions.jump.util.FileUtil;

abstract public class AbstractWMSRequest implements WMSRequest {

  protected WMService service;
  protected String version;
  protected HttpURLConnection con = null;

  protected AbstractWMSRequest(WMService service) {
    this.service = service;
    // we use the services version by default,
    // can be overwritten later via setter
    this.version = service.getWmsVersion();
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
  protected HttpURLConnection prepareConnection() throws IOException {
    URL requestUrl = getURL();
    // by default we follow redirections
    con = (HttpURLConnection) URLConnectionProvider.getJUMP_URLConnectionProvider().getHttpConnection(requestUrl, true);

    return con;
  }

  /**
   * for implementations seeking to work with the connection to retrieve headers
   * or such
   * 
   * @Override
   */
  public HttpURLConnection getConnection() throws IOException {
    if (con == null)
      con = prepareConnection();

    return con;
  }

  /**
   * Connect to the service and get an Image of the map.
   * 
   * @return the retrieved map Image
   */
  public Image getImage() throws IOException {
    HttpURLConnection con = getConnection();

    boolean httpOk = con.getResponseCode() == HttpURLConnection.HTTP_OK;

    boolean isImage = false;
    if (httpOk) {
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
    }

    if (isImage)
      return ImageIO.read(con.getInputStream());

    // finally, no image? let's throw some error
    readToError(con);
    
    return null;
  }

  /**
   * connect and retrieve response as inputStream
   *
   * @return
   * @throws IOException
   */
  public InputStream getInputStream() throws IOException {
    HttpURLConnection con = getConnection();
    boolean httpOk = con.getResponseCode() == HttpURLConnection.HTTP_OK;
    if (!httpOk)
      readToError(con);

    return con.getInputStream();
  }

  /**
   * connect and retrieve response as text
   * 
   * @return
   * @throws IOException
   */
  public String getText() throws IOException {
    HttpURLConnection con = getConnection();
    boolean httpOk = con.getResponseCode() == HttpURLConnection.HTTP_OK;
    if (!httpOk)
      readToError(con);
    
    return readConnection(con, 0);
  }

  protected String readToError(HttpURLConnection con) throws IOException {
    String url = con.getURL().toExternalForm();
    String headers = con.getHeaderFields().toString();
    String result = readConnection(con, 1024);
    throw new WMSException( "Request url: " + url +
        "\nResponse code: " + con.getResponseCode() + "\nHeaders:\n" + headers + "\nResponse body:\n" + result);
  }

  protected String readConnection(HttpURLConnection con, long limit) throws IOException {
    boolean httpOk = con.getResponseCode() == HttpURLConnection.HTTP_OK;
    // get correct stream
    InputStream in = httpOk ? con.getInputStream() : con.getErrorStream();

    String result = "";
    if (in!=null) {
      // limit max chars
      BoundedInputStream bin = new BoundedInputStream(in, limit > 0 ? limit : -1);
      result = IOUtils.toString(bin);
      FileUtil.close(bin);
    }

    return result;
  }

}
