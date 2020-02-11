package org.openjump.util;

import static javax.swing.JOptionPane.YES_NO_OPTION;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.network.ProxySettingsOptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.wms.WMSException;

import net.iharder.Base64;

public class URLConnectionProvider {

  public static String KEY = URLConnectionProvider.class.getName() + " - UNCERTIFIED_AUTHORIZED_URL";
  private Blackboard blackboard;
  private Set<String> authorizedURL;

  public static URLConnectionProvider OJ_URL_CONNECTION_PROVIDER;

  public URLConnectionProvider(Blackboard blackboard) {
    this.blackboard = blackboard;
    this.authorizedURL = (Set<String>)this.blackboard.get(KEY, new HashSet<String>());
  }

  public static URLConnectionProvider getJUMP_URLConnectionProvider() {
    if (OJ_URL_CONNECTION_PROVIDER == null) {
      OJ_URL_CONNECTION_PROVIDER = new URLConnectionProvider(
              PersistentBlackboardPlugIn.get(JUMPWorkbench.getInstance().getContext())
      );
    }
    return OJ_URL_CONNECTION_PROVIDER;
  }

  public URLConnectionProvider(PlugInContext plugInContext) {
    this.blackboard = PersistentBlackboardPlugIn.get(plugInContext.getWorkbenchContext());
    this.authorizedURL = (Set<String>)this.blackboard.get(KEY, new HashSet<String>());
  }

  public HttpURLConnection getHttpConnection(URL url, boolean followRedirects) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) getHttpConnection(url);

    // we handle redirects ourselfs
    connection.setInstanceFollowRedirects(false);
    
    int numRedirects = 0;
    URL prev = null, next = connection.getURL();
    while (followRedirects && !next.equals(prev) ) {
      // redirect max 20 times, see 
      // https://stackoverflow.com/questions/9384474/in-chrome-how-many-redirects-are-too-many
      if (++numRedirects >= 20)
        throw new WMSException("To many redirects ("+numRedirects+") for Url: "+url);

      connection = getHttpConnection(next);
      // we handle redirects ourselfs
      connection.setInstanceFollowRedirects(false);
      
      switch (connection.getResponseCode())
      {
         case HttpURLConnection.HTTP_MOVED_PERM:
         case HttpURLConnection.HTTP_MOVED_TEMP:
            String location = connection.getHeaderField("Location");
            location = URLDecoder.decode(location, "UTF-8");
            prev = connection.getURL();
            next = new URL(prev, location);  // compute relative URLs
            Logger.warn("Follow http redirect to: "+next);
            continue;
      }
      break;
    }

    return connection;
  }

  /**
   * @deprecated use getHttpConnection(url,followRedirects) instead
   * @param url
   * @return
   * @throws IOException
   */
  @Deprecated
  public URLConnection getConnection(URL url) throws IOException {
    return getHttpConnection(url, true);
  }

  public HttpURLConnection getHttpConnection(URL url) throws IOException {
    String protocol = url.getProtocol();
//    if (!protocol.equals("https")) return url.openConnection();
    
    if (!protocol.matches("^(?i:https?)$"))
      throw new IOException("Please provide an http(s):// url.");

    HttpURLConnection connection = (HttpURLConnection)url.openConnection();

    // add auth info if any
    String userInfo = url.getUserInfo();
    if (userInfo != null) {
      String auth = Base64.encodeBytes(UriUtil.urlDecode(userInfo).getBytes(Charset.forName("UTF-8")));
      connection.setRequestProperty("Authorization", "Basic " + auth);
      Logger.trace("Added auth header 'Authorization: Basic "+auth+"'");
    }

    // apply timeouts from settings
    connection.setConnectTimeout(Integer.parseInt(
        ProxySettingsOptionsPanel.getInstance().getSetting(ProxySettingsOptionsPanel.OPEN_TIMEOUT_KEY).toString()));
    connection.setReadTimeout(Integer.parseInt(
        ProxySettingsOptionsPanel.getInstance().getSetting(ProxySettingsOptionsPanel.READ_TIMEOUT_KEY).toString()));

    try {
      setTrustOption(false, url);
      connection.connect(); // try to connect
      return connection;    // can connect
    } catch(GeneralSecurityException e) {
      String baseURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath()).toString();
      if (authorizedURL.contains(baseURL) || acceptConnection(url)) {
        try {
          setTrustOption(true, url);
          connection = (HttpURLConnection) url.openConnection();
          authorizedURL.add(baseURL);
          //setTrustOption(false, null);
          return connection;
        } catch(GeneralSecurityException ex2) {
          throw new IOException(ex2);
        }
      } else {
        throw new IOException(e);
      }
    }
  }

  private boolean acceptConnection(URL url) {
    int r = JOptionPane.showConfirmDialog(
            null,
            I18N.getMessage(
                    "com.vididsolutions.wms.WMService.UnverifiedCertificate",
                    UriUtil.urlStripPassword(url.toString())
            ),
            "Confirmation dialog",
            YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
    return r == JOptionPane.YES_OPTION;
  }

  private TrustManager trm = new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() { return null; }
    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
  };
  private Set<URL> trustedURLs = new HashSet<>();

  /**
   * setDefaultSSLSocketFactory of HttpsURLConnection to a dummy trust managed
   * in case user requested to do so, remember this choice during runtime
   * 
   * @param trust
   * @param url
   * @throws GeneralSecurityException
   */
  private void setTrustOption(boolean trust, URL url) throws GeneralSecurityException {
    SSLContext sc = SSLContext.getInstance("SSL");
    String host = url != null ? url.getHost() : "";
    if (trust || (url != null && trustedURLs.contains(url))) {
      Logger.info("Certificate verification for trusted host '" + host + "' is disabled'");
      sc.init(null, new TrustManager[] { trm }, null);
      trustedURLs.add(url);
    } else {
      Logger.info("Using the system trust manager to verify certificate for host '"+host+"'.");
      sc.init(null, null, null);
    }
    // TODO: we should maybe not set a factory for _all_ connections here
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }
}
