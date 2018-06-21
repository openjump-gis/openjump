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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.openjump.core.ccordsys.Unit;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
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

    private final String DRAWN = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.draw-linstring-as-trace");
    private final String SELECTED = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.use-selected-linstring-as-trace");;

    private final String sName = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphPlugIn.Profile-Graph");
    private final String WARNING = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.select-one-linstring");
    public final String PROFILE_INFO = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");
    public final String PLOT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot");
    private final String OPTIONS = I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn");
    public static String HEIGHT = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");
    public static String WIDTH = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance");
    public final String DESCRIPTION = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Description");
    private static String SELECT_BAND = I18N
            .get("org.openjump.core.ui.plugin.raster.HistogramPlugIn.select-one-band");
    private static String LAYER_UNIT = I18N
            .get("org.openjump.core.ui.plugin.file.ProjectInfoPlugIn.srs-unit");
    private static String VERICAL_AXES_LABEL = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.vertical-axes-label");
    private static String HORIZONTAL_AXES_LABEL = I18N
            .get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.horizontal-axes-label");

    private final List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();
    public static String CLAYER = GenericNames.SELECT_LAYER;
    private boolean drawnType = true;
    private boolean selectedType = false;
    public static String UNIT;

    public static RasterImageLayer rLayer;

    public static int numband;
    public static JTextField unitfiled = new JTextField("");

    public static MultiInputDialog dialog;

    JRadioButton radioButton1 = new JRadioButton(DRAWN, drawnType);
    JRadioButton radioButton2 = new JRadioButton(SELECTED, selectedType);
    JComboBox<RasterImageLayer> box;
    JComboBox<String> comboBox = new JComboBox<String>();
    JPanel panel1 = new JPanel(new FlowLayout());
    JLabel label1;

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
        drawnType = dialog.getBoolean(DRAWN);
        selectedType = dialog.getBoolean(SELECTED);
        HEIGHT = dialog.getText(HORIZONTAL_AXES_LABEL);
        WIDTH = dialog.getText(VERICAL_AXES_LABEL);
        UNIT = dialog.getText(LAYER_UNIT);
        rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);

    }

    private void setDialogValues(final MultiInputDialog dialog, PlugInContext context) {
        final Collection<RasterImageLayer> rlayers = context.getTask()
                .getLayerManager().getLayerables(RasterImageLayer.class);
        final ArrayList<String> srsArray = new ArrayList<String>();
        srsArray.add("metre");
        srsArray.add("foot");
        for (final RasterImageLayer currentLayer : rlayers) {
            final String srs = currentLayer.getSRSInfo().getUnit().toString();

            if (!srsArray.contains(srs)) {
                srsArray.add(Unit.find(srs).toString());
            }

        }

        final RasterImageLayer firstElement = (RasterImageLayer) rlayers
                .toArray()[0];
        final String srsCode = firstElement.getSRSInfo().getUnit().toString();

        final String OUTPUT_GROUP = "Match Type";
        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addSubTitle(PLOT);
        box = dialog.addLayerableComboBox(CLAYER, context.getLayerManager()
                .getRasterImageLayers().get(0), "", rlayers);
        // box.setSelectedItem(srsCode);

        // unitfiled = dialog.addTextField(LAYER_UNIT, srsCode, 20, null, null);
        // unitfiled.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        // unitfiled.setEditable(false);
        comboBox = dialog.addComboBox(LAYER_UNIT, "", srsArray, null);
        // Arrays.asList("metre", "foot", "Unknown"), null);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(Graphics g) {
                setForeground(Color.BLACK);
                super.paint(g);
            }
        });
        comboBox.setEnabled(srsCode.equals("Unknown"));

        comboBox.setSelectedItem(srsCode);

        dialog.addRadioButton(DRAWN, OUTPUT_GROUP, drawnType, null);

        final Collection<Feature> features = context.getLayerViewPanel()
                .getSelectionManager().getFeaturesWithSelectedItems();
        if (features.size() == 0 || features.size() > 1) {
            dialog.addRadioButton(SELECTED, OUTPUT_GROUP, selectedType, null)
                    .setEnabled(false);
        } else {
            dialog.addRadioButton(SELECTED, OUTPUT_GROUP, selectedType, null)
                    .setEnabled(true);
        }

        dialog.addSubTitle(OPTIONS);

        dialog.addTextField(HORIZONTAL_AXES_LABEL, WIDTH, 20, null, null);
        dialog.addTextField(VERICAL_AXES_LABEL, HEIGHT, 20, null, null);
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String layerUnit = getLayer().getSRSInfo().getUnit()
                        .toString();

                unitfiled.setText(layerUnit);
                updateComponents();
                dialog.repaint();

            }
        });

    }

    public void updateComponents() {

        final String layerUnit = getLayer().getSRSInfo().getUnit().toString();
        comboBox.setEnabled(layerUnit.equals("Unknown"));
        comboBox.setSelectedItem(layerUnit);

    }

    public static RasterImageLayer getLayer() {
        return dialog.getRasterLayer(ProfileGraphPlugIn.CLAYER);

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

        monitor.allowCancellationRequests();
        monitor.report(getName()
                + ": "
                + I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.computing"));
        if (ProfileGraphGUI.resultFC != null || ProfileGraphGUI.nPoints > 0) {
            ProfileGraphGUI.resultFC.clear();
            ProfileGraphGUI.nPoints = 0;
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

        final RasterImageLayer selLayer = dialog
                .getRasterLayer(ProfileGraphPlugIn.CLAYER);

        numband = 0;
        if (selLayer.getNumBands() > 1) {
            final String[] bands = { "0", "1", "2" };
            final String stringInput = (String) JOptionPane.showInputDialog(
                    JUMPWorkbench.getInstance().getFrame(), SELECT_BAND, sName,
                    JOptionPane.PLAIN_MESSAGE, null, bands, "0");

            try {
                numband = Integer.parseInt(stringInput);
            } catch (final NumberFormatException e) {
                return; // The typed text was not an integer
                // band = 0;
            }
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

                    ProfileGraphGUI.calculateProfile(coords);

                } else {
                    context.getWorkbenchFrame().warnUser(WARNING);

                }

            }
        }

    }

    /*
     * Return type of the Sextante Raster Layer as String
     */
    public String filetype(String ext) {
        return Unit.find(ext).toString();
    }

}
