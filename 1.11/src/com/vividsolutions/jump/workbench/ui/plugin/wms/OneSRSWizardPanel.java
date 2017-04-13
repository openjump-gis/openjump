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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.coordsys.impl.PredefinedCoordinateSystems;

import static com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel.FORMAT_LIST_KEY;
import static java.awt.GridBagConstraints.WEST;


public class OneSRSWizardPanel extends JPanel implements WizardPanel {
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private Map dataMap;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JLabel srsLabel = new JLabel();
    private DefaultComboBoxModel formatBoxModel = new DefaultComboBoxModel();
    private JLabel formatLabel;
    private JComboBox formatBox;
    private JPanel fillerPanel = new JPanel();
    private JTextField textField = new JTextField();

    public OneSRSWizardPanel() {
        try {
            jbInit();
            textField.setFont(new JLabel().getFont());
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
        return I18N.get("ui.plugin.wms.OneSRSWizardPanel.the-layers-you-have-chosen-support-only-one-coordinate-system");
    }

    void jbInit() throws Exception {
        srsLabel.setText(I18N.get("ui.plugin.wms.OneSRSWizardPanel.select-coordinate-reference-system"));
        formatLabel = new JLabel(I18N.get("ui.plugin.wms.SRSWizardPanel.image-format"));
        formatBox = new JComboBox();
        this.setLayout(gridBagLayout1);
        textField.setEnabled(false);
        textField.setOpaque(false);
        textField.setPreferredSize(new Dimension(125, 21));
        textField.setDisabledTextColor(Color.black);
        textField.setEditable(false);
        textField.setText("jTextField1");
        GridBagConstraints gb = new GridBagConstraints();
        gb.anchor = WEST;
        gb.gridx = 0;
        gb.gridy = 0;
        gb.insets = new Insets(5, 5, 5, 5);
        add(srsLabel, gb);
        ++gb.gridx;
        //add(comboBox, gb);
        add(textField, gb);

        ++gb.gridy;
        gb.gridx = 0;

        add(formatLabel, gb);
        ++gb.gridx;
        add(formatBox, gb);
        //this.add(srsLabel,
        //    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
        //        GridBagConstraints.CENTER, GridBagConstraints.NONE,
        //        new Insets(0, 0, 0, 4), 0, 0));
        //this.add(fillerPanel,
        //    new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0,
        //        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        //        new Insets(0, 0, 0, 0), 0, 0));
        //this.add(textField,
        //    new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
        //        GridBagConstraints.CENTER, GridBagConstraints.NONE,
        //        new Insets(0, 0, 0, 0), 0, 0));
    }

    public void exitingToRight() {
        //int index = comboBox.getSelectedIndex();
        //String srsCode = (String) getCommonSrsList().get( index );
        //dataMap.put( SRS_KEY, srsCode );
        dataMap.put(URLWizardPanel.FORMAT_KEY, formatBox.getSelectedItem());
    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;

        List commonSRSList = (List) dataMap.get(MapLayerWizardPanel.COMMON_SRS_LIST_KEY);
        Assert.isTrue(commonSRSList.size() == 1);
        String srs = (String) commonSRSList.get(0);
        dataMap.put( SRSWizardPanel.SRS_KEY, srs );

        String stringToShow = SRSUtils.getName( srs );
        textField.setText( stringToShow );

        String[] formats = (String[]) dataMap.get(FORMAT_LIST_KEY);
        formatBoxModel.removeAllElements();
        for (String f : formats) {
            formatBoxModel.addElement(f);
        }
        formatBox.setModel(formatBoxModel);
    }


    public String getTitle() {
        return I18N.get("ui.plugin.wms.OneSRSWizardPanel.select-coordinate-reference-system");
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
