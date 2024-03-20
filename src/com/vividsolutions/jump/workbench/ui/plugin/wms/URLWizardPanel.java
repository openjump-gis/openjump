/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.CancelNextException;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanelV2;
import com.vividsolutions.wms.MapImageFormatChooser;
import com.vividsolutions.wms.WMService;

public class URLWizardPanel extends JPanel implements WizardPanelV2 {
  
  private static URLWizardPanel instance = null;
  
  public static final String SERVICE_KEY = "SERVICE";

  public static final String FORMAT_KEY = "FORMAT";

  public static final String URL_KEY = "URL";

  public static final String API_KEY_NAME_AND_VALUE = "API_KEY_NAME_AND_VALUE";
  
  public static final String I18N_PREFIX = "ui.plugin.wms.URLWizardPanel.";

  private final InputChangedFirer inputChangedFirer = new InputChangedFirer();

  private Map<String,Object> dataMap;

  private String[] urlList;
  private SelectUrlWithAuthPanel urlPanel;
  private ApiKeyPanel apiKeyPanel;

  // [UT]
  public static final String VERSION_KEY = "WMS_VERSION";
  public static final String TITLE = I18N.getInstance().get(I18N_PREFIX + "select-uniform-resource-locator-url");

  public static final String API_KEY_AUTHENTICATION = I18N.getInstance().get(I18N_PREFIX + "api-key-authentication");
  public static final String API_KEY_NAME = I18N.getInstance().get(I18N_PREFIX + "api-key-name");
  public static final String API_KEY_VALUE = I18N.getInstance().get(I18N_PREFIX + "api-key-value");

  // this is a hack, guess why
  public static String wmsVersion = WMService.WMS_1_3_0;
  public static final String[] wmsVersions = new String[] {
      WMService.WMS_1_0_0,
      WMService.WMS_1_1_0,
      WMService.WMS_1_1_1,
      WMService.WMS_1_3_0
  };
  private String[] initialUrls = new String[0];

  private boolean lossyPreferred = true;

  private URLWizardPanel() {
    try {
      String urlString = (String)PersistentBlackboardPlugIn.getInstance()
          .get(AddWmsLayerWizard.CACHED_URL_KEY);
          this.initialUrls = (urlString != null) ? urlString.split(",") : AddWmsLayerWizard.DEFAULT_URLS;
      
      this.dataMap = new LinkedHashMap<>();
      
      jbInit();
    } catch (Exception ex) {
      Logger.warn(ex);
    }
  }

  public void add(InputChangedListener listener) {
    inputChangedFirer.add(listener);
  }

  public void remove(InputChangedListener listener) {
    inputChangedFirer.remove(listener);
  }

  void jbInit() {

    // [UT] 20.10.2005 not added yet; need more testing
    /*
     * this.add(createLossyCheckBox(), new GridBagConstraints(1, 4, 1, 1, 0.0,
     * 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
     * 0, 0), 0, 0));
     */

    JPanel keepNorth = new JPanel(new GridBagLayout());
    
    urlList = (String[])dataMap.get(URL_KEY);
    if (urlList==null)
      urlList = initialUrls;

    urlPanel = new SelectUrlWithAuthPanel(urlList);
    urlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), getTitle()));

    keepNorth.add(urlPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(2, 2, 2, 2), 0, 0));

    JPanel versionPanel = createVersionPanel();
    versionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getInstance().get("ui.GenericNames.version")));
    keepNorth.add(versionPanel, new GridBagConstraints(0, 1, 1, 1, 1, 1,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(2, 2, 2, 2), 0, 0));

    apiKeyPanel = new ApiKeyPanel();
    apiKeyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), API_KEY_AUTHENTICATION));
    keepNorth.add(apiKeyPanel, new GridBagConstraints(0, 2, 1, 1, 1, 1,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(2, 2, 2, 2), 0, 0));

    this.setLayout(new GridBagLayout());
    this.add(keepNorth, new GridBagConstraints(0, 1, 1, 1, 1, 1,
        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,
            0, 0, 0), 0, 0));
  }

  public String getInstructions() {
    return I18N.getInstance().get(I18N_PREFIX + "please-enter-the-url-of-the-wms-server");
  }

  public void exitingToRight() throws WorkbenchException {
    try {
      String url = urlPanel.getUrl();
      url = UriUtil.urlAddCredentials(url, urlPanel.getUser(), urlPanel.getPass());
      String apiKeyNameAndValue = apiKeyPanel.getApiKeyNameAndValue();
      if (apiKeyNameAndValue != null) {
        if (url.endsWith("?")) url = url + apiKeyNameAndValue;
        else url = url + "&" + apiKeyNameAndValue;
      }

      WMService service = new WMService(url, wmsVersion);

      service.initialize(true);

      Set<String> list = new LinkedHashSet<>();
      // insert latest on top 
      list.add(url);
      // add the rest
      list.addAll(Arrays.asList(urlList));
      
      dataMap.put(URL_KEY, list.toArray(new String[0]));

      dataMap.put(SERVICE_KEY, service);
      // [UT] 20.04.2005 added version
      MapImageFormatChooser formatChooser = new MapImageFormatChooser(wmsVersion);

      formatChooser.setPreferLossyCompression(false);
      formatChooser.setTransparencyRequired(true);

      String format = formatChooser.chooseFormat(service.getCapabilities()
        .getMapFormats());

      if (format == null) {
        throw new WorkbenchException(
          I18N.getInstance().get(I18N_PREFIX + "the-server-does-not-support-gif-png-or-jpeg-format"));
      }

      dataMap.put(MapLayerWizardPanel.FORMAT_LIST_KEY, service.getCapabilities().getMapFormats());
      dataMap.put(FORMAT_KEY, format);
      dataMap.put(MapLayerWizardPanel.INITIAL_LAYER_NAMES_KEY, null);
      dataMap.put(VERSION_KEY, wmsVersion);
      dataMap.put(API_KEY_NAME_AND_VALUE, apiKeyNameAndValue);
    } catch (IOException e) {
      throw new CancelNextException(e);
    }
  }

  public void enteredFromLeft(Map<String,Object> dataMap) {
    this.dataMap = dataMap;
  }

  public void enteredFromRight() {
    if (dataMap.get(URL_KEY) != null)
      urlPanel.setUrlsList((String[]) dataMap.get(URL_KEY));
  }

  public String getTitle() {
    return TITLE;
  }

  public String getID() {
    return getClass().getName();
  }

  public boolean isInputValid() {
    try {
      String urlString = urlPanel.getUrl();
      String host = new URL(urlString).getHost();
      return !host.isEmpty();
    } catch (Throwable e) {
      return false;
    }
  }

  public String getNextID() {
    return MapLayerWizardPanel.class.getName();
  }

  // [UT] 10.01.2005
  public JPanel createVersionPanel() {
    JPanel p = new JPanel(new GridBagLayout());

    // automatically save version change in URLWizardPanel
    ActionListener al = e -> {
      JRadioButton jb = (JRadioButton)e.getSource();
      URLWizardPanel.wmsVersion = jb.getText();
    };

    String[] versions  = URLWizardPanel.wmsVersions;
    ButtonGroup group = new ButtonGroup();
    JRadioButton[] buttons = new JRadioButton[versions.length];
    for (int i = 0; i < buttons.length; i++) {
      buttons[i] = new JRadioButton(versions[i]);
      buttons[i].addActionListener(al);
      group.add(buttons[i]);
      p.add(buttons[i]);
      // click the last one
      if (versions[i].equals(wmsVersion)) {
        buttons[i].setSelected(true);
      }
    }

    return p;
  }

  private static class ApiKeyPanel extends JPanel {
    public ApiKeyPanel() {
      super(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(2,2,2,2);
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.NONE;
      c.gridx = 0; c.gridy = 0; c.weightx = 0.1;
      add(keyNameLabel, c);
      c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
      add(keyNameField, c);
      c.fill = GridBagConstraints.NONE;
      c.gridx = 0; c.gridy = 1; c.weightx = 0.1;
      add(keyValueName, c);
      c.gridx = 1; c.gridy = 1; c.weightx = 1.0;
      add(keyValueField, c);
    }
    JLabel keyNameLabel = new JLabel(API_KEY_NAME);
    JTextField keyNameField = new JTextField(12);
    JLabel keyValueName = new JLabel(API_KEY_VALUE);
    JTextField keyValueField = new JTextField(12);
    public String getApiKeyNameAndValue() throws UnsupportedEncodingException {
      if (keyNameField.getText() != null && !keyNameField.getText().trim().isEmpty() &&
          keyValueField.getText() != null && !keyValueField.getText().trim().isEmpty()) {
        return URLEncoder.encode(keyNameField.getText(), "UTF-8") + "=" +
            URLEncoder.encode(keyValueField.getText(), "UTF-8");
      } else return null;
    }
  }

  
  // [UT] 20.10.2005
  private Component createLossyCheckBox() {
    JPanel p = new JPanel();
    JCheckBox checkBox = new JCheckBox(I18N.getInstance().get(I18N_PREFIX + "prefer-lossy-images"), true);

    checkBox.setToolTipText("This will try to load JPEG images, if the WMS allows it.");
    checkBox.addActionListener(e -> lossyPreferred = ((JCheckBox)e.getSource()).isSelected());
    p.add(checkBox);

    return p;
  }
  

  public static URLWizardPanel getInstance(){
    if (instance == null) {
      instance = new URLWizardPanel();
    }
    
    return instance;
  }

  @Override
  public void exitingToLeft() {
    // nothing to do
  }
}
