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
    private JPanel fillerPanel = new JPanel();
    private DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private JComboBox comboBox = new JComboBox();

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
        this.setLayout(gridBagLayout1);
        this.add(srsLabel,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        this.add(comboBox,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    public void exitingToRight() {
        int index = comboBox.getSelectedIndex();
        String srsCode = (String) getCommonSrsList().get( index );
        dataMap.put( SRS_KEY, srsCode );
    }

    private List getCommonSrsList() {
        return (List) dataMap.get(MapLayerWizardPanel.COMMON_SRS_LIST_KEY);
    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;

        for (Iterator i = getCommonSrsList().iterator();i.hasNext();) {
            String srs = (String) i.next();
            String srsName = SRSUtils.getName( srs );
            comboBoxModel.addElement( srsName );
        }

        comboBox.setModel(comboBoxModel);
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
