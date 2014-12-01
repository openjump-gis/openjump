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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.openjump.OpenJumpConfiguration;
import org.openjump.core.ui.plugin.edit.CopyBBoxPlugin;
import org.openjump.core.ui.plugin.edit.InvertSelectionPlugIn;
import org.openjump.core.ui.plugin.layer.CombineSelectedLayersPlugIn;
import org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn;
import org.openjump.core.ui.plugin.mousemenu.DuplicateItemPlugIn;
import org.openjump.core.ui.plugin.mousemenu.MergeSelectedFeaturesPlugIn;
import org.openjump.core.ui.plugin.mousemenu.PasteItemsAtPlugIn;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneDown;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryOneUp;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToBottom;
import org.openjump.core.ui.plugin.mousemenu.category.MoveCategoryToTop;
import org.openjump.core.ui.plugin.mousemenu.category.SetCategoryVisibilityPlugIn;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureTool;
import org.openjump.core.ui.plugin.tools.ZoomRealtimeTool;
import org.openjump.core.ui.plugin.view.ShowScalePlugIn;
import org.openjump.core.ui.plugin.view.SuperZoomPanTool;
import org.openjump.core.ui.plugin.view.helpclassescale.InstallShowScalePlugIn;
import org.openjump.core.ui.plugin.view.helpclassescale.ShowScaleRenderer;
import org.openjump.core.ui.plugin.window.ArrangeViewsPlugIn;
import org.openjump.core.ui.plugin.window.MosaicInternalFramesPlugIn;
import org.openjump.core.ui.plugin.window.SynchronizationPlugIn;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver;
import com.vividsolutions.jump.workbench.datasource.AbstractSaveDatasetAsPlugIn;
import com.vividsolutions.jump.workbench.datasource.InstallStandardDataSourceQueryChoosersPlugIn;
import com.vividsolutions.jump.workbench.datasource.LoadDatasetPlugIn;
import com.vividsolutions.jump.workbench.datasource.SaveDatasetAsPlugIn;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ApplicationExitHandler;
import com.vividsolutions.jump.workbench.ui.AttributeTab;
import com.vividsolutions.jump.workbench.ui.CloneableInternalFrame;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
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
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.AboutPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewCategoryPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewFeaturesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.AddWMSDemoBoxEasterEggPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ClearSelectionPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.CloneWindowPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.CombineSelectedFeaturesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.CopySchemaPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.DeleteAllFeaturesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.DeleteSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.EditSelectedFeaturePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.EditablePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ExplodeSelectedFeaturesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInfoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.FirstTaskFramePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.GenerateLogPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.InstallStandardFeatureTextWritersPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.MapToolTipsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.MoveLayerablePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.NewTaskPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.OutputWindowPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PasteSchemaPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.RedoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.RemoveSelectedCategoriesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.RemoveSelectedLayersPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SaveImageAsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectAsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SelectablePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ShortcutKeysPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.UndoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.VerticesInFencePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ViewAttributesPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ViewSchemaPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopyImagePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopySelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopySelectedLayersPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopyThisCoordinatePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CutSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CutSelectedLayersPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteLayersPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.AddDatastoreLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.InstallDatastoreLayerRendererHintsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.RefreshDataStoreLayerPlugin;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.ImageLayerManagerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.InstallScaleBarPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.ScaleBarPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.ScaleBarRenderer;
import com.vividsolutions.jump.workbench.ui.plugin.skin.InstallSkinsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.wms.AddWMSQueryPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.wms.EditWMSQueryPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.WmsLayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.CircleLineStringEndpointStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.MetricsLineStringSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexIndexLineSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexXYLineSegmentStyle;
import com.vividsolutions.jump.workbench.ui.snap.InstallGridPlugIn;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;
import com.vividsolutions.jump.workbench.ui.style.CopyStylesPlugIn;
import com.vividsolutions.jump.workbench.ui.style.PasteStylesPlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import com.vividsolutions.jump.workbench.ui.zoom.InstallZoomBarPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomBarPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomNextPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomPreviousPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToClickPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToCoordinatePlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFencePlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFullExtentPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;

import de.latlon.deejump.plugin.style.DeeChangeStylesPlugIn;

/**
 * Initializes the Workbench with various menus and cursor tools. Accesses the
 * Workbench structure through a WorkbenchContext.
 */
public class JUMPConfiguration implements Setup {

  /**
   * Built-in plugins must be defined as instance variables, since they are
   * located for iniatialization via reflection on this class
   */

  private InstallShowScalePlugIn installShowScalePlugIn = new InstallShowScalePlugIn();

  private InstallScaleBarPlugIn installScaleBarPlugIn = new InstallScaleBarPlugIn();

  private InstallGridPlugIn installGridPlugIn = new InstallGridPlugIn();

  // FirstTaskFramePlugIn will be initialized using reflection in
  // #initializePlugIns [Jon Aquino]
  private FirstTaskFramePlugIn firstTaskFramePlugIn = new FirstTaskFramePlugIn();

  private InstallZoomBarPlugIn installZoomBarPlugIn = new InstallZoomBarPlugIn();

  private MoveLayerablePlugIn moveUpPlugIn = MoveLayerablePlugIn.UP;

  private InstallStandardDataSourceQueryChoosersPlugIn installStandardDataSourceQueryChoosersPlugIn = new InstallStandardDataSourceQueryChoosersPlugIn();

  private InstallStandardFeatureTextWritersPlugIn installStandardFeatureTextWritersPlugIn = new InstallStandardFeatureTextWritersPlugIn();

  private ShortcutKeysPlugIn shortcutKeysPlugIn = new ShortcutKeysPlugIn();

  private ClearSelectionPlugIn clearSelectionPlugIn = new ClearSelectionPlugIn();

  private EditWMSQueryPlugIn editWMSQueryPlugIn = new EditWMSQueryPlugIn();

  private MoveLayerablePlugIn moveDownPlugIn = MoveLayerablePlugIn.DOWN;

  private AddWMSQueryPlugIn addWMSQueryPlugIn = new AddWMSQueryPlugIn();

  private AddNewFeaturesPlugIn addNewFeaturesPlugIn = new AddNewFeaturesPlugIn();

  private OptionsPlugIn optionsPlugIn = new OptionsPlugIn();

  private AddNewCategoryPlugIn addNewCategoryPlugIn = new AddNewCategoryPlugIn();

  private CloneWindowPlugIn cloneWindowPlugIn = new CloneWindowPlugIn();

  private CopySelectedItemsPlugIn copySelectedItemsPlugIn = new CopySelectedItemsPlugIn();
  
  private PasteItemsAtPlugIn pasteItemsAtPlugIn = new PasteItemsAtPlugIn();

  private CopyThisCoordinatePlugIn copyThisCoordinatePlugIn = new CopyThisCoordinatePlugIn();

  private CopyImagePlugIn copyImagePlugIn = new CopyImagePlugIn();

  private MapToolTipsPlugIn toolTipsPlugIn = new MapToolTipsPlugIn();

  private CopySelectedLayersPlugIn copySelectedLayersPlugIn = new CopySelectedLayersPlugIn();

  private AddNewLayerPlugIn addNewLayerPlugIn = new AddNewLayerPlugIn();

  private AddWMSDemoBoxEasterEggPlugIn addWMSDemoBoxEasterEggPlugIn = new AddWMSDemoBoxEasterEggPlugIn();

  private EditSelectedFeaturePlugIn editSelectedFeaturePlugIn = new EditSelectedFeaturePlugIn();

  private EditingPlugIn editingPlugIn = EditingPlugIn.getInstance();

  private EditablePlugIn editablePlugIn = new EditablePlugIn(editingPlugIn);

  private SelectablePlugIn selectablePlugIn = new SelectablePlugIn();

  // [Michael Michaud 2007-03-23] Moved BeanShellPlugIn initialization in
  // OpenJUMPConfiguration
  // private BeanShellPlugIn beanShellPlugIn = new BeanShellPlugIn();

  private LoadDatasetPlugIn loadDatasetPlugIn = new LoadDatasetPlugIn();
  private SaveDatasetAsPlugIn saveDatasetAsPlugIn = new SaveDatasetAsPlugIn();
  // [sstein 18.Oct.2011] Not needed anymore after fix for MacOSX bug 3428076
  // private SaveDatasetAsFilePlugIn saveDatasetAsFilePlugIn = new
  // SaveDatasetAsFilePlugIn();
  private SaveImageAsPlugIn saveImageAsPlugIn = new SaveImageAsPlugIn();

  private GenerateLogPlugIn generateLogPlugIn = new GenerateLogPlugIn();

  private NewTaskPlugIn newTaskPlugIn = new NewTaskPlugIn();

  // Moved to OpenJUMPConfigurationPlugIn
  // private OpenProjectPlugIn openProjectPlugIn = new OpenProjectPlugIn();

  private PasteItemsPlugIn pasteItemsPlugIn = new PasteItemsPlugIn();

  private PasteLayersPlugIn pasteLayersPlugIn = new PasteLayersPlugIn();

  // Not necessary as we have "Delete Selected Items"
  private DeleteAllFeaturesPlugIn deleteAllFeaturesPlugIn = new DeleteAllFeaturesPlugIn();

  private DeleteSelectedItemsPlugIn deleteSelectedItemsPlugIn = new DeleteSelectedItemsPlugIn();

  private RemoveSelectedLayersPlugIn removeSelectedLayersPlugIn = new RemoveSelectedLayersPlugIn();

  private RemoveSelectedCategoriesPlugIn removeSelectedCategoriesPlugIn = new RemoveSelectedCategoriesPlugIn();

  private SaveProjectAsPlugIn saveProjectAsPlugIn = new SaveProjectAsPlugIn();

  private SaveProjectPlugIn saveProjectPlugIn = new SaveProjectPlugIn(
      saveProjectAsPlugIn);

  private ShowScalePlugIn showScalePlugIn = new ShowScalePlugIn();

  private ScaleBarPlugIn scaleBarPlugIn = new ScaleBarPlugIn();

  private ZoomBarPlugIn zoomBarPlugIn = new ZoomBarPlugIn();

  private DeeChangeStylesPlugIn changeStylesPlugIn = new DeeChangeStylesPlugIn();

  private UndoPlugIn undoPlugIn = new UndoPlugIn();

  private RedoPlugIn redoPlugIn = new RedoPlugIn();

  private ViewAttributesPlugIn viewAttributesPlugIn = new ViewAttributesPlugIn();

  private ViewSchemaPlugIn viewSchemaPlugIn = new ViewSchemaPlugIn(
      editingPlugIn);

  private CopySchemaPlugIn copySchemaPlugIn = new CopySchemaPlugIn();

  private PasteSchemaPlugIn pasteSchemaPlugIn = new PasteSchemaPlugIn();

  private FeatureInfoPlugIn featureInfoPlugIn = new FeatureInfoPlugIn();

  private OutputWindowPlugIn outputWindowPlugIn = new OutputWindowPlugIn();

  private VerticesInFencePlugIn verticesInFencePlugIn = new VerticesInFencePlugIn();

  private ZoomNextPlugIn zoomNextPlugIn = new ZoomNextPlugIn();

  private ZoomToClickPlugIn zoomToClickPlugIn = new ZoomToClickPlugIn(0.5);

  private ZoomPreviousPlugIn zoomPreviousPlugIn = new ZoomPreviousPlugIn();

  private ZoomToFencePlugIn zoomToFencePlugIn = new ZoomToFencePlugIn();

  private ZoomToCoordinatePlugIn zoomToCoordinatePlugIn = new ZoomToCoordinatePlugIn();

  private ZoomToFullExtentPlugIn zoomToFullExtentPlugIn = new ZoomToFullExtentPlugIn();

  private ZoomToLayerPlugIn zoomToLayerPlugIn = new ZoomToLayerPlugIn();

  private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn = new ZoomToSelectedItemsPlugIn();

  private CutSelectedItemsPlugIn cutSelectedItemsPlugIn = new CutSelectedItemsPlugIn();

  private CutSelectedLayersPlugIn cutSelectedLayersPlugIn = new CutSelectedLayersPlugIn();

  private CopyStylesPlugIn copyStylesPlugIn = new CopyStylesPlugIn();

  private PasteStylesPlugIn pasteStylesPlugIn = new PasteStylesPlugIn();

  private CombineSelectedFeaturesPlugIn combineSelectedFeaturesPlugIn = new CombineSelectedFeaturesPlugIn();

  private MergeSelectedFeaturesPlugIn mergeSelectedFeaturesPlugIn = new MergeSelectedFeaturesPlugIn();

  private ExplodeSelectedFeaturesPlugIn explodeSelectedFeaturesPlugIn = new ExplodeSelectedFeaturesPlugIn();

  // superseded by org/openjump/OpenJumpConfiguration.java
  // private InstallReferencedImageFactoriesPlugin
  // installReferencedImageFactoriesPlugin = new
  // InstallReferencedImageFactoriesPlugin();

  private ImageLayerManagerPlugIn imageLayerManagerPlugIn = new ImageLayerManagerPlugIn();

  private RefreshDataStoreLayerPlugin refreshDataStoreLayerPlugin = new RefreshDataStoreLayerPlugin();

  private DuplicateItemPlugIn duplicateItemPlugIn = new DuplicateItemPlugIn();

  // / Initialize ArrangeViewsPlugIn G. Aruta 2012-13-12
  private ArrangeViewsPlugIn arrangeHorizontalPlugIn = new ArrangeViewsPlugIn(1);

  private ArrangeViewsPlugIn arrangeVerticalPlugIn = new ArrangeViewsPlugIn(2);

  private ArrangeViewsPlugIn arrangeCascadePlugIn = new ArrangeViewsPlugIn(3);

  private ArrangeViewsPlugIn arrangeAllPlugIn = new ArrangeViewsPlugIn(4);

  // ////////////////////////////////////////////////////////////////////
  public void setup(WorkbenchContext workbenchContext) throws Exception {

    // first things first, make persistent data available early
    PersistentBlackboardPlugIn persistentBlackboard = new PersistentBlackboardPlugIn();
    persistentBlackboard.initialize(workbenchContext.createPlugInContext());

    // this restores the saved laf, so it must be loaded early
    new InstallSkinsPlugIn().initialize(workbenchContext.createPlugInContext());

    configureStyles(workbenchContext);
    configureDatastores(workbenchContext);

    workbenchContext.getWorkbench().getBlackboard()
        .put(SnapToVerticesPolicy.ENABLED_KEY, true);

    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
    configureToolBar(workbenchContext, checkFactory);
    configureMainMenus(workbenchContext, checkFactory, featureInstaller);
    configureLayerPopupMenu(workbenchContext, featureInstaller, checkFactory);
    configureAttributePopupMenu(workbenchContext, featureInstaller,
        checkFactory);
    configureWMSQueryNamePopupMenu(workbenchContext, featureInstaller,
        checkFactory);
    configureCategoryPopupMenu(workbenchContext, featureInstaller);
    configureLayerViewPanelPopupMenu(workbenchContext, checkFactory,
        featureInstaller);
    // org.openjump.core.feature.OperationFactory.init(workbenchContext.createPlugInContext());

    initializeRenderingManager();

    /********************************************
     * [sstein] 11.08.2005 the following line calls the new OpenJump plugins
     *******************************************/
    OpenJumpConfiguration.loadOpenJumpPlugIns(workbenchContext);

    // Call #initializeBuiltInPlugIns after #configureToolBar so that any
    // plug-ins that
    // add items to the toolbar will add them to the *end* of the toolbar.
    // [Jon Aquino]
    initializeBuiltInPlugIns(workbenchContext);
  }

  private void initializeRenderingManager() {
    RenderingManager
        .setRendererFactory(Layer.class, new LayerRendererFactory());
    RenderingManager.setRendererFactory(WMSLayer.class,
        new WmsLayerRendererFactory());
  }

  private void configureCategoryPopupMenu(WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller) throws Exception {
    // fetch the menu reference
    JPopupMenu menu = workbenchContext.getWorkbench().getFrame()
        .getCategoryPopupMenu();

    PlugInContext pc = workbenchContext.createPlugInContext();
    SetCategoryVisibilityPlugIn.getInstance(pc).initialize(pc);

    featureInstaller.addPopupMenuPlugin(menu, addNewLayerPlugIn,
            addNewLayerPlugIn.getName(), false, IconLoader.icon("layers.png"), null);

    featureInstaller.addPopupMenuPlugin(menu, addNewCategoryPlugIn);

    menu.addSeparator(); // ===================
    
    // [sstein 20.01.2006] added again after user request
    featureInstaller.addPopupMenuPlugin(menu, loadDatasetPlugIn,
        loadDatasetPlugIn.getName() + "...", false,
        LoadDatasetPlugIn.getIcon(),
        LoadDatasetPlugIn.createEnableCheck(workbenchContext));
    // --
    // featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
    // .getFrame().getCategoryPopupMenu(), loadDatasetFromFilePlugIn,
    // loadDatasetFromFilePlugIn.getName() + "...", false, null,
    // LoadDatasetPlugIn.createEnableCheck(workbenchContext));

    // [sstein 21.Mar.2008] removed since now contained in new open menu
    // [sstein 2.June.2008] added back due to table list history (need to check
    // other way?)
    featureInstaller.addPopupMenuPlugin(menu, addDatastoreLayerPlugIn,
        addDatastoreLayerPlugIn.getName() + "...", false,
        addDatastoreLayerPlugIn.ICON, null);
    /*
     * //[sstein 21.Mar.2008] removed since now contained in new open menu
     * featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
     * .getFrame().getCategoryPopupMenu(), addWMSQueryPlugIn,
     * addWMSQueryPlugIn.getName() + "...", false, null, null);
     * 
     * featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
     * .getFrame().getCategoryPopupMenu(), addImageLayerPlugIn,
     * addImageLayerPlugIn.getName() + "...", false, null, null);
     */

    featureInstaller.addPopupMenuPlugin(menu, pasteLayersPlugIn,
        pasteLayersPlugIn.getNameWithMnemonic(), false, null,
        pasteLayersPlugIn.createEnableCheck(workbenchContext));
    
    menu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuPlugin(menu, removeSelectedCategoriesPlugIn,
        removeSelectedCategoriesPlugIn.getName(), false, null,
        removeSelectedCategoriesPlugIn.createEnableCheck(workbenchContext));
    
    menu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuPlugin(menu, new MoveCategoryToTop());
    featureInstaller.addPopupMenuPlugin(menu, new MoveCategoryOneUp());
    featureInstaller.addPopupMenuPlugin(menu, new MoveCategoryOneDown());
    featureInstaller.addPopupMenuPlugin(menu, new MoveCategoryToBottom());
  }

  private void configureWMSQueryNamePopupMenu(
      final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
    JPopupMenu wmsLayerNamePopupMenu = workbenchContext.getWorkbench()
        .getFrame().getWMSLayerNamePopupMenu();
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
        editWMSQueryPlugIn, editWMSQueryPlugIn.getName() + "...", false, null,
        editWMSQueryPlugIn.createEnableCheck(workbenchContext));
    wmsLayerNamePopupMenu.addSeparator(); // ===================
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu, moveUpPlugIn,
        moveUpPlugIn.getName(), false, MoveLayerablePlugIn.UPICON,
        moveUpPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu, moveDownPlugIn,
        moveDownPlugIn.getName(), false, MoveLayerablePlugIn.DOWNICON,
        moveDownPlugIn.createEnableCheck(workbenchContext));
    wmsLayerNamePopupMenu.addSeparator(); // ===================
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
        cutSelectedLayersPlugIn, cutSelectedLayersPlugIn.getNameWithMnemonic(),
        false, cutSelectedLayersPlugIn.ICON,
        cutSelectedLayersPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
        copySelectedLayersPlugIn,
        copySelectedLayersPlugIn.getNameWithMnemonic(), false,
        copySelectedLayersPlugIn.ICON,
        copySelectedLayersPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
        removeSelectedLayersPlugIn, removeSelectedLayersPlugIn.getName(),
        false, RemoveSelectedLayersPlugIn.ICON,
        removeSelectedLayersPlugIn.createEnableCheck(workbenchContext));
  }

  private void configureAttributePopupMenu(
      final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
    AttributeTab.addPopupMenuItem(workbenchContext, editablePlugIn,
        editablePlugIn.getName(), true, EditablePlugIn.ICON,
        editablePlugIn.createEnableCheck(workbenchContext));
    AttributeTab.addPopupMenuItem(workbenchContext, viewSchemaPlugIn,
        viewSchemaPlugIn.getName(), false, ViewSchemaPlugIn.ICON,
        ViewSchemaPlugIn.createEnableCheck(workbenchContext));
    AttributeTab.addPopupMenuItem(workbenchContext, featureInfoPlugIn,
        featureInfoPlugIn.getName(), false, FeatureInfoPlugIn.ICON,
        FeatureInfoPlugIn.createEnableCheck(workbenchContext));
    AttributeTab.addPopupMenuItem(workbenchContext, cutSelectedItemsPlugIn,
        cutSelectedItemsPlugIn.getName(), false, CutSelectedItemsPlugIn.ICON,
        cutSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    AttributeTab.addPopupMenuItem(workbenchContext, copySelectedItemsPlugIn,
        copySelectedItemsPlugIn.getNameWithMnemonic(), false,
        copySelectedItemsPlugIn.ICON,
        CopySelectedItemsPlugIn.createEnableCheck(workbenchContext));
    AttributeTab.addPopupMenuItem(workbenchContext, deleteSelectedItemsPlugIn,
        deleteSelectedItemsPlugIn.getName(), false,
        DeleteSelectedItemsPlugIn.ICON,
        DeleteSelectedItemsPlugIn.createEnableCheck(workbenchContext));
  }

  private void configureLayerPopupMenu(final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {

    JPopupMenu layerNamePopupMenu = workbenchContext.getWorkbench().getFrame()
        .getLayerNamePopupMenu();

    featureInstaller.addPopupMenuItem(layerNamePopupMenu, editablePlugIn,
        editablePlugIn.getName(), true, EditablePlugIn.ICON,
        editablePlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, selectablePlugIn,
        selectablePlugIn.getName(), true, selectablePlugIn.ICON,
        selectablePlugIn.createEnableCheck(workbenchContext));
    
    layerNamePopupMenu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        removeSelectedLayersPlugIn, removeSelectedLayersPlugIn.getName(),
        false, RemoveSelectedLayersPlugIn.ICON,
        removeSelectedLayersPlugIn.createEnableCheck(workbenchContext));

    layerNamePopupMenu.addSeparator(); // ===================

    featureInstaller.addPopupMenuPlugin(layerNamePopupMenu, new LayerPropertiesPlugIn());
    
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, zoomToLayerPlugIn,
        zoomToLayerPlugIn.getName(), false, ZoomToLayerPlugIn.ICON,
        zoomToLayerPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addPopupMenuItem(layerNamePopupMenu, viewAttributesPlugIn,
        viewAttributesPlugIn.getName(), false,
        GUIUtil.toSmallIcon(viewAttributesPlugIn.getIcon()),
        viewAttributesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, viewSchemaPlugIn,
        new String[] { MenuNames.SCHEMA }, viewSchemaPlugIn.getName() + "...",
        false, ViewSchemaPlugIn.ICON,
        ViewSchemaPlugIn.createEnableCheck(workbenchContext));
    FeatureInstaller.childMenuItem(MenuNames.SCHEMA, layerNamePopupMenu)
        .setIcon(ViewSchemaPlugIn.ICON);

    featureInstaller.addPopupMenuItem(layerNamePopupMenu, changeStylesPlugIn,
        new String[] { MenuNames.STYLE }, changeStylesPlugIn.getName() + "...",
        false, GUIUtil.toSmallIcon(changeStylesPlugIn.getIcon()),
        changeStylesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, copyStylesPlugIn,
        new String[] { MenuNames.STYLE }, copyStylesPlugIn.getName(), false,
        GUIUtil.toSmallIcon(copyStylesPlugIn.getIcon()),
        CopyStylesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuPlugin(layerNamePopupMenu, pasteStylesPlugIn,
        new String[] { MenuNames.STYLE });
    FeatureInstaller.childMenuItem(MenuNames.STYLE, layerNamePopupMenu)
        .setIcon(IconLoader.icon("color_wheel.png"));

    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        refreshDataStoreLayerPlugin, new String[] { MenuNames.DATASTORE },
        refreshDataStoreLayerPlugin.getName(), false,
        RefreshDataStoreLayerPlugin.ICON,
        RefreshDataStoreLayerPlugin.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        imageLayerManagerPlugIn, imageLayerManagerPlugIn.getName() + "...",
        false, imageLayerManagerPlugIn.getIcon(),
        ImageLayerManagerPlugIn.createEnableCheck(workbenchContext));
    FeatureInstaller.childMenuItem(MenuNames.DATASTORE, layerNamePopupMenu)
        .setIcon(IconLoader.icon("database_gear.png"));

    layerNamePopupMenu.addSeparator(); // ===================

    // [sstein 18.Oct.2011] re-added the saveDatasetAsFilePlugIn and the MacOSX
    // check
    // since under MacOSX the SaveDatasetAsPlugIn is missing a text field to
    // write the name of the new file. Hence, the dialog could only be used
    // for saving to an existing dataset.
    // [sstein 7.Nov.2011] not needed anymore as the bug 3428076 could be fixed
    /*
     * if(CheckOS.isMacOsx()){
     * featureInstaller.addPopupMenuItem(layerNamePopupMenu,
     * saveDatasetAsFilePlugIn, saveDatasetAsFilePlugIn.getName() + "...",
     * false, SaveDatasetAsPlugIn.ICON, AbstractSaveDatasetAsPlugIn
     * .createEnableCheck(workbenchContext)); }
     */
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, saveDatasetAsPlugIn,
        saveDatasetAsPlugIn.getName() + "...", false, SaveDatasetAsPlugIn.ICON,
        AbstractSaveDatasetAsPlugIn.createEnableCheck(workbenchContext));

    layerNamePopupMenu.addSeparator(); // ===================
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, moveUpPlugIn,
        moveUpPlugIn.getName(), false, MoveLayerablePlugIn.UPICON,
        moveUpPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, moveDownPlugIn,
        moveDownPlugIn.getName(), false, MoveLayerablePlugIn.DOWNICON,
        moveDownPlugIn.createEnableCheck(workbenchContext));

    layerNamePopupMenu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        cutSelectedLayersPlugIn, cutSelectedLayersPlugIn.getNameWithMnemonic(),
        false, cutSelectedLayersPlugIn.ICON,
        cutSelectedLayersPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        copySelectedLayersPlugIn,
        copySelectedLayersPlugIn.getNameWithMnemonic(), false,
        copySelectedLayersPlugIn.ICON,
        copySelectedLayersPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuPlugin(layerNamePopupMenu, new CombineSelectedLayersPlugIn());

    layerNamePopupMenu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, addNewFeaturesPlugIn,
        addNewFeaturesPlugIn.getName() + "...", false,
        AddNewFeaturesPlugIn.ICON,
        AddNewFeaturesPlugIn.createEnableCheck(workbenchContext));

    // <<TODO:REFACTORING>> JUMPConfiguration is polluted with a lot of
    // EnableCheck
    // logic. This logic should simply be moved to the individual PlugIns.
    // [Jon Aquino]
    featureInstaller.addPopupMenuItem(layerNamePopupMenu, pasteItemsPlugIn,
        pasteItemsPlugIn.getNameWithMnemonic(), false,
        pasteItemsPlugIn.getIcon(),
        PasteItemsPlugIn.createEnableCheck(workbenchContext));

    // [sstein 22 Jan 2012] Not necessary as we have "Delete Selected Items" in
    // "Edit" menu
    // but for now put it in separators, to avoid accidental use
    layerNamePopupMenu.addSeparator(); // ===================
    
    featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        deleteAllFeaturesPlugIn, deleteAllFeaturesPlugIn.getName(), false,
        DeleteAllFeaturesPlugIn.ICON,
        deleteAllFeaturesPlugIn.createEnableCheck(workbenchContext));
    
    layerNamePopupMenu.addSeparator(); // ===================

  }

  private void configureLayerViewPanelPopupMenu(
      WorkbenchContext workbenchContext, EnableCheckFactory checkFactory,
      FeatureInstaller featureInstaller) {
    JPopupMenu popupMenu = LayerViewPanel.popupMenu();
    featureInstaller.addPopupMenuItem(popupMenu, featureInfoPlugIn,
        featureInfoPlugIn.getName(), false, FeatureInfoPlugIn.ICON,
        FeatureInfoPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(
        popupMenu,
        verticesInFencePlugIn,
        verticesInFencePlugIn.getName(),
        false,
        null,
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createFenceMustBeDrawnCheck()));
    popupMenu.addSeparator(); // ===================
    featureInstaller.addPopupMenuItem(
        popupMenu,
        zoomToFencePlugIn,
        new String[] { I18N.get("ui.MenuNames.ZOOM") },
        I18N.get("JUMPConfiguration.fence"),
        false,
        GUIUtil.toSmallIcon(zoomToFencePlugIn.getIcon()),
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createFenceMustBeDrawnCheck()));
    featureInstaller.addPopupMenuItem(popupMenu, zoomToSelectedItemsPlugIn,
        new String[] { I18N.get("ui.MenuNames.ZOOM") },
        zoomToSelectedItemsPlugIn.getName(), false,
        GUIUtil.toSmallIcon(zoomToSelectedItemsPlugIn.getIcon()),
        ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    // featureInstaller.addPopupMenuItem(popupMenu, zoomToClickPlugIn,
    // I18N.get("JUMPConfiguration.zoom-out"), false, null, null);
    popupMenu.addSeparator(); // ===================

    featureInstaller.addPopupMenuItem(popupMenu, cutSelectedItemsPlugIn,
        cutSelectedItemsPlugIn.getName(), false, CutSelectedItemsPlugIn.ICON,
        cutSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuPlugin(popupMenu, copySelectedItemsPlugIn,
        copySelectedItemsPlugIn.getNameWithMnemonic(), false,
        copySelectedItemsPlugIn.getIcon(),
        CopySelectedItemsPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuPlugin(popupMenu, pasteItemsAtPlugIn);
    featureInstaller.addPopupMenuItem(popupMenu, editSelectedFeaturePlugIn,
        editSelectedFeaturePlugIn.getName(), false,
        editSelectedFeaturePlugIn.getIcon(),
        EditSelectedFeaturePlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(popupMenu, deleteSelectedItemsPlugIn,
        deleteSelectedItemsPlugIn.getName(), false,
        deleteSelectedItemsPlugIn.getIcon(),
        DeleteSelectedItemsPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addPopupMenuItem(popupMenu, duplicateItemPlugIn,
        duplicateItemPlugIn.getName(), false, duplicateItemPlugIn.getIcon(),
        DuplicateItemPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addPopupMenuItem(popupMenu, combineSelectedFeaturesPlugIn,
        combineSelectedFeaturesPlugIn.getName(), false,
        combineSelectedFeaturesPlugIn.getIcon(),
        combineSelectedFeaturesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(popupMenu, mergeSelectedFeaturesPlugIn,
        mergeSelectedFeaturesPlugIn.getName(), false,
        mergeSelectedFeaturesPlugIn.getIcon(),
        mergeSelectedFeaturesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addPopupMenuItem(popupMenu, explodeSelectedFeaturesPlugIn,
        explodeSelectedFeaturesPlugIn.getName(), false,
        explodeSelectedFeaturesPlugIn.getIcon(),
        explodeSelectedFeaturesPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addPopupMenuItem(popupMenu, copyThisCoordinatePlugIn,
        copyThisCoordinatePlugIn.getName(), false, null,
        CopyThisCoordinatePlugIn.createEnableCheck(workbenchContext));

  }

  private void configureMainMenus(final WorkbenchContext workbenchContext,
      final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
      throws Exception {

    /**
     * FILE ===================================================================
     */
    String[] fileMenuPath = new String[] { MenuNames.FILE };

    featureInstaller.addMenuSeparator(fileMenuPath);
    // menu exit item
    workbenchContext.getWorkbench().getFrame().new ExitPlugin()
        .initialize(workbenchContext.createPlugInContext());

    FeatureInstaller.addMainMenu(featureInstaller,
        fileMenuPath, MenuNames.FILE_NEW, 0);
    featureInstaller.addMainMenuPlugin(new NewTaskPlugIn() {
      public String getName() {
        return I18N.get("ui.WorkbenchFrame.task");
      }
    }, new String[] { MenuNames.FILE, MenuNames.FILE_NEW });
    featureInstaller.addMenuSeparator(new String[] { MenuNames.FILE,
        MenuNames.FILE_NEW }); // =============================================
    featureInstaller
        .addMainMenuItem(
            addNewLayerPlugIn,
            new String[] { MenuNames.FILE, MenuNames.FILE_NEW },
            new JMenuItem(
                I18N.get("com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn.name"),
                IconLoader.icon("layers.png")), checkFactory
                .createWindowWithLayerViewPanelMustBeActiveCheck());
    featureInstaller.addMainMenuPlugin(new AddNewCategoryPlugIn() {
      public String getName() {
        return I18N
            .get("com.vividsolutions.jump.workbench.ui.plugin.AddNewCategoryPlugIn.name");
      }
    }, new String[] { MenuNames.FILE, MenuNames.FILE_NEW });

    featureInstaller.addMenuSeparator(MenuNames.FILE); // =====================

    featureInstaller.addMainMenuItem(saveDatasetAsPlugIn,
        fileMenuPath, saveDatasetAsPlugIn.getName() + "...",
        false, SaveDatasetAsPlugIn.ICON,
        SaveDatasetAsPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(saveProjectPlugIn,
        fileMenuPath, saveProjectPlugIn.getName(), false,
        SaveProjectPlugIn.ICON,
        checkFactory.createTaskWindowMustBeActiveCheck());
    featureInstaller.addMainMenuItem(saveProjectAsPlugIn,
        fileMenuPath, saveProjectAsPlugIn.getName() + "...",
        false, SaveProjectAsPlugIn.ICON,
        checkFactory.createTaskWindowMustBeActiveCheck());
    FeatureInstaller.addMainMenu(featureInstaller,
        fileMenuPath, MenuNames.FILE_SAVEVIEW, 5);
    featureInstaller.addMainMenuItem(saveImageAsPlugIn, new String[] {
        MenuNames.FILE, MenuNames.FILE_SAVEVIEW }, saveImageAsPlugIn.getName()
        + "...", false, null,
        SaveImageAsPlugIn.createEnableCheck(workbenchContext));

    /**
     * EDIT ===================================================================
     */
    featureInstaller.addMainMenuItem(undoPlugIn,
        new String[] { MenuNames.EDIT }, undoPlugIn.getName(), false,
        GUIUtil.toSmallIcon(undoPlugIn.getIcon()),
        undoPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(redoPlugIn,
        new String[] { MenuNames.EDIT }, redoPlugIn.getName(), false,
        GUIUtil.toSmallIcon(redoPlugIn.getIcon()),
        redoPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================

    featureInstaller.addMainMenuItem(cutSelectedItemsPlugIn,
        new String[] { MenuNames.EDIT }, cutSelectedItemsPlugIn.getName(),
        false, CutSelectedItemsPlugIn.ICON,
        cutSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuPlugin(copySelectedItemsPlugIn,
        new String[] { MenuNames.EDIT });
    featureInstaller.addMainMenuPlugin(pasteItemsPlugIn,
        new String[] { MenuNames.EDIT });

    featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================

    featureInstaller.addMainMenuItem(deleteSelectedItemsPlugIn,
        new String[] { MenuNames.EDIT }, deleteSelectedItemsPlugIn.getName(),
        false, DeleteSelectedItemsPlugIn.ICON,
        DeleteSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================

    FeatureInstaller.addMainMenu(featureInstaller,
         new String[] { MenuNames.EDIT }, MenuNames.SELECTION, -1);
     
    featureInstaller.addMainMenuPlugin(new InvertSelectionPlugIn(), new String[] { MenuNames.EDIT });

    featureInstaller.addMainMenuItem(clearSelectionPlugIn,
        new String[] { MenuNames.EDIT }, clearSelectionPlugIn.getName(), false,
        null, clearSelectionPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================
    
    featureInstaller.addMainMenuItem(addNewFeaturesPlugIn,
        new String[] { MenuNames.EDIT },
        addNewFeaturesPlugIn.getName() + "...", false,
        AddNewFeaturesPlugIn.ICON,
        AddNewFeaturesPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(editSelectedFeaturePlugIn,
        new String[] { MenuNames.EDIT }, editSelectedFeaturePlugIn.getName(),
        false, editSelectedFeaturePlugIn.getIcon(),
        EditSelectedFeaturePlugIn.createEnableCheck(workbenchContext));


    /**
     * VIEW =================================================================
     */
    
    featureInstaller.addMainMenuItem(zoomPreviousPlugIn,
        new String[] { MenuNames.VIEW }, zoomPreviousPlugIn.getName(), false,
        GUIUtil.toSmallIcon(zoomPreviousPlugIn.getIcon()),
        zoomPreviousPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(zoomNextPlugIn,
        new String[] { MenuNames.VIEW }, zoomNextPlugIn.getName(), false,
        GUIUtil.toSmallIcon(zoomNextPlugIn.getIcon()),
        zoomNextPlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
    
    editingPlugIn.createMainMenuItem(new String[] { MenuNames.VIEW },
        GUIUtil.toSmallIcon(EditingPlugIn.ICON), workbenchContext);
    featureInstaller.addMainMenuItem(copyImagePlugIn,
        new String[] { MenuNames.FILE }, copyImagePlugIn.getName(), false,
        copyImagePlugIn.ICON,
        CopyImagePlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMainMenuItem(toolTipsPlugIn,
        new String[] { MenuNames.VIEW }, toolTipsPlugIn.getName(), true,
        IconLoader.icon("show_tooltip.png"),
        MapToolTipsPlugIn.createEnableCheck(workbenchContext));
    zoomBarPlugIn.createMainMenuItem(new String[] { MenuNames.VIEW }, null,
        workbenchContext);

    featureInstaller.addMainMenuItem(
        showScalePlugIn,
        new String[] { MenuNames.VIEW, MenuNames.MAP_DECORATIONS },
        showScalePlugIn.getName(),
        true,
        IconLoader.icon("show_scale_text.png"),
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(new EnableCheck() {
              public String check(JComponent component) {
                ((JCheckBoxMenuItem) component).setSelected(ShowScaleRenderer
                    .isEnabled(workbenchContext.getLayerViewPanel()));
                return null;
              }
            }));
    featureInstaller.addMainMenuItem(
        scaleBarPlugIn,
        new String[] { MenuNames.VIEW, MenuNames.MAP_DECORATIONS },
        scaleBarPlugIn.getName(),
        true,
        IconLoader.icon("show_scale.png"),
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(new EnableCheck() {
              public String check(JComponent component) {
                ((JCheckBoxMenuItem) component).setSelected(ScaleBarRenderer
                    .isEnabled(workbenchContext.getLayerViewPanel()));
                return null;
              }
            }));
   
    featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
    
    featureInstaller.addMainMenuItem(zoomToFullExtentPlugIn,
        new String[] { MenuNames.VIEW }, zoomToFullExtentPlugIn.getName(),
        false, zoomToFullExtentPlugIn.getIcon16(),
        zoomToFullExtentPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(
        zoomToFencePlugIn,
        new String[] { MenuNames.VIEW },
        zoomToFencePlugIn.getName(),
        false,
        GUIUtil.toSmallIcon(zoomToFencePlugIn.getIcon()),
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createFenceMustBeDrawnCheck()));
    featureInstaller.addMainMenuItem(zoomToSelectedItemsPlugIn,
        new String[] { MenuNames.VIEW }, zoomToSelectedItemsPlugIn.getName(),
        false, GUIUtil.toSmallIcon(zoomToSelectedItemsPlugIn.getIcon()),
        ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(zoomToCoordinatePlugIn,
        new String[] { MenuNames.VIEW }, zoomToCoordinatePlugIn.getName()
            + "...", false, null,
        zoomToCoordinatePlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
    
    featureInstaller.addMainMenuItem(featureInfoPlugIn,
        new String[] { MenuNames.VIEW }, featureInfoPlugIn.getName(), false,
        FeatureInfoPlugIn.ICON,
        FeatureInfoPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(
        verticesInFencePlugIn,
        new String[] { MenuNames.VIEW },
        verticesInFencePlugIn.getName(),
        false,
        null,
        new MultiEnableCheck().add(
            checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createFenceMustBeDrawnCheck()));
    featureInstaller.addMainMenuPlugin(new CopyBBoxPlugin(), new String[] { MenuNames.VIEW });

    featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================

    /**
     * LAYER ==================================================================
     */
    configLayer(workbenchContext, checkFactory, featureInstaller);

    /**
     * WINDOW =================================================================
     */
    featureInstaller.addMainMenuItem(outputWindowPlugIn,
        new String[] { MenuNames.WINDOW }, outputWindowPlugIn.getName(), false,
        GUIUtil.toSmallIcon(outputWindowPlugIn.getIcon()), null);
    featureInstaller.addMainMenuItem(generateLogPlugIn,
        new String[] { MenuNames.WINDOW }, generateLogPlugIn.getName(), false,
        GUIUtil.toSmallIcon(generateLogPlugIn.getIcon()), null);
    featureInstaller.addMenuSeparator(MenuNames.WINDOW); // ===================

    featureInstaller.addMainMenuPlugin(arrangeHorizontalPlugIn,
        new String[] { MenuNames.WINDOW });
    featureInstaller.addMainMenuPlugin(arrangeVerticalPlugIn,
        new String[] { MenuNames.WINDOW });
    featureInstaller.addMainMenuPlugin(arrangeCascadePlugIn,
        new String[] { MenuNames.WINDOW });
    featureInstaller.addMainMenuPlugin(arrangeAllPlugIn,
        new String[] { MenuNames.WINDOW });
    
    new MosaicInternalFramesPlugIn().initialize(new PlugInContext(workbenchContext, null, null,
        null, null));
    
    featureInstaller.addMenuSeparator(MenuNames.WINDOW); // ===================
    
    featureInstaller.addMainMenuPlugin(cloneWindowPlugIn,
        new String[] { MenuNames.WINDOW });
    
    new SynchronizationPlugIn("").initialize(new PlugInContext(workbenchContext, null, null,
      null, null));

    featureInstaller.addMenuSeparator(MenuNames.WINDOW); // ===================
    
    /**
     * HELP ===================================================================
     */
    featureInstaller.addMainMenuItem(shortcutKeysPlugIn,
        new String[] { MenuNames.HELP }, shortcutKeysPlugIn.getName() + "...",
        false, ShortcutKeysPlugIn.ICON, null);
    new FeatureInstaller(workbenchContext).addMainMenuItem(new AboutPlugIn(),
        new String[] { MenuNames.HELP }, I18N.get("JUMPConfiguration.about"),
        false, AboutPlugIn.ICON, null);

    /**
     * CUSTOMIZE ==============================================================
     */
    featureInstaller.addMainMenuItem(optionsPlugIn,
        new String[] { MenuNames.CUSTOMIZE }, optionsPlugIn.getName() + "...",
        false, null, null);
  }

  private AddDatastoreLayerPlugIn addDatastoreLayerPlugIn = new AddDatastoreLayerPlugIn();
  private InstallDatastoreLayerRendererHintsPlugIn installDatastoreLayerRendererHintsPlugIn = new InstallDatastoreLayerRendererHintsPlugIn();

  private void configLayer(final WorkbenchContext workbenchContext,
      final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
      throws Exception {

    String MENU_LAYER = MenuNames.LAYER;

    // featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
    featureInstaller.addMainMenuItem(cutSelectedLayersPlugIn,
        new String[] { MENU_LAYER },
        new JMenuItem(cutSelectedLayersPlugIn.getNameWithMnemonic(),
            cutSelectedLayersPlugIn.ICON), cutSelectedLayersPlugIn
            .createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(copySelectedLayersPlugIn,
        new String[] { MENU_LAYER },
        new JMenuItem(copySelectedLayersPlugIn.getNameWithMnemonic(),
            copySelectedLayersPlugIn.ICON), copySelectedLayersPlugIn
            .createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuItem(pasteLayersPlugIn,
        new String[] { MENU_LAYER },
        new JMenuItem(pasteLayersPlugIn.getNameWithMnemonic()),
        pasteLayersPlugIn.createEnableCheck(workbenchContext));
    featureInstaller.addMainMenuPlugin(new CombineSelectedLayersPlugIn(), new String[] { MENU_LAYER });

    featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
    featureInstaller.addMainMenuItem(removeSelectedLayersPlugIn,
        new String[] { MENU_LAYER },
        new JMenuItem(removeSelectedLayersPlugIn.getName(),
            RemoveSelectedLayersPlugIn.ICON), removeSelectedLayersPlugIn
            .createEnableCheck(workbenchContext));
    featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
    featureInstaller.addMainMenuItem(removeSelectedCategoriesPlugIn,
        new String[] { MENU_LAYER }, new JMenuItem(
            removeSelectedCategoriesPlugIn.getName()),
        removeSelectedCategoriesPlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
    featureInstaller.addMainMenuPlugin(imageLayerManagerPlugIn,
        new String[] { MENU_LAYER });
  }

  public void configureDatastores(final WorkbenchContext context)
      throws Exception {

    context.getRegistry().createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
        new PostgisDataStoreDriver());

    // update exit handler
    final ApplicationExitHandler oldApplicationExitHandler = context
        .getWorkbench().getFrame().getApplicationExitHandler();
    context.getWorkbench().getFrame()
        .setApplicationExitHandler(new ApplicationExitHandler() {
          public void exitApplication(JFrame mainFrame) {
            try {
              ConnectionManager.instance(context).closeConnections();
            } catch (DataStoreException e) {
              throw new RuntimeException(e);
            }
            oldApplicationExitHandler.exitApplication(mainFrame);
          }
        });
  }

  private void configureStyles(WorkbenchContext workbenchContext) {
    WorkbenchFrame frame = workbenchContext.getWorkbench().getFrame();
    frame.addChoosableStyleClass(VertexXYLineSegmentStyle.VertexXY.class);
    frame.addChoosableStyleClass(VertexIndexLineSegmentStyle.VertexIndex.class);
    frame
        .addChoosableStyleClass(MetricsLineStringSegmentStyle.LengthAngle.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.Open.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.Solid.class);
    frame.addChoosableStyleClass(ArrowLineStringSegmentStyle.NarrowSolid.class);
    frame
        .addChoosableStyleClass(ArrowLineStringEndpointStyle.FeathersStart.class);
    frame
        .addChoosableStyleClass(ArrowLineStringEndpointStyle.FeathersEnd.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.OpenStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.OpenEnd.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.SolidStart.class);
    frame.addChoosableStyleClass(ArrowLineStringEndpointStyle.SolidEnd.class);
    frame
        .addChoosableStyleClass(ArrowLineStringEndpointStyle.NarrowSolidStart.class);
    frame
        .addChoosableStyleClass(ArrowLineStringEndpointStyle.NarrowSolidEnd.class);
    frame.addChoosableStyleClass(CircleLineStringEndpointStyle.Start.class);
    frame.addChoosableStyleClass(CircleLineStringEndpointStyle.End.class);
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
    frame.getToolBar().addPlugIn(zoomToFullExtentPlugIn.getIcon(),
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
        return I18N.get("JUMPConfiguration.fence");
      }
    }.add(new DrawRectangleFenceTool()).add(new DrawPolygonFenceTool()),
        workbenchContext);
    add(new FeatureInfoTool(), workbenchContext);
    frame.getToolBar().addSeparator();
    configureEditingButton(workbenchContext);
    frame.getToolBar().addSeparator();
    // add(new MeasureTool(), workbenchContext);
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
            I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasurePlugin.OptionPanelTitle"),
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
    // Last of all, add a separator because some plug-ins may add
    // CursorTools.
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

  /**
   * Call each PlugIn's #initialize() method. Uses reflection to build a list of
   * plug-ins.
   * 
   * @param workbenchContext
   *          Description of the Parameter
   * @exception Exception
   *              Description of the Exception
   */
  private void initializeBuiltInPlugIns(WorkbenchContext workbenchContext)
      throws Exception {
    Field[] fields = getClass().getDeclaredFields();

    Object field = null;
    final PlugInContext plugInContext = new PlugInContext(workbenchContext,
        null, null, null, null);
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
      plugIn.initialize(plugInContext);

      // register shortcuts of plugins
      AbstractPlugIn.registerShortcuts(plugIn);
    }
  }
}
