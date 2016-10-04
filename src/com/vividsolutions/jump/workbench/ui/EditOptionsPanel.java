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

package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import org.saig.jump.widgets.config.ConfigTooltipPanel;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * Implements an {@link OptionsPanel} for Edit.
 * 
 * [2015-1-4] added option for advanced layer tooltip [2016-4-10] Giuseppe Aruta
 * [2016-4-10] Giuseppe Aruta - added option to select the geometry after it has
 * been drawn
 */

public class EditOptionsPanel extends JPanel implements OptionsPanel {
    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel jPanel1 = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JCheckBox preventEditsCheckBox = new JCheckBox();
    private JPanel jPanel2 = new JPanel();
    private Blackboard blackboard;
    private JPanel tooltipPanel;
    private JCheckBox tooltipCheck;
    public static JCheckBox geometryCheck;

    /** Opciones de tooltip */
    public static final String LAYER_TOOLTIPS_ON = ConfigTooltipPanel.class
            .getName() + " - LAYER_TOOLTIPS"; //$NON-NLS-1$

    /** Option selected geometry */
    /*
     * public static final String SELECTED_GEOMETRY = ConfigTooltipPanel.class
     * .getName() + " - SELECTED_GEOMETRY"; //$NON-NLS-1$
     */

    public EditOptionsPanel(final Blackboard blackboard,
            JDesktopPane desktopPane) {
        this.blackboard = blackboard;
        try {
            jbInit();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
        }

        this.blackboard = blackboard;
        this.setLayout(new GridBagLayout());

        // Anyadimos los paneles
        FormUtils.addRowInGBL(this, 1, 0, getEditPanel());
        FormUtils.addRowInGBL(this, 3, 0, getTooltipPanel());
        FormUtils.addFiller(this, 4, 0);
    }

    /**
     * 
     * @return
     */
    private JPanel getTooltipPanel() {
        if (tooltipPanel == null) {
            tooltipPanel = new JPanel(new GridBagLayout());
            TitledBorder titledBorder1 = new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(
                            148, 145, 140)),
                    I18N.get("ui.EditOptionsPanel.configure-layer-tree-tooltip"));
            tooltipPanel.setBorder(titledBorder1); //$NON-NLS-1$
            tooltipCheck = new JCheckBox(
                    I18N.get("ui.EditOptionsPanel.enable-JUMP-basic-tooltips")); //$NON-NLS-1$
            FormUtils.addRowInGBL(tooltipPanel, 0, 0, tooltipCheck);
        }
        return tooltipPanel;
    }

    public String validateInput() {
        return null;
    }

    public void okPressed() {
        blackboard.put(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY,
                preventEditsCheckBox.isSelected());
        PersistentBlackboardPlugIn.get(blackboard).put(LAYER_TOOLTIPS_ON,
                tooltipCheck.isSelected());
        /*
         * PersistentBlackboardPlugIn.get(blackboard).put(SELECTED_GEOMETRY,
         * geometryCheck.isSelected());
         */

    }

    public void init() {
        preventEditsCheckBox.setSelected(blackboard.get(
                EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false));
        boolean layerTooltipsOn = PersistentBlackboardPlugIn.get(blackboard)
                .get(LAYER_TOOLTIPS_ON, false);
        if (layerTooltipsOn) {
            tooltipCheck.setSelected(true);
        }
        /*
         * boolean geometryOn = PersistentBlackboardPlugIn.get(blackboard).get(
         * SELECTED_GEOMETRY, false); if (geometryOn) {
         * geometryCheck.setSelected(true); }
         */
    }

    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        jPanel1.setLayout(gridBagLayout1);
        preventEditsCheckBox
                .setText(I18N
                        .get("ui.EditOptionsPanel.prevent-edits-resulting-in-invalid-geometries"));
        geometryCheck = new JCheckBox(
                I18N.get("ui.EditOptionsPanel.select-geometry")); //$NON-NLS-1$
        geometryCheck
                .setToolTipText("ui.EditOptionsPanel.select-geometry-warning");
        this.add(jPanel1, BorderLayout.EAST);
        TitledBorder titledBorder2 = new TitledBorder(
                BorderFactory.createEtchedBorder(Color.white, new Color(148,
                        145, 140)), I18N.get("ui.EditOptionsPanel.edit"));
        jPanel1.setBorder(titledBorder2); //$NON-NLS-1$

        FormUtils.addRowInGBL(jPanel1, 0, 0, preventEditsCheckBox);
        FormUtils.addRowInGBL(jPanel1, 1, 0, geometryCheck);
        jPanel1.add(jPanel2, new GridBagConstraints(200, 200, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                        0, 0, 0, 0), 0, 0));
        // FormUtils.addRowInGBL( jPanel1, 0, 0, preventEditsCheckBox);
    }

    private JPanel getEditPanel() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel(new GridBagLayout());
            TitledBorder titledBorder2 = new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(
                            148, 145, 140)),
                    I18N.get("ui.EditOptionsPanel.edit"));
            jPanel1.setBorder(titledBorder2); //$NON-NLS-1$
            preventEditsCheckBox = new JCheckBox(
                    "ui.EditOptionsPanel.prevent-edits-resulting-in-invalid-geometries");
            geometryCheck = new JCheckBox(
                    "Select the geometry after it has been drawn"); //$NON-NLS-1$
            FormUtils.addRowInGBL(jPanel1, 0, 0, preventEditsCheckBox);
            FormUtils.addRowInGBL(jPanel1, 1, 0, geometryCheck);
            /*
             * jPanel1.add(preventEditsCheckBox, new GridBagConstraints(0, 0, 1,
             * 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
             * new Insets(10, 10, 4, 0), 0, 0)); jPanel1.add(geometryCheck, new
             * GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
             * GridBagConstraints.NONE, new Insets(10, 10, 4, 0), 0, 0));
             */

            // FormUtils.addRowInGBL(tooltipPanel, 0, 0, tooltipCheck);
        }
        return jPanel1;
    }
}
