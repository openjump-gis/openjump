package org.openjump.core.ui.plugin.raster;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.algorithms.VectorizeAlgorithm;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.util.LayerableUtil;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.Viewport;

public class VectorizeToContoursPlugIn extends ThreadedBasePlugIn {

    private static I18N i18n = I18N.getInstance();
    private static String PROCESSING = I18N.getInstance().get("jump.plugin.edit.NoderPlugIn.processing");
    private static String sLayer = I18N.getInstance().get("ui.GenericNames.Source-Layer");
    private static String NAME = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.Name");
    private static String sValue = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.value");

    private static String sStyle = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.apply-random-style");

    public static String contour_baseContour = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.base-contour");
    public static String contour_distanceContours = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.interval");
    //  public static String contour_zeroElevation = RasterMenuNames.contour_zeroElevation;
    public static String contour_minContour = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.min-contour");
    public static String contour_maxcontour = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.max-contour");
    public static String contour_contourNumber = I18N.getInstance().get("ui.plugin.raster.VectorizeToContoursPlugIn.contour-number");

    private final String MIN = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.min");
    private final String MAX = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.nodata.max");

    JTextField jTextField_ContBase, jTextField_ContIntv, jTextField_ContMin,
            jTextField_ContMax, jTextField_ContCount, max, min;

    private double min_value, max_value;

    private int contCount = -1;
    private double contBase = 0;
    private int contIntv = 100;
    private double contMin = -1;
    private double contMax = -1;
    boolean applystyleb = false;

    List<RasterImageLayer> fLayers = new ArrayList<RasterImageLayer>();
    JComboBox<RasterImageLayer> layerableComboBox = new JComboBox<RasterImageLayer>();

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

        findParameters(layer);
        fLayers = context.getTask().getLayerManager()
                .getLayerables(RasterImageLayer.class);
        layerableComboBox = dialog.addLayerableComboBox(sLayer, layer, "",
                fLayers);
        layerableComboBox.setSize(200,
                layerableComboBox.getPreferredSize().height);
        layerableComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                layer = (RasterImageLayer) dialog.getLayerable(sLayer);
                findParameters(layer);

                dialog.pack();
                dialog.repaint();
            }
        });

        min = dialog.addDoubleField(MIN, min_value, 20);
        min.setEditable(false);
        max = dialog.addDoubleField(MAX, max_value, 20);
        max.setEditable(false);

        jTextField_ContBase = dialog.addDoubleField(contour_baseContour,
                contBase, 20);
        jTextField_ContBase.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                contIntv = dialog.getInteger(contour_distanceContours);
                contBase = dialog.getDouble(contour_baseContour);
                findParameters(layer);

                dialog.pack();
                dialog.repaint();
            }
        });
        jTextField_ContIntv = dialog.addIntegerField(contour_distanceContours,
                contIntv, 20, "");
        jTextField_ContIntv.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                contIntv = dialog.getInteger(contour_distanceContours);
                contBase = dialog.getDouble(contour_baseContour);
                findParameters(layer);

                dialog.pack();
                dialog.repaint();
            }
        });

        jTextField_ContMin = dialog.addDoubleField(contour_minContour, contMin,
                20);
        jTextField_ContMin.setEditable(false);
        jTextField_ContMax = dialog.addDoubleField(contour_maxcontour, contMax,
                20);
        jTextField_ContMax.setEditable(false);
        jTextField_ContCount = dialog.addIntegerField(contour_contourNumber,
                contCount, 20, "");
        jTextField_ContCount.setEditable(false);
        dialog.addCheckBox(sStyle, true);

    }

    /*
     * The following code derives from AdbToolbox RasterToContour and has been modified to work with OpenJUMP
     */
    private void findParameters(RasterImageLayer layer) {

        min_value = layer.getMetadata().getStats().getMin(0);
        max_value = layer.getMetadata().getStats().getMax(0);

        try {

            // Bad implementation: try and find

            if (contBase < min_value) {
                contMin = contBase;
                while (contMin < min_value) {
                    contMin = contMin + contIntv;
                }
                contMax = contMin;
                while (contMax < max_value) {
                    contMax = contMax + contIntv;
                }
                contMax = contMax - contIntv;
            } else if (contBase == min_value) {
                contMin = min_value;
                contMax = min_value;
                while (contMax < max_value) {
                    contMax = contMax + contIntv;
                }
                contMax = contMax - contIntv;
            } else if (contBase > min_value && contBase < max_value) {
                contMin = contBase;
                while (contMin > min_value) {
                    contMin = contMin - contIntv;
                }
                contMin = contMin + contIntv;
                contMax = contBase;
                while (contMax < max_value) {
                    contMax = contMax + contIntv;
                }
                contMax = contMax - contIntv;
            } else if (contBase == max_value) {
                contMin = contMax;
                while (contMin > min_value) {
                    contMin = contMin - contIntv;
                }
                contMin = contMin + contIntv;
                contMax = max_value;
            } else if (contBase > max_value) {
                contMax = contBase;
                while (contMax > max_value) {
                    contMax = contMax - contIntv;
                }
                contMin = contMax;
                while (contMin > min_value) {
                    contMin = contMin - contIntv;
                }
                contMin = contMin + contIntv;
            }

            contCount = (int) Math.ceil((contMax - contMin) / contIntv) + 1;

            min.setText(min_value + "");
            max.setText(max_value + "");
            jTextField_ContMin.setText(contMin + "");
            jTextField_ContMax.setText(contMax + "");
            jTextField_ContCount.setText(contCount + "");

        } catch (final Exception ex) {

            return;

        }
    }

    private void getDialogValues(MultiInputDialog dialog) {

        layer = (RasterImageLayer) dialog.getLayerable(sLayer);
        contIntv = dialog.getInteger(contour_distanceContours);

        applystyleb = dialog.getBoolean(sStyle);

    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        monitor.report(PROCESSING);
        reportNothingToUndoYet(context);
        //  Utils.zoom(layer);
        final OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
        rstLayer.create(layer, true);
        final GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
                rstLayer, rstLayer.getLayerGridExtent());
        final FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("geometry", AttributeType.GEOMETRY);
        fs.addAttribute(sValue, AttributeType.DOUBLE);
        FeatureCollection featDataset = new VectorizeAlgorithm()
                .toContours(gwrapper, contMin, contMax, contIntv, sValue, 0);

        final Layer vlayer = context.addLayer(StandardCategoryNames.WORKING,
                rstLayer.getName() + "_" + "vectorized", featDataset);

        if (applystyleb) {
            Utils.applyRandomGradualStyle(vlayer, sValue);

        }

    }

    /**
     * Load a file into the workbench
     * @param inputFile
     *          eg. "new File(C:/folder/fileName.tif)"
     * @param category
     *          eg. "Working"
     * @throws NoninvertibleTransformException if a NoninvertibleTransformException occurs
     * @throws TiffReadingException if a TiffReadingException occurs
     * @throws Exception if a Exception occurs
     */
    public void load(File inputFile, String category, PlugInContext context)
            throws NoninvertibleTransformException, TiffReadingException,
            Exception {

        final RasterImageIO rasterImageIO = new RasterImageIO();
        final Point point = RasterImageIO.getImageDimensions(inputFile
                .getAbsolutePath());
        final Envelope env = RasterImageIO.getGeoReferencing(
               inputFile.getAbsolutePath(), true, point);
         
        final Viewport viewport = context.getLayerViewPanel().getViewport();
        final Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        final ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
                /*context.getWorkbenchContext(),*/ inputFile.getAbsolutePath(),
                null, viewport.getEnvelopeInModelCoordinates(), requestedRes);
        final RasterImageLayer ril = new RasterImageLayer(inputFile.getName(),
                context.getLayerManager(), inputFile.getAbsolutePath(),
                imageAndMetadata.getImage(), env);
        try {
            category = ((Category) context.getLayerableNamePanel()
                    .getSelectedCategories().toArray()[0]).getName();
        } catch (final RuntimeException e) {

        }
        context.getLayerManager().addLayerable(category, ril);
    }

    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesOfTypeMustExistCheck(
                        1, RasterImageLayer.class)).add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        final List<RasterImageLayer> mLayer = new ArrayList<>();
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
                            msg = I18N.getInstance().get("plugin.EnableCheckFactory.at-least-one-single-banded-layer-should-exist");;
                        }
                        return msg;
                    }
                });
    }

}
