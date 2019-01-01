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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

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
import com.vividsolutions.wms.WMSException;
import com.vividsolutions.wms.WMService;

public class URLWizardPanel extends JPanel implements WizardPanelV2 {
  
  private static URLWizardPanel instance = null;
  
  public static final String SERVICE_KEY = "SERVICE";

  public static final String FORMAT_KEY = "FORMAT";

  public static final String URL_KEY = "URL";
  
  public static final String I18N_PREFIX = "ui.plugin.wms.URLWizardPanel.";

  private InputChangedFirer inputChangedFirer = new InputChangedFirer();

  private Map dataMap;

  private String[] urlList;
  private SelectUrlWithAuthPanel urlPanel;

  // [UT]
  public static final String VERSION_KEY = "WMS_VERSION";
  public static final String TITLE = I18N.get(I18N_PREFIX + "select-uniform-resource-locator-url");

  // this is a hack, guess why
  public static String wmsVersion = WMService.WMS_1_1_1;
  public static final String[] wmsVersions = new String[] { WMService.WMS_1_0_0,
      WMService.WMS_1_1_0, WMService.WMS_1_1_1, WMService.WMS_1_3_0 };
  private String[] initialUrls = new String[0];

  private boolean lossyPreferred = true;

  private URLWizardPanel() {
    try {
      String urlString = (String)PersistentBlackboardPlugIn.getInstance()
          .get(AddWmsLayerWizard.CACHED_URL_KEY);
          this.initialUrls = (urlString != null) ? urlString.split(",") : AddWmsLayerWizard.DEFAULT_URLS;
      
      this.dataMap = new LinkedHashMap<String,Object>();
      
      jbInit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void add(InputChangedListener listener) {
    inputChangedFirer.add(listener);
  }

  public void remove(InputChangedListener listener) {
    inputChangedFirer.remove(listener);
  }

  void jbInit() throws Exception {

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
    urlPanel.setBorder(BorderFactory.createTitledBorder(getTitle()));

    keepNorth.add(urlPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,
            0, 0, 0), 0, 0));

    JPanel versionPanel = createVersionPanel();
    versionPanel.setBorder(BorderFactory.createTitledBorder(I18N.get("ui.GenericNames.version")));
    keepNorth.add(versionPanel, new GridBagConstraints(0, 1,
        1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));

    this.setLayout(new GridBagLayout());
    this.add(keepNorth, new GridBagConstraints(0, 1, 1, 1, 1, 1,
        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,
            0, 0, 0), 0, 0));
  }

  public String getInstructions() {
    return I18N.get(I18N_PREFIX + "please-enter-the-url-of-the-wms-server");
  }

  public void exitingToRight() throws IOException, WorkbenchException {
    try {
      String url = urlPanel.getUrl();
      url = UriUtil.urlAddCredentials(url, urlPanel.getUser(), urlPanel.getPass());

      // [UT]
      // String ver = (String)dataMap.get(VERSION_KEY);

      url = WMService.legalize(url);
      // [UT] 20.04.2005
      WMService service = new WMService(url, wmsVersion);
      // WMService service = new WMService( url );

      service.initialize(true);
      
      Set<String> list = new LinkedHashSet<String>();
      // insert latest on top 
      list.add(url);
      // add the rest
      list.addAll(Arrays.asList(urlList));
      
      dataMap.put(URL_KEY, list.toArray(new String[0]));
      //PersistentBlackboardPlugIn.get
      
      dataMap.put(SERVICE_KEY, service);
      // [UT] 20.04.2005 added version
      MapImageFormatChooser formatChooser = new MapImageFormatChooser(wmsVersion);

      formatChooser.setPreferLossyCompression(false);
      formatChooser.setTransparencyRequired(true);

      String format = formatChooser.chooseFormat(service.getCapabilities()
        .getMapFormats());

      if (format == null) {
        throw new WorkbenchException(
          I18N.get(I18N_PREFIX + "the-server-does-not-support-gif-png-or-jpeg-format"));
      }

      dataMap.put(MapLayerWizardPanel.FORMAT_LIST_KEY, service.getCapabilities().getMapFormats());
      dataMap.put(FORMAT_KEY, format);
      dataMap.put(MapLayerWizardPanel.INITIAL_LAYER_NAMES_KEY, null);
      dataMap.put(VERSION_KEY, wmsVersion);
    } catch (WMSException e) {
      throw new CancelNextException(e);
    } catch (IOException e) {
      throw new CancelNextException(e);
    }
  }

  public void enteredFromLeft(Map dataMap) {
    this.dataMap = dataMap;
//    urls.getEditor().selectAll();
  }

  public void enteredFromRight() throws Exception {
    // System.out.println(Arrays.toString((String[])dataMap.get(URL_KEY)));
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
      return host.length() > 0;
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

    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JRadioButton jb = (JRadioButton)e.getSource();
        URLWizardPanel.wmsVersion = jb.getText();
      }
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


  
  // [UT] 20.10.2005
  private Component createLossyCheckBox() {
    JPanel p = new JPanel();
    JCheckBox checkBox = new JCheckBox(I18N.get(I18N_PREFIX + "prefer-lossy-images"), true);
    // );
    checkBox.setToolTipText("This will try to load JPEG images, if the WMS allows it.");
    checkBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lossyPreferred = ((JCheckBox)e.getSource()).isSelected();
      }
    });
    p.add(checkBox);
    return p;
  }
  


  public static URLWizardPanel getInstance(){
    if (instance == null) {
      instance = new URLWizardPanel();
    }
    
    return instance;
  }
}


