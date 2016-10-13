/*
 * Created on Aug 11, 2005
 * 
 * description:
 *   This class loads all basic OpenJUMP plugins. Other
 *   additional - i.e. nice to have - plugins are initialized
 *   using the file "default-plugins.xml". 
 *   The contained method loadOpenJumpPlugIns() is called from 
 *   com.vividsolutions.jump.workbench.JUMPConfiguaration. 
 *   
 *   Note, the menu order of functionality may change if changes 
 *   are made in here. The plugins in this file are initialized 
 *   before the plugins from default-plugins.xml. 
 *   
 */
package org.openjump;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

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
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;
import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.core.ui.style.decoration.ArrowLineStringMiddlepointStyle;
import org.openjump.core.ui.style.decoration.SegmentDownhillArrowStyle;
import org.openjump.core.ui.style.decoration.VertexZValueStyle;
import org.openjump.core.ui.swing.factory.field.ComboBoxFieldComponentFactory;
import org.openjump.core.ui.swing.factory.field.FieldComponentFactoryRegistry;
import org.openjump.core.ui.swing.factory.field.FileFieldComponentFactory;
import org.openjump.core.ui.swing.wizard.WizardGroup;

import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
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
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;

/**
 * This class loads all OpenJUMP plugins. The method
 *               loadOpenJumpPlugIns() is called from
 *               com.vividsolutions.jump.workbench.JUMPConfiguaration.
 * @author sstein
 */
public class OpenJumpConfiguration {

  public static void loadOpenJumpPlugIns(final WorkbenchContext workbenchContext)
    throws Exception {
    PlugInContext pluginContext = workbenchContext.createPlugInContext();

    /*-----------------------------------------------
     *  add here first the field which holds the plugin
     *  and afterwards initialize it for the menu
     *-----------------------------------------------*/

    /***************************************************************************
     * Field Component Factories
     **************************************************************************/
    FieldComponentFactoryRegistry.setFactory(workbenchContext, "FileString",
      new FileFieldComponentFactory(workbenchContext));
    FieldComponentFactoryRegistry.setFactory(workbenchContext, "CharSetComboBoxField",
      new ComboBoxFieldComponentFactory(workbenchContext, null, Charset.availableCharsets().keySet().toArray()));

    /***************************************************************************
     * OperationFactories
     **************************************************************************/
    BeanshellAttributeOperationFactory baof = new BeanshellAttributeOperationFactory(pluginContext);

    /***************************************************************************
     * Decoration
     **************************************************************************/

    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame workbenchFrame = workbench.getFrame();
    workbenchFrame.addChoosableStyleClass(ArrowLineStringMiddlepointStyle.NarrowSolidMiddle.class);

    workbenchFrame.addChoosableStyleClass(SegmentDownhillArrowStyle.NarrowSolidMiddle.class);
    workbenchFrame.addChoosableStyleClass(SegmentDownhillArrowStyle.Open.class);
    workbenchFrame.addChoosableStyleClass(SegmentDownhillArrowStyle.Solid.class);

    workbenchFrame.addChoosableStyleClass(VertexZValueStyle.VertexZValue.class);

    /***************************************************************************
     * Set Defaults
     **************************************************************************/
    // -- disable drawing of invalid polygons by default (can be changed during
    // work in EditOptionsPanel)
    PersistentBlackboardPlugIn.get(workbenchContext).put(
      EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, true);

    
    /***************************************************************************
     * testing
     **************************************************************************/
    /*
     * ProjectionPlugIn projectionPlugin = new ProjectionPlugIn();
     * projectionPlugin.initialize(new PlugInContext(workbenchContext, null,
     * null, null, null));
     */
  }

  public static void postExtensionInitialization(
    WorkbenchContext workbenchContext) {
    
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
      AddWritableDataStoreLayerWizard addWritableDataStoreLayerWizard = new AddWritableDataStoreLayerWizard(
              workbenchContext);
      OpenWizardPlugIn.addWizard(workbenchContext, addWritableDataStoreLayerWizard);
    
    // [mmichaud 2012-09-01] changed how RasterImageLayerRendererFactory is initialized to fix bug 3526653
    RenderingManager.setRendererFactory(RasterImageLayer.class, new RasterImageLayerRendererFactory()); 
    
    //-- adds the context menu for (Pirol/Sextante) Raster Images
    workbenchContext.getWorkbench().getFrame().getNodeClassToPopupMenuMap().put(RasterImageLayer.class, RasterImageContextMenu.getInstance(workbenchContext.createPlugInContext()));
    
    Registry registry = workbenchContext.getRegistry();
    List loadChoosers = DataSourceQueryChooserManager.get(
        workbenchContext.getBlackboard()).getLoadDataSourceQueryChoosers();
    for (Object chooser : loadChoosers) {
      if (chooser instanceof FileDataSourceQueryChooser) {
        FileDataSourceQueryChooser fileChooser = (FileDataSourceQueryChooser) chooser;
        Class dataSourceClass = fileChooser.getDataSourceClass();
        String description = fileChooser.getDescription();
        List<String> extensions = Arrays.asList(fileChooser.getExtensions());
        DataSourceFileLayerLoader fileLoader = new DataSourceFileLayerLoader(
            workbenchContext, dataSourceClass, description, extensions);
        if (description == "GML 2.0") {
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
            if (((Boolean) showCharsetSelection).booleanValue()) {
              fileLoader.addOption("charset", "CharSetComboBoxField", Charset
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
    for (DataSourceQueryChooser chooser : (List<DataSourceQueryChooser>)manager.getLoadDataSourceQueryChoosers()) {
      if (!(chooser instanceof FileDataSourceQueryChooser)) {
        WizardGroup wizard = new DataSourceQueryChooserOpenWizard(
          workbenchContext, chooser);
        OpenWizardPlugIn.addWizard(workbenchContext, wizard);
      }
    }

  }

  private static void addImageFactory(WorkbenchContext workbenchContext,
    Registry registry, ReferencedImageFactory factory,
    String[] supportFileExtensions) {
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
