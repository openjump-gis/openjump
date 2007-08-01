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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.TransparencyPanel;
import com.vividsolutions.wms.WMService;


public class EditWMSQueryPanel extends JPanel {
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private MapLayerPanel mapLayerPanel = new MapLayerPanel();
    private JLabel srsLabel = new JLabel();
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private JComboBox srsComboBox = new JComboBox(comboBoxModel);
    private Border border1;
    private TransparencyPanel transparencyPanel = new TransparencyPanel();
    private JLabel transparencyLabel = new JLabel();
    private JLabel urlLabel = new JLabel();
    private JTextField urlTextField = new JTextField();

    private EnableCheck[] enableChecks =
        new EnableCheck[] {
            new EnableCheck() { public String check(JComponent component) {
                return mapLayerPanel.getChosenMapLayers().isEmpty()
                    ? I18N.get("ui.plugin.wms.EditWMSQueryPanel.at-least-one-wms-must-be-chosen")
                    : null;
            }
        }, new EnableCheck() {
            public String check(JComponent component) {
                return srsComboBox.getSelectedItem() == null
                    ? MapLayerWizardPanel.NO_COMMON_SRS_MESSAGE
                    : null;
            }
        }
    };


    public EditWMSQueryPanel(
        WMService service,
        List initialChosenMapLayers,
        String initialSRS,
        int alpha) {
        try {
            jbInit();
            String url = service.getServerUrl();
            if (url.endsWith("?") || url.endsWith("&")) {
                url = url.substring(0, url.length() - 1);
            }
            urlTextField.setText(url);
            mapLayerPanel.init(service, initialChosenMapLayers);

            updateComboBox();
            String srsName = SRSUtils.getName( initialSRS );
            srsComboBox.setSelectedItem(srsName);

            mapLayerPanel.add(new InputChangedListener() {
                public void inputChanged() {
                    updateComboBox();
                }
            });
            setAlpha(alpha);
        } catch (Exception ex) {
            ex.printStackTrace();
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
        String srsCode = (String) mapLayerPanel.commonSRSList().get( index );
        return srsCode;
    }

    /**
    * Method updateComboBox.
    */
    private void updateComboBox() {
        String selectedSRS = (String) srsComboBox.getSelectedItem();
        
        // this method does get called many times when no SRS are available here
        // this makes sure that the selected SRS stays selected when available
        if(mapLayerPanel.commonSRSList().size() == 0) {
            return;
        }
        
        comboBoxModel.removeAllElements();

        for (Iterator i = mapLayerPanel.commonSRSList().iterator(); i.hasNext();) {
            String commonSRS = (String) i.next();
            String srsName = SRSUtils.getName( commonSRS );
            comboBoxModel.addElement( srsName );
        }

        //selectedSRS might no longer be in the combobox, in which case nothing will be selected. [Jon Aquino]
        srsComboBox.setSelectedItem(selectedSRS);
        if ((srsComboBox.getSelectedItem() == null)
            && (srsComboBox.getItemCount() > 0)) {
            srsComboBox.setSelectedIndex(0);
        }
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        this.setLayout(gridBagLayout1);
        srsLabel.setText(I18N.get("ui.plugin.wms.EditWMSQueryPanel.coordinate-reference-system"));
        this.setBorder(border1);
        this.setToolTipText("");
        srsComboBox.setMinimumSize(new Dimension(125, 21));
        srsComboBox.setToolTipText("");
        transparencyLabel.setText(I18N.get("ui.plugin.wms.EditWMSQueryPanel.transparency"));
        urlLabel.setText("URL:");
        urlTextField.setBorder(null);
        urlTextField.setOpaque(false);
        urlTextField.setEditable(false);
        this.add(
            mapLayerPanel,
            new GridBagConstraints(
                1,
                2,
                3,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(10, 0, 10, 0),
                0,
                0));
        this.add(
            srsLabel,
            new GridBagConstraints(
                1,
                3,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 10, 5),
                0,
                0));
        this.add(
            srsComboBox,
            new GridBagConstraints(
                3,
                3,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 10, 0),
                0,
                0));
        this.add(
            transparencyPanel,
            new GridBagConstraints(
                3,
                6,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            transparencyLabel,
            new GridBagConstraints(
                1,
                6,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0,
                0));
        this.add(
            urlLabel,
            new GridBagConstraints(
                1,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5),
                0,
                0));
        this.add(
            urlTextField,
            new GridBagConstraints(
                2,
                0,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
    }
    public List getChosenMapLayers() {
        return mapLayerPanel.getChosenMapLayers();
    }
    public EnableCheck[] getEnableChecks() {
        return enableChecks;
    }
}
