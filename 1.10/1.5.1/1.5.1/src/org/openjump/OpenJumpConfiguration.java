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

import com.vividsolutions.jump.I18N;
import static com.vividsolutions.jump.workbench.ui.MenuNames.LAYER;

import java.util.Arrays;
import java.util.List;

import javax.swing.JPopupMenu;

import org.openjump.core.ccordsys.srid.EnsureAllLayersHaveSRIDStylePlugIn;
import org.openjump.core.rasterimage.AddRasterImageLayerWizard;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterImageLayerRendererFactory;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.ReferencedImageFactoryFileLayerLoader;
import org.openjump.core.ui.plugin.datastore.AddDataStoreLayerWizard;
import org.openjump.core.ui.plugin.datastore.RefreshDataStoreQueryPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.ConstrainedMoveVertexPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawCircleWithGivenRadiusPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedArcPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedCirclePlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedLineStringPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedPolygonPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.RotateSelectedItemPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.SelectOneItemPlugIn;
import org.openjump.core.ui.plugin.file.DataSourceQueryChooserOpenWizard;
import org.openjump.core.ui.plugin.file.FileDragDropPlugin;
import org.openjump.core.ui.plugin.file.OpenFilePlugIn;
import org.openjump.core.ui.plugin.file.OpenProjectPlugIn;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.plugin.file.OpenWizardPlugIn;
import org.openjump.core.ui.plugin.layer.ChangeLayerableNamePlugIn;
import org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn;
import org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn;
import org.openjump.core.ui.plugin.layer.ToggleVisiblityPlugIn;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;
import org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneDown;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneUp;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToBottom;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToTop;
import org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn;
import org.openjump.core.ui.plugin.style.ImportArcMapStylePlugIn;
import org.openjump.core.ui.plugin.style.ImportSLDPlugIn;
import org.openjump.core.ui.plugin.view.EasyButtonsPlugin;
import org.openjump.core.ui.plugin.view.MapToolTipPlugIn;
import org.openjump.core.ui.plugin.view.ShowFullPathPlugIn;
import org.openjump.core.ui.plugin.view.ShowScalePlugIn;
import org.openjump.core.ui.plugin.view.ZoomToScalePlugIn;
import org.openjump.core.ui.plugin.window.MosaicInternalFramesPlugIn;
import org.openjump.core.ui.plugin.window.SynchronizationPlugIn;
import org.openjump.core.ui.plugin.wms.AddWmsLayerWizard;
import org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn;
import org.openjump.core.ui.style.decoration.ArrowLineStringMiddlepointStyle;
import org.openjump.core.ui.style.decoration.SegmentDownhillArrowStyle;
import org.openjump.core.ui.style.decoration.VertexZValueStyle;
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
import com.vividsolutions.jump.workbench.imagery.ecw.ECWImageFactory;
import com.vividsolutions.jump.workbench.imagery.ecw.JPEG2000ImageFactory;
import com.vividsolutions.jump.workbench.imagery.geotiff.GeoTIFFImageFactory;
import com.vividsolutions.jump.workbench.imagery.graphic.GraphicImageFactory;
import com.vividsolutions.jump.workbench.imagery.mrsid.MrSIDImageFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.AddImageLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;

import de.latlon.deejump.plugin.SaveLegendPlugIn;
import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;
import java.nio.charset.Charset;
import org.openjump.core.ui.DatasetOptionsPanel;
import org.openjump.core.ui.swing.factory.field.ComboBoxFieldComponentFactory;

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
    PersistentBlackboardPlugIn persistentBlackboard = new PersistentBlackboardPlugIn();
    persistentBlackboard.initialize(pluginContext);

    /***************************************************************************
     * Field Component Factories
     **************************************************************************/
    FieldComponentFactoryRegistry.setFactory(workbenchContext, "FileString",
      new FileFieldComponentFactory(workbenchContext));
	FieldComponentFactoryRegistry.setFactory(workbenchContext, "CharSetComboBoxField",
      new ComboBoxFieldComponentFactory(workbenchContext, null, Charset.availableCharsets().keySet().toArray()));


    /***************************************************************************
     * menu FILE
     **************************************************************************/
    //--[sstein 10.July.2008] I leave these plugins in this class, as they seem to me
    //	essential to be removable, similar for the others that are still initialized here
    
    OpenWizardPlugIn open = new OpenWizardPlugIn();
    open.initialize(pluginContext);

    OpenFilePlugIn openFile = new OpenFilePlugIn();
    openFile.initialize(pluginContext);
    
    RunDatastoreQueryPlugIn runDatastoreQueryPlugIn = new RunDatastoreQueryPlugIn();
    runDatastoreQueryPlugIn.initialize(pluginContext);

    AddImageLayerPlugIn addImageLayerPlugIn = new AddImageLayerPlugIn();
    addImageLayerPlugIn.initialize(pluginContext);

    OpenProjectPlugIn openProject = new OpenProjectPlugIn();
    openProject.initialize(pluginContext);

    OpenRecentPlugIn openRecent = OpenRecentPlugIn.get(workbenchContext);
    openRecent.initialize(pluginContext);

    FileDragDropPlugin fileDragDropPlugin = new FileDragDropPlugin();
    fileDragDropPlugin.initialize(pluginContext);

    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    SaveImageAsSVGPlugIn imageSvgPlugin = new SaveImageAsSVGPlugIn();
    imageSvgPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    
    /***************************************************************************
     * menu EDIT
     **************************************************************************/
    
    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    SelectItemsByFenceFromSelectedLayersPlugIn selectItemsFromLayersPlugIn = new SelectItemsByFenceFromSelectedLayersPlugIn();
    selectItemsFromLayersPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
    
    SelectItemsByCircleFromSelectedLayersPlugIn selectItemsFromCirclePlugIn = new SelectItemsByCircleFromSelectedLayersPlugIn();
    selectItemsFromCirclePlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
    
    SelectAllLayerItemsPlugIn selectAllLayerItemsPlugIn = new SelectAllLayerItemsPlugIn();
    selectAllLayerItemsPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));

    ReplicateSelectedItemsPlugIn replicatePlugIn = new ReplicateSelectedItemsPlugIn();
    replicatePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SelectByTypePlugIn mySelectByGeomTypePlugIn = new SelectByTypePlugIn();
    mySelectByGeomTypePlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
    
	ExtractLayersByGeometry myExtractLayersByGeometryPlugin = new ExtractLayersByGeometry();
	myExtractLayersByGeometryPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
	
	ExtractLayerInFence myExtractLayerInFence = new ExtractLayerInFence();
	myExtractLayerInFence.initialize(new PlugInContext(workbenchContext, null, null, null, null));
	*/
    
    /***************************************************************************
     * menu VIEW
     **************************************************************************/
    
    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    new CopyBBoxPlugin().initialize(new PlugInContext(workbenchContext, null, null, null, null));
    */
    EasyButtonsPlugin myEasyButtonsPlugIn = new EasyButtonsPlugin();
    myEasyButtonsPlugIn.initialize(new PlugInContext(workbenchContext, null,
    	      null, null, null));
    
    ZoomToWMSPlugIn myZoomToWMSPlugIn = new ZoomToWMSPlugIn();
    myZoomToWMSPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    ZoomToScalePlugIn myZoomToScalePlugIn = new ZoomToScalePlugIn();
    myZoomToScalePlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    ShowScalePlugIn myShowScalePlugIn = new ShowScalePlugIn();
    myShowScalePlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    MapToolTipPlugIn myMapTipPlugIn = new MapToolTipPlugIn();
    myMapTipPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));


    // -- deeJUMP function by LAT/LON [01.08.2006 sstein]
    //LayerStyle2SLDPlugIn mySytle2SLDplugIn = new LayerStyle2SLDPlugIn();
    //mySytle2SLDplugIn.initialize(new PlugInContext(workbenchContext, null,
    //  null, null, null));
    
    //new ImportSLDPlugIn().initialize(pluginContext);
    //new ImportArcMapStylePlugIn().initialize(pluginContext);
    
    pluginContext.getFeatureInstaller().addMenuSeparator(LAYER);

    // --this caused problems with the postgis plugin [sstein]
    // TODO: the problem has been solved (using try/catch) but still class has
    // to be
    // changed using LayerListener LayerEventType.ADDED event instead of
    // layerSelectionChanged() from LayerNamePanelListener
    ShowFullPathPlugIn myFullPathPlugin = new ShowFullPathPlugIn();
    myFullPathPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	
    /***************************************************************************
     * menu LAYER
     **************************************************************************/

    ToggleVisiblityPlugIn myToggleVisPlugIn = new ToggleVisiblityPlugIn();
    myToggleVisPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    //-- [sstein 21March2008] unnecessary with new menu structure
    //	 MRSIDtype is added with new open file dialog (see below)
    /*
    AddSIDLayerPlugIn myMrSIDPlugIn = new AddSIDLayerPlugIn();
    myMrSIDPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    
    ChangeSRIDPlugIn myChangeSRIDPlugIn = new ChangeSRIDPlugIn();
    myChangeSRIDPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    /***************************************************************************
     * menu TOOLS
     **************************************************************************/
    
    /** ** ANALYSIS *** */
    /*
    JoinAttributesSpatiallyPlugIn mySpatialJoin = new JoinAttributesSpatiallyPlugIn();
    mySpatialJoin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- SIGLE PlugIn
    PlanarGraphPlugIn coveragePlugIn = new PlanarGraphPlugIn();
    coveragePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    IntersectPolygonLayersPlugIn intersectLayers = new IntersectPolygonLayersPlugIn();
    intersectLayers.initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));
    
    UnionByAttributePlugIn unionByAttribute = new UnionByAttributePlugIn();
    unionByAttribute.initialize(new PlugInContext(workbenchContext,
  	      null, null, null, null)); 
    */
    /** ** GENERATE *** */
    /*
    ConvexHullPlugIn myConvHullPlugIn = new ConvexHullPlugIn();
    myConvHullPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    CreateThiessenPolygonsPlugIn myThiessenPlugin = new CreateThiessenPolygonsPlugIn();
    myThiessenPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    /** ** QUERY *** */
    /*
    SimpleQueryPlugIn mySimpleQueryPlugIn = new SimpleQueryPlugIn();
    mySimpleQueryPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));
	*/
    /** ** QA *** */
    /*
    DeleteEmptyGeometriesPlugIn myDelGeomPlugin = new DeleteEmptyGeometriesPlugIn();
    myDelGeomPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    /** ** EDIT_GEOMETRY *** */
    /*
    JoinWithArcPlugIn myJoinWithArcPlugIn = new JoinWithArcPlugIn();
    myJoinWithArcPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    BlendLineStringsPlugIn myLSBlender = new BlendLineStringsPlugIn();
    myLSBlender.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    MergeTwoSelectedPolygonsPlugIn twopolymerger = new MergeTwoSelectedPolygonsPlugIn();
    twopolymerger.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SplitPolygonPlugIn cutpoly = new SplitPolygonPlugIn();
    cutpoly.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));
	*/
    /** ** EDIT_ATTIBUTES **** */
    /*
    ReplaceValuePlugIn myRepVal = new ReplaceValuePlugIn();
    myRepVal.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));

    EditAttributeByFormulaPlugIn formulaEdit = new EditAttributeByFormulaPlugIn();
    formulaEdit.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    */
    /* sstein 31.March08
     * function replaced by JUMP function of similar name that works better
    SpatialJoinPlugIn spatialJoinPlugIn = new SpatialJoinPlugIn();
    spatialJoinPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));
     */

    /** ** GENERALIZATION *** */
    /*
    ReducePointsISAPlugIn mySimplifyISA = new ReducePointsISAPlugIn();
    mySimplifyISA.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    LineSimplifyJTS15AlgorithmPlugIn jtsSimplifier = new LineSimplifyJTS15AlgorithmPlugIn();
    jtsSimplifier.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    */
	/** ** OTHER TOOLS *** */
    /*
    MeasureM_FPlugIn myFeetPlugIn = new MeasureM_FPlugIn();
    myFeetPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    
    /***************************************************************************
     * menu CUSTOMIZE [added by Michael Michaud on 2007-03-04]
     **************************************************************************/
    // -- deeJUMP function by LAT/LON [05.08.2006 sstein]
    /*
    ExtensionManagerPlugIn extensionManagerPlugIn = new ExtensionManagerPlugIn();
    extensionManagerPlugIn.install(new PlugInContext(workbenchContext, null,
      null, null, null));
      
    BeanShellPlugIn beanShellPlugIn = new BeanShellPlugIn();
    beanShellPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- Larry's BeanToolsPlugIn
    BeanToolsPlugIn beanTools = new BeanToolsPlugIn();
    beanTools.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));
	*/
    /***************************************************************************
     * menu WINDOW
     **************************************************************************/

    MosaicInternalFramesPlugIn mosaicInternalFramesPlugIn = new MosaicInternalFramesPlugIn();
    mosaicInternalFramesPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SynchronizationPlugIn synchronizationPlugIn = new SynchronizationPlugIn("");
    synchronizationPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    pluginContext.getFeatureInstaller().addMenuSeparator(MenuNames.WINDOW);

    /***************************************************************************
     * menu HELP
     **************************************************************************/

    /***************************************************************************
     * Right click menus
     **************************************************************************/
    JPopupMenu popupMenu = LayerViewPanel.popupMenu();
    popupMenu.addSeparator();
    
    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    MoveAlongAnglePlugIn myMoveAlongAnglePlugin = new MoveAlongAnglePlugIn();
    myMoveAlongAnglePlugin.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    RotatePlugIn myRotatePlugin = new RotatePlugIn();
    myRotatePlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SelectLayersWithSelectedItemsPlugIn selectLayersWithSelectedItemsPlugIn = new SelectLayersWithSelectedItemsPlugIn();
    selectLayersWithSelectedItemsPlugIn.initialize(new PlugInContext(
      workbenchContext, null, null, null, null));
	*/
    //-- [sstein 10.July.2008] leave them, as they seem to be essential
    SaveDatasetsPlugIn mySaveDataSetPlugIn = new SaveDatasetsPlugIn();
    mySaveDataSetPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    LayerPropertiesPlugIn myLayerPropertiesPlugIn = new LayerPropertiesPlugIn();
    myLayerPropertiesPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));	
    
    //ChangeLayerableNamePlugIn changeLayerableNamePlugIn = new ChangeLayerableNamePlugIn();
    //changeLayerableNamePlugIn.initialize(new PlugInContext(workbenchContext,
    //  null, null, null, null));
    
    RefreshDataStoreQueryPlugIn refreshDataStoreQueryPlugIn = new RefreshDataStoreQueryPlugIn();
    refreshDataStoreQueryPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
    
    // -- deeJUMP function by LAT/LON [01.08.2006 sstein]
    new LayerStyle2SLDPlugIn().initialize(pluginContext);
    new ImportSLDPlugIn().initialize(pluginContext);
    new ImportArcMapStylePlugIn().initialize(pluginContext);
    
    //featureInstaller.addPopupMenuItem(layerNamePopupMenu, refreshDataStoreQueryPlugin,
    //        new String[]{MenuNames.DATASTORE}, refreshDataStoreQueryPlugin.getName() + "...", false, RefreshDataStoreQueryPlugin.ICON,
    //            RefreshDataStoreQueryPlugin.createEnableCheck(workbenchContext));
    
    //-- [sstein 22.Feb.2009]
    //-- adds renderer for (Pirol/Sextante) raster images
    RenderingManager.putRendererForLayerable(RasterImageLayer.class, new RasterImageLayerRendererFactory(pluginContext.getWorkbenchContext()));
    //-- adds the context menu for (Pirol/Sextante) Raster Images
    pluginContext.getWorkbenchFrame().getNodeClassToPopupMenuMap().put(RasterImageLayer.class, RasterImageContextMenu.getInstance(pluginContext));
    
    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    EditSelectedSidePlugIn myEditSidePlugin = new EditSelectedSidePlugIn();
    myEditSidePlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    // -- deeJUMP plugin
    SaveLegendPlugIn saveLegend = new SaveLegendPlugIn();
    saveLegend.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));

    // -- SIGLE plugin
    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    /*
    JoinTablePlugIn joinTablePlugIn = new JoinTablePlugIn();
    joinTablePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    
    PasteItemsAtPlugIn pasteItemsAtPlugIn = new PasteItemsAtPlugIn();
    pasteItemsAtPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
    */
    /**+++++++++++++++++++++++
     * Category Context menu
     *++++++++++++++++++++++++**/
    
    // -- Pirol plugins
    SetCategoryVisibilityPlugIn.getInstance(workbenchContext.createPlugInContext()).initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));
    new MoveCategoryToTop().initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));
    new MoveCategoryOneUp().initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));
    new MoveCategoryOneDown().initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));
    new MoveCategoryToBottom().initialize(new PlugInContext(workbenchContext,
    	      null, null, null, null));

    /***************************************************************************
     * EDITing toolbox
     **************************************************************************/
    
    //-- [sstein 10.July.2008] leave them, as they seem to be essential
    //   note: it is intended to replace the original JUMP edition tools with the constrained tools
    DrawConstrainedPolygonPlugIn myConstrainedPolygonPlugIn = new DrawConstrainedPolygonPlugIn();
    myConstrainedPolygonPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));

    DrawConstrainedLineStringPlugIn myConstrainedLSPlugIn = new DrawConstrainedLineStringPlugIn();
    myConstrainedLSPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    DrawConstrainedCirclePlugIn myConstrainedCPlugIn = new DrawConstrainedCirclePlugIn();
    myConstrainedCPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    DrawConstrainedArcPlugIn myConstrainedArcPlugIn = new DrawConstrainedArcPlugIn();
    myConstrainedArcPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    ConstrainedMoveVertexPlugIn myCMVPlugIn = new ConstrainedMoveVertexPlugIn();
    myCMVPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    RotateSelectedItemPlugIn myRotateSIPlugIn = new RotateSelectedItemPlugIn();
    myRotateSIPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SelectOneItemPlugIn mySelectOnePlugin = new SelectOneItemPlugIn();
    mySelectOnePlugin.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    DrawCircleWithGivenRadiusPlugIn drawCirclePlugin = new DrawCircleWithGivenRadiusPlugIn();
    drawCirclePlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    //   as these are advanced editing tools [i.e. more for experts?]
    /*
    CutPolygonSIGLEPlugIn cutPolyPlugin = new CutPolygonSIGLEPlugIn();
    cutPolyPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    
    AutoCompletePolygonPlugIn myAutoCompletePlugIn = new AutoCompletePolygonPlugIn();
    myAutoCompletePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	*/
    // -- now initialized in #EditingPlugIn.java to fill toolbox
    /*
     * ScaleSelectedItemsPlugIn myScaleItemsPlugin = new
     * ScaleSelectedItemsPlugIn(); myScaleItemsPlugin.initialize(new
     * PlugInContext(workbenchContext, null, null, null, null));
     */

    /***************************************************************************
     * others
     **************************************************************************/

    //-- [sstein 10.July.2008] now initialized with default-plugins.xml file
    // takes care of keyboard navigation
    /*
    new InstallKeyPanPlugIn().initialize(new PlugInContext(workbenchContext,
      null, null, null, null));
	*/
    // -- enables to store the SRID = EPSG code as style for every Layer
    // since it is stored as style it should be saved in the project file
    EnsureAllLayersHaveSRIDStylePlugIn ensureLayerSRIDPlugin = new EnsureAllLayersHaveSRIDStylePlugIn();
    ensureLayerSRIDPlugin.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

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
    workbenchContext.getBlackboard().put(
      EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, true);

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
    Registry registry = workbenchContext.getRegistry();
    List loadChoosers = DataSourceQueryChooserManager.get(
      workbenchContext.getBlackboard()).getLoadDataSourceQueryChoosers();
    for (Object chooser : loadChoosers) {
      if (chooser instanceof FileDataSourceQueryChooser) {
        FileDataSourceQueryChooser fileChooser = (FileDataSourceQueryChooser)chooser;
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
			Object showCharsetSelection = PersistentBlackboardPlugIn.get(workbenchContext).get(DatasetOptionsPanel.BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION);
			if (showCharsetSelection instanceof Boolean) {
				if (((Boolean) showCharsetSelection).booleanValue()) {
					fileLoader.addOption("charset", "CharSetComboBoxField", Charset.defaultCharset().displayName(), true);
				}
			}
		}
        registry.createEntry(FileLayerLoader.KEY, fileLoader);
      }
    }
    // supersedes com.vividsolutions.jump.workbench.ui.plugin.imagery.InstallReferencedImageFactoriesPlugin
    // register layerloader with worldfile support and plain factories for imagelayermanager
    addImageFactory(workbenchContext, registry, new GraphicImageFactory(),
      new String[] {
        "wld", "bpw", "jpw", "gfw"
      });
    addImageFactory(workbenchContext, registry, new ECWImageFactory(), null);
    addImageFactory(workbenchContext, registry, new JPEG2000ImageFactory(), null);
    addImageFactory(workbenchContext, registry, new GeoTIFFImageFactory(),
      new String[] {
        "tfw"
      });
    addImageFactory(workbenchContext, registry, new MrSIDImageFactory(), null);

    //
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
