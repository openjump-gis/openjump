/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2009, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.0 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Información Geográfica, S.L.
 * Avnda. República Argentina, 28
 * Edificio Domocenter Planta 2ª Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * España / Spain
 *
 * Teléfono / Phone Number
 * +34 954 788876
 * 
 * Correo electrónico / Email
 * info@saig.es
 *
 */
package com.vividsolutions.jump.workbench.ui.network;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import org.saig.jump.widgets.config.HTTPProxySettings;
import org.saig.jump.widgets.config.ProxyAuth;
import org.saig.jump.widgets.util.AbstractWaitDialog;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.OptionsPanelV2;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Formatter;
import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

/**
 * Allows to configure the network connection (through a proxy HTTP or SOCKS)
 * <p>
 * Configures the proxy properties and allows to check if the connection is
 * correct
 * </p>
 * 
 * @author Sergio Baños Calvo
 * @author Ede
 * @since Kosmo 2.0
 * @since OJ 1.8
 */
public class ProxySettingsOptionsPanel extends OptionsPanelV2 {

  /** long serialVersionUID field */
  private static final long serialVersionUID = 1L;
  
  /** default values for timeouts */
  private static final int DEFAULT_TIMEOUT_OPEN =  5000;
  private static final int DEFAULT_TIMEOUT_READ = 20000;


  /** Panel icon */
  public final static Icon ICON = IconLoader.icon("fugue/globe-network.png");

  /** Network configuration keys */
  public final static String HTTP_PROXY_SETTINGS_ENABLED = ProxySettingsOptionsPanel.class
      .getName() + "-Enabled";
  public final static String HTTP_PROXY_SETTINGS_KEY = ProxySettingsOptionsPanel.class
      .getName() + "-Settings";
  public final static String TEST_URL_KEY = ProxySettingsOptionsPanel.class
      .getName() + "-TestUrl";
  public final static String READ_TIMEOUT_KEY = ProxySettingsOptionsPanel.class
      .getName() + "-ReadTimeout";
  public final static String CONNECTION_TIMEOUT_KEY = ProxySettingsOptionsPanel.class
      .getName() + "-ConnectionTimeout";

  /** Connection status icons and labels */
  public final static Icon SUCCESSFULL_CONNECTION_ICON = IconLoader
      .icon("saig/internet_connection_ok.png");
  public final static Icon FAILED_CONNECTION_ICON = IconLoader
      .icon("saig/internet_connection_failed.png");
  public final static String SUCCESSFULL_CONNECTION_LABEL = getMessage("connected");
  public final static String FAILED_CONNECTION_LABEL = getMessage("not-connected");

  /** Nombre asociado al panel de configuracion */
  public final static String NAME = getMessage("network-properties");
  
  private final static String DEFAULT_TEST_URL = "http://www.osgeo.org/";
  private static final String DEFAULT_TEST_URL_REGEX = "^https?://www.osgeo.org/?$";
  
  private final static String[] uservars = new String[]{ "User", "user" , "UserName", "Username", "username" };
  private final static String[] passvars = new String[] { "Pass", "pass", "PassWord", "Password", "password" };

  /** Test connection panel */
  private JPanel testConnectionPanel;
  private JButton testConnectionButton;
  private JLabel connectionResultsLabel;
  private JTextArea connectionErrorText;

  /** Proxy settings panel */
  private JPanel proxySettingsPanel;
  private JCheckBox proxyHTTPEnabledCheckBox;
  private JTextField proxyHostTextField;
  private JTextField proxyPortTextField;
  private JTextField proxyUserTextField;
  private JPasswordField proxyPasswordTextField;

  /** Timeouts settings panel */
  private JPanel timeoutSettingsPanel;
  // uses formatted text fields to handle int values only
  private JFormattedTextField connectionTimeoutTextField;
  private JFormattedTextField readTimeoutTextField;
  
  
  private JTextField directConnectToTextField;

  private JTextField testUrlTextField;
  JScrollPane scroller;

  /** System blackboard */
  protected Blackboard blackboard;

  static {
    // only add new ciphers if java version is >= 1.7
    boolean newCiphers = false;
    try {
      newCiphers = (Double.parseDouble(System.getProperty("java.version")
          .substring(0, 3)) > 1.6);
    } catch (Exception e) {
    }
    // set cipher priorities, last tried first
    System.setProperty("https.protocols", "SSLv3,TLSv1"
        + (newCiphers ? ",TLSv1.1,TLSv1.2" : ""));
  }

  /**
   * @param bb blackboard
   */
  public ProxySettingsOptionsPanel(Blackboard bb) {
    this.blackboard = bb;
    this.setLayout(new GridBagLayout());

    // Add the panels
    FormUtils.addRowInGBL(this, 0, 0, getProxySettingsPanel());
    FormUtils.addRowInGBL(this, 1, 0, getTestConnectionPanel());
    FormUtils.addRowInGBL(this, 2, 0, getTimeoutSettingsPanel());
    FormUtils.addFiller(this, 3, 0);
  }

  /**
   * Builds the test connection panel
   * 
   * @return
   */
  private JPanel getTestConnectionPanel() {
    if (testConnectionPanel == null) {
      testConnectionPanel = new JPanel(new GridBagLayout());
      testConnectionPanel.setBorder(BorderFactory
          .createTitledBorder(getMessage("connection-status")));

      JLabel testUrlLabel = new JLabel(getMessage("test-url"));
      testUrlTextField = new JTextField();

      // gray out and insert default value
      testUrlTextField.getDocument().addDocumentListener(
          new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
              reset(e);
            }

            public void removeUpdate(DocumentEvent e) {
              reset(e);
            }

            public void insertUpdate(DocumentEvent e) {
              reset(e);
            }

            public void reset(DocumentEvent e) {
              String testUrl = testUrlTextField.getText();
              if (testUrl.isEmpty()) {
                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    if (testUrlTextField.getText().isEmpty())
                      testUrlTextField.setText(DEFAULT_TEST_URL);
                  }
                });
                ;

              }
              boolean defValue = isDefaultTestUrl(testUrl);
              testUrlTextField.setForeground(defValue ? Color.gray
                  : Color.black);
            }
          });

      // Create the panel components
      JPanel buttonPanel = new JPanel(new FlowLayout());
      testConnectionButton = new JButton(
          getMessage("check-internet-connection"));
      testConnectionButton.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          // Check that the parameters are ok
          String errorMessage = validateInput();
          if (StringUtils.isNotEmpty(errorMessage)) {
            JOptionPane.showConfirmDialog(null, errorMessage,
                getMessage("config-error"), JOptionPane.ERROR_MESSAGE);
            return;
          }
          new AbstractWaitDialog(JUMPWorkbench.getInstance().getFrame(),
              getMessage("checking-internet-connection")) {

            protected void methodToPerform() {
              String result;
              try {
                // run the test
                result = isConnected();
                // check http status
                if (!result.matches("^(?i)HTTP/[0-9\\.]+\\s+[123].*"))
                  throw new Exception(result);
                connectionResultsLabel.setText(SUCCESSFULL_CONNECTION_LABEL);
                connectionResultsLabel.setIcon(SUCCESSFULL_CONNECTION_ICON);
              } catch (Exception e) {
                connectionResultsLabel.setText(FAILED_CONNECTION_LABEL);
                connectionResultsLabel.setIcon(FAILED_CONNECTION_ICON);
                result = e.getClass().getName() + " -> " + e.getMessage();
                JUMPWorkbench.getInstance().getFrame()
                    .log(Arrays.toString(e.getStackTrace()));
                ;
              }
              connectionErrorText.setText(result);
              scroller.setVisible(!result.isEmpty());
              testConnectionPanel.revalidate();
            }
          }.setVisible(true);

        }

      });

      JPanel connectionResultsPanel = new JPanel(new FlowLayout());
      connectionResultsLabel = new JLabel();
      connectionResultsLabel.setAlignmentX(CENTER_ALIGNMENT);
      connectionResultsLabel.setAlignmentY(CENTER_ALIGNMENT);
      connectionErrorText = new JTextArea(5, 5);
      connectionErrorText.setEditable(false);
      connectionErrorText.setLineWrap(true);
      connectionErrorText.setFont(connectionResultsLabel.getFont());
      scroller = new JScrollPane(connectionErrorText);
      scroller.setMinimumSize(connectionErrorText
          .getPreferredScrollableViewportSize());
      scroller.setVisible(false);

      // Add the components to the panel
      buttonPanel.add(testConnectionButton);
      connectionResultsPanel.add(connectionResultsLabel);

      FormUtils.addRowInGBL(testConnectionPanel, 0, 0, testUrlLabel,
          testUrlTextField);
      FormUtils.addRowInGBL(testConnectionPanel, 1, 0, connectionResultsPanel);
      FormUtils.addRowInGBL(testConnectionPanel, 2, 0, buttonPanel);
      FormUtils.addRowInGBL(testConnectionPanel, 3, 0, scroller);

    }
    return testConnectionPanel;
  }

  /**
   * Builds the proxy settings panel
   * 
   * @return
   */
  private JPanel getProxySettingsPanel() {
    if (proxySettingsPanel == null) {
      proxySettingsPanel = new JPanel(new GridBagLayout());
      proxySettingsPanel.setBorder(BorderFactory
          .createTitledBorder(getMessage("proxy")));

      // Create the panel components
      proxyHTTPEnabledCheckBox = new JCheckBox(
          getMessage("enable-proxy-connection-through-http(s)"));
      proxyHTTPEnabledCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          refreshEditability();
        }

      });

      JLabel proxyHostLabel = new JLabel(getMessage("proxy-server"));
      proxyHostTextField = new JTextField();

      JLabel proxyPortLabel = new JLabel(getMessage("proxy-port"));
      proxyPortTextField = new JTextField();
      proxyPortTextField.setInputVerifier(new InputVerifier() {
        @Override
        public boolean verify(JComponent input) {
          try {
            int port = Integer.parseInt(proxyPortTextField.getText().trim());
            //System.out.println(port + "/" + (port > 0 && port <= 65535));
            return (port > 0 && port <= 65535);
          } catch (Exception e) {
            // TODO: handle exception
          }
          return false;
        }
      });

      JLabel proxyUserLabel = new JLabel(getMessage("user"));
      proxyUserTextField = new JTextField();

      JLabel proxyPasswordLabel = new JLabel(getMessage("password"));
      proxyPasswordTextField = new JPasswordField();

      JLabel directConnectToLabel = new JLabel(getMessage("direct-connection"));
      directConnectToTextField = new JTextField();

      // Add the components to the panel
      FormUtils.addRowInGBL(proxySettingsPanel, 0, 0, proxyHTTPEnabledCheckBox);
      FormUtils.addRowInGBL(proxySettingsPanel, 1, 0, proxyHostLabel,
          proxyHostTextField);
      FormUtils.addRowInGBL(proxySettingsPanel, 2, 0, proxyPortLabel,
          proxyPortTextField);
      FormUtils.addRowInGBL(proxySettingsPanel, 3, 0, proxyUserLabel,
          proxyUserTextField);
      FormUtils.addRowInGBL(proxySettingsPanel, 4, 0, proxyPasswordLabel,
          proxyPasswordTextField);
      FormUtils.addRowInGBL(proxySettingsPanel, 5, 0, directConnectToLabel,
          directConnectToTextField);

    }
    return proxySettingsPanel;
  }

  /**
   * New panel to configure connection and read timeout for OGC services and proxy connection
   * @return the Timeout settings panel
   */
  private JPanel getTimeoutSettingsPanel() {
    if (timeoutSettingsPanel == null) {
      timeoutSettingsPanel = new JPanel(new GridBagLayout());
      timeoutSettingsPanel.setBorder(BorderFactory
          .createTitledBorder(getMessage("timeout")));

      JLabel ogcServicesTimeoutLabel = new JLabel(getMessage("ogc-services-timeout"));
      
      
      JLabel readTimeoutLabel = new JLabel(getMessage("read-timeout"));
      NumberFormatter readFormatter = getIntegerFormatter(false, false);
      readTimeoutTextField = new JFormattedTextField(readFormatter);
      
      JLabel connectionTimeoutLabel = new JLabel(getMessage("connection-timeout"));
      NumberFormatter cnxFormatter = getIntegerFormatter(false, false);
      connectionTimeoutTextField = new JFormattedTextField(cnxFormatter);
      
      // Add the components to the panel
      FormUtils.addRowInGBL(timeoutSettingsPanel, 0, 0, ogcServicesTimeoutLabel,
          new JLabel(""));
      FormUtils.addRowInGBL(timeoutSettingsPanel, 1, 0, readTimeoutLabel,
          readTimeoutTextField);
      FormUtils.addRowInGBL(timeoutSettingsPanel, 2, 0, connectionTimeoutLabel,
          connectionTimeoutTextField);

    }
    return timeoutSettingsPanel;
  }

  /**
   * Refresh the components editability depending on the http proxy checkbox
   */
  protected void refreshEditability() {
    boolean isHTTPProxyEnabled = proxyHTTPEnabledCheckBox.isSelected();

    proxyHostTextField.setEnabled(isHTTPProxyEnabled);
    proxyPortTextField.setEnabled(isHTTPProxyEnabled);
    proxyUserTextField.setEnabled(isHTTPProxyEnabled);
    proxyPasswordTextField.setEnabled(isHTTPProxyEnabled);
    directConnectToTextField.setEnabled(isHTTPProxyEnabled);
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void init() {
    // Read the stored values from the blackboard (or, if they doesn´t exist,
    // from the System
    // properties)
    connectionResultsLabel.setIcon(null);

    // Recover the values
    HTTPProxySettings settings = (HTTPProxySettings) blackboard
        .get(HTTP_PROXY_SETTINGS_KEY);

    proxyHTTPEnabledCheckBox.setSelected(settings != null && settings.isEnabled());
    if (settings != null) {
      proxyHostTextField.setText(settings.getHost());
      proxyPortTextField.setText("" + settings.getPort());
      proxyUserTextField.setText(settings.getUserName());
      proxyPasswordTextField.setText(settings.getPassword());
      directConnectToTextField.setText(settings.getDirectConnectionTo());
    }

    String testUrl = (String) blackboard.get(TEST_URL_KEY);
    if (testUrl != null)
      testUrlTextField.setText(testUrl);
    else
      testUrlTextField.setText(DEFAULT_TEST_URL);
    
    // recover new timeout values
    Integer connectionTimeout = (Integer) blackboard.get(CONNECTION_TIMEOUT_KEY);
    if (connectionTimeout != null)
      connectionTimeoutTextField.setText(connectionTimeout.toString());
    else
      connectionTimeoutTextField.setText(
          String.valueOf(
              ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_OPEN));
    
    Integer readTimeout = (Integer) blackboard.get(READ_TIMEOUT_KEY);
    if (readTimeout != null)
      readTimeoutTextField.setText(readTimeout.toString());
    else
      readTimeoutTextField.setText(
          String.valueOf(
              ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_READ));
    

    refreshEditability();

    // Properties ps = System.getProperties();
    // TreeSet<String> v = new TreeSet(ps.keySet());
    // String out = "";
    // for (String key : v) {
    // if (key.matches("^http.*"))
    // out += key + "=" + ps.getProperty(key) + "\n";
    // }
    // System.out.println(out);
  }

  @Override
  public void okPressed() {
    // Save the results into the blackboard
    HTTPProxySettings settings = buildSettingsFromUserParameters();
    applySettingsToSystem(settings);
    // save proxy settings to blackboard
    blackboard.put(HTTP_PROXY_SETTINGS_KEY, settings);

    // save enabled state
    blackboard.put(HTTP_PROXY_SETTINGS_ENABLED, settings.isEnabled());
    
    // save testurl setting to blackboard
    String testUrl = testUrlTextField.getText().trim();
    if (!testUrl.isEmpty() && !isDefaultTestUrl(testUrl))
      blackboard.put(TEST_URL_KEY, testUrl);
    else
      blackboard.remove(TEST_URL_KEY);
    
    // save timeout value as Integer, after converting them from a potentially formatted int (with thousand separator
    // for instance) to a non-formatted value to store in bboard:
    Integer readTimeout = null;
    Integer cnxTimeout = null;
    try {
      readTimeout = NumberFormat.getInstance().parse(
          readTimeoutTextField.getText().trim()).intValue();
    } catch (ParseException pe) {
      readTimeout = ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_READ;
    }
    try {
      cnxTimeout = NumberFormat.getInstance().parse(
          connectionTimeoutTextField.getText().trim()).intValue();
    } catch (ParseException pe) {
      cnxTimeout = ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_OPEN;
    }
    blackboard.put(READ_TIMEOUT_KEY, readTimeout);
    blackboard.put(CONNECTION_TIMEOUT_KEY, cnxTimeout);
  }

  /**
   * Builds the HTTPProxySettings from the user options
   * 
   * @return HTTPProxySettings
   */
  private HTTPProxySettings buildSettingsFromUserParameters() {

    HTTPProxySettings settings = new HTTPProxySettings(
        proxyHTTPEnabledCheckBox.isSelected());
    settings.setHost(StringUtils.trim(proxyHostTextField.getText()));
    settings.setPort(StringUtils.trim(proxyPortTextField.getText()));
    settings.setUserName(StringUtils.trim(proxyUserTextField.getText()));
    settings.setPassword(StringUtils.trim(new String(proxyPasswordTextField
        .getPassword())));
    // preprocess direct connect value
    // - we allow commas (;,) as separator
    // - we remove space chars as they confuse the jre
    String directConnectTo = directConnectToTextField.getText()
        .replaceAll("[,;]+", "|").replaceAll("\\s", "");
    settings.setDirectConnectionTo(directConnectTo);

    settings.setEnabled(proxyHTTPEnabledCheckBox.isSelected());

    return settings;
  }

  @Override
  public String validateInput() {
    String errorMessage = null;

    // Check that if the HTTPProxy is enabled, the host and port have been set
    if (proxyHTTPEnabledCheckBox.isSelected()) {
      String host = StringUtils.trim(proxyHostTextField.getText());
      String port = StringUtils.trim(proxyPortTextField.getText());

      if (!proxyPortTextField.getInputVerifier().verify(proxyPortTextField)
          || StringUtils.isEmpty(host) || StringUtils.isEmpty(port)
          || !StringUtil.isNumber(port)) {
        errorMessage = getMessage("server-or-proxy-port-is-not-correct-check-provided-parameters");
      } else {
        try {
          StringBuffer strUrl = new StringBuffer();
          // add "http://" prefix if it wasn't included.
          strUrl.append(host.startsWith("http://") ? host.toLowerCase()
              : "http://" + host.toLowerCase());

          // add port
          strUrl.append(StringUtils.isNotEmpty(port) ? ":" + port : "");

          // Check that the URL is correctly constructed
          URL url = new URL(strUrl.toString());

          // check if we can resolve the hostname
          String urlHost = url.getHost();
          DNSResolver dnsRes = new DNSResolver(host);
          Thread t = new Thread(dnsRes);
          t.start();
          t.join(2000);
          InetAddress inetAddr = dnsRes.get();
          if (inetAddr == null)
            throw new UnknownHostException(urlHost);

        } catch (Exception e) {
          Logger.error(e);
          errorMessage = getMessage("server-or-proxy-port-is-not-correct-check-provided-parameters");
          errorMessage += "\n " + e.getClass().getName() + " -> "
              + e.getMessage();
        }
      }
    }
    return errorMessage;
  }

  private boolean isDefaultTestUrl(String urlString) {
    return urlString.matches(DEFAULT_TEST_URL_REGEX);
  }

  /**
   * Check if the user is connected to internet using the current configuration
   * 
   * @return
   * @throws Exception
   */
  private String isConnected() throws Exception {

    Properties systemProperties = System.getProperties();

    //printProps("vorher");
    
    // Backup current properties
    List<String> backupVars = new ArrayList(Arrays.asList(new String[] {
        "http.proxyHost", "https.proxyHost", "http.proxyPort",
        "https.proxyPort", "http.nonProxyHosts" }));

    // we double username/password here as both seem to be used by different packages
    // java standard is http.proxyUser/http.proxyPass
    // deegree2 uses http.proxyUser/http.proxyPassword via the deprecated commons httpclient
    List<String> authVars = new ArrayList<String>(Arrays.asList(uservars));
    authVars.addAll(Arrays.asList(passvars));
    for (String string : authVars) {
      backupVars.add("http.proxy"+string);
      backupVars.add("https.proxy"+string);
    }
    
    // now backup defined settings
    Map backupSettings = new HashMap<String, Object>();
    for (String key : backupVars) {
      Object value = systemProperties.get(key);
      if (value!=null)
        backupSettings.put(key, value);
    }

    URLConnection con = null;
    try {
      HTTPProxySettings settings = buildSettingsFromUserParameters();
      applySettingsToSystem(settings);

      //printProps("während");
      
      String testUrl = testUrlTextField.getText().trim();

      URL url = new URL(testUrl);
      con = url.openConnection();
      tuneConnection(con);

      // get all headers
      Map<String, List<String>> map = con.getHeaderFields();
      if (map.values().isEmpty()) {
        readConnection(con);
        throw new Exception("empty document");
      }

      String value = "";
      // find http header
      for (Entry<String, List<String>> entry : map.entrySet()) {
        if (entry.getKey() == null && entry.getValue() != null
            && entry.getValue().size() == 1
            && entry.getValue().get(0).matches("^(?i)HTTP/[0-9\\.]+.*"))
          value = entry.getValue().get(0);
      }

      return value;

    } finally {

      // Restore settings as they were before
      for (String key : backupVars) {
        Object value = backupSettings.get(key);
        if (value != null)
          systemProperties.put(key, value);
        else
          systemProperties.remove(key);
      }

      //printProps("danach");
    }
    
  }

  private void readConnection(URLConnection con) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(
        con.getInputStream()));
    String inputLine;
    while ((inputLine = in.readLine()) != null);
      //System.out.println(inputLine);
    in.close();
  }

  private void tuneConnection(URLConnection con) {
    Integer readTimeout = null;
    Integer cnxTimeout = null;
    try {
      readTimeout = NumberFormat.getInstance().parse(
          readTimeoutTextField.getText().trim()).intValue();
    } catch (ParseException pe) {
      readTimeout = ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_READ;
    }
    try {
      cnxTimeout = NumberFormat.getInstance().parse(
          connectionTimeoutTextField.getText().trim()).intValue();
    } catch (ParseException pe) {
      cnxTimeout = ProxySettingsOptionsPanel.DEFAULT_TIMEOUT_OPEN;
    }

    con.setConnectTimeout(cnxTimeout);
    con.setReadTimeout(readTimeout);
    con.setUseCaches(false);
  }

  private static String getMessage(String id) {
    return I18N.get(ProxySettingsOptionsPanel.class.getName() + "." + id);
  }

  /**
   * Sets the current network settings for the session
   * 
   * @param settings
   *          Settings to stablish
   */
  private static void applySettingsToSystem(HTTPProxySettings settings) {
    // Set the properties to the current session
    Properties systemSettings = System.getProperties();
    if (settings !=null && settings.isEnabled()) {

      systemSettings.put("http.proxyHost", settings.getHost());
      systemSettings.put("https.proxyHost", settings.getHost());
      systemSettings.put("http.proxyPort", settings.getPort() + "");
      systemSettings.put("https.proxyPort", settings.getPort() + "");

      // we double username/password here as both seem to be used by different packages
      // java standard is http.proxyUser/http.proxyPass
      // deegree2 uses http.proxyUser/http.proxyPassword

      if (StringUtils.isNotEmpty(settings.getUserName())) {
        for (String id : uservars) {
          systemSettings.put("http.proxy"+id, settings.getUserName());
          systemSettings.put("https.proxy"+id, settings.getUserName());
        }
      } else {
        for (String id : uservars) {
          systemSettings.remove("http.proxy"+id);
          systemSettings.remove("https.proxy"+id);
        }
      }

      if (StringUtils.isNotEmpty(settings.getPassword())) {
        for (String id : passvars) {
          systemSettings.put("http.proxy" + id, settings.getPassword());
          systemSettings.put("https.proxy" + id, settings.getPassword());
        }
      } else {
        for (String id : passvars) {
          systemSettings.remove("http.proxy" + id);
          systemSettings.remove("https.proxy" + id);
        }
      }

      if (StringUtils.isNotEmpty(settings.getDirectConnectionTo())) {
        systemSettings.put("http.nonProxyHosts",
            settings.getDirectConnectionTo());
      } else {
        systemSettings.remove("http.nonProxyHosts");
      }

      // this is a WORKAROUND for 
      // "java caches the first successful auth and does not allow to change it anymore afterwards"
      // see http://stackoverflow.com/questions/480895/reset-the-authenticator-credentials
      // sun.net.www.protocol.http.AuthCacheValue.setAuthCache(new
      // sun.net.www.protocol.http.AuthCacheImpl());
      try {
        Class clazzParam = Class.forName("sun.net.www.protocol.http.AuthCache");
        Class clazzParamImpl = Class
            .forName("sun.net.www.protocol.http.AuthCacheImpl");
        Object cache = clazzParamImpl.newInstance();
        Class clazz = Class.forName("sun.net.www.protocol.http.AuthCacheValue");
        // for (Method m : clazz.getDeclaredMethods()) {
        // System.out.println(m);
        // }
        Method method = clazz.getDeclaredMethod("setAuthCache", clazzParam);
        method.setAccessible(true);
        Object o = method.invoke(null, cache);
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (!settings.getUserName().isEmpty()) {
        Authenticator.setDefault(new ProxyAuth(settings.getUserName(), settings
            .getPassword()));
      }

    } else {
      systemSettings.remove("http.proxyHost");
      systemSettings.remove("https.proxyHost");
      systemSettings.remove("http.proxyPort");
      systemSettings.remove("https.proxyPort");
      // remove all user/pass variables
      ArrayList<String> authVars = new ArrayList<String>(Arrays.asList(uservars));
      authVars.addAll(Arrays.asList(passvars));
      for (String string : authVars) {
        systemSettings.remove("http.proxy"+string);
        systemSettings.remove("https.proxy"+string);
      }
      systemSettings.remove("http.nonProxyHosts");
    }
  }

  public static void restoreSystemSettings(Blackboard blackboard) {
    // Recover the values
    HTTPProxySettings settings = (HTTPProxySettings) blackboard
        .get(HTTP_PROXY_SETTINGS_KEY);
    if (settings != null) {
      boolean enabled = blackboard.get(HTTP_PROXY_SETTINGS_ENABLED, true);
      settings.setEnabled(enabled);
      applySettingsToSystem(settings);
    }
  }

  private static void printProps(String title){
    Properties ps = System.getProperties();
    TreeSet<String> v = new TreeSet(ps.keySet());
    String out = title+"\n";
    for (String key : v) {
      if (key.matches("^http.*"))
        out += key + "=" + ps.getProperty(key) + "\n";
    }
    System.out.println(out);
  }
  
    private NumberFormatter getIntegerFormatter(boolean allowInvalid, boolean commitsOnValidEdit) {
    NumberFormat format = NumberFormat.getInstance();
    NumberFormatter formatter = new NumberFormatter(format);
    formatter.setValueClass(Integer.class);
    formatter.setMinimum(0);
    formatter.setMaximum(Integer.MAX_VALUE);
    formatter.setAllowsInvalid(allowInvalid);
    // If you want the value to be committed on each keystroke instead of focus lost
    formatter.setCommitsOnValidEdit(commitsOnValidEdit);
    
    return formatter;
  }

}

class DNSResolver implements Runnable {
  private String domain;
  private InetAddress inetAddr;

  public DNSResolver(String domain) {
    this.domain = domain;
  }

  public void run() {
    try {
      InetAddress addr = InetAddress.getByName(domain);
      set(addr);
    } catch (UnknownHostException e) {

    }
  }

  public synchronized void set(InetAddress inetAddr) {
    this.inetAddr = inetAddr;
  }

  public synchronized InetAddress get() {
    return inetAddr;
  }
  
}
