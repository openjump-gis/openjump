package de.latlon.deejump.wfs.client;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;

public class WFSHttpClient extends HttpClient {
  HttpClient nonProxyClient;
  String nonProxyHosts = "";
  
  public WFSHttpClient() {
    super();
    this._init();
  }

  public WFSHttpClient(HttpClientParams params,
      HttpConnectionManager httpConnectionManager) {
    super(params, httpConnectionManager);
    this._init();
  }

  public WFSHttpClient(HttpClientParams arg0) {
    super(arg0);
    this._init();
  }

  public WFSHttpClient(HttpConnectionManager httpConnectionManager) {
    super(httpConnectionManager);
    this._init();
  }

  private void _init() {
    HttpClientParams clientPars = new HttpClientParams();
    // set timeout to 5s
    clientPars.setConnectionManagerTimeout( 5000 );
    this.setParams( clientPars );
    
//    nonProxyClient = new HttpClient();
    
//    // Recover the proxy settings from the System, if they exist
//    Properties systemSettings = System.getProperties();
//    if (systemSettings != null) {
//
//      String proxySet = systemSettings.getProperty("http.proxySet", "false");
//      if (StringUtils.isNotEmpty(proxySet) && proxySet.equals("true")) {
//        String proxyHost = systemSettings.getProperty("http.proxyHost");
//        String proxyPort = systemSettings.getProperty("http.proxyPort");
//
//        this.getHostConfiguration().setProxy(proxyHost,
//            Integer.valueOf(proxyPort));
//
//        String proxyUser = systemSettings.getProperty("http.proxyUser");
//        String proxyPass = systemSettings.getProperty("http.proxyPass");
//
//        if (StringUtils.isNotEmpty(proxyUser)) {
//          Credentials credentials = new UsernamePasswordCredentials(
//              proxyUser, proxyPass);
//          AuthScope scope = new AuthScope(AuthScope.ANY_HOST,
//              AuthScope.ANY_PORT);
//          this.getState().setProxyCredentials(scope, credentials);
//        }
//
//        nonProxyHosts = systemSettings.getProperty("http.nonProxyHosts");
//      }
//    }
  }

//  @Override
//  public int executeMethod(HostConfiguration hostconfig, HttpMethod method,
//      HttpState state) throws IOException, HttpException {
//    System.out.println("e1"+(hostconfig!=null?hostconfig.getHost():getHost()));
//    System.out.println(method.getHostConfiguration().getHost());
//    try {
//      String host = method.getHostConfiguration().getHost();
//      System.out.println(nonProxyHosts.split("\\|"));
//    } catch (NullPointerException e) {
//    }
//    // TODO Auto-generated method stub
//    return super.executeMethod(hostconfig, method, state);
//  }

//  @Override
//  public int executeMethod(HostConfiguration hostConfiguration,
//      HttpMethod method) throws IOException, HttpException {
//    System.out.println("e2"+(hostConfiguration!=null?hostConfiguration.getHost():getHost()));
//    // TODO Auto-generated method stub
//    return super.executeMethod(hostConfiguration, method);
//  }
//
//  @Override
//  public int executeMethod(HttpMethod method) throws IOException, HttpException {
//    System.out.println("e3"+method.getHostConfiguration().getHost());
//    // TODO Auto-generated method stub
//    return super.executeMethod(method);
//  }

  
}
