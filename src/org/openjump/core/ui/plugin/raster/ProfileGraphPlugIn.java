/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.raster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JRadioButton;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

//import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

public class ProfileGraphPlugIn extends ThreadedBasePlugIn {

    /**
     * 2015_01_31. Giuseppe Aruta Add new panel which display profile info:
     * length, mean slope, coordinates of starting and ending points, cell
     * dimension, cell statistics.
     */

    private final List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();

    private RasterImageLayer rLayer = null;

    final static String drawn = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.draw-linstring-as-trace");
    final static String selected = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.use-selected-linstring-as-trace");;
    private String sName;
    private final String warning = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.select-one-linstring");;
    final static String MONITOR_STRING = "Calculating profile...";

    private boolean drawnType = true;
    private boolean selectedType = false;
    public static MultiInputDialog dialog;
    JRadioButton radioButton1 = new JRadioButton(drawn, drawnType);
    JRadioButton radioButton2 = new JRadioButton(selected, selectedType);

    @Override
    public void initialize(PlugInContext context) throws Exception {
        sName = I18N
                .get("org.openjump.core.ui.plugin.raster.ProfileGraphPlugIn.Profile-Graph");
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.RASTER }, sName + "...", false,
                getIcon(), createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        final MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add(
                checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class)).add(
                checkFactory
                        .createRasterImageLayerExactlyNBandsMustExistCheck(1));

        return multiEnableCheck;
    }

    private void getDialogValues(MultiInputDialog dialog) {
        drawnType = dialog.getBoolean(drawn);
        selectedType = dialog.getBoolean(selected);
        // dialog.getLayer(CLAYER);
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        final String OUTPUT_GROUP = "Match Type";
        dialog.setTitle(sName);
        dialog.addRadioButton(drawn, OUTPUT_GROUP, drawnType, null);

        final Collection<Feature> features = context.getLayerViewPanel()
                .getSelectionManager().getFeaturesWithSelectedItems();
        if (features.size() == 0 || features.size() > 1) {
            dialog.addRadioButton(selected, OUTPUT_GROUP, selectedType, null)
                    .setEnabled(false);
        } else {
            dialog.addRadioButton(selected, OUTPUT_GROUP, selectedType, null)
                    .setEnabled(true);
        }

        dialog.setResizable(false);

    }

    public Icon getIcon() {
        return IconLoader.icon("profile.png");
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(),
                true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        if (ProfileUtils.resultFC != null || ProfileUtils.nPoints > 0) {
            ProfileUtils.resultFC.clear();
            ProfileUtils.nPoints = 0;
        }

        savedCoordinates.clear();
        rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context,
                RasterImageLayer.class);
        if (rLayer == null) {
            context.getLayerViewPanel()
                    .getContext()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }

        if (!dialog.wasOKPressed()) {
            return;
        }

        getDialogValues(dialog);

        if (drawnType) {
            final ProfileGraphTool profileTool = new ProfileGraphTool();
            context.getLayerViewPanel().setCurrentCursorTool(profileTool);
        }

        else if (selectedType) {

            final Collection<Feature> features = context.getLayerViewPanel()
                    .getSelectionManager().getFeaturesWithSelectedItems();
            if (features.size() == 0 || features.size() > 1) {
                JUMPWorkbench
                        .getInstance()
                        .getFrame()
                        .warnUser(
                                I18N.getMessage(
                                        "com.vividsolutions.jump.workbench.plugin.Exactly-n-features-must-be-selected", //$NON-NLS-1$
                                        new Object[] { 1 }));

            } else {
                final Geometry geom = features.iterator().next().getGeometry();
                if (geom instanceof LineString) {
                    final Coordinate[] coords = geom.getCoordinates();
                    ProfileUtils.calculateProfile(coords);
                } else {
                    JUMPWorkbench.getInstance().getFrame().warnUser(warning);
                }

            }
        }

    }
}
