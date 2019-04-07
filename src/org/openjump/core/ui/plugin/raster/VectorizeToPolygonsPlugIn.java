package org.openjump.core.ui.plugin.raster;

import static com.vividsolutions.jump.I18N.get;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.algorithms.VectorizeAlgorithm;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.util.LayerableUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class VectorizeToPolygonsPlugIn extends ThreadedBasePlugIn {

    private final String PROCESSING = I18N
            .get("jump.plugin.edit.NoderPlugIn.processing");

    String sLayer = I18N.get("ui.GenericNames.Source-Layer");
    String sExplode = RasterMenuNames.ExplodeMultipolygons;
    String NAME = RasterMenuNames.VectorizeToPolygon;
    String sStyle = RasterMenuNames.ApplyStyle;
    String sValue = RasterMenuNames.Value;
    String alg1 = "AdbToolbox";
    String alg2 = "Sextante";
    String algorithms = RasterMenuNames.Algorithms;
    String choose;
    boolean explodeb = true;
    boolean applystyleb = false;

    private JCheckBox explode = new JCheckBox();
    private JComboBox<String> comboBox = new JComboBox<String>();
    private List<RasterImageLayer> fLayers = new ArrayList<RasterImageLayer>();
    private JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();

    RasterImageLayer layer;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), NAME, true);
        setDialogValues(dialog, context);
        if (fLayers.isEmpty()) {
            return false;
        }
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        } else if (dialog.wasOKPressed()) {
            getDialogValues(dialog);
            return true;
        }
        return false;
    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) throws IOException {
        dialog.setSideBarDescription(NAME);
        if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            layer = (RasterImageLayer) LayerTools.getSelectedLayerable(context,
                    RasterImageLayer.class);
        } else {
            layer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        layerableComboBox = dialog.addLayerableComboBox(sLayer, layer, "",
                fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        final ArrayList<String> srsArray = new ArrayList<String>();
        srsArray.add(alg1);
        srsArray.add(alg2);
        comboBox = dialog.addComboBox(algorithms, "", srsArray, null);
        comboBox.setSize(200, comboBox.getPreferredSize().height);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGUI(e, dialog);

                dialog.pack();
                dialog.repaint();
            }
        });
        explode = dialog.addCheckBox(sExplode, true);
        dialog.addCheckBox(sStyle, true);

    }

    private void updateGUI(ActionEvent evt, MultiInputDialog dialog) {
        switch (comboBox.getSelectedIndex()) {
        case 0:
            explode.setEnabled(true);
            break;
        case 1:
            explode.setEnabled(false);
            break;

        }
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = (RasterImageLayer) dialog.getLayerable(sLayer);
        explodeb = dialog.getBoolean(sExplode);
        applystyleb = dialog.getBoolean(sStyle);
        choose = dialog.getText(algorithms);

    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(PROCESSING);
        reportNothingToUndoYet(context);
        Utils.zoom(layer);
        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(layer, true);
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);
        fs.addAttribute(sValue, AttributeType.INTEGER);
        FeatureCollection featDataset = new FeatureDataset(fs);

        switch (comboBox.getSelectedIndex()) {
        case 0:
            if (explodeb = true) {
                featDataset = VectorizeAlgorithm.toPolygonsAdbToolBox(gwrapper,
                        true, sValue, 0);
            } else {
                featDataset = VectorizeAlgorithm.toPolygonsAdbToolBox(gwrapper,
                        false, sValue, 0);
            }
            break;
        case 1:
            featDataset = VectorizeAlgorithm.toPolygonsSextante(gwrapper,
                    sValue, 0);
            break;
        }
        final Layer vlayer = context.addLayer(StandardCategoryNames.WORKING,
                rstLayer.getName() + "_" + "vectorized", featDataset);

        if (applystyleb) {
            Utils.applyRandomGradualStyle(vlayer, sValue);

        }

    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class)).add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        final List<RasterImageLayer> mLayer = new ArrayList<RasterImageLayer>();
                        final Collection<RasterImageLayer> rlayers = workbenchContext
                                .getLayerManager().getLayerables(
                                        RasterImageLayer.class);
                        for (final RasterImageLayer currentLayer : rlayers) {
                            if (LayerableUtil.isMonoband(currentLayer)) {
                                mLayer.add(currentLayer);
                            }
                        }
                        if (!mLayer.isEmpty()) {
                            return null;
                        }
                        String msg = null;
                        if (mLayer.isEmpty()) {
                            msg = get(CHECK_FILE);
                        }
                        return msg;
                    }
                });
    }

    private final static String CHECK_FILE = I18N
            .get("plugin.EnableCheckFactory.at-least-one-single-banded-layer-should-exist");

    @Override
    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller.getInstance()
                .addMainMenuPlugin(
                        this, //exe
                        new String[] { MenuNames.RASTER,
                                MenuNames.RASTER_VECTORIALIZE }, //menu path
                        NAME, false, //checkbox
                        null, //icon
                        createEnableCheck(context.getWorkbenchContext()));
    }

}
