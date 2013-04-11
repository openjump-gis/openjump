/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class GeometryInfoTab extends JPanel {
    private BorderLayout borderLayout2 = new BorderLayout();

    private JToggleButton showAttributesButton = new JToggleButton();

    private JToggleButton showGeometriesButton = new JToggleButton();

    private EnableableToolBar toolBar = new EnableableToolBar();

    private GeometryInfoPanel geometryInfoPanel;

    private EnableCheck geometriesShownEnableCheck = new EnableCheck() {
        public String check(JComponent component) {
            return (!showGeometriesButton.isSelected()) ? "X" : null;
        }
    };

    public GeometryInfoTab(InfoModel model, WorkbenchContext context) {
        geometryInfoPanel = new GeometryInfoPanel(model);

        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        toolBar.add(showGeometriesButton, I18N.get("ui.GeometryInfoTab.geometries"), IconLoader
                .icon("Geometry.gif"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateText();
            }
        }, new MultiEnableCheck());
        toolBar.addSpacer();
        toolBar.add(showAttributesButton, I18N.get("ui.GeometryInfoTab.attributes"), IconLoader
                .icon("Attribute.gif"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateText();
            }
        }, new MultiEnableCheck());
        Dimension buttonSize = new Dimension((int)(showGeometriesButton
                .getPreferredSize().width * 1.5), showGeometriesButton
                .getPreferredSize().height);
        toolBar.addSpacer();
        ButtonGroup buttonGroup = new ButtonGroup();
        for (Iterator i = context.getFeatureTextWriterRegistry().iterator(); i
                .hasNext(); ) {
            final AbstractFeatureTextWriter writer = (AbstractFeatureTextWriter) i
                    .next();
            JToggleButton button = new JToggleButton(writer
                    .getShortDescription());
            button.setFont(button.getFont().deriveFont(10f));
            buttonGroup.add(button);
            toolBar.add(button, writer.getDescription(), null,
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (showGeometriesButton.isSelected()) {
                                geometryWriter = new FeatureInfoWriterAdapter(
                                        writer);
                            }
                            updateText();
                        }
                    }, geometriesShownEnableCheck);
            button.setPreferredSize(buttonSize);
            button.setMinimumSize(buttonSize);
            button.setMaximumSize(buttonSize);
            button.setSize(buttonSize);
        }
        showGeometriesButton.doClick();
        ((JToggleButton) buttonGroup.getElements().nextElement()).doClick();
    }

    private void updateText() {
        if (showAttributesButton.isSelected()) {
            geometryInfoPanel
                    .setAttributeWriter(FeatureInfoWriter.ATTRIBUTE_WRITER);
        } else {
            geometryInfoPanel
                    .setAttributeWriter(FeatureInfoWriter.EMPTY_WRITER);
        }

        if (showGeometriesButton.isSelected()) {
            geometryInfoPanel.setGeometryWriter(geometryWriter);
        } else {
            geometryInfoPanel.setGeometryWriter(FeatureInfoWriter.EMPTY_WRITER);
        }

        geometryInfoPanel.updateText();
    }

    void jbInit() throws Exception {
        setLayout(borderLayout2);
        toolBar.setOrientation(JToolBar.VERTICAL);
        add(geometryInfoPanel, BorderLayout.CENTER);
        add(toolBar, BorderLayout.WEST);
    }

    private FeatureInfoWriter.Writer geometryWriter;

    private class FeatureInfoWriterAdapter implements FeatureInfoWriter.Writer {
        private AbstractFeatureTextWriter featureTextWriter;

        public FeatureInfoWriterAdapter(
                AbstractFeatureTextWriter featureTextWriter) {
            this.featureTextWriter = featureTextWriter;
        }

        public String toHTML(Feature feature) {
            return featureTextWriter.isWrapping() ? "<BR><CODE>"
                    + GUIUtil.escapeHTML(featureTextWriter.write(feature),
                            false, false) + "</CODE>" : "<pre>"
                    + GUIUtil.escapeHTML(featureTextWriter.write(feature),
                            false, false) + "</pre>";
        }
    }
}
