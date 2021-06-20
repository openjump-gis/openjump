

/**
 * created:  		21. Sept.2020
 *
 * @author Giuseppe Aruta
 * @description: A tool to rasterize a vector layer.
 */

package org.openjump.core.ui.plugin.tools.generate;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.rasterimage.algorithms.RasterizeAlgorithm;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import it.betastudio.adbtoolbox.libs.FileOperations;


public class RasterizePlugIn extends AbstractPlugIn
    implements ThreadedPlugIn {

  private Layer sourceLayer;
  private JTextField cellYextFiels;
  private JCheckBox externalLayerCheck, expandCheck, loadCheck;
  private JComboBox<Layer> selectLayerComboBox;
  private JComboBox<Layerable> layerableExtentComboBox;
  private JComboBox<String> jcb_attribute;
  private LayerNameRenderer layerListCellRenderer = new LayerNameRenderer();

  private static String selAttribute = null;
  private final String ATTRIBUTE = GenericNames.SELECT_ATTRIBUTE;
  private String path;
  double cellValue;
  private final ImageIcon icon16 = IconLoader
      .icon("fugue/folder-horizontal-open_16.png");
  JTextField jTextField_RasterOut = new JTextField();
  private final String OUTPUT_FILE = I18N.getInstance().get("driver.DriverManager.file-to-save");
  private final String CHECK = I18N.getInstance().get("ui.GenericNames.check-field");
  private final static String SOURCE_LAYER = I18N.getInstance().get("ui.GenericNames.Source-Layer");
  private final static String TARGET_LAYER = I18N.getInstance().get("ui.GenericNames.Target-Layer");
  private final static String CELL_SIZE = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.size");
  public static final Icon ICON = IconLoader.icon("rasterize.png");

  private final static String RASTERIZE_VECTOR = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.Name");
  private final static String RASTERIZING_VECTOR = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.rasterizing-layer");
  private final static String PREPARING_VECTOR = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.preparing-layer");
  private final static String USE_EXTERNAL_EXTENT = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.use-extent");
  private final static String DESCRIPTION = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.description");

  //TODO internationalize
  private final static String EXPAND_ONE_CELL = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.expand-one-cell");
  private final static String EXPAND_ONE_CELL_TIP = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.expand-one-cell-tip");
  private final static String LOAD_RASTER_INTO_VIEW = I18N.getInstance().get("ui.plugin.tools.generate.RasterizePlugIn.load-raster");

  @Override
  public boolean execute(PlugInContext context) throws Exception {
    MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
    initDialog(dialog, context);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) {
      return false;
    }
    getDialogValues(dialog);
    return true;
  }


  @Override
  public String getName() {
    return RASTERIZE_VECTOR;
  }

 /*   @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS,MenuNames.TOOLS_GENERATE},
                getName(), false,ICON,
                createEnableCheck(context.getWorkbenchContext()), -1);
    }*/

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createTaskWindowMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }


  private final EnableCheck[] saveCheck = new EnableCheck[]{new EnableCheck() {
    @Override
    public String check(JComponent component) {
      return jTextField_RasterOut.getText().isEmpty() ? CHECK
          .concat(": ").concat(OUTPUT_FILE) : null;
    }
  }};


  @SuppressWarnings("unchecked")
  private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarDescription(DESCRIPTION);
    selectLayerComboBox = dialog.addLayerComboBox(SOURCE_LAYER, context.getCandidateLayer(0),
        context.getLayerManager());
    selectLayerComboBox.setSize(240,
        selectLayerComboBox.getPreferredSize().height);
    final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
        .filter(context.getCandidateLayer(0));
    final String val = list.size() > 0 ? list.get(0) : null;
    jcb_attribute = dialog.addComboBox(ATTRIBUTE,
        val, list, ATTRIBUTE);

    cellYextFiels = dialog.addDoubleField(CELL_SIZE, 5, 10);
    cellYextFiels.setSize(jcb_attribute.getWidth(),
        jcb_attribute.getPreferredSize().height);
    externalLayerCheck = dialog.addCheckBox(USE_EXTERNAL_EXTENT, false);

    List<Layerable> layerables = new ArrayList<>();
    for (Layerable layerable : JUMPWorkbench.getInstance()
        .getContext().getLayerManager().getLayerables(Layerable.class)) {
      if (layerable instanceof Layer || layerable instanceof RasterImageLayer) {
        layerables.add(layerable);
      }

    }
    layerableExtentComboBox = dialog.addLayerableComboBox(TARGET_LAYER, context.getCandidateLayer(0),
        null, layerables);
    layerListCellRenderer = new LayerNameRenderer();
    layerListCellRenderer.setCheckBoxVisible(false);
    layerListCellRenderer.setProgressIconLabelVisible(false);
    layerableExtentComboBox.setRenderer(layerListCellRenderer);
    layerableExtentComboBox.setEnabled(false);
    layerableExtentComboBox.setSize(240,
        layerableExtentComboBox.getPreferredSize().height);
    expandCheck = dialog.addCheckBox(EXPAND_ONE_CELL, false, EXPAND_ONE_CELL_TIP);
    expandCheck.setEnabled(false);
    selectLayerComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
            .filter(dialog.getLayer(SOURCE_LAYER));
        jcb_attribute.setModel(new DefaultComboBoxModel<>(list
            .toArray(new String[0])));
        expandCheck.setEnabled(externalLayerCheck.isSelected());
      }
    });
    externalLayerCheck.addActionListener(e ->
        layerableExtentComboBox.setEnabled(externalLayerCheck.isSelected()));
    //[Giuseppe Aruta 2020-09-27] deactivated. As suggested by Roberto Rossi
    //It is better to leave a standard value for cell that user can modified
    // [Giuseppe Aruta 2020-09-29]  reactivated
    layerableExtentComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Layerable slayer = (Layerable) layerableExtentComboBox
            .getSelectedItem();
        if (slayer instanceof RasterImageLayer) {
          cellYextFiels.setText("" + ((RasterImageLayer) slayer).getMetadata().getOriginalCellSize());
        }
      }
    });

    final FileNameExtensionFilter filter;
    filter = new FileNameExtensionFilter("TIF", "tif");
    dialog.addRow("Save", new JLabel(OUTPUT_FILE + ":"),
        createOutputFilePanel(filter), saveCheck, null);
    loadCheck = dialog.addCheckBox(LOAD_RASTER_INTO_VIEW, true);
    GUIUtil.centreOnWindow(dialog);
  }

  Envelope envWanted, fix;

  //Expand ouput envelope only if required
  private void getCroppedEnvelope(Layer layer) {
    Envelope env = null;
    if (externalLayerCheck.isSelected()) {
      envWanted = new Envelope();
      final Layerable slayer = (Layerable) layerableExtentComboBox
          .getSelectedItem();
      if (slayer instanceof Layer) {
        env = ((Layer) slayer)
            .getFeatureCollectionWrapper().getEnvelope().intersection(layer.getFeatureCollectionWrapper().getEnvelope());

      } else if (slayer instanceof RasterImageLayer) {
        env = ((RasterImageLayer) slayer)
            .getWholeImageEnvelope().intersection(layer.getFeatureCollectionWrapper().getEnvelope());

      }
      if (expandCheck.isSelected()) {
        envWanted = new Envelope(env.getMinX() - cellValue, env.getMaxX() + 2 * cellValue,
            env.getMinY() - cellValue, env.getMaxY() + cellValue);
      } else {
        envWanted = env;
      }
    } else {
      envWanted = sourceLayer.getFeatureCollectionWrapper().getEnvelope();
    }
    fix = envWanted;
  }


  private void getDialogValues(MultiInputDialog dialog) {
    sourceLayer = dialog.getLayer(SOURCE_LAYER);
    cellValue = dialog.getDouble(CELL_SIZE);
    selAttribute = dialog.getText(ATTRIBUTE);
    path = getOutputFilePath();
    final int i = path.lastIndexOf('.');
    if (i > 0) {
      path = path.substring(0, path.length() - path.length() + i);
    }
    getCroppedEnvelope(sourceLayer);
  }


  public String getOutputFilePath() {
    return jTextField_RasterOut.getText();
  }


  @Override
  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

    monitor.allowCancellationRequests();
    FeatureCollection fc = sourceLayer.getFeatureCollectionWrapper();
    final File outFile = FileUtil.addExtensionIfNone(new File(path), "tif");
    monitor.report(RASTERIZING_VECTOR + "...");
    RasterizeAlgorithm rasterize = new RasterizeAlgorithm(fix, fc, selAttribute, cellValue);
    rasterize.process();
    rasterize.saveToFile(outFile);

    String catName = StandardCategoryNames.WORKING;
    try {
      catName = ((Category) context.getLayerNamePanel()
          .getSelectedCategories().toArray()[0]).getName();

    } catch (final RuntimeException e1) {

      Logger.error(e1);
    }
    if (loadCheck.isSelected()) {
      load(outFile, context, catName);
    }

  }


  public JPanel createOutputFilePanel(FileNameExtensionFilter filter) {
    JPanel jPanel = new JPanel();
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
    jTextField_RasterOut.setPreferredSize(new Dimension(200, 20));

    FormUtils.addRowInGBL(jPanel, 3, 0, jTextField_RasterOut);
    FormUtils.addRowInGBL(jPanel, 3, 1, jButton_Dir, true, true);
    return jPanel;
  }

  public static void load(File file, PlugInContext context, String category)
      throws Exception {

    RasterImageIO rasterImageIO = new RasterImageIO();
    Viewport viewport = context.getWorkbenchContext().getLayerViewPanel()
        .getViewport();
    Resolution requestedRes = RasterImageIO
        .calcRequestedResolution(viewport);
    ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(
        /*context.getWorkbenchContext(),*/ file.getAbsolutePath(), null,
        viewport.getEnvelopeInModelCoordinates(), requestedRes);
    Point point = RasterImageIO.getImageDimensions(file.getAbsolutePath());
    Envelope env = RasterImageIO.getGeoReferencing(file.getAbsolutePath(),
        true, point);

    RasterImageLayer ril = new RasterImageLayer(file.getName(), context
        .getWorkbenchContext().getLayerManager(),
        file.getAbsolutePath(), imageAndMetadata.getImage(), env);
    try {
      category = ((Category) context.getLayerNamePanel()
          .getSelectedCategories().toArray()[0]).getName();
    } catch (RuntimeException e1) {
      Logger.error(e1);
    }
    context.getLayerManager().addLayerable(category, ril);
  }


}
