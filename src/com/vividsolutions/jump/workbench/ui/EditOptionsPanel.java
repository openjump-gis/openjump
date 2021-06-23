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

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import org.saig.jump.widgets.config.ConfigTooltipPanel;

import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;

/**
 * Implements an {@link OptionsPanel} for Edit.
 * 
 * [2015-04-01] Giuseppe Aruta - add option for advanced layer tooltip
 * [2016-10-04] Giuseppe Aruta - add option to select the geometry after it has
 * been drawn
 * [2016-10-09] Micha�l Michaud - add option to limit the number of editable
 * layers to one
 * [2020-06-16] Giuseppe Aruta - Feature request #245 Create form to edit attribute values 
 * Added option to open Infoframe after digitalized a new feature
 */ 

public class EditOptionsPanel extends JPanel implements OptionsPanel {

	private JPanel editPanel;
	private JCheckBox preventEditsCheckBox;
	private JCheckBox selectNewGeometryCheckBox;
	private JCheckBox selectInfoNewGeometryCheckBox;
	private JCheckBox singleEditableLayerCheckBox;

	private JPanel layerToolTipPanel;
	private Blackboard blackboard;
	private JCheckBox tooltipCheckBox;


	/** Option de editable layer number */
	public static final String SELECT_NEW_GEOMETRY_KEY = ConfigTooltipPanel.class
			.getName() + " - SELECT_NEW_GEOMETRY";

	/** Option de editable layer number */
	public static final String SELECT_INFO_GEOMETRY_KEY = ConfigTooltipPanel.class
			.getName() + " - SELECT_INFO_GEOMETRY";

	/** Option de editable layer number */
	public static final String SINGLE_EDITABLE_LAYER_KEY = ConfigTooltipPanel.class
			.getName() + " - SINGLE_EDITABLE_LAYER";

	/** Option de tooltip */
	public static final String LAYER_TOOLTIPS_KEY = ConfigTooltipPanel.class
			.getName() + " - LAYER_TOOLTIPS";

	public static final String EDIT_PANEL =
			I18N.getInstance().get("ui.EditOptionsPanel.edit-panel");
	public static final String PREVENT_INVALID_EDIT =
			I18N.getInstance().get("ui.EditOptionsPanel.prevent-edits-resulting-in-invalid-geometries");
	public static final String SELECT_NEW_GEOMETRY =
			I18N.getInstance().get("ui.EditOptionsPanel.select-new-geometry"); // Select the geometry after it has been drawn

	public static final String SELECT_INFO_GEOMETRY ="..."+I18N.getInstance().get("ui.EditOptionsPanel.open.info.frame");

	public static final String SELECT_NEW_GEOMETRY_WARNING =
			I18N.getInstance().get("ui.EditOptionsPanel.select-new-geometry-deselect-previous-selection"); // Select the geometry after it has been drawn
	public static final String SINGLE_EDITABLE_LAYER =
			I18N.getInstance().get("ui.EditOptionsPanel.single-editable-layer");

	public static final String CONFIGURE_LAYERTREE_PANEL =
			I18N.getInstance().get("ui.EditOptionsPanel.configure-layer-tree-tooltip");
	public static final String LAYER_TOOLTIP =
			I18N.getInstance().get("ui.EditOptionsPanel.enable-JUMP-basic-tooltips");


	public EditOptionsPanel(final Blackboard blackboard) {
		this.blackboard = blackboard;
		this.setLayout(new GridBagLayout());

		// Add titled panels
		FormUtils.addRowInGBL(this, 1, 0, getEditPanel());
		FormUtils.addRowInGBL(this, 3, 0, getTooltipPanel());
		FormUtils.addFiller(this, 4, 0);

		try {
			init();
		} catch (Exception e) {
			Assert.shouldNeverReachHere(e.toString());
		}

		selectNewGeometryCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {//checkbox has been selected
					selectInfoNewGeometryCheckBox.setEnabled(true);
				} else {//checkbox has been deselected
					selectInfoNewGeometryCheckBox.setSelected(false);
					selectInfoNewGeometryCheckBox.setEnabled(false);
				};
			}
		});
	}

	@Override
	public void init() {
		preventEditsCheckBox.setSelected(blackboard.get(
				EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false));
		selectNewGeometryCheckBox.setSelected(blackboard.get(
				SELECT_NEW_GEOMETRY_KEY, false));
		selectInfoNewGeometryCheckBox.setSelected(blackboard.get(
				SELECT_INFO_GEOMETRY_KEY, false));
		selectInfoNewGeometryCheckBox.setEnabled(blackboard.get(
				SELECT_NEW_GEOMETRY_KEY, true));



		singleEditableLayerCheckBox.setSelected(blackboard.get(
				SINGLE_EDITABLE_LAYER_KEY, true));
		tooltipCheckBox.setSelected(blackboard.get(
				LAYER_TOOLTIPS_KEY, false));

		blackboard.put(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, preventEditsCheckBox.isSelected());
		blackboard.put(SELECT_NEW_GEOMETRY_KEY, selectNewGeometryCheckBox.isSelected());
		blackboard.put(SELECT_INFO_GEOMETRY_KEY, selectInfoNewGeometryCheckBox.isSelected());
		blackboard.put(SINGLE_EDITABLE_LAYER_KEY, singleEditableLayerCheckBox.isSelected());
		blackboard.put(LAYER_TOOLTIPS_KEY, tooltipCheckBox.isSelected());
	}

	@Override
	public String validateInput() {
		return null;
	}

	@Override
	public void okPressed() {
		blackboard.put(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY,
				preventEditsCheckBox.isSelected());
		blackboard.put(SELECT_NEW_GEOMETRY_KEY,
				selectNewGeometryCheckBox.isSelected());

		blackboard.put(SELECT_INFO_GEOMETRY_KEY,
				selectInfoNewGeometryCheckBox.isSelected());
		blackboard.put(SINGLE_EDITABLE_LAYER_KEY,
				singleEditableLayerCheckBox.isSelected());
		blackboard.put(LAYER_TOOLTIPS_KEY,
				tooltipCheckBox.isSelected());
	}


	private JPanel getEditPanel() {
		if (editPanel == null) {
			editPanel = new JPanel(new GridBagLayout());
			TitledBorder titledBorder2 = new TitledBorder(
					BorderFactory.createEtchedBorder(
							Color.white, new Color(148, 145, 140)
							),
					EDIT_PANEL);
			editPanel.setBorder(titledBorder2);

			preventEditsCheckBox = new JCheckBox(PREVENT_INVALID_EDIT);
			selectNewGeometryCheckBox = new JCheckBox(SELECT_NEW_GEOMETRY);
			selectInfoNewGeometryCheckBox= new JCheckBox(SELECT_INFO_GEOMETRY);
			selectInfoNewGeometryCheckBox.setEnabled(selectNewGeometryCheckBox.isSelected());

			selectNewGeometryCheckBox.setToolTipText(SELECT_NEW_GEOMETRY_WARNING);
			singleEditableLayerCheckBox = new JCheckBox(SINGLE_EDITABLE_LAYER);

			FormUtils.addRowInGBL(editPanel, 0, 0, preventEditsCheckBox);
			FormUtils.addRowInGBL(editPanel, 1, 0, selectNewGeometryCheckBox);
			FormUtils.addRowInGBL(editPanel, 2, 0, selectInfoNewGeometryCheckBox);
			FormUtils.addRowInGBL(editPanel, 3, 0, singleEditableLayerCheckBox);
		}
		return editPanel;
	}

	/**
	 *
	 * @return the Panel containing the ToolTip
	 */
	private JPanel getTooltipPanel() {
		if (layerToolTipPanel == null) {

			layerToolTipPanel = new JPanel(new GridBagLayout());
			TitledBorder titledBorder1 = new TitledBorder(
					BorderFactory.createEtchedBorder(
							Color.white, new Color(148, 145, 140)
							),
					CONFIGURE_LAYERTREE_PANEL);
			layerToolTipPanel.setBorder(titledBorder1);

			tooltipCheckBox = new JCheckBox(LAYER_TOOLTIP);

			FormUtils.addRowInGBL(layerToolTipPanel, 0, 0, tooltipCheckBox);
		}
		return layerToolTipPanel;
	}
}
