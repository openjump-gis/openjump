package com.vividsolutions.wms;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * a barebone {@link AbstractWMSRequest} implementation. this should really only
 * be used if implementing proper requests like {@link MapRequest} or
 * {@link FeatureInfoRequest} is too much of an effort
 */
public class BasicRequest extends AbstractWMSRequest {
  private URL url;

  public BasicRequest(WMService service, URL url) {
    super(service);
    if (url == null)
      throw new IllegalArgumentException("url must not be null");
    this.url = url;
  }

  @Override
  public URL getURL() throws MalformedURLException {
    return url;
  }
}
