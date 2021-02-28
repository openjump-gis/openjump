package com.vividsolutions.wms;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.openjump.util.URLConnectionProvider;

import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.Logger;

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
   * @param version the WMS version to be used
   */
  public void setWMSVersion(String version) {
    this.version = version;
  }

  /**
   * must be implemented according to the specific needs
   * 
   * @return the URL of the WMS request
   * @throws MalformedURLException
   */
  abstract public URL getURL() throws MalformedURLException;

  /**
   * unified way to create a url connection, may be overwritten and modified
   * 
   * @return the HttpURLConnection to use for the request
   * @throws IOException
   */
  protected HttpURLConnection prepareConnection() throws IOException {
    URL requestUrl = getURL();
    // by default we follow redirections
    con = URLConnectionProvider.getInstance().getHttpConnection(requestUrl, true);

    return con;
  }

  /**
   * for implementations seeking to work with the connection to retrieve headers
   * or such.
   * 
   * {@inheritDoc}
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
   * @return the request response as an InputStream
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
   * @return the request response as text
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

  private static Pattern charsetPattern = null;

  protected String readConnection(HttpURLConnection con, long limit) throws IOException {
    boolean httpOk = con.getResponseCode() == HttpURLConnection.HTTP_OK;
    // get correct stream
    InputStream in = httpOk ? con.getInputStream() : con.getErrorStream();

    //Logger.trace(con.getURL().toString());

    String contentType = con.getContentType();
    Charset charset = StandardCharsets.UTF_8;
    try {
      if (contentType != null) {
        // avoid recompiling regex pattern
        if ( charsetPattern == null )
          charsetPattern = Pattern.compile("(?i:charset)=[\"']?([\\w-]+)[\\\"']?");
        Matcher matcher = charsetPattern.matcher(contentType);
        if (matcher.find()) {
          String charsetName = matcher.group(1);
          charset = Charset.forName(charsetName);
        }
      }
    } catch (Exception e) {
      Logger.error("Content-type charset raised error. "+contentType,e);
    }

    String result = "";
    if (in!=null) {
      // limit max chars
      BoundedInputStream bin = new BoundedInputStream(in, limit > 0 ? limit : -1);
      // we use parsed HttpURLConnection.getContentType() charset here
      result = IOUtils.toString(bin, charset);
      FileUtil.close(bin);
    }

    return result;
  }

}
