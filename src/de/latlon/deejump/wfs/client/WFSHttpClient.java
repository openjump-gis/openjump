package de.latlon.deejump.wfs.client;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.deegree.enterprise.WebUtils;
import org.openjump.util.UriUtil;

import com.vividsolutions.wms.WMService;

/**
 * a client specifically tailored to do our WFS requests
 */
public class WFSHttpClient extends HttpClient {
  AbstractWFSWrapper wfsService;

  public WFSHttpClient(AbstractWFSWrapper wfsService) {
    super();
    this.wfsService = wfsService;
    this._init();
  }

  public WFSHttpClient() {
    super();
    this._init();
  }

  private WFSHttpClient(HttpClientParams params,
      HttpConnectionManager httpConnectionManager) {
    super(params, httpConnectionManager);
    this._init();
  }

  private WFSHttpClient(HttpClientParams arg0) {
    super(arg0);
    this._init();
  }

  private WFSHttpClient(HttpConnectionManager httpConnectionManager) {
    super(httpConnectionManager);
    this._init();
  }

  private void _init() {
    HttpClientParams clientPars = new HttpClientParams();
    // set timeout to 5s
    clientPars.setConnectionManagerTimeout(WMService.TIMEOUT_OPEN);
    clientPars.setSoTimeout(WMService.TIMEOUT_READ);
    clientPars.setContentCharset("UTF-8");
    this.setParams(clientPars);

    // always add auth, we use this client for this specific wfs server only
    // anyway
    if (wfsService != null) {
      Credentials creds = new UsernamePasswordCredentials(wfsService
          .getLogins().getUsername(), wfsService.getLogins().getPassword());
      getState().setCredentials(AuthScope.ANY, creds);
    }
  }

  /**
   * fiddle in some default processing before firing the request eg. setting
   * proxies, auth
   */
  @Override
  public int executeMethod(HostConfiguration hostconfig, HttpMethod method,
      HttpState state) throws IOException, HttpException {

    if (!(method instanceof WFSHttpMethod))
      throw new IllegalArgumentException(
          "WFSHttpClient only executes WFSMethod's");

    String url = ((WFSHttpMethod) method).getWfsUri();
    // enable proxy usage
    WebUtils.enableProxyUsage(this, new URL(url));

    // set auth from url
    if (!UriUtil.urlGetUser(url).isEmpty()) {
      Credentials creds = new UsernamePasswordCredentials(
          UriUtil.urlGetUser(url), UriUtil.urlGetPassword(url));
      getState().setCredentials(AuthScope.ANY, creds);
    }

    return super.executeMethod(hostconfig, method, state);
  }

}
