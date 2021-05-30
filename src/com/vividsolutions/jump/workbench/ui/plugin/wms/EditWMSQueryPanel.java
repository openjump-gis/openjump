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
import java.util.*;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.wms.MapStyle;
import org.apache.commons.lang3.ArrayUtils;
import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.TransparencyPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;

/**
 * Panel used to edit elements of an existing connection in order to change
 * some of its parameters.
 */
public class EditWMSQueryPanel extends JPanel {

  private final MapLayerPanel mapLayerPanel = new MapLayerPanel();
  private final JComboBox<String> srsComboBox = new JComboBox<>();
  private final JComboBox<String> formatComboBox = new JComboBox<>();
  private final JComboBox<MapStyle> styleComboBox = new JComboBox<>();
  private final JTextField moreParametersTextField = new JTextField(16);
  private final TransparencyPanel transparencyPanel = new TransparencyPanel();

  private SelectUrlWithAuthPanel urlPanel;
  String[] savedUrlList;
  private JButton connectButton;

  private final EnableCheck[] enableChecks = new EnableCheck[] {
          component -> mapLayerPanel.getChosenMapLayers().isEmpty() ?
                  I18N.get("ui.plugin.wms.EditWMSQueryPanel.at-least-one-wms-must-be-chosen")
                  : null,
          component -> srsComboBox.getSelectedItem() == null ?
                  MapLayerWizardPanel.NO_COMMON_SRS_MESSAGE : null};

  private final WMSLayer layer;

  public EditWMSQueryPanel(WMSLayer layer) {

    this.layer = layer;
    WMService service;

    try {
      service = layer.getService();
      if (service != null) {
        jbInit();
        //String url = service.getServerUrl();
        //if (url.endsWith("?") || url.endsWith("&")) {
        //  url = url.substring(0, url.length() - 1);
        //}
        mapLayerPanel.init(service, layer.getLayerNames());
        refreshParamCombos();
        mapLayerPanel.add(this::refreshParamCombos);
        setAlpha(layer.getAlpha());
      }
    } catch (Exception e) {
      Logger.warn(e);
    }
  }

  /**
   * cleans the map table, enables reconnect button to visualize a reconnect is
   * necessary
   */
  private void resetConnection() {
    mapLayerPanel.reset();
    refreshParamCombos();
    connectButton.setEnabled(true);
  }

  /**
   * refresh parameter fields according to wms service or when null emtpy them
   */
  private void refreshParamCombos() {
    WMService service = mapLayerPanel.getService();

    refreshComboBox(srsComboBox, layer.getSRS(),
        mapLayerPanel.commonSRSList().toArray(new String[0]));

    refreshComboBox(formatComboBox, layer.getFormat(),
        service.getCapabilities().getMapFormats());

    refreshComboBox(styleComboBox, layer.getStyle(),
        mapLayerPanel.commonStyleList().toArray(new MapStyle[0]));

    /*
    // uhhh ohh
    if (service == null) {
      srsComboBox.setModel(new DefaultComboBoxModel<>());
      formatComboBox.setModel(new DefaultComboBoxModel<>());
      stylesComboBox.setModel(new DefaultComboBoxModel<>());
      return;
    }

    // update SRS dropdown
    Object prevSelected;
    DefaultComboBoxModel<String> comboBoxModel;

    prevSelected = srsComboBox.getSelectedItem();
    if (prevSelected == null)
      prevSelected = layer.getSRS();
    comboBoxModel = new DefaultComboBoxModel<>();
    for (String commonSRS : mapLayerPanel.commonSRSList()) {
      //String srsName = SRSUtils.getName(commonSRS);
      //comboBoxModel.addElement(srsName);
      comboBoxModel.addElement(commonSRS);
    }
    srsComboBox.setModel(comboBoxModel);

    // selectedSRS might no longer be in the combobox, in which case nothing
    // will be selected. [Jon Aquino]
    srsComboBox.setSelectedItem(prevSelected);
    if ((srsComboBox.getSelectedItem() == null)
        && (srsComboBox.getItemCount() > 0)) {
      srsComboBox.setSelectedIndex(0);
    }

    // update format dropdown
    prevSelected = formatComboBox.getSelectedItem();
    if (prevSelected == null)
      prevSelected = layer.getFormat();
    comboBoxModel = new DefaultComboBoxModel<>();
    for (String f : service.getCapabilities().getMapFormats()) {
      comboBoxModel.addElement(f);
    }
    formatComboBox.setModel(comboBoxModel);

    formatComboBox.setSelectedItem(prevSelected);
    if ((formatComboBox.getSelectedItem() == null)
        && (formatComboBox.getItemCount() > 0)) {
      String format = srsComboBox.getItemAt(0);
      layer.setFormat(format);
      srsComboBox.setSelectedIndex(0);
    }
    */
  }

  private <T> void refreshComboBox(JComboBox<T> comboBox,  T defaultValue, T[] newList) {
    WMService service = mapLayerPanel.getService();
    if (service == null) {
      comboBox.setModel(new DefaultComboBoxModel<>());
    } else {
      // Get previously selected srs
      Object prevSelected = comboBox.getSelectedItem();
      if (prevSelected == null)
        prevSelected = defaultValue;

      // Populate comboBoxModel in case MapLayer has changed
      DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>(newList);
      comboBox.setModel(model);

      // preserve previous selection it it still exists [Jon Aquino]
      comboBox.setSelectedItem(prevSelected);
      if (comboBox.getSelectedItem() == null && comboBox.getItemCount() > 0) {
        comboBox.setSelectedIndex(0);
      }
    }
  }

  public int getAlpha() {
    return 255 - transparencyPanel.getSlider().getValue();
  }

  private void setAlpha(int alpha) {
    transparencyPanel.getSlider().setValue(255 - alpha);
  }

  public String getSRS() {
    int index = srsComboBox.getSelectedIndex();
    // Return selected srs code
    return mapLayerPanel.commonSRSList().get(index);
  }

  /**
   * retrieve the list of urls in the dropdown
   */
  public String[] getUrlList() {
    return savedUrlList.clone();
  }

  public MapStyle getStyle() {
    return (MapStyle)styleComboBox.getSelectedItem();
  }

  public String getMoreParameters() {
    return moreParametersTextField.getText();
  }

  public String getFormat() {
    return (String) formatComboBox.getSelectedItem();
  }

  void jbInit() throws Exception {
    //final JLabel srsLabel =
    //    new JLabel(I18N.get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system"));
    //final JLabel formatLabel =
    //    new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
    //final JLabel stylesLabel =
    //    new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.styles"));
    //final JLabel moreParametersLabel =
    //    new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.more-parameters"));
    final JLabel transparencyLabel =
        new JLabel(I18N.get("ui.plugin.wms.EditWMSQueryPanel.transparency"));

    final Border border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    this.setLayout(new GridBagLayout());
    this.setBorder(border1);
    this.setToolTipText("");

    srsComboBox.setToolTipText("");

    Insets defaultInsets = new Insets(3, 3, 3, 3);

    this.add(mapLayerPanel, new GridBagConstraints(1, 2, 5, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0,
            10, 0), 0, 0));

    Insets zeroInsets = new Insets(0, 0, 0, 0);

    this.add(createParametersPanel(), new GridBagConstraints(1, 3, 5, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    //this.add(srsLabel, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    //this.add(srsComboBox, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    //this.add(stylesLabel, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    //this.add(stylesComboBox, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    //this.add(moreParametersLabel, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    //this.add(moreParametersTextField, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    //this.add(formatLabel, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    //this.add(formatComboBox, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0,
    //    GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    this.add(transparencyLabel, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    // transparencyPanel.setBorder(new LineBorder(Color.black));
    this.add(transparencyPanel, new GridBagConstraints(2, 5, 5, 1, 1.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, defaultInsets,
        0, 0));

    // retrieve persistently saved url list
    String urlString = (String) PersistentBlackboardPlugIn.getInstance().get(
        AddWmsLayerWizard.CACHED_URL_KEY);
    savedUrlList = (urlString != null) ? urlString.split(",")
        : AddWmsLayerWizard.DEFAULT_URLS;

    // put our entry on top, gets autoselected
    Set<String> newUrlList = new LinkedHashSet<>();
    // insert latest on top
    newUrlList.add(layer.getService().getServerUrl());
    // add the rest
    newUrlList.addAll(Arrays.asList(savedUrlList));

    urlPanel = new SelectUrlWithAuthPanel(newUrlList.toArray(new String[0]));
    urlPanel.setBorder(BorderFactory.createTitledBorder(URLWizardPanel.TITLE));
    this.add(urlPanel, new GridBagConstraints(1, 0, 7, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, zeroInsets, 0,
        0));

    // reset on changes in the textfields
    final DocumentListener doli = new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        reset();
      }
      @Override
      public void insertUpdate(DocumentEvent e) {
        reset();
      }
      @Override
      public void changedUpdate(DocumentEvent e) {
        reset();
      }
      private void reset() {
        resetConnection();
      }
    };

    JPanel versionPanel = URLWizardPanel.getInstance().createVersionPanel();
    versionPanel.setBorder(BorderFactory.createTitledBorder(
            I18N.get("GenericNames.version")));
    this.add(versionPanel, new GridBagConstraints(1, 1, 3, 1, 1.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, zeroInsets,
        0, 0));

    // reset all on wms version switch
    final ActionListener ali = e -> resetConnection();

    // find all components to add ali/doli to them in the next step
    final Component[] cs = ArrayUtils.addAll(
        versionPanel.getComponents(), urlPanel.getComponents());
    // give the combobox a chance to fill them first
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        for (final Component c : cs) {
          //System.out.println(c);
          if (c instanceof AbstractButton)
            ((AbstractButton) c).addActionListener(ali);
          else if (c instanceof JTextField)
            ((JTextField) c).getDocument().addDocumentListener(doli);
          else if (c instanceof JComboBox) {
            ((JComboBox<?>) c).addActionListener(ali);
            // also reset if url is edited manually
            Component editor = ((JComboBox<?>) c).getEditor().getEditorComponent();
            if ( editor instanceof JTextComponent) {
              ((JTextComponent)editor).getDocument().addDocumentListener(doli);
            }
          }
        }
      }
    });

    connectButton = new JButton(I18N.get("GenericNames.reconnect"));
    connectButton.addActionListener(e -> reinitializeService());
    // only active if version or urlpanel components was changed
    connectButton.setEnabled(false);

    this.add(connectButton, new GridBagConstraints(4, 1, 2, 1, 0.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, zeroInsets, 0, 0));

  }

  private JPanel createParametersPanel() {
    JPanel panel = new JPanel(new FlowLayout());
    final JLabel srsLabel =
        new JLabel(I18N.get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system"));
    final JLabel formatLabel =
        new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
    final JLabel stylesLabel =
        new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.styles"));
    final JLabel moreParametersLabel =
        new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.more-parameters"));

    panel.add(srsLabel);
    panel.add(srsComboBox);

    panel.add(stylesLabel);
    panel.add(styleComboBox);

    panel.add(moreParametersLabel);
    panel.add(moreParametersTextField);

    panel.add(formatLabel);
    panel.add(formatComboBox);

    return panel;
  }

  protected void reinitializeService() {
    String url = urlPanel.getUrl();
    url = UriUtil.urlAddCredentials(url, urlPanel.getUser(), urlPanel.getPass()).trim();
    WMService service = new WMService(url, URLWizardPanel.wmsVersion);

    try {
      service.initialize(true);
      // refresh list of available wms layers on the server
      mapLayerPanel.init(service, layer.getLayerNames());
      connectButton.setEnabled(false);
    } catch (Exception e) {
      mapLayerPanel.reset();
      JUMPWorkbench.getInstance().getFrame().handleThrowable(e);
    } finally {
      refreshParamCombos();
    }
  }

  public List<MapLayer> getChosenMapLayers() {
    return mapLayerPanel.getChosenMapLayers();
  }

  public EnableCheck[] getEnableChecks() {
    return enableChecks;
  }

  public WMService getService() {
    return mapLayerPanel.getService();
  }

}
