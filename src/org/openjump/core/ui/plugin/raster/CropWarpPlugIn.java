package org.openjump.core.ui.plugin.raster;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.algorithms.GenericRasterAlgorithm;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

//import de.latlon.deejump.wfs.jump.WFSLayer;
import it.betastudio.adbtoolbox.libs.FileOperations;

public class CropWarpPlugIn extends ThreadedBasePlugIn {

    private final String SELECTED = I18N.getInstance().get("jump.plugin.edit.PolygonizerPlugIn.Use-selected-features-only");
    private final String VIEW = I18N.getInstance().get("ui.MenuNames.VIEW");
    private final String LAYER = I18N.getInstance().get("ui.MenuNames.LAYER");
    private final String PROCESSING = I18N.getInstance().get("jump.plugin.edit.NoderPlugIn.processing");
    private final String CLAYER = I18N.getInstance().get("ui.GenericNames.Source-Layer");
    private final String OUTPUT_FILE = I18N.getInstance().get("driver.DriverManager.file-to-save");

    private final ImageIcon icon16 = IconLoader
            .icon("fugue/folder-horizontal-open_16.png");

    private JLabel cutLayerLabel, cutObjectLabel;
    JTextField jTextField_RasterOut = new JTextField();
    JTextField jTextField_RasterIn = new JTextField();
    private JPanel cropPanel;
    private JComboBox<String> comboBox = new JComboBox<>();
    private JComboBox<String> cropComboBox = new JComboBox<>();
    private JComboBox<Object> layerComboBox = new JComboBox<>();
    private String ACTION;
    private String CROP;
    private String path;
    Envelope envWanted = new Envelope();
    Envelope fix = new Envelope();
    private MultiInputDialog dialog;
    public static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();
    private RasterImageLayer rLayer;

    private final String NAME = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.Name");
    private final String Target_OBJECT = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.target-object");
    private final String CROP_RASTER = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.crop-raster");
    private final String CROP_RASTER_TIP = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.crop-raster-tip");
    private final String WARP_RASTER = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.warp-raster");
    private final String WARP_RASTER_TIP = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.warp-raster-tip");
    private final String TARGET_LAYER = I18N.getInstance().get("ui.plugin.raster.CropWarpPlugIn.target-layer");


    private final String CHECK = RasterMenuNames.Check_field;
    private final String NO_OVERWRITE = I18N.getInstance().get("ui.GenericNames.cannot-overwrite-input-layer");
    private final String ACTION_LABEL = RasterMenuNames.Choose_an_action;

    private void updateGUI1(ActionEvent evt, MultiInputDialog dialog) {
        switch (cropComboBox.getSelectedIndex()) {
        case 0:
            cutLayerLabel.setEnabled(true);
            layerComboBox.setEnabled(true);
            break;
        case 1:
            cutLayerLabel.setEnabled(false);
            layerComboBox.setEnabled(false);
            break;
        case 2:
            cutLayerLabel.setEnabled(false);
            layerComboBox.setEnabled(false);
            break;
        }
    }

    private JPanel jBasePanel() {
        cropPanel = new JPanel(new GridBagLayout());
        final ArrayList<String> array = new ArrayList<String>();
        array.add(LAYER);
        array.add(SELECTED);
        array.add(VIEW);
        cropComboBox = new JComboBox<>(new Vector<>(array));
        cropComboBox.setSelectedItem(array.get(0));
        cropComboBox.setSize(200, cropComboBox.getPreferredSize().height);
        cropComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGUI1(e, dialog);
                dialog.pack();
                dialog.repaint();
            }
        });
        cutObjectLabel = new JLabel(Target_OBJECT);
        FormUtils.addRowInGBL(cropPanel, 1, 0, cutObjectLabel, cropComboBox);
        final List<Layerable> layerables = JUMPWorkbench.getInstance()
                .getContext().getLayerManager().getLayerables(Layerable.class);
        layerComboBox = new JComboBox<>(new Vector<>(layerables));
        layerComboBox.setSelectedItem(layerables.get(0));
        layerComboBox.setSize(200, layerComboBox.getPreferredSize().height);
        cutLayerLabel = new JLabel(TARGET_LAYER);
        FormUtils.addRowInGBL(cropPanel, 2, 0, cutLayerLabel, layerComboBox);
        return cropPanel;
    }

    private void setDialogValues(PlugInContext context) throws IOException {
        dialog.setSideBarDescription(CROP_RASTER_TIP);
       if (!context.getLayerNamePanel().selectedNodes(RasterImageLayer.class)
                .isEmpty()) {
            rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                    context, RasterImageLayer.class);
        }
        else {
            rLayer = context.getTask().getLayerManager()
                    .getLayerables(RasterImageLayer.class).get(0);
        }
        List<RasterImageLayer> fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        JComboBox<RasterImageLayer> layerableComboBox =
                dialog.addLayerableComboBox(CLAYER, rLayer, "",
                fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        layerableComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.pack();
                dialog.repaint();
            }
        });
        final ArrayList<String> srsArray = new ArrayList<>();
        srsArray.add(CROP_RASTER);
        srsArray.add(WARP_RASTER);
        comboBox = dialog.addComboBox(ACTION_LABEL, srsArray.get(0), srsArray,
                null);
        comboBox.setSize(200, comboBox.getPreferredSize().height);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGUI(e, dialog);
                dialog.pack();
                dialog.repaint();
            }
        });
        dialog.addRow("base", jBasePanel(), null, null);
        final FileNameExtensionFilter filter;
        filter = new FileNameExtensionFilter("TIF", "tif");
        dialog.addRow("Save", new JLabel(OUTPUT_FILE + ":"),
                createOutputFilePanel(filter), saveCheck, null);
    }

    private final EnableCheck[] saveCheck = new EnableCheck[] { new EnableCheck() {
        @Override
        public String check(JComponent component) {
        	  rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);
            return jTextField_RasterOut.getText().equals(rLayer.getImageFileName()) ? 
            		NO_OVERWRITE : null;
        }
    },  new EnableCheck() {
        @Override
        public String check(JComponent component) {
            return jTextField_RasterOut.getText().isEmpty() ? CHECK
                    .concat(OUTPUT_FILE) : null;
        }
    } };

    

    private void getDialogValues(MultiInputDialog dialog) {
    	rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);
        ACTION = dialog.getText(ACTION_LABEL);
        CROP = cropComboBox.getSelectedItem().toString();
        getCroppedEnvelope();
        path = getOutputFilePath();
        final int i = path.lastIndexOf('.');
        if (i > 0) {
            path = path.substring(0, i);
        }
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), NAME, true);
        setDialogValues(context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        if (!dialog.wasOKPressed()) {
            return false;
        } else if (dialog.wasOKPressed()) {
            getDialogValues(dialog);
            return true;
        }
        return false;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(PROCESSING);
        reportNothingToUndoYet(context);
        final File outFile = FileUtil.addExtensionIfNone(new File(path), "tif");
        GenericRasterAlgorithm IO = new GenericRasterAlgorithm();
        if (ACTION.equals(CROP_RASTER)) {

            IO.save_CropToEnvelope(outFile, rLayer, fix);

        } else if (ACTION.equals(WARP_RASTER)) {
            IO.save_WarpToEnvelope(outFile, rLayer, envWanted);
        }
        String catName = StandardCategoryNames.WORKING;
        try {
            catName = ((Category) context.getLayerNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e1) {
        }
        IO.load(outFile, catName);
    }

    private void getCroppedEnvelope() {
        envWanted = new Envelope();
        fix = new Envelope();
        RasterImageLayer rLayer = (RasterImageLayer) dialog.getLayerable(CLAYER);
        if (CROP.equals(LAYER)) {
            final Layerable slayer = (Layerable) layerComboBox
                    .getSelectedItem();
            if (slayer instanceof WMSLayer) {
                envWanted.expandToInclude(((WMSLayer) slayer).getEnvelope());
            //} else if (slayer instanceof WFSLayer) {
            //    envWanted.expandToInclude(((WFSLayer) slayer)
            //            .getFeatureCollectionWrapper().getEnvelope());
            } else if (slayer instanceof Layer) {
                envWanted.expandToInclude(((Layer) slayer)
                        .getFeatureCollectionWrapper().getEnvelope());
            } else if (slayer instanceof RasterImageLayer) {
                envWanted.expandToInclude(((RasterImageLayer) slayer)
                        .getWholeImageEnvelope());
            }
            fix = envWanted.intersection(rLayer.getWholeImageEnvelope());
        } else if (CROP.equals(SELECTED)) {
            final SelectionManager smgr = ((SelectionManagerProxy) frame
                    .getActiveInternalFrame()).getSelectionManager();
            final Collection<Feature> features = smgr
                    .getFeaturesWithSelectedItems();
            final Feature feature = features.iterator().next().clone();
            final GeometryFactory factory = new GeometryFactory();
            feature.setGeometry(factory.createGeometryCollection(FeatureUtil
                    .toGeometries(features).toArray(
                            new Geometry[features.size()])));
            envWanted = feature.getGeometry().getEnvelopeInternal();
            fix = envWanted.intersection(rLayer.getWholeImageEnvelope());
        } else if (CROP.equals(VIEW)) {
            final Viewport viewport = frame.getContext().getLayerViewPanel()
                    .getViewport();
            envWanted = viewport.getEnvelopeInModelCoordinates();
            fix = envWanted.intersection(rLayer.getWholeImageEnvelope());
        }
    }

    private void updateGUI(ActionEvent evt, MultiInputDialog dialog) {
        switch (comboBox.getSelectedIndex()) {
        case 0:
            dialog.setSideBarDescription(CROP_RASTER_TIP);
            cutObjectLabel.setText(Target_OBJECT);
            break;
        case 1:
            dialog.setSideBarDescription(WARP_RASTER_TIP);
            cutObjectLabel.setText(Target_OBJECT);
            break;
        }
    }

    public JPanel createOutputFilePanel(FileNameExtensionFilter filter) {
        JPanel jPanel = new javax.swing.JPanel();
        jTextField_RasterOut = new JTextField();
        final JButton jButton_Dir = new JButton();
        jTextField_RasterOut.setText("");
        jButton_Dir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                File outputPathFile;
                final JFileChooser chooser = new GUIUtil.FileChooserWithOverwritePrompting();
                chooser.setDialogTitle(getName());
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setSelectedFile(FileOperations.lastVisitedFolder);
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                GUIUtil.removeChoosableFileFilters(chooser);
                chooser.setFileFilter(filter);
                final int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    outputPathFile = FileUtil.removeExtensionIfAny(chooser
                            .getSelectedFile());
                    jTextField_RasterOut.setText(outputPathFile.getPath()
                            .concat(".tif"));
                    FileOperations.lastVisitedFolder = outputPathFile;
                }
            }
        });
        jTextField_RasterOut.setEditable(true);
        jButton_Dir.setIcon(icon16);
        jTextField_RasterOut.setPreferredSize(new Dimension(250, 20));
        FormUtils.addRowInGBL(jPanel, 3, 0, jTextField_RasterOut);
        FormUtils.addRowInGBL(jPanel, 3, 1, jButton_Dir);
        return jPanel;
    }

  

    public String getOutputFilePath() {
        return jTextField_RasterOut.getText();
    }

    @Override
    public String getName() {
        return NAME;
    }
 
 

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class));
    }

}