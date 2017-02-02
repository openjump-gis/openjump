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
package org.openjump.core.ui.plugin.wms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.ui.plugin.wms.panels.WMSScaleStylePanel;
import org.openjump.core.ui.plugin.wms.panels.WMSTransparencyPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * @version July 3 2015 [Giuseppe Aruta] WMS style plugin Added Transparency and
 *          scale display to WMS layers
 * @version July 3 2015 [Giuseppe Aruta] correct a bug when Largest
 *          scale>Smallest scale
 */

public class WMSStylePlugIn extends AbstractPlugIn {

    public WMSStylePlugIn() {
    }

    public boolean execute(final PlugInContext context) throws Exception {
        final WMSLayer layer = (WMSLayer) LayerTools.getSelectedLayerable(
                context, WMSLayer.class);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(),
                I18N.get("ui.style.ChangeStylesPlugIn.change-styles") + " - "
                        + layer.getName() + " (WMS)", true);
        dialog.setSize(500, 400);
        // dialog.setInset(0);
        dialog.setSideBarImage(IconLoader.icon("Symbology.gif"));

        dialog.setApplyVisible(true);

        final WMSScaleStylePanel panel = new WMSScaleStylePanel(layer,
                context.getLayerViewPanel());
        JTabbedPane tabbedPane = new JTabbedPane();

        final WMSTransparencyPanel trppanel = new WMSTransparencyPanel(layer,
                context.getLayerViewPanel());
        tabbedPane.add(
                I18N.get("ui.renderer.style.ColorThemingPanel.transparency"),
                trppanel);
        tabbedPane.add(I18N.get("ui.style.ScaleStylePanel.scale"), panel);
        dialog.addRow(tabbedPane);

        dialog.setApplyVisible(true);

        dialog.addEnableChecks(panel.getTitle(),
                new EnableCheck() {
                    public String check(JComponent component) {
                        return panel.validateInput();
                    }
                });

        dialog.addOKCancelApplyPanelActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (dialog.wasApplyPressed()) {

                    if (panel.LSCale().doubleValue() > panel.SSCale()
                            .doubleValue()) {

                        JOptionPane.showMessageDialog(
                                null,
                                I18N.get("ui.style.ScaleStylePanel.units-pixel-at-smallest-scale-must-be-larger-than-units-pixel-at-largest-scale"),
                                "Jump", JOptionPane.ERROR_MESSAGE);

                    } else {
                        trppanel.updateStyles();
                        panel.updateStyles();

                    }

                }
            }
        });
        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {

            panel.updateStyles();
            trppanel.updateStyles();
            return true;

        }

        return false;
    }

    public ImageIcon getIcon() {
        return IconLoader.icon("Palette.png");
    }

    public String getName() {
        return I18N.get("ui.style.ChangeStylesPlugIn.change-styles");
    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory
                        .createWindowWithLayerViewPanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, WMSLayer.class));
    }
}
