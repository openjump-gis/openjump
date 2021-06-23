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

import static com.vividsolutions.jump.workbench.ui.plugin.wms.MapLayerWizardPanel.*;
import static java.awt.GridBagConstraints.WEST;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;
import com.vividsolutions.wms.MapStyle;


public class SRSWizardPanel extends JPanel implements WizardPanel {

    public static final String SRS_KEY = "SRS";
    public static final String FORMAT_KEY = "FORMAT";
    public static final String STYLE_KEY = "STYLE";
    public static final String ADDITIONAL_PARAMETERS_KEY = "ADDITIONAL_PARAMETERS";

    private final InputChangedFirer inputChangedFirer = new InputChangedFirer();

    private final DefaultComboBoxModel<String> srsBoxModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<String> formatBoxModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<MapStyle> styleBoxModel = new DefaultComboBoxModel<>();

    private final JComboBox<String> srsComboBox = new JComboBox<>();
    private final JComboBox<String> formatComboBox = new JComboBox<>();
    private final JComboBox<MapStyle> styleComboBox = new JComboBox<>();
    private final JTextField moreParametersTextField = new JTextField(24);

    private Map<String,Object> dataMap = new HashMap<>();

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
        return I18N.getInstance().get("ui.plugin.wms.SRSWizardPanel.the-layers-you-chosen-support-more-than-one-coordinate-reference");
    }

    void jbInit() {
        JLabel srsLabel = new JLabel(I18N.getInstance().get(
                "ui.plugin.wms.SRSWizardPanel.coordinate-reference-system"));
        JLabel formatLabel = new JLabel(I18N.getInstance().get(
                "ui.plugin.wms.SRSWizardPanel.image-format"));
        JLabel styleLabel = new JLabel(I18N.getInstance().get(
                "ui.plugin.wms.SRSWizardPanel.style"));
        JLabel moreParamLabel = new JLabel(I18N.getInstance().get(
                "ui.plugin.wms.SRSWizardPanel.more-parameters"));
        //formatBox = new JComboBox();
        GridBagLayout gridBagLayout1 = new GridBagLayout();
        this.setLayout(gridBagLayout1);
        GridBagConstraints gb = new GridBagConstraints();
        gb.anchor = WEST;
        gb.gridx = 0;
        gb.gridy = 0;
        gb.insets = new Insets(5, 5, 5, 5);
        add(srsLabel, gb);
        ++gb.gridx;
        add(srsComboBox, gb);

        ++gb.gridy;
        gb.gridx = 0;
        add(formatLabel, gb);
        ++gb.gridx;
        add(formatComboBox, gb);

        ++gb.gridy;
        gb.gridx = 0;
        add(styleLabel, gb);
        ++gb.gridx;
        add(styleComboBox, gb);

        ++gb.gridy;
        gb.gridx = 0;
        add(moreParamLabel, gb);
        ++gb.gridx;
        add(moreParametersTextField, gb);
    }

    public void exitingToRight() {
        int index = srsComboBox.getSelectedIndex();
        String srsCode = getCommonSrsList().get( index );
        dataMap.put( SRS_KEY, srsCode );
        dataMap.put( FORMAT_KEY, formatComboBox.getSelectedItem());
        dataMap.put( STYLE_KEY, styleComboBox.getSelectedItem());
        dataMap.put( ADDITIONAL_PARAMETERS_KEY, moreParametersTextField.getText());
    }

    private static final Pattern NB = Pattern.compile("^(.*):0*(\\d+)$");

    private final static Comparator<String> cleverStringComparator = (o1, o2) -> {
        Matcher m1 = NB.matcher(o1);
        Matcher m2 = NB.matcher(o2);
        if (m1.matches() && m2.matches()
            && m1.group(1).equals(m2.group(1))) {
            //m1 = NB.matcher(o1);
            //m2 = NB.matcher(o2);
            //m1.find(); m2.find();
            return Integer.valueOf(m1.group(2)).compareTo(Integer.valueOf(m2.group(2)));
        }
        else return o1.compareTo(o2);
    };


    private List<String> getCommonSrsList() {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>)dataMap.get(SRS_LIST_KEY);
        String first = list.isEmpty() ? null : list.get(0);
        list.sort(cleverStringComparator);
        // List is sort to ease the search, but first srs kept at first place
        // as it has more chance to be the desired srs
        if (first != null) {
            list.remove(first);
            list.add(0, first);
        }
        return list;
    }


    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;

        srsBoxModel.removeAllElements();
        for (String srs : getCommonSrsList()) {
            // TODO don't change srs name as it may be used to match the bbox srs name
            //String srsName = SRSUtils.getName( srs );
            //srsBoxModel.addElement( srsName );
            srsBoxModel.addElement(srs);
        }
        srsComboBox.setModel(srsBoxModel);

        String[] formats = (String[]) dataMap.get(FORMAT_LIST_KEY);
        formatBoxModel.removeAllElements();
        for (String f : formats) {
            formatBoxModel.addElement(f);
        }
        formatComboBox.setModel(formatBoxModel);

        @SuppressWarnings("unchecked")
        List<MapStyle> styles = (List<MapStyle>) dataMap.get(STYLE_LIST_KEY);
        styleBoxModel.removeAllElements();
        for (MapStyle f : styles) {
            styleBoxModel.addElement(f);
        }
        styleComboBox.setModel(styleBoxModel);

        moreParametersTextField.setText("");
    }

    public String getTitle() {
        return I18N.getInstance().get("ui.plugin.wms.SRSWizardPanel.select-coordinate-reference-system");
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
