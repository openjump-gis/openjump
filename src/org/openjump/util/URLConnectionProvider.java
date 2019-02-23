package org.openjump.util;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import static javax.swing.JOptionPane.YES_NO_OPTION;

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

  public URLConnection getConnection(URL url) throws IOException {
    String protocol = url.getProtocol();
    if (!protocol.equals("https")) return url.openConnection();
    URLConnection connection;
    try {
      connection = url.openConnection();
      connection.connect(); // try to connect
      return connection;    // can connect
    } catch(IOException e) {
      String baseURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath()).toString();
      if (authorizedURL.contains(baseURL) || acceptConnection(url)) {
        try {
          setTrustOption(true, url);
          connection = url.openConnection();
          authorizedURL.add(baseURL);
          setTrustOption(false, url);
          return connection;
        } catch(KeyManagementException|NoSuchAlgorithmException ex2) {
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

  private void setTrustOption(boolean trust, URL url)
          throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sc = SSLContext.getInstance("SSL");
    if (trust || trustedURLs.contains(url)) {
      sc.init(null, new TrustManager[]{trm}, null);
      trustedURLs.add(url);
    } else {
      sc.init(null, null, null);
    }
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }
}
