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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.TransparencyPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.wms.WMService;

public class EditWMSQueryPanel extends JPanel {
  private MapLayerPanel mapLayerPanel = new MapLayerPanel();
  private JLabel srsLabel = new JLabel();
  private JLabel formatLabel = new JLabel();
  private JComboBox srsComboBox = new JComboBox();
  private JComboBox formatComboBox = new JComboBox();

  private Border border1;
  private TransparencyPanel transparencyPanel = new TransparencyPanel();
  private JLabel transparencyLabel = new JLabel();

  private SelectUrlWithAuthPanel urlPanel;

  private EnableCheck[] enableChecks = new EnableCheck[] { new EnableCheck() {
    public String check(JComponent component) {
      return mapLayerPanel.getChosenMapLayers().isEmpty() ? I18N
          .get("ui.plugin.wms.EditWMSQueryPanel.at-least-one-wms-must-be-chosen")
          : null;
    }
  }, new EnableCheck() {
    public String check(JComponent component) {
      return srsComboBox.getSelectedItem() == null ? MapLayerWizardPanel.NO_COMMON_SRS_MESSAGE
          : null;
    }
  } };

  private WMSLayer layer;

  public EditWMSQueryPanel(WMSLayer layer) {

    this.layer = layer;
    WMService service = null;
    int alpha = layer.getAlpha();

    try {
      service = layer.getService();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      jbInit();
      String url = service.getServerUrl();
      if (url.endsWith("?") || url.endsWith("&")) {
        url = url.substring(0, url.length() - 1);
      }
      urlPanel.setUrl(url);

      mapLayerPanel.init(service, layer.getLayerNames());
      refreshParamCombos();

      mapLayerPanel.add(new InputChangedListener() {
        public void inputChanged() {
          refreshParamCombos();
        }
      });
      
      setAlpha(alpha);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void refreshParamCombos() {
    WMService service = mapLayerPanel.getService();
    // uhhh ohh
    if (service == null){
      srsComboBox.setModel(new DefaultComboBoxModel());
      formatComboBox.setModel(new DefaultComboBoxModel());
      return;
    }
    
      // update SRS dropdown
      Object prevSelected;
      DefaultComboBoxModel comboBoxModel;

      prevSelected = (String) srsComboBox.getSelectedItem();
      if (prevSelected == null)
        prevSelected = layer.getSRS();
      comboBoxModel = new DefaultComboBoxModel();
      for (Iterator i = mapLayerPanel.commonSRSList().iterator(); i.hasNext();) {
        String commonSRS = (String) i.next();
        String srsName = SRSUtils.getName(commonSRS);
        comboBoxModel.addElement(srsName);
      }
      srsComboBox.setModel(comboBoxModel);

      // selectedSRS might no longer be in the combobox, in which case nothing
      // will be selected. [Jon Aquino]
      srsComboBox.setSelectedItem(prevSelected);
      if ((srsComboBox.getSelectedItem() == null)
          && (srsComboBox.getItemCount() > 0)) {
        String srs = srsComboBox.getItemAt(0).toString();
        srsComboBox.setSelectedIndex(0);
      }

      // update format dropdown
      prevSelected = (String) formatComboBox.getSelectedItem();
      if (prevSelected == null)
        prevSelected = layer.getFormat();
      comboBoxModel = new DefaultComboBoxModel();
      for (String f : service.getCapabilities().getMapFormats()) {
        comboBoxModel.addElement(f);
      }
      formatComboBox.setModel(comboBoxModel);

      formatComboBox.setSelectedItem(prevSelected);
      if ((formatComboBox.getSelectedItem() == null)
          && (formatComboBox.getItemCount() > 0)) {
        String format = srsComboBox.getItemAt(0).toString();
        layer.setFormat(format);
        srsComboBox.setSelectedIndex(0);
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
    String srsCode = (String) mapLayerPanel.commonSRSList().get(index);
    return srsCode;
  }

  public String getFormat() {
    return (String) formatComboBox.getSelectedItem();
  }

  void jbInit() throws Exception {
    border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    this.setLayout(new GridBagLayout());
    srsLabel.setText(I18N
        .get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system"));
    formatLabel.setText(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
    this.setBorder(border1);
    this.setToolTipText("");

    srsComboBox.setToolTipText("");
    transparencyLabel.setText(I18N
        .get("ui.plugin.wms.EditWMSQueryPanel.transparency"));

    Insets defaultInsets = new Insets(3, 3, 3, 3);

    this.add(mapLayerPanel, new GridBagConstraints(1, 2, 5, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0,
            10, 0), 0, 0));

    Insets zeroInsets = new Insets(0, 0, 0, 0);

    this.add(srsLabel, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    this.add(srsComboBox, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    this.add(formatLabel, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    this.add(formatComboBox, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));

    this.add(transparencyLabel, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
    // transparencyPanel.setBorder(new LineBorder(Color.black));
    this.add(transparencyPanel, new GridBagConstraints(2, 5, 5, 1, 1.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, defaultInsets,
        0, 0));

    String[] urlList;
    String urlString = (String) PersistentBlackboardPlugIn.getInstance().get(
        AddWmsLayerWizard.CACHED_URL_KEY);
    urlList = (urlString != null) ? urlString.split(",")
        : AddWmsLayerWizard.DEFAULT_URLS;

    urlPanel = new SelectUrlWithAuthPanel(urlList);
    urlPanel.setBorder(BorderFactory.createTitledBorder(URLWizardPanel.TITLE));
    this.add(urlPanel, new GridBagConstraints(1, 0, 7, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, zeroInsets, 0,
        0));

    JPanel versionPanel = URLWizardPanel.getInstance().createVersionPanel();
    versionPanel.setBorder(BorderFactory.createTitledBorder(I18N
        .get("GenericNames.version")));
    this.add(versionPanel, new GridBagConstraints(1, 1, 3, 1, 1.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, zeroInsets,
        0, 0));

    JButton connectButton = new JButton(I18N.get("GenericNames.reconnect"));
    connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        reinitializeService();
      }
    });

    this.add(connectButton, new GridBagConstraints(4, 1, 2, 1, 0.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, zeroInsets, 0, 0));

  }

  protected void reinitializeService() {
    String url = urlPanel.getUrl();
    url = UriUtil
        .urlAddCredentials(url, urlPanel.getUser(), urlPanel.getPass());

    url = WMService.legalize(url);
    // [UT] 20.04.2005
    WMService service = new WMService(url, URLWizardPanel.wmsVersion);

    try {
      service.initialize(true);
      mapLayerPanel.init(service, layer.getLayerNames());
    } catch (Exception e) {
      e.printStackTrace();
      mapLayerPanel.reset();
    } finally {
      refreshParamCombos();;
    }
  }

  public List getChosenMapLayers() {
    return mapLayerPanel.getChosenMapLayers();
  }

  public EnableCheck[] getEnableChecks() {
    return enableChecks;
  }

  public WMService getService(){
    return mapLayerPanel.getService();
  }
}
