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
package com.vividsolutions.jump.workbench;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.swing.JToggleButton;

import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.imagery.ecw.ECWImageFactory;
import com.vividsolutions.jump.workbench.imagery.ecw.JPEG2000ImageFactory;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.imagery.geotiff.GeoTIFFImageFactory;
import com.vividsolutions.jump.workbench.imagery.graphic.CommonsImageFactory;
import com.vividsolutions.jump.workbench.imagery.mrsid.MrSIDImageFactory;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import org.openjump.core.feature.BeanshellAttributeOperationFactory;
import org.openjump.core.rasterimage.AddRasterImageLayerWizard;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayerRendererFactory;
import org.openjump.core.ui.DatasetOptionsPanel;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.datastore.AddDataStoreLayerWizard;
import org.openjump.core.ui.plugin.datastore.AddWritableDataStoreLayerWizard;
import org.openjump.core.ui.plugin.file.DataSourceQueryChooserOpenWizard;
import org.openjump.core.ui.plugin.file.OpenWizardPlugIn;
import org.openjump.core.ui.plugin.layer.LayerableStylePlugIn;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureTool;
import org.openjump.core.ui.plugin.tools.ZoomRealtimeTool;
import org.openjump.core.ui.plugin.view.SuperZoomPanTool;

import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DrawPolygonFenceTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DrawRectangleFenceTool;
import com.vividsolutions.jump.workbench.ui.cursortool.FeatureInfoTool;
import com.vividsolutions.jump.workbench.ui.cursortool.OrCompositeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectFeaturesTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.WmsLayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.style.*;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomNextPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomPreviousPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFencePlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFullExtentPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;
import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.core.ui.style.decoration.ArrowLineStringMiddlepointStyle;
import org.openjump.core.ui.style.decoration.SegmentDownhillArrowStyle;
import org.openjump.core.ui.style.decoration.VertexZValueStyle;
import org.openjump.core.ui.swing.factory.field.ComboBoxFieldComponentFactory;
import org.openjump.core.ui.swing.factory.field.FieldComponentFactoryRegistry;
import org.openjump.core.ui.swing.factory.field.FileFieldComponentFactory;
import org.openjump.core.ui.swing.wizard.WizardGroup;


/**
 * Initializes the Workbench with various menus and cursor tools. Accesses the
 * Workbench structure through a WorkbenchContext.
 */
public class JUMPConfiguration implements Setup {

  /**
   * Built-in plugins must be defined as instance variables, since they are
   * located for iniatialization via reflection on this class
   */

  private final ClearSelectionPlugIn clearSelectionPlugIn = new ClearSelectionPlugIn();

  private final EditingPlugIn editingPlugIn = EditingPlugIn.getInstance();

  private final NewTaskPlugIn newTaskPlugIn = new NewTaskPlugIn();

  private final LayerableStylePlugIn changeStylesPlugIn = new LayerableStylePlugIn();

  private final UndoPlugIn undoPlugIn = new UndoPlugIn();

  private final RedoPlugIn redoPlugIn = new RedoPlugIn();

  private final ViewAttributesPlugIn viewAttributesPlugIn = new ViewAttributesPlugIn();

  private final OutputWindowPlugIn outputWindowPlugIn = new OutputWindowPlugIn();

  private final ZoomNextPlugIn zoomNextPlugIn = new ZoomNextPlugIn();

  private final ZoomPreviousPlugIn zoomPreviousPlugIn = new ZoomPreviousPlugIn();

  private final ZoomToFencePlugIn zoomToFencePlugIn = new ZoomToFencePlugIn();

  private final ZoomToFullExtentPlugIn zoomToFullExtentPlugIn = new ZoomToFullExtentPlugIn();

  private final ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn = new ZoomToSelectedItemsPlugIn();


  // ////////////////////////////////////////////////////////////////////
  public void setup(WorkbenchContext workbenchContext) throws Exception {

    final PlugInContext plugInContext = new PlugInContext(workbenchContext,
            null, null, null, null);

    configureStyles(workbenchContext);

    workbenchContext.getWorkbench().getBlackboard()
        .put(SnapToVerticesPolicy.ENABLED_KEY, true);

    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
    configureToolBar(workbenchContext, checkFactory);
    configureMainMenus(workbenchContext, checkFactory, featureInstaller);
    //configureLayerPopupMenu(workbenchContext, featureInstaller, checkFactory);
    //configureAttributePopupMenu(workbenchContext, featureInstaller,
    //    checkFactory);
    //configureWMSQueryNamePopupMenu(workbenchContext, featureInstaller,
    //    checkFactory);
    //configureCategoryPopupMenu(workbenchContext, featureInstaller);
    //configureLayerViewPanelPopupMenu(workbenchContext, checkFactory,
    //    featureInstaller);

    initializeRenderingManager();

    initializeFieldComponentFactories(workbenchContext);

    initializeAttributeOperationFactories(plugInContext);

    // Call #initializeBuiltInPlugIns after #configureToolBar so that any
    // plug-ins that
    // add items to the toolbar will add them to the *end* of the toolbar.
    // [Jon Aquino]
    initializeBuiltInPlugIns(plugInContext);

    // Disable drawing of invalid polygons by default (can be changed during
    // work in EditOptionsPanel)
    PersistentBlackboardPlugIn.get(workbenchContext).put(
            EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, true);
  }

  private void initializeRenderingManager() {
    RenderingManager.setRendererFactory(Layer.class,
            new LayerRendererFactory());
    RenderingManager.setRendererFactory(WMSLayer.class,
            new WmsLayerRendererFactory());
  }

  //private void configureCategoryPopupMenu(WorkbenchContext workbenchContext,
  //    FeatureInstaller featureInstaller) throws Exception {
  //  // fetch the menu reference
  //  JPopupMenu menu = workbenchContext.getWorkbench().getFrame()
  //      .getCategoryPopupMenu();
  //
  //  PlugInContext pc = workbenchContext.createPlugInContext();
  //}

  //private void configureWMSQueryNamePopupMenu(
  //    final WorkbenchContext workbenchContext,
  //    FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
  //  JPopupMenu wmsLayerNamePopupMenu = workbenchContext.getWorkbench()
  //      .getFrame().getWMSLayerNamePopupMenu();
  //}

  //private void configureAttributePopupMenu(
  //    final WorkbenchContext workbenchContext,
  //    FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
  //}

  //private void configureLayerPopupMenu(final WorkbenchContext workbenchContext,
  //    FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
  //}

  //private void configureLayerViewPanelPopupMenu(
  //    WorkbenchContext workbenchContext, EnableCheckFactory checkFactory,
  //    FeatureInstaller featureInstaller) {
  //}

  private void configureMainMenus(final WorkbenchContext workbenchContext,
      final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
      throws Exception {

    // FILE ===================================================================
    String[] fileMenuPath = new String[] { MenuNames.FILE };

    featureInstaller.addMenuSeparator(fileMenuPath);
    // menu exit item
    workbenchContext.getWorkbench().getFrame().new ExitPlugin()
        .initialize(workbenchContext.createPlugInContext());

    // LAYER ==================================================================
    //configLayer(workbenchContext, checkFactory, featureInstaller);
  }


  //private void configLayer(final WorkbenchContext workbenchContext,
  //    final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
  //    throws Exception {
  //}

  private void configureStyles(WorkbenchContext workbenchContext) {
    WorkbenchFrame frame = workbenchContext.getWorkbench().getFrame();
    frame.addChoosableStyleClass(VertexXYLineSegmentStyle.VertexXY.class);
    frame.addChoosableStyleClass(VertexIndexLineSegmentStyle.VertexIndex.class);
    frame.addChoosableStyleClass(MetricsLineStringSegmentStyle.LengthAngle.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.Open.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.Solid.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.NarrowSolid.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.FeathersStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.FeathersEnd.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.OpenStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.OpenEnd.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.SolidStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.SolidEnd.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.NarrowSolidStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.NarrowSolidEnd.class);
    frame.addChoosableStyleClass(CircleLineStringEndpointStyle.Start.class);
    frame.addChoosableStyleClass(CircleLineStringEndpointStyle.End.class);

    frame.addChoosableStyleClass(ArrowLineStringMiddlepointStyle.NarrowSolidMiddle.class);
    frame.addChoosableStyleClass(SegmentDownhillArrowStyle.NarrowSolidMiddle.class);
    frame.addChoosableStyleClass(SegmentDownhillArrowStyle.Open.class);
    frame.addChoosableStyleClass(SegmentDownhillArrowStyle.Solid.class);
    frame.addChoosableStyleClass(VertexZValueStyle.VertexZValue.class);
  }

  private QuasimodeTool add(CursorTool tool, WorkbenchContext context) {
    return context.getWorkbench().getFrame().getToolBar().addCursorTool(tool)
        .getQuasimodeTool();
  }

  private void configureToolBar(final WorkbenchContext workbenchContext,
      EnableCheckFactory checkFactory) {
    WorkbenchFrame frame = workbenchContext.getWorkbench().getFrame();
    frame.getToolBar().addPlugIn(newTaskPlugIn.getIcon(20), newTaskPlugIn,
        NewTaskPlugIn.createEnableCheck(workbenchContext), workbenchContext);
    frame.getToolBar().addSeparator();
    add(new ZoomTool(), workbenchContext);
    add(new PanTool(), workbenchContext);
    // Test for the new Zoom/Pan tool, comment the following line out, if it
    // makes problems
    add(new SuperZoomPanTool(), workbenchContext);
    frame.getToolBar().addSeparator();
    frame.getToolBar().addPlugIn(zoomToFullExtentPlugIn.getIcon(new Dimension(20, 20)),
        zoomToFullExtentPlugIn,
        zoomToFullExtentPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    frame.getToolBar().addPlugIn(zoomToSelectedItemsPlugIn.getIcon(),
        zoomToSelectedItemsPlugIn,
        ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    add(new ZoomRealtimeTool(), workbenchContext); // TODO: move to
                                                   // OpenJumpConfiguration if
                                                   // possible
    frame.getToolBar().addPlugIn(
        zoomToFencePlugIn.getIcon(),
        zoomToFencePlugIn,
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createFenceMustBeDrawnCheck()), workbenchContext);
    frame.getToolBar().addPlugIn(zoomPreviousPlugIn.getIcon(),
        zoomPreviousPlugIn,
        zoomPreviousPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    frame.getToolBar().addPlugIn(zoomNextPlugIn.getIcon(), zoomNextPlugIn,
        zoomNextPlugIn.createEnableCheck(workbenchContext), workbenchContext);
    frame.getToolBar().addPlugIn(changeStylesPlugIn.getIcon(),
        changeStylesPlugIn,
        changeStylesPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    frame.getToolBar().addPlugIn(viewAttributesPlugIn.getIcon(),
        viewAttributesPlugIn,
        viewAttributesPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    frame.getToolBar().addSeparator();

    SelectFeaturesTool sft = new SelectFeaturesTool();
    add( sft, workbenchContext);
    // [mmichaud 2012-07-12] by default, the first CursorTool (zoom) is
    // activated. After that, the SelectTool button will be selected.
    // See also the end of JUMPWorkbench.main() where the SelectFeatureTool
    // will really be activated (it takes place later because it needs the
    // LayerViewPanel to be initialized)
    frame.getToolBar().getButton(sft.getClass()).doClick();

    frame.getToolBar().addPlugIn(ClearSelectionPlugIn.getIcon(),
        clearSelectionPlugIn,
        clearSelectionPlugIn.createEnableCheck(workbenchContext),
        workbenchContext);
    add(new OrCompositeTool() {
      public String getName() {
        return I18N.getInstance().get("JUMPConfiguration.fence");
      }
    }.add(new DrawRectangleFenceTool()).add(new DrawPolygonFenceTool()),
        workbenchContext);
    add(new FeatureInfoTool(), workbenchContext);
    frame.getToolBar().addSeparator();
    configureEditingButton(workbenchContext);
    frame.getToolBar().addSeparator();
    AdvancedMeasureTool advancedMeasureTool = new AdvancedMeasureTool(
        workbenchContext);
    workbenchContext
        .getWorkbench()
        .getFrame()
        .getToolBar()
        .addCursorTool(advancedMeasureTool,
            advancedMeasureTool.getToolbarButton());
    OptionsDialog
        .instance(workbenchContext.getWorkbench())
        .addTab(
            I18N.getInstance().get("org.openjump.core.ui.plugin.tools.AdvancedMeasurePlugin.OptionPanelTitle"),
            new AdvancedMeasureOptionsPanel(workbenchContext));

    frame.getToolBar().addSeparator();
    frame.getToolBar().addPlugIn(undoPlugIn.getIcon(), undoPlugIn,
        undoPlugIn.createEnableCheck(workbenchContext), workbenchContext);
    frame.getToolBar().addPlugIn(redoPlugIn.getIcon(), redoPlugIn,
        redoPlugIn.createEnableCheck(workbenchContext), workbenchContext);
    frame.getToolBar().addSeparator();
    workbenchContext
        .getWorkbench()
        .getFrame()
        .getOutputFrame()
        .setButton(
            frame.getToolBar().addPlugIn(outputWindowPlugIn.getIcon(),
                outputWindowPlugIn, new MultiEnableCheck(), workbenchContext));
    // Last of all, add a separator because some plug-ins may add CursorTools.
    // [Jon Aquino]
    frame.getToolBar().addSeparator();
  }

  private void configureEditingButton(final WorkbenchContext workbenchContext) {
    final JToggleButton toggleButton = new JToggleButton();
    workbenchContext
        .getWorkbench()
        .getFrame()
        .getToolBar()
        .add(
            toggleButton,
            editingPlugIn.getName(),
            EditingPlugIn.ICON,
            AbstractPlugIn.toActionListener(editingPlugIn, workbenchContext,
                new TaskMonitorManager()), null);
    workbenchContext.getWorkbench().getFrame()
        .addComponentListener(new ComponentAdapter() {

          public void componentShown(ComponentEvent e) {
            // Can't #getToolbox before Workbench is thrown.
            // Otherwise, get
            // IllegalComponentStateException. Thus, do it inside
            // #componentShown. [Jon Aquino]
            editingPlugIn.getToolbox(workbenchContext).addComponentListener(
                new ComponentAdapter() {

                  // There are other ways to show/hide the
                  // toolbox. Track 'em. [Jon Aquino]
                  public void componentShown(ComponentEvent e) {
                    toggleButton.setSelected(true);
                  }

                  public void componentHidden(ComponentEvent e) {
                    toggleButton.setSelected(false);
                  }
                });
          }
        });
  }

  private void initializeFieldComponentFactories(WorkbenchContext workbenchContext) {
    // Used in some com.vividsolutions.jump.workbench.ui.wizard classes
    FieldComponentFactoryRegistry.setFactory(workbenchContext, "FileString",
            new FileFieldComponentFactory(workbenchContext));
    FieldComponentFactoryRegistry.setFactory(workbenchContext, "CharSetComboBoxField",
            new ComboBoxFieldComponentFactory(workbenchContext,
                    null, Charset.availableCharsets().keySet().toArray()));
  }

  private void initializeAttributeOperationFactories(PlugInContext context) {
    new BeanshellAttributeOperationFactory(context);
  }

  /**
   * Call each PlugIn's #initialize() method. Uses reflection to build a list of
   * plug-ins.
   */
  private void initializeBuiltInPlugIns(PlugInContext context)
      throws Exception {
    Field[] fields = getClass().getDeclaredFields();

    Object field = null;
    for (int i = 0; i < fields.length; i++) {
      try {
        field = fields[i].get(this);
      } catch (IllegalAccessException e) {
        Assert.shouldNeverReachHere();
      }

      if (!(field instanceof PlugIn)) {
        continue;
      }

      PlugIn plugIn = (PlugIn) field;
      plugIn.initialize(context);

      // register shortcuts of plugins
      AbstractPlugIn.registerShortcuts(plugIn);
    }
  }

  public void postExtensionInitialization(WorkbenchContext workbenchContext) {

    /***************************************************************************
     * Open Wizards
     **************************************************************************/
    AddDataStoreLayerWizard addDataStoreLayerWizard = new AddDataStoreLayerWizard(
            workbenchContext);
    OpenWizardPlugIn.addWizard(workbenchContext, addDataStoreLayerWizard);

    AddWmsLayerWizard addWmsLayerWizard = new AddWmsLayerWizard(
            workbenchContext);
    OpenWizardPlugIn.addWizard(workbenchContext, addWmsLayerWizard);

    //[sstein] 22.Feb.2009 -- added to load Pirol/Sextante images
    AddRasterImageLayerWizard addRasterImageLayerWizard = new AddRasterImageLayerWizard(
            workbenchContext);
    OpenWizardPlugIn.addWizard(workbenchContext, addRasterImageLayerWizard);

    // [mmichaud 2013-11-08] add new AddWritableDataStoreLayerWizard
    AddWritableDataStoreLayerWizard addWritableDataStoreLayerWizard =
            new AddWritableDataStoreLayerWizard(
                    workbenchContext,
                    "org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager"
            );
    OpenWizardPlugIn.addWizard(workbenchContext, addWritableDataStoreLayerWizard);

    // [mmichaud 2012-09-01] changed how RasterImageLayerRendererFactory is initialized to fix bug 3526653
    RenderingManager.setRendererFactory(RasterImageLayer.class, new RasterImageLayerRendererFactory());

    //-- adds the context menu for (Pirol/Sextante) Raster Images
    workbenchContext.getWorkbench().getFrame().getNodeClassToPopupMenuMap().put(RasterImageLayer.class, RasterImageContextMenu.getInstance(workbenchContext.createPlugInContext()));

    Registry registry = workbenchContext.getRegistry();
    List<DataSourceQueryChooser> loadChoosers = DataSourceQueryChooserManager.get(
            workbenchContext.getBlackboard()).getLoadDataSourceQueryChoosers();
    for (Object chooser : loadChoosers) {
      if (chooser instanceof FileDataSourceQueryChooser) {
        FileDataSourceQueryChooser fileChooser = (FileDataSourceQueryChooser) chooser;
        Class<?> dataSourceClass = fileChooser.getDataSourceClass();
        String description = fileChooser.getDescription();
        List<String> extensions = Arrays.asList(fileChooser.getExtensions());
        DataSourceFileLayerLoader fileLoader = new DataSourceFileLayerLoader(
                workbenchContext, dataSourceClass, description, extensions);
        if (description.equals("GML 2.0")) {
          fileLoader.addOption(
                  StandardReaderWriterFileDataSource.GML.INPUT_TEMPLATE_FILE_KEY,
                  "FileString", true);
        }
        // for Shapefiles we check if we should show the charset selection
        if (dataSourceClass == StandardReaderWriterFileDataSource.Shapefile.class) {
          Object showCharsetSelection = PersistentBlackboardPlugIn.get(
                  workbenchContext).get(
                  DatasetOptionsPanel.BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION);
          if (showCharsetSelection instanceof Boolean) {
            if ((Boolean) showCharsetSelection) {
              fileLoader.addOption(DataSource.CHARSET_KEY, "CharSetComboBoxField", Charset
                      .defaultCharset().displayName(), true);
            }
          }
        }
        registry.createEntry(FileLayerLoader.KEY, fileLoader);
      }
    }
    // the next two factories are deprecated. the same and better functionality is in GeoImageFactoryFileLayerLoader
    //addImageFactory(workbenchContext, registry, new IOGraphicImageFactory(), null);
    //addImageFactory(workbenchContext, registry, new JAIGraphicImageFactory(), null);
    addImageFactory(workbenchContext, registry, new CommonsImageFactory(), null);
    addImageFactory(workbenchContext, registry, new ECWImageFactory(), null);
    addImageFactory(workbenchContext, registry, new JPEG2000ImageFactory(), null);
    addImageFactory(workbenchContext, registry, new GeoTIFFImageFactory(), null);
    addImageFactory(workbenchContext, registry, new MrSIDImageFactory(), null);

    // register revamped geoimage
    // Nicolas Ribot: 04 dec: protection against java.lang.NoClassDefFoundError:
    // it/geosolutions/imageio/gdalframework/GDALImageReaderSpi on Mac OSX
    // though jar is in classpath and class exists in the Jar.
    // (missing native Mac OSX gdal files ?)
    try {
      GeoImageFactoryFileLayerLoader.register(workbenchContext);
    } catch (Throwable th) {
      th.printStackTrace();
    }

    DataSourceQueryChooserManager manager = DataSourceQueryChooserManager.get(workbenchContext.getWorkbench()
            .getBlackboard());
    for (DataSourceQueryChooser chooser : manager.getLoadDataSourceQueryChoosers()) {
      if (!(chooser instanceof FileDataSourceQueryChooser)) {
        WizardGroup wizard = new DataSourceQueryChooserOpenWizard(
                workbenchContext, chooser);
        OpenWizardPlugIn.addWizard(workbenchContext, wizard);
      }
    }

  }

  private void addImageFactory(
          WorkbenchContext workbenchContext, Registry registry,
          ReferencedImageFactory factory, String[] supportFileExtensions) {
    if (factory.isAvailable(workbenchContext)) {
      ReferencedImageFactoryFileLayerLoader loader = new ReferencedImageFactoryFileLayerLoader(
              workbenchContext, factory, supportFileExtensions);
      // add registry entry as FileLayerLoader
      registry.createEntry(FileLayerLoader.KEY, loader);
      // register as imagefactory (Imagelayermanager)
      registry.createEntry( ReferencedImageFactory.REGISTRY_CLASSIFICATION, factory );
    }
  }
}
