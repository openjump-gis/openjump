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

import static com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel.COMMON_SRS_LIST_KEY;
import static com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel.FORMAT_LIST_KEY;
import static java.awt.GridBagConstraints.WEST;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;


public class SRSWizardPanel extends JPanel implements WizardPanel {
    public static final String SRS_KEY = "SRS";
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private Map dataMap;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JLabel srsLabel = new JLabel();
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private DefaultComboBoxModel formatBoxModel = new DefaultComboBoxModel();
    private JComboBox comboBox = new JComboBox();
    private JLabel formatLabel;
    private JComboBox formatBox;

    public SRSWizardPanel() {
        try {
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

    public String getInstructions() {
        return I18N.get("ui.plugin.wms.SRSWizardPanel.the-layers-you-chosen-support-more-than-one-coordinate-reference");
    }

    void jbInit() throws Exception {
        srsLabel.setText(I18N.get("ui.plugin.wms.SRSWizardPanel.coordinate-reference-system"));
        formatLabel = new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
        formatBox = new JComboBox();
        this.setLayout(gridBagLayout1);
        GridBagConstraints gb = new GridBagConstraints();
        gb.anchor = WEST;
        gb.gridx = 0;
        gb.gridy = 0;
        gb.insets = new Insets(5, 5, 5, 5);
        add(srsLabel, gb);
        ++gb.gridx;
        add(comboBox, gb);

        ++gb.gridy;
        gb.gridx = 0;

        add(formatLabel, gb);
        ++gb.gridx;
        add(formatBox, gb);
    }

    public void exitingToRight() {
        int index = comboBox.getSelectedIndex();
        String srsCode = (String) getCommonSrsList().get( index );
        dataMap.put( SRS_KEY, srsCode );
        dataMap.put(URLWizardPanel.FORMAT_KEY, formatBox.getSelectedItem());
    }

    private List getCommonSrsList() {
        return (List) dataMap.get(COMMON_SRS_LIST_KEY);
    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;

        comboBoxModel.removeAllElements();
        for (Iterator i = getCommonSrsList().iterator();i.hasNext();) {
            String srs = (String) i.next();
            String srsName = SRSUtils.getName( srs );
            comboBoxModel.addElement( srsName );
        }
        comboBox.setModel(comboBoxModel);

        String[] formats = (String[]) dataMap.get(FORMAT_LIST_KEY);
        formatBoxModel.removeAllElements();
        for (String f : formats) {
            formatBoxModel.addElement(f);
        }
        formatBox.setModel(formatBoxModel);
    }

    public String getTitle() {
        return I18N.get("ui.plugin.wms.SRSWizardPanel.select-coordinate-reference-system");
    }

    public String getID() {
        return getClass().getName();
    }

    public boolean isInputValid() {
        return true;
    }

    public String getNextID() {
        return null;
    }
}
