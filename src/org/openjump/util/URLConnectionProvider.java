package org.openjump.util;

import static javax.swing.JOptionPane.YES_NO_OPTION;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.*;
import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.network.ProxySettingsOptionsPanel;
import com.vividsolutions.wms.WMSException;

public class URLConnectionProvider {

  public static String KEY = URLConnectionProvider.class.getName() + " - UNCERTIFIED_AUTHORIZED_URL";
  // keep list of trusted url per session
  private static Set<URL> trustedURLs = new HashSet<URL>();

  public static URLConnectionProvider instance;

  // use getInstance() instead
  private URLConnectionProvider() {
    super();
  }

  public static URLConnectionProvider getInstance() {
    if (instance == null) {
      instance = new URLConnectionProvider();
    }
    return instance;
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
   * @param url URL
   * @return a URLConnection
   * @throws IOException if an IOException occurs
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
    // apply auth and timeouts
    connection = applyParametersAndSettings(connection);

    // use base url, no need to bother user with the full http query
    URL baseURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath());
    try {
      setTrustOption(false, baseURL);
      connection.connect(); // try to connect
      return connection;    // can connect
    } catch(GeneralSecurityException|SSLException e) {
      if (isTrusted(baseURL) || askIfUserAllowsInvalidCertificate(baseURL)) {
        try {
          // we are in the list or just allowed by user
          setTrustOption(true, baseURL);
          connection = (HttpURLConnection) url.openConnection();
          // apply auth and timeouts
          connection = applyParametersAndSettings(connection);

          return connection;
        } catch(GeneralSecurityException ex2) {
          throw new IOException(ex2);
        }
      } else {
        throw new IOException(e);
      }
    }
  }

  // run this *every time* after url.openConnection() to make sure auth and default timeouts are set
  private HttpURLConnection applyParametersAndSettings( HttpURLConnection connection ) {
    // add auth info if any
    String userInfo = connection.getURL().getUserInfo();
    if (userInfo != null) {
      byte[] userInfoBytes = UriUtil.urlDecode(userInfo).getBytes(StandardCharsets.UTF_8);
      String auth = org.apache.commons.codec.binary.Base64.encodeBase64String(userInfoBytes);
      connection.setRequestProperty("Authorization", "Basic " + auth);
      Logger.trace("Added auth header 'Authorization: Basic "+auth+"'");
    }

    // apply timeouts from settings
    connection.setConnectTimeout(Integer.parseInt(
        ProxySettingsOptionsPanel.getInstance().getSetting(ProxySettingsOptionsPanel.OPEN_TIMEOUT_KEY).toString()));
    connection.setReadTimeout(Integer.parseInt(
        ProxySettingsOptionsPanel.getInstance().getSetting(ProxySettingsOptionsPanel.READ_TIMEOUT_KEY).toString()));

    return connection;
  }

  private boolean askIfUserAllowsInvalidCertificate(URL url) {
    String text = I18N.getInstance().get(
        "com.vididsolutions.wms.WMService.UnverifiedCertificate",
        UriUtil.urlStripPassword(url.toString()));

    // JEditor wraps nicely, allow Scrollbar resizing
    JEditorPane textPane = new JEditorPane("text/plain", text) {
      @Override
      public boolean getScrollableTracksViewportWidth() {
        return true;
      }
    };

    // set a proper initial width
    textPane.setSize(new Dimension(400, 10));
    textPane.setPreferredSize(new Dimension(400, textPane.getPreferredSize().height));

    // fixup look
    textPane.setBackground(new JOptionPane().getBackground());
    textPane.setBorder(null);

    // make it scrollable
    JScrollPane scrollPane = new JScrollPane(textPane);
    scrollPane.setBorder(null);

    // make the JOptionPane resizable
    textPane.addHierarchyListener(new HierarchyListener() {
      public void hierarchyChanged(HierarchyEvent e) {
        Window window = SwingUtilities.getWindowAncestor(textPane);
        if (window instanceof Dialog) {
          Dialog dialog = (Dialog) window;
          if (!dialog.isResizable()) {
            dialog.setResizable(true);
          }
          // remove icon
          Image img = new BufferedImage(1, 1,BufferedImage.TYPE_INT_ARGB_PRE);
          dialog.setIconImage(img);
          // pack to fit screen
          dialog.pack();
        }
      }
    });

    int r = JOptionPane.showConfirmDialog(
            null,scrollPane,
            "Confirmation dialog",
            YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
    return r == JOptionPane.YES_OPTION;
  }

  // a dummy trust manager that actually accepts everything
  private TrustManager trm = new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() { return null; }
    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
  };

  /**
   * setDefaultSSLSocketFactory of HttpsURLConnection to a dummy trust managed
   * in case user requested to do so, remember this choice during runtime
   * 
   * @param trust true if the client must be trust
   * @param url the URL to connect to
   * @throws GeneralSecurityException if a GeneralSecurityException occurs
   */
  private void setTrustOption(boolean trust, URL url) throws GeneralSecurityException {
    SSLContext sc = SSLContext.getInstance("SSL");
    String host = url != null ? url.getHost() : "";
    if (trust || (url != null && trustedURLs.contains(url))) {
      Logger.info("Certificate verification for trusted host '" + host + "' is disabled'");
      sc.init(null, new TrustManager[] { trm }, null);
      trustedURLs.add(url);
    } else {
      Logger.debug("Using the system trust manager to verify certificate for host '"+host+"'.");
      sc.init(null, null, null);
    }
    // TODO: we should maybe not set a factory for _all_ connections here, 
    //       or at least reset when we are done, maybe rewriting WMS using 
    //       a more sophisticated http client is the way to go?
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }

  private boolean isTrusted(URL url) {
    return trustedURLs.contains(url);
  }
}
