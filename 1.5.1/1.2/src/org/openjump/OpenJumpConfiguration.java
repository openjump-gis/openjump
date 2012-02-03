/*
 * Created on Aug 11, 2005
 * 
 * description:
 *   This class loads all openjump plugins.
 *   The method loadOpenJumpPlugIns() is called from 
 *   com.vividsolutions.jump.workbench.JUMPConfiguaration. 
 *
 *
 */
package org.openjump;

import static com.vividsolutions.jump.workbench.ui.MenuNames.LAYER;

import java.util.Arrays;
import java.util.List;

import javax.swing.JPopupMenu;

import org.openjump.core.ccordsys.srid.EnsureAllLayersHaveSRIDStylePlugIn;
import org.openjump.core.ui.io.file.DataSourceFileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.ReferencedImageFactoryFileLayerLoader;
import org.openjump.core.ui.plugin.customize.BeanToolsPlugIn;
import org.openjump.core.ui.plugin.datastore.AddDataStoreLayerWizard;
import org.openjump.core.ui.plugin.edit.CopyBBoxPlugin;
import org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn;
import org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn;
import org.openjump.core.ui.plugin.edit.SelectByTypePlugIn;
import org.openjump.core.ui.plugin.edit.SelectItemsByCircleFromSelectedLayersPlugIn;
import org.openjump.core.ui.plugin.edit.SelectItemsByFenceFromSelectedLayersPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.ConstrainedMoveVertexPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.CutPolygonSIGLEPlugIn;
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
import org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn;
import org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn;
import org.openjump.core.ui.plugin.layer.ChangeLayerableNamePlugIn;
import org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn;
import org.openjump.core.ui.plugin.layer.ExtractLayerInFence;
import org.openjump.core.ui.plugin.layer.ExtractLayersByGeometry;
import org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn;
import org.openjump.core.ui.plugin.layer.ToggleVisiblityPlugIn;
import org.openjump.core.ui.plugin.mousemenu.EditSelectedSidePlugIn;
import org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn;
import org.openjump.core.ui.plugin.mousemenu.PasteItemsAtPlugIn;
import org.openjump.core.ui.plugin.mousemenu.RotatePlugIn;
import org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn;
import org.openjump.core.ui.plugin.mousemenu.SelectLayersWithSelectedItemsPlugIn;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneDown;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneUp;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToBottom;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToTop;
import org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn;
import org.openjump.core.ui.plugin.queries.SimpleQueryPlugIn;
import org.openjump.core.ui.plugin.style.ImportSLDPlugIn;
import org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn;
import org.openjump.core.ui.plugin.tools.ConvexHullPlugIn;
import org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn;
import org.openjump.core.ui.plugin.tools.DeleteEmptyGeometriesPlugIn;
import org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn;
import org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn;
import org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn;
import org.openjump.core.ui.plugin.tools.MeasureM_FPlugIn;
import org.openjump.core.ui.plugin.tools.MergeTwoSelectedPolygonsPlugIn;
import org.openjump.core.ui.plugin.tools.ReducePointsISAPlugIn;
import org.openjump.core.ui.plugin.tools.SplitPolygonPlugIn;
import org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn;
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
//import org.openjump.sigle.plugin.geoprocessing.layers.SpatialJoinPlugIn;
import org.openjump.sigle.plugin.geoprocessing.oneLayer.topology.PlanarGraphPlugIn;
import org.openjump.sigle.plugin.joinTable.JoinTablePlugIn;
import org.openjump.sigle.plugin.replace.ReplaceValuePlugIn;

import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.imagery.ecw.ECWImageFactory;
import com.vividsolutions.jump.workbench.imagery.geotiff.GeoTIFFImageFactory;
import com.vividsolutions.jump.workbench.imagery.graphic.GraphicImageFactory;
import com.vividsolutions.jump.workbench.imagery.mrsid.MrSIDImageFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.BeanShellPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import de.fho.jump.pirol.plugins.EditAttributeByFormula.EditAttributeByFormulaPlugIn;
import de.latlon.deejump.plugin.SaveLegendPlugIn;
import de.latlon.deejump.plugin.manager.ExtensionManagerPlugIn;
import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;

/**
 * @description: This class loads all openjump plugins. The method
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

    /***************************************************************************
     * menu FILE
     **************************************************************************/
    OpenWizardPlugIn open = new OpenWizardPlugIn();
    open.initialize(pluginContext);

    OpenFilePlugIn openFile = new OpenFilePlugIn();
    openFile.initialize(pluginContext);

    OpenProjectPlugIn openProject = new OpenProjectPlugIn();
    openProject.initialize(pluginContext);

    OpenRecentPlugIn openRecent = OpenRecentPlugIn.get(workbenchContext);
    openRecent.initialize(pluginContext);

    FileDragDropPlugin fileDragDropPlugin = new FileDragDropPlugin();
    fileDragDropPlugin.initialize(pluginContext);

    SaveImageAsSVGPlugIn imageSvgPlugin = new SaveImageAsSVGPlugIn();
    imageSvgPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /***************************************************************************
     * menu EDIT
     **************************************************************************/
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


    new CopyBBoxPlugin().initialize(new PlugInContext(workbenchContext, null, null, null, null));

    /***************************************************************************
     * menu VIEW
     **************************************************************************/

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

    pluginContext.getFeatureInstaller().addMenuSeparator(LAYER);

    // -- deeJUMP function by LAT/LON [01.08.2006 sstein]
    LayerStyle2SLDPlugIn mySytle2SLDplugIn = new LayerStyle2SLDPlugIn();
    mySytle2SLDplugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    new ImportSLDPlugIn().initialize(pluginContext);
    
    pluginContext.getFeatureInstaller().addMenuSeparator(LAYER);

    // -- to install in Toolbar
    // mySytle2SLDplugIn.install(new PlugInContext(workbenchContext, null, null,
    // null, null));

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
    
    AddSIDLayerPlugIn myMrSIDPlugIn = new AddSIDLayerPlugIn();
    myMrSIDPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
	
    
    ChangeSRIDPlugIn myChangeSRIDPlugIn = new ChangeSRIDPlugIn();
    myChangeSRIDPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    /***************************************************************************
     * menu TOOLS
     **************************************************************************/

    /** ** ANALYSIS *** */
    JoinAttributesSpatiallyPlugIn mySpatialJoin = new JoinAttributesSpatiallyPlugIn();
    mySpatialJoin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- SIGLE PlugIn
    PlanarGraphPlugIn coveragePlugIn = new PlanarGraphPlugIn();
    coveragePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /** ** GENERATE *** */
    ConvexHullPlugIn myConvHullPlugIn = new ConvexHullPlugIn();
    myConvHullPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    CreateThiessenPolygonsPlugIn myThiessenPlugin = new CreateThiessenPolygonsPlugIn();
    myThiessenPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /** ** QUERY *** */
    SimpleQueryPlugIn mySimpleQueryPlugIn = new SimpleQueryPlugIn();
    mySimpleQueryPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    /** ** QA *** */
    DeleteEmptyGeometriesPlugIn myDelGeomPlugin = new DeleteEmptyGeometriesPlugIn();
    myDelGeomPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /** ** EDIT_GEOMETRY *** */
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

    /** ** EDIT_ATTIBUTES **** */
    ReplaceValuePlugIn myRepVal = new ReplaceValuePlugIn();
    myRepVal.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));

    EditAttributeByFormulaPlugIn formulaEdit = new EditAttributeByFormulaPlugIn();
    formulaEdit.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    
    /* sstein 31.March08
     * function replaced by JUMP function of similar name that works better
    SpatialJoinPlugIn spatialJoinPlugIn = new SpatialJoinPlugIn();
    spatialJoinPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));
     */

    /** ** GENERALIZATION *** */
    ReducePointsISAPlugIn mySimplifyISA = new ReducePointsISAPlugIn();
    mySimplifyISA.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    LineSimplifyJTS15AlgorithmPlugIn jtsSimplifier = new LineSimplifyJTS15AlgorithmPlugIn();
    jtsSimplifier.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /** ** tools main *** */

    // -- [sstein] do this to avoid that the programming menu is created after
    // MeasureM_FPlugIn is added to the tools menu
    // [Michael Michaud 2007-03-23] : put programming plugins in
    // MenuNames.CUSTOMIZE menu
    /*
     * PlugInContext pc = new PlugInContext(workbenchContext, null, null, null,
     * null); FeatureInstaller fi = pc.getFeatureInstaller(); JMenu menuTools =
     * fi.menuBarMenu(MenuNames.TOOLS); fi.createMenusIfNecessary(menuTools, new
     * String[]{MenuNames.TOOLS_PROGRAMMING});
     */

    // -- deeJUMP function by LAT/LON [05.08.2006 sstein]
    // [Michael Michaud 2007-03-23] move the plugin to the CUSTOMIZE menu (see
    // here after)
    /*
     * ExtensionManagerPlugIn extensionManagerPlugIn = new
     * ExtensionManagerPlugIn(); extensionManagerPlugIn.install(new
     * PlugInContext(workbenchContext, null, null, null, null));
     */

    MeasureM_FPlugIn myFeetPlugIn = new MeasureM_FPlugIn();
    myFeetPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    /***************************************************************************
     * menu CUSTOMIZE [added by Michael Michaud on 2007-03-04]
     **************************************************************************/
    // -- deeJUMP function by LAT/LON [05.08.2006 sstein]
    ExtensionManagerPlugIn extensionManagerPlugIn = new ExtensionManagerPlugIn();
    extensionManagerPlugIn.install(new PlugInContext(workbenchContext, null,
      null, null, null));

    // -- [michael michaud] move from JUMPConfiguration
    BeanShellPlugIn beanShellPlugIn = new BeanShellPlugIn();
    beanShellPlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- [michael michaud] add Larry's BeanToolsPlugIn
    BeanToolsPlugIn beanTools = new BeanToolsPlugIn();
    beanTools.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));

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

    MoveAlongAnglePlugIn myMoveAlongAnglePlugin = new MoveAlongAnglePlugIn();
    myMoveAlongAnglePlugin.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    RotatePlugIn myRotatePlugin = new RotatePlugIn();
    myRotatePlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    SelectLayersWithSelectedItemsPlugIn selectLayersWithSelectedItemsPlugIn = new SelectLayersWithSelectedItemsPlugIn();
    selectLayersWithSelectedItemsPlugIn.initialize(new PlugInContext(
      workbenchContext, null, null, null, null));

    SaveDatasetsPlugIn mySaveDataSetPlugIn = new SaveDatasetsPlugIn();
    mySaveDataSetPlugIn.initialize(new PlugInContext(workbenchContext, null,
      null, null, null));

    LayerPropertiesPlugIn myLayerPropertiesPlugIn = new LayerPropertiesPlugIn();
    myLayerPropertiesPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));	
    
    ChangeLayerableNamePlugIn changeLayerableNamePlugIn = new ChangeLayerableNamePlugIn();
    changeLayerableNamePlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));

    EditSelectedSidePlugIn myEditSidePlugin = new EditSelectedSidePlugIn();
    myEditSidePlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- deeJUMP plugin
    SaveLegendPlugIn saveLegend = new SaveLegendPlugIn();
    saveLegend.initialize(new PlugInContext(workbenchContext, null, null, null,
      null));

    // -- SIGLE plugin
    JoinTablePlugIn joinTablePlugIn = new JoinTablePlugIn();
    joinTablePlugIn.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));
    
    PasteItemsAtPlugIn pasteItemsAtPlugIn = new PasteItemsAtPlugIn();
    pasteItemsAtPlugIn.initialize(new PlugInContext(workbenchContext,
      null, null, null, null));

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
    
    CutPolygonSIGLEPlugIn cutPolyPlugin = new CutPolygonSIGLEPlugIn();
    cutPolyPlugin.initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    // -- now initialized in #EditingPlugIn.java to fill toolbox
    /*
     * ScaleSelectedItemsPlugIn myScaleItemsPlugin = new
     * ScaleSelectedItemsPlugIn(); myScaleItemsPlugin.initialize(new
     * PlugInContext(workbenchContext, null, null, null, null));
     */

    /***************************************************************************
     * others
     **************************************************************************/

    // takes care of keyboard navigation
    new InstallKeyPanPlugIn().initialize(new PlugInContext(workbenchContext,
      null, null, null, null));

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
        registry.createEntry(FileLayerLoader.KEY, fileLoader);
      }
    }
    addFactory(workbenchContext, registry, new GraphicImageFactory(),
      new String[] {
        "wld", "bpw", "jpw", "gfw"
      });
    addFactory(workbenchContext, registry, new ECWImageFactory(), null);
    addFactory(workbenchContext, registry, new GeoTIFFImageFactory(),
      new String[] {
        "tfw"
      });
    addFactory(workbenchContext, registry, new MrSIDImageFactory(), null);

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

  private static void addFactory(WorkbenchContext workbenchContext,
    Registry registry, ReferencedImageFactory factory,
    String[] supportFileExtensions) {
    if (factory.isAvailable()) {
      ReferencedImageFactoryFileLayerLoader loader = new ReferencedImageFactoryFileLayerLoader(
        workbenchContext, factory, supportFileExtensions);
      registry.createEntry(FileLayerLoader.KEY, loader);
    }
  }
}
