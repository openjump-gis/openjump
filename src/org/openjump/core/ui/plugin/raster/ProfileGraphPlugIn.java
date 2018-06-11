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

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

//import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

public class ProfileGraphPlugIn extends ThreadedBasePlugIn {

    /**
     * 2015_01_31. Giuseppe Aruta Add new panel which display profile info:
     * length, mean slope, coordinates of starting and ending points, cell
     * dimension, cell statistics.
     */

    private final List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();

    final static String drawn = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.draw-linstring-as-trace");
    final static String selected = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.use-selected-linstring-as-trace");;
    private final String sName = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphPlugIn.Profile-Graph");
    private final String warning = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.select-one-linstring");
    public final static String PROFILE_INFO = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");
    public final static String PLOT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot");

    public final static String SLOPE = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Calculate-slope-profile");
    public final static String DESCRIPTION = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Description");
    final static String MONITOR_STRING = "Calculating profile...";
    public static String CLAYER = GenericNames.SELECT_LAYER;
    private boolean drawnType = true;
    private boolean selectedType = false;
    public static MultiInputDialog dialog;
    // public static JCheckBox infoBox;
    JRadioButton radioButton1 = new JRadioButton(drawn, drawnType);
    JRadioButton radioButton2 = new JRadioButton(selected, selectedType);

    @Override
    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller.getInstance().addMainMenuPlugin(this,
                new String[] { MenuNames.RASTER }, sName + "...", false,
                getIcon(), check());
    }

    public static MultiEnableCheck check() {
        final EnableCheckFactory checkFactory = EnableCheckFactory
                .getInstance();
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class));
    }

    private void getDialogValues(MultiInputDialog dialog) {
        drawnType = dialog.getBoolean(drawn);
        selectedType = dialog.getBoolean(selected);

    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        final Collection<RasterImageLayer> rlayers = context.getTask()
                .getLayerManager().getLayerables(RasterImageLayer.class);
        final String OUTPUT_GROUP = "Match Type";

        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addLayerableComboBox(CLAYER, context.getLayerManager()
                .getRasterImageLayers().get(0), "", rlayers);

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
        getDialogValues(dialog);
        // rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(context,
        // RasterImageLayer.class);
        // if (layerName == null) {
        // context.getLayerViewPanel()
        // .getContext()
        // .warnUser(
        // I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
        // return;
        // }

        if (!dialog.wasOKPressed()) {
            return;
        }

        if (drawnType) {
            final ProfileGraphTool profileTool = new ProfileGraphTool();
            context.getLayerViewPanel().setCurrentCursorTool(profileTool);
        }

        else if (selectedType) {

            final Collection<Feature> features = context.getLayerViewPanel()
                    .getSelectionManager().getFeaturesWithSelectedItems();
            if (features.size() == 0 || features.size() > 1) {
                context.getWorkbenchFrame()
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
                    context.getWorkbenchFrame().warnUser(warning);
                }

            }
        }

    }
}
