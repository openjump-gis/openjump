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

import com.vividsolutions.jts.util.*;
import org.openjump.OpenJumpConfiguration;
import com.vividsolutions.jump.I18N;

import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.datastore.postgis.*;
import com.vividsolutions.jump.workbench.datasource.*;
import com.vividsolutions.jump.workbench.datastore.*;
import com.vividsolutions.jump.workbench.datasource.LoadDatasetPlugIn;
import com.vividsolutions.jump.workbench.datasource.SaveDatasetAsPlugIn;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.cursortool.*;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.*;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.workbench.ui.plugin.analysis.*;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.*;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.*;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.AddImageLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.ImageLayerManagerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.imagery.InstallReferencedImageFactoriesPlugin;
import com.vividsolutions.jump.workbench.ui.plugin.scalebar.*;
import com.vividsolutions.jump.workbench.ui.plugin.skin.InstallSkinsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.test.*;
import com.vividsolutions.jump.workbench.ui.plugin.wms.*;
import com.vividsolutions.jump.workbench.ui.renderer.style.*;
import com.vividsolutions.jump.workbench.ui.snap.*;
import com.vividsolutions.jump.workbench.ui.style.*;
import com.vividsolutions.jump.workbench.ui.task.*;
import com.vividsolutions.jump.workbench.ui.warp.*;
import com.vividsolutions.jump.workbench.ui.zoom.*;
import com.vividsolutions.jump.plugin.qa.*;
import com.vividsolutions.jump.plugin.edit.*;

//import de.latlon.deejump.plugin.style.DeeChangeStylesPlugIn;

import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;

/**
 * Initializes the Workbench with various menus and cursor tools. Accesses the
 * Workbench structure through a WorkbenchContext.
 */
public class JUMPConfiguration implements Setup {

	/**
	 * Built-in plugins must be defined as instance variables, since
	 * they are located for iniatialization via reflection on this class
	 */
    private InstallScaleBarPlugIn installScaleBarPlugIn = new InstallScaleBarPlugIn();

    private InstallGridPlugIn installGridPlugIn = new InstallGridPlugIn();

    private PersistentBlackboardPlugIn persistentBlackboardPlugIn = new PersistentBlackboardPlugIn();

    //FirstTaskFramePlugIn will be initialized using reflection in
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

    private CopyThisCoordinatePlugIn copyThisCoordinatePlugIn = new CopyThisCoordinatePlugIn();

    private CopyImagePlugIn copyImagePlugIn = new CopyImagePlugIn();

    private MapToolTipsPlugIn toolTipsPlugIn = new MapToolTipsPlugIn();

    private CopySelectedLayersPlugIn copySelectedLayersPlugIn = new CopySelectedLayersPlugIn();

    private AddNewLayerPlugIn addNewLayerPlugIn = new AddNewLayerPlugIn();

    private AddWMSDemoBoxEasterEggPlugIn addWMSDemoBoxEasterEggPlugIn = new AddWMSDemoBoxEasterEggPlugIn();

    private EditSelectedFeaturePlugIn editSelectedFeaturePlugIn = new EditSelectedFeaturePlugIn();

    private EditingPlugIn editingPlugIn = new EditingPlugIn();

    private EditablePlugIn editablePlugIn = new EditablePlugIn(editingPlugIn);

    // [Michael Michaud 2007-03-23] Moved BeanShellPlugIn initialization in OpenJUMPConfiguration
    // private BeanShellPlugIn beanShellPlugIn = new BeanShellPlugIn();


    private LoadDatasetPlugIn loadDatasetPlugIn = new LoadDatasetPlugIn();
    //private LoadDatasetFromFilePlugIn loadDatasetFromFilePlugIn = new LoadDatasetFromFilePlugIn();
    private SaveDatasetAsPlugIn saveDatasetAsPlugIn = new SaveDatasetAsPlugIn();
    //private SaveDatasetAsFilePlugIn saveDatasetAsFilePlugIn = new SaveDatasetAsFilePlugIn();
    private SaveImageAsPlugIn saveImageAsPlugIn = new SaveImageAsPlugIn();

    private GenerateLogPlugIn generateLogPlugIn = new GenerateLogPlugIn();

    private NewTaskPlugIn newTaskPlugIn = new NewTaskPlugIn();

    private OpenProjectPlugIn openProjectPlugIn = new OpenProjectPlugIn();


    private PasteItemsPlugIn pasteItemsPlugIn = new PasteItemsPlugIn();

    private PasteLayersPlugIn pasteLayersPlugIn = new PasteLayersPlugIn();

    private DeleteAllFeaturesPlugIn deleteAllFeaturesPlugIn = new DeleteAllFeaturesPlugIn();

    private DeleteSelectedItemsPlugIn deleteSelectedItemsPlugIn = new DeleteSelectedItemsPlugIn();

    private RemoveSelectedLayersPlugIn removeSelectedLayersPlugIn = new RemoveSelectedLayersPlugIn();

    private RemoveSelectedCategoriesPlugIn removeSelectedCategoriesPlugIn = new RemoveSelectedCategoriesPlugIn();

    private SaveProjectAsPlugIn saveProjectAsPlugIn = new SaveProjectAsPlugIn();

    private SaveProjectPlugIn saveProjectPlugIn = new SaveProjectPlugIn(
            saveProjectAsPlugIn);

    private SelectFeaturesInFencePlugIn selectFeaturesInFencePlugIn = new SelectFeaturesInFencePlugIn();

    private ScaleBarPlugIn scaleBarPlugIn = new ScaleBarPlugIn();

    private ZoomBarPlugIn zoomBarPlugIn = new ZoomBarPlugIn();

// [sstein, 01.08.2006] commented out for test reasons with DeeChangeStylePlugIn
// [sstein, 27.09.2006] comment in - problem with colour theming    
    private ChangeStylesPlugIn changeStylesPlugIn = new ChangeStylesPlugIn();

    private UndoPlugIn undoPlugIn = new UndoPlugIn();

    private RedoPlugIn redoPlugIn = new RedoPlugIn();



    private ViewAttributesPlugIn viewAttributesPlugIn = new ViewAttributesPlugIn();

    private ViewSchemaPlugIn viewSchemaPlugIn = new ViewSchemaPlugIn(
            editingPlugIn);

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

    private ExplodeSelectedFeaturesPlugIn explodeSelectedFeaturesPlugIn = new ExplodeSelectedFeaturesPlugIn();
    
    private InstallSkinsPlugIn installSkinsPlugIn = new InstallSkinsPlugIn(); 
    
    //[ssein 01.08.2006] -- added for test reasons
    //[sstein, 27.09.2006] comment out - problem with colour theming reported by Ole 
  	//private DeeChangeStylesPlugIn changeStylesPlugIn = new DeeChangeStylesPlugIn(); 
  	
    private InstallReferencedImageFactoriesPlugin installReferencedImageFactoriesPlugin = new InstallReferencedImageFactoriesPlugin();

    private ImageLayerManagerPlugIn imageLayerManagerPlugIn = new ImageLayerManagerPlugIn();

  	private RefreshDataStoreLayerPlugin refreshDataStoreLayerPlugin = new RefreshDataStoreLayerPlugin();
  	
    public void setup(WorkbenchContext workbenchContext) throws Exception {
        configureStyles(workbenchContext);
        configureDatastores(workbenchContext);

        workbenchContext.getWorkbench().getBlackboard().put(
                SnapToVerticesPolicy.ENABLED_KEY, true);

        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        FeatureInstaller featureInstaller = new FeatureInstaller(
                workbenchContext);
        configureToolBar(workbenchContext, checkFactory);
        configureMainMenus(workbenchContext, checkFactory, featureInstaller);
        configureLayerPopupMenu(workbenchContext, featureInstaller,
                checkFactory);
        configureAttributePopupMenu(workbenchContext, featureInstaller,
                checkFactory);
        configureWMSQueryNamePopupMenu(workbenchContext, featureInstaller,
                checkFactory);
        configureCategoryPopupMenu(workbenchContext, featureInstaller);
        configureLayerViewPanelPopupMenu(workbenchContext, checkFactory,
                featureInstaller);

        /********************************************
         * [sstein] 11.08.2005
         * the following line calls the new OpenJump plugins
         *******************************************/
        OpenJumpConfiguration.loadOpenJumpPlugIns(workbenchContext);
        
        //Call #initializeBuiltInPlugIns after #configureToolBar so that any
        // plug-ins that
        //add items to the toolbar will add them to the *end* of the toolbar.
        // [Jon Aquino]
        initializeBuiltInPlugIns(workbenchContext);
    }

    private void configureCategoryPopupMenu(WorkbenchContext workbenchContext,
            FeatureInstaller featureInstaller) {
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), addNewLayerPlugIn,
                addNewLayerPlugIn.getName(), false, null, null);  

    	//[sstein 20.01.2006] added again after user request
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), loadDatasetPlugIn,
                loadDatasetPlugIn.getName() + "...", false, LoadDatasetPlugIn.getIcon(),
                LoadDatasetPlugIn.createEnableCheck(workbenchContext));
        //--
        /*featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), loadDatasetFromFilePlugIn,
                loadDatasetFromFilePlugIn.getName() + "...", false, null,
                LoadDatasetPlugIn.createEnableCheck(workbenchContext));*/
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), addDatastoreLayerPlugIn,
                addDatastoreLayerPlugIn.getName() + "...", false, null,
                null);
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), addWMSQueryPlugIn,
                addWMSQueryPlugIn.getName() + "...", false, null, null);
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), addImageLayerPlugIn,
                addImageLayerPlugIn.getName() + "...", false, null, null);
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(), pasteLayersPlugIn,
                pasteLayersPlugIn.getNameWithMnemonic(), false, null,
                pasteLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(workbenchContext.getWorkbench()
                .getFrame().getCategoryPopupMenu(),
                removeSelectedCategoriesPlugIn, removeSelectedCategoriesPlugIn
                        .getName(), false, null, removeSelectedCategoriesPlugIn
                        .createEnableCheck(workbenchContext));
    }

    private void configureWMSQueryNamePopupMenu(
            final WorkbenchContext workbenchContext,
            FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
        JPopupMenu wmsLayerNamePopupMenu = workbenchContext.getWorkbench()
                .getFrame().getWMSLayerNamePopupMenu();
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
                editWMSQueryPlugIn, editWMSQueryPlugIn.getName() + "...",
                false, null, editWMSQueryPlugIn
                        .createEnableCheck(workbenchContext));
        wmsLayerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu, moveUpPlugIn,
                moveUpPlugIn.getName(), false, null, moveUpPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
                moveDownPlugIn, moveDownPlugIn.getName(), false, null,
                moveDownPlugIn.createEnableCheck(workbenchContext));
        wmsLayerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
                cutSelectedLayersPlugIn, cutSelectedLayersPlugIn
                        .getNameWithMnemonic(), false, null,
                cutSelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
                copySelectedLayersPlugIn, copySelectedLayersPlugIn
                        .getNameWithMnemonic(), false, null,
                copySelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(wmsLayerNamePopupMenu,
                removeSelectedLayersPlugIn, removeSelectedLayersPlugIn
                        .getName(), false, null, removeSelectedLayersPlugIn
                        .createEnableCheck(workbenchContext));
    }

    private void configureAttributePopupMenu(
            final WorkbenchContext workbenchContext,
            FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
        AttributeTab.addPopupMenuItem(workbenchContext, editablePlugIn,
                editablePlugIn.getName(), true, null, editablePlugIn
                        .createEnableCheck(workbenchContext));
        AttributeTab.addPopupMenuItem(workbenchContext, featureInfoPlugIn,
                featureInfoPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(FeatureInfoTool.ICON), FeatureInfoPlugIn
                        .createEnableCheck(workbenchContext));
        AttributeTab.addPopupMenuItem(workbenchContext, viewSchemaPlugIn,
                viewSchemaPlugIn.getName(), false, ViewSchemaPlugIn.ICON,
                ViewSchemaPlugIn.createEnableCheck(workbenchContext));
        AttributeTab.addPopupMenuItem(workbenchContext, cutSelectedItemsPlugIn,
                cutSelectedItemsPlugIn.getName(), false, null,
                cutSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        AttributeTab.addPopupMenuItem(workbenchContext,
                copySelectedItemsPlugIn, copySelectedItemsPlugIn
                        .getNameWithMnemonic(), false, null,
                CopySelectedItemsPlugIn.createEnableCheck(workbenchContext));
        AttributeTab.addPopupMenuItem(workbenchContext,
                deleteSelectedItemsPlugIn, deleteSelectedItemsPlugIn.getName(),
                false, null, DeleteSelectedItemsPlugIn
                        .createEnableCheck(workbenchContext));
    }

    private void configureLayerPopupMenu(
            final WorkbenchContext workbenchContext,
            FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {

        JPopupMenu layerNamePopupMenu = workbenchContext.getWorkbench()
                .getFrame().getLayerNamePopupMenu();

        featureInstaller.addPopupMenuItem(layerNamePopupMenu, editablePlugIn,
                editablePlugIn.getName(), true, null, editablePlugIn
                        .createEnableCheck(workbenchContext));

        layerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                zoomToLayerPlugIn, zoomToLayerPlugIn.getName(), false, null,
                zoomToLayerPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                changeStylesPlugIn, changeStylesPlugIn.getName() + "...",
                false, GUIUtil.toSmallIcon(changeStylesPlugIn.getIcon()),
                changeStylesPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, copyStylesPlugIn,
                copyStylesPlugIn.getName(), false, null,
                CopyStylesPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, pasteStylesPlugIn,
                pasteStylesPlugIn.getName(), false, null,
                PasteStylesPlugIn.createEnableCheck(workbenchContext));

        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                viewAttributesPlugIn, viewAttributesPlugIn.getName(), false,
                GUIUtil.toSmallIcon(viewAttributesPlugIn.getIcon()),
                viewAttributesPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, viewSchemaPlugIn,
                viewSchemaPlugIn.getName(), false, ViewSchemaPlugIn.ICON,
                ViewSchemaPlugIn.createEnableCheck(workbenchContext));

        featureInstaller.addPopupMenuItem(layerNamePopupMenu, imageLayerManagerPlugIn,
                imageLayerManagerPlugIn.getName() + "...", false, null,
                ImageLayerManagerPlugIn.createEnableCheck(workbenchContext));
        
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, refreshDataStoreLayerPlugin,
        		refreshDataStoreLayerPlugin.getName() + "...", false, null,
                RefreshDataStoreLayerPlugin.createEnableCheck(workbenchContext));


        layerNamePopupMenu.addSeparator(); // ===================
        /*featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                saveDatasetAsFilePlugIn, saveDatasetAsFilePlugIn.getName() + "...",
                false, null, AbstractSaveDatasetAsPlugIn
                        .createEnableCheck(workbenchContext));*/
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
        		saveDatasetAsPlugIn, saveDatasetAsPlugIn.getName() + "...",
                false, null, AbstractSaveDatasetAsPlugIn
                        .createEnableCheck(workbenchContext));

        layerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, moveUpPlugIn,
                moveUpPlugIn.getName(), false, null, moveUpPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, moveDownPlugIn,
                moveDownPlugIn.getName(), false, null, moveDownPlugIn
                        .createEnableCheck(workbenchContext));

        layerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                cutSelectedLayersPlugIn, cutSelectedLayersPlugIn
                        .getNameWithMnemonic(), false, null,
                cutSelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                copySelectedLayersPlugIn, copySelectedLayersPlugIn
                        .getNameWithMnemonic(), false, null,
                copySelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                removeSelectedLayersPlugIn, removeSelectedLayersPlugIn
                        .getName(), false, null, removeSelectedLayersPlugIn
                        .createEnableCheck(workbenchContext));

        layerNamePopupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                addNewFeaturesPlugIn, addNewFeaturesPlugIn.getName() + "...",
                false, null, AddNewFeaturesPlugIn
                        .createEnableCheck(workbenchContext));

        //<<TODO:REFACTORING>> JUMPConfiguration is polluted with a lot of
        // EnableCheck
        //logic. This logic should simply be moved to the individual PlugIns.
        // [Jon Aquino]
        featureInstaller.addPopupMenuItem(layerNamePopupMenu, pasteItemsPlugIn,
                pasteItemsPlugIn.getNameWithMnemonic(), false, null,
                PasteItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(layerNamePopupMenu,
                deleteAllFeaturesPlugIn, deleteAllFeaturesPlugIn.getName(),
                false, null, deleteAllFeaturesPlugIn
                        .createEnableCheck(workbenchContext));
    }

    private void configureLayerViewPanelPopupMenu(
            WorkbenchContext workbenchContext, EnableCheckFactory checkFactory,
            FeatureInstaller featureInstaller) {
        JPopupMenu popupMenu = LayerViewPanel.popupMenu();
        featureInstaller.addPopupMenuItem(popupMenu, featureInfoPlugIn,
                featureInfoPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(FeatureInfoTool.ICON), FeatureInfoPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller
                .addPopupMenuItem(
                        popupMenu,
                        verticesInFencePlugIn,
                        verticesInFencePlugIn.getName(),
                        false,
                        null,
                        new MultiEnableCheck()
                                .add(
                                        checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(checkFactory.createFenceMustBeDrawnCheck()));
        popupMenu.addSeparator(); // ===================
        featureInstaller
                .addPopupMenuItem(
                        popupMenu,
                        zoomToFencePlugIn,
                        I18N.get("JUMPConfiguration.fence"),
                        false,
                        GUIUtil.toSmallIcon(zoomToFencePlugIn.getIcon()),
                        new MultiEnableCheck()
                                .add(checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(checkFactory.createFenceMustBeDrawnCheck()));
        featureInstaller.addPopupMenuItem(popupMenu, zoomToSelectedItemsPlugIn,
                zoomToSelectedItemsPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(zoomToSelectedItemsPlugIn.getIcon()),
                ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, zoomToClickPlugIn,
                I18N.get("JUMPConfiguration.zoom-out"), false, null, null);
        popupMenu.addSeparator(); // ===================
        featureInstaller.addPopupMenuItem(popupMenu,
                selectFeaturesInFencePlugIn, selectFeaturesInFencePlugIn
                        .getName(), false, null, SelectFeaturesInFencePlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, cutSelectedItemsPlugIn,
                cutSelectedItemsPlugIn.getName(), false, null,
                cutSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, copySelectedItemsPlugIn,
                copySelectedItemsPlugIn.getNameWithMnemonic(), false, null,
                CopySelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, copyThisCoordinatePlugIn,
                copyThisCoordinatePlugIn.getName(), false, null,
                CopyThisCoordinatePlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, editSelectedFeaturePlugIn,
                editSelectedFeaturePlugIn.getName(), false, null,
                EditSelectedFeaturePlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu, deleteSelectedItemsPlugIn,
                deleteSelectedItemsPlugIn.getName(), false, null,
                DeleteSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu,
                combineSelectedFeaturesPlugIn, combineSelectedFeaturesPlugIn
                        .getName(), false, null, combineSelectedFeaturesPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addPopupMenuItem(popupMenu,
                explodeSelectedFeaturesPlugIn, explodeSelectedFeaturesPlugIn
                        .getName(), false, null, explodeSelectedFeaturesPlugIn
                        .createEnableCheck(workbenchContext));
    }


    private void configureMainMenus(final WorkbenchContext workbenchContext,
            final EnableCheckFactory checkFactory,
		//-- FILE
            FeatureInstaller featureInstaller) throws Exception {
        /*featureInstaller.addMainMenuItemWithJava14Fix(loadDatasetFromFilePlugIn, new String[] {MenuNames.FILE},
                loadDatasetFromFilePlugIn.getName() + "...", false, null, AbstractLoadDatasetPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(saveDatasetAsFilePlugIn, new String[] {MenuNames.FILE},
                saveDatasetAsFilePlugIn.getName() + "...", false, null,
                AbstractSaveDatasetAsPlugIn.createEnableCheck(workbenchContext));*/
        featureInstaller.addMainMenuItemWithJava14Fix(loadDatasetPlugIn, new String[] {MenuNames.FILE},
                loadDatasetPlugIn.getName() + "...", false, LoadDatasetPlugIn.getIcon(), LoadDatasetPlugIn
                        .createEnableCheck(workbenchContext));        
        featureInstaller.addMainMenuItemWithJava14Fix(saveDatasetAsPlugIn, new String[] {MenuNames.FILE},
                saveDatasetAsPlugIn.getName() + "...", false, null,
                SaveDatasetAsPlugIn.createEnableCheck(workbenchContext));  
        featureInstaller.addMenuSeparator(MenuNames.FILE); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(newTaskPlugIn, new String[] {MenuNames.FILE}, newTaskPlugIn
                .getName()
                + "...", false, NewTaskPlugIn.getIcon(), null);
        featureInstaller.addMainMenuItemWithJava14Fix(openProjectPlugIn, new String[] {MenuNames.FILE},
                openProjectPlugIn.getName() + "...", false, null,
                new MultiEnableCheck());
        featureInstaller.addMainMenuItemWithJava14Fix(saveProjectPlugIn, new String[] {MenuNames.FILE},
                saveProjectPlugIn.getName(), false, null, checkFactory
                        .createTaskWindowMustBeActiveCheck());
        featureInstaller.addMainMenuItemWithJava14Fix(saveProjectAsPlugIn, new String[] {MenuNames.FILE},
                saveProjectAsPlugIn.getName() + "...", false, null, checkFactory
                        .createTaskWindowMustBeActiveCheck());
        featureInstaller.addMenuSeparator(MenuNames.FILE); // ===================        
        featureInstaller.addMainMenuItemWithJava14Fix(
        		saveImageAsPlugIn, 
				//TODO :unfortunately i am not able to define the menu position 
				//      for sub-menus. so i comment it out [sstein: 12.09.2005]  
        		//new String[] {MenuNames.FILE, MenuNames.FILE_EXPORTLAYERVIEW},
        		new String[] {MenuNames.FILE},
                saveImageAsPlugIn.getName() + "...", 
				false,
				null, 
				SaveImageAsPlugIn.createEnableCheck(workbenchContext));      
		//-- EDIT
        featureInstaller.addMainMenuItemWithJava14Fix(undoPlugIn, new String[] {MenuNames.EDIT}, undoPlugIn
                .getName(), false, GUIUtil.toSmallIcon(undoPlugIn.getIcon()),
                undoPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(redoPlugIn, new String[] {MenuNames.EDIT}, redoPlugIn
                .getName(), false, GUIUtil.toSmallIcon(redoPlugIn.getIcon()),
                redoPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(addNewFeaturesPlugIn, new String[] {MenuNames.EDIT},
                addNewFeaturesPlugIn.getName() + "...", false, null,
                AddNewFeaturesPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(editSelectedFeaturePlugIn, new String[] {MenuNames.EDIT},
                editSelectedFeaturePlugIn.getName(), false, null,
                EditSelectedFeaturePlugIn.createEnableCheck(workbenchContext));
        featureInstaller
        	.addMainMenuItemWithJava14Fix(selectFeaturesInFencePlugIn, new String[] {MenuNames.EDIT},
                        selectFeaturesInFencePlugIn.getName(), false, null,
                        SelectFeaturesInFencePlugIn
                                .createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(clearSelectionPlugIn, new String[] {MenuNames.EDIT},
                clearSelectionPlugIn.getName(),false, null, clearSelectionPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(cutSelectedItemsPlugIn, new String[] {MenuNames.EDIT},
                cutSelectedItemsPlugIn.getName(), false, null, cutSelectedItemsPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItem(copySelectedItemsPlugIn, new String[] {MenuNames.EDIT},
                copySelectedItemsPlugIn.getNameWithMnemonic(), false, null,
                CopySelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItem(pasteItemsPlugIn, new String[] {MenuNames.EDIT},
                pasteItemsPlugIn.getNameWithMnemonic(), false, null, PasteItemsPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(deleteSelectedItemsPlugIn, new String[] {MenuNames.EDIT},
                deleteSelectedItemsPlugIn.getName(), false, null,
                DeleteSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        //featureInstaller.addMenuSeparator(MenuNames.EDIT); // ===================
        /*//--[sstein 24 march 2007] moved to new customize menu 
        featureInstaller.addMainMenuItemWithJava14Fix(optionsPlugIn, new String[] {MenuNames.EDIT}, optionsPlugIn
                .getName()
                + "...", false, null, null);
        */
		//-- VIEW        
        editingPlugIn.createMainMenuItem(new String[] { MenuNames.VIEW}, GUIUtil
                .toSmallIcon(EditingPlugIn.ICON), workbenchContext);
        featureInstaller.addMainMenuItemWithJava14Fix(copyImagePlugIn, new String[]{MenuNames.VIEW},
                copyImagePlugIn.getName(), false, null, CopyImagePlugIn
                        .createEnableCheck(workbenchContext));        
        featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(featureInfoPlugIn, new String[] {MenuNames.VIEW},
                featureInfoPlugIn.getName(), false,GUIUtil
                        .toSmallIcon(FeatureInfoTool.ICON), FeatureInfoPlugIn
                        .createEnableCheck(workbenchContext));
        featureInstaller
        		.addMainMenuItemWithJava14Fix(
                        verticesInFencePlugIn,
                        new String[] {MenuNames.VIEW},
                        verticesInFencePlugIn.getName(),
						false,
                        null,
                        new MultiEnableCheck()
                                .add(
                                        checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(checkFactory.createFenceMustBeDrawnCheck()));
        featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
        featureInstaller.addMainMenuItemWithJava14Fix(zoomToFullExtentPlugIn, new String[]{MenuNames.VIEW},
                zoomToFullExtentPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(zoomToFullExtentPlugIn.getIcon()),
                zoomToFullExtentPlugIn.createEnableCheck(workbenchContext));
        featureInstaller
        			.addMainMenuItemWithJava14Fix(
                        zoomToFencePlugIn,
                        new String[] {MenuNames.VIEW},
                        zoomToFencePlugIn.getName(),
						false,
                        GUIUtil.toSmallIcon(zoomToFencePlugIn.getIcon()),
                        new MultiEnableCheck()
                                .add(
                                        checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(checkFactory.createFenceMustBeDrawnCheck()));
        featureInstaller.addMainMenuItemWithJava14Fix(zoomToSelectedItemsPlugIn, new String[] {MenuNames.VIEW},
                zoomToSelectedItemsPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(zoomToSelectedItemsPlugIn.getIcon()),
                ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(zoomToCoordinatePlugIn, new String[]{MenuNames.VIEW},
                zoomToCoordinatePlugIn.getName() + "...", false, null,
                zoomToCoordinatePlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(zoomPreviousPlugIn, new String[] {MenuNames.VIEW},
                zoomPreviousPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(zoomPreviousPlugIn.getIcon()),
                zoomPreviousPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItemWithJava14Fix(zoomNextPlugIn, new String[] {MenuNames.VIEW}, zoomNextPlugIn
                .getName(), false, GUIUtil.toSmallIcon(zoomNextPlugIn.getIcon()),
                zoomNextPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMenuSeparator(MenuNames.VIEW); // ===================
        featureInstaller
        	.addMainMenuItemWithJava14Fix(
                        scaleBarPlugIn,
                        new String[] { MenuNames.VIEW},
                        scaleBarPlugIn.getName(),
                        true,
                        null,
                        new MultiEnableCheck()
                                .add(
                                        checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(new EnableCheck() {

                                    public String check(JComponent component) {
                                        ((JCheckBoxMenuItem) component)
                                                .setSelected(ScaleBarRenderer
                                                        .isEnabled(workbenchContext
                                                                .getLayerViewPanel()));

                                        return null;
                                    }
                                }));
        featureInstaller.addMainMenuItemWithJava14Fix(toolTipsPlugIn,
                new String[] {MenuNames.VIEW}, toolTipsPlugIn.getName(), true, null,
                MapToolTipsPlugIn.createEnableCheck(workbenchContext));
        zoomBarPlugIn.createMainMenuItem(new String[] { MenuNames.VIEW}, null,
                workbenchContext);
        //-- LAYER       
        //-- [sstein: 23.02.2006 new sub method in VividJump]
        configLayer(workbenchContext, checkFactory, featureInstaller);
        
        //-- WINDOW
        /*
        featureInstaller.addMainMenuItemWithJava14Fix(optionsPlugIn, new String[] {MenuNames.WINDOW}, optionsPlugIn
                .getName()
                + "...", false, null, null);
        */
        featureInstaller.addMainMenuItemWithJava14Fix(outputWindowPlugIn, new String[] {MenuNames.WINDOW},
                outputWindowPlugIn.getName(), false, GUIUtil
                        .toSmallIcon(outputWindowPlugIn.getIcon()), null);        
        featureInstaller.addMainMenuItemWithJava14Fix(generateLogPlugIn, new String[] {MenuNames.WINDOW},
                generateLogPlugIn.getName() + "...", false, null, null);        
        featureInstaller.addMenuSeparator(MenuNames.WINDOW); // ===================
        
        featureInstaller.addMainMenuItemWithJava14Fix(cloneWindowPlugIn, new String[] {MenuNames.WINDOW},
                cloneWindowPlugIn.getName(), false, null, new EnableCheck() {

                    public String check(JComponent component) {
                        return (!(workbenchContext.getWorkbench().getFrame()
                                .getActiveInternalFrame() instanceof CloneableInternalFrame)) ? I18N.get("JUMPConfiguration.not-available-for-the-current-window")
                                : null;
                    }
                });
      
        featureInstaller.addMenuSeparator(MenuNames.WINDOW); // ===================

        //-- TOOLS
        //-- [sstein: 23.02.2006 new in VividJump]
        configToolsAnalysis(workbenchContext, checkFactory, featureInstaller);
        configToolsEdit(workbenchContext, checkFactory, featureInstaller);
        configToolsQA(workbenchContext, checkFactory, featureInstaller);        

        featureInstaller.addMainMenuItemWithJava14Fix(shortcutKeysPlugIn, new String[]{MenuNames.HELP},
                shortcutKeysPlugIn.getName() + "...", false, null, null);
        new FeatureInstaller(workbenchContext).addMainMenuItemWithJava14Fix(
                new AboutPlugIn(), new String[]{MenuNames.HELP}, I18N.get("JUMPConfiguration.about"), false, null, null);
    
        //-- CUSTOMIZE
        //-- [sstein: 24.03.2007 new menu]
        featureInstaller.addMainMenuItemWithJava14Fix(optionsPlugIn, new String[] {MenuNames.CUSTOMIZE}, optionsPlugIn
                .getName()
                + "...", false, null, null);
    }

    //==== [sstein: 23.02.2006] ====== 
    // this is a new method in VividJump (December 2005) to initialize 
    // plugins working on layers
    //================================
    //public static String MENU_LAYER = MenuNames.LAYER;

        private AddDatastoreLayerPlugIn addDatastoreLayerPlugIn = new AddDatastoreLayerPlugIn();
        private RunDatastoreQueryPlugIn runDatastoreQueryPlugIn = new RunDatastoreQueryPlugIn();
        private InstallDatastoreLayerRendererHintsPlugIn installDatastoreLayerRendererHintsPlugIn = new InstallDatastoreLayerRendererHintsPlugIn();
        private AddImageLayerPlugIn addImageLayerPlugIn = new AddImageLayerPlugIn();
        
    private void configLayer(final WorkbenchContext workbenchContext,
                final EnableCheckFactory checkFactory,
                FeatureInstaller featureInstaller) throws Exception {

      String MENU_LAYER = MenuNames.LAYER;
      featureInstaller.addLayerViewMenuItem(addNewLayerPlugIn, MENU_LAYER,
              addNewLayerPlugIn.getName());
      featureInstaller.addLayerViewMenuItem(addDatastoreLayerPlugIn, MENU_LAYER,
              addDatastoreLayerPlugIn.getName() + "...");
      featureInstaller.addLayerViewMenuItem(runDatastoreQueryPlugIn, MENU_LAYER,
              runDatastoreQueryPlugIn.getName() + "...");

        featureInstaller.addLayerViewMenuItem(addWMSQueryPlugIn, MENU_LAYER,
                addWMSQueryPlugIn.getName() + "...");
        
        featureInstaller.addLayerViewMenuItem(addImageLayerPlugIn, MENU_LAYER,
                addImageLayerPlugIn.getName() + "...");
        
        featureInstaller.addMainMenuItem(addNewCategoryPlugIn, MENU_LAYER,
                addNewCategoryPlugIn.getName(), null, addNewCategoryPlugIn
                        .createEnableCheck(workbenchContext));

        featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
        featureInstaller.addMainMenuItem(cutSelectedLayersPlugIn, MENU_LAYER,
                cutSelectedLayersPlugIn.getNameWithMnemonic(), null,
                cutSelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItem(copySelectedLayersPlugIn, MENU_LAYER,
                copySelectedLayersPlugIn.getNameWithMnemonic(), null,
                copySelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItem(pasteLayersPlugIn, MENU_LAYER,
                pasteLayersPlugIn.getNameWithMnemonic(), null,
                pasteLayersPlugIn.createEnableCheck(workbenchContext));

        featureInstaller.addMenuSeparator(MENU_LAYER); // ===================
        featureInstaller.addMainMenuItem(removeSelectedLayersPlugIn, MENU_LAYER,
                removeSelectedLayersPlugIn.getName(), null,
                removeSelectedLayersPlugIn.createEnableCheck(workbenchContext));
        featureInstaller.addMainMenuItem(removeSelectedCategoriesPlugIn,
                MENU_LAYER, removeSelectedCategoriesPlugIn.getName(), null,
                removeSelectedCategoriesPlugIn
                        .createEnableCheck(workbenchContext));
                }

    // MD - following is proposed new pattern for defining built-in menus

//[sstein, 15.07.2006] - don't initialize as "static" (!) class variables because it causes
//    					 that the menu names are not translated into the correct language.
//						 I have put them now into the method.    
//public static String MENU_TOOLS = MenuNames.TOOLS;
//public static String MENU_ANALYSIS = MenuNames.TOOLS_ANALYSIS;
//public static String[] MENU_TOOLS_ANALYSIS = new String[] { MENU_TOOLS, MENU_ANALYSIS};

    // these must be defined as instance vars for initialization to be performed
	private SpatialQueryPlugIn spatialQueryPlugIn = new SpatialQueryPlugIn();
    private AttributeQueryPlugIn attrQueryPlugIn = new AttributeQueryPlugIn();
	private SpatialJoinPlugIn spatialJoinPlugIn = new SpatialJoinPlugIn();
    private UnionPlugIn unionPlugIn = new UnionPlugIn();
    private UnionByAttributePlugIn unionByAttributePlugIn = new UnionByAttributePlugIn();
    private GeometryFunctionPlugIn geometryFunctionPlugIn = new GeometryFunctionPlugIn();
    private OverlayPlugIn overlayPlugIn = new OverlayPlugIn();
    private ConvexHullPlugIn convexHullPI = new ConvexHullPlugIn();
    private BufferPlugIn bufferPlugIn = new BufferPlugIn();
    private CalculateAreasAndLengthsPlugIn calculateAreasAndLengthsPlugIn = new CalculateAreasAndLengthsPlugIn();

    private void configToolsAnalysis(final WorkbenchContext workbenchContext,
                final EnableCheckFactory checkFactory,
                FeatureInstaller featureInstaller) throws Exception {

    String MENU_TOOLS = MenuNames.TOOLS;
    String MENU_ANALYSIS = MenuNames.TOOLS_ANALYSIS;
    String[] MENU_TOOLS_ANALYSIS = new String[] { MENU_TOOLS, MENU_ANALYSIS};
    	
	featureInstaller
	.addMainMenuItem(
			spatialQueryPlugIn,
			new String[] { MenuNames.TOOLS, MenuNames.TOOLS_QUERIES},
			spatialQueryPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(
					checkFactory
					.createWindowWithLayerNamePanelMustBeActiveCheck())
					.add(
							checkFactory
							.createAtLeastNLayersMustExistCheck(2)));
	
	featureInstaller
	        .addMainMenuItem(
	                attrQueryPlugIn,
	    			new String[] { MenuNames.TOOLS, MenuNames.TOOLS_QUERIES},
	                attrQueryPlugIn.getName() + "...",
	                false,
	                null,
	                new MultiEnableCheck()
	                        .add(
	                                checkFactory
	                                        .createWindowWithLayerNamePanelMustBeActiveCheck())
	                        .add(
	                                checkFactory
	                                        .createAtLeastNLayersMustExistCheck(1)));
    
	featureInstaller
	.addMainMenuItem(
			geometryFunctionPlugIn,
			MENU_TOOLS_ANALYSIS,
			geometryFunctionPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(
					checkFactory
					.createWindowWithLayerNamePanelMustBeActiveCheck())
					.add(
							checkFactory
							.createAtLeastNLayersMustExistCheck(1)));
 
	featureInstaller.addMainMenuItem(
            spatialJoinPlugIn,
            MENU_TOOLS_ANALYSIS,
            spatialJoinPlugIn.getName() + "...",
            false,
            null,
            new MultiEnableCheck()
                    .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                    .add(checkFactory.createAtLeastNLayersMustExistCheck(2)));
	//======================================
	//featureInstaller.addMenuSeparator(MENU_TOOLS_ANALYSIS);
	featureInstaller
	.addMainMenuItem(
			unionPlugIn,
			MENU_TOOLS_ANALYSIS, //maybe move to analysis join (but currently leave)
			unionPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(
					checkFactory
					.createWindowWithLayerNamePanelMustBeActiveCheck())
					.add(
							checkFactory
							.createAtLeastNLayersMustExistCheck(1)));
    featureInstaller
	.addMainMenuItem(
			unionByAttributePlugIn,
			MENU_TOOLS_ANALYSIS, //maybe move to analysis join (but currently leave)
			unionByAttributePlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(
					checkFactory
					.createWindowWithLayerNamePanelMustBeActiveCheck())
					.add(
							checkFactory
							.createAtLeastNLayersMustExistCheck(1)));
	featureInstaller
	.addMainMenuItem(
			bufferPlugIn,
			new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
			bufferPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(
					checkFactory
					.createWindowWithLayerNamePanelMustBeActiveCheck())
					.add(
							checkFactory
							.createAtLeastNLayersMustExistCheck(1)));

	featureInstaller
	.addMainMenuItem(
			convexHullPI,
	        new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
	        convexHullPI.getName() + "...",
	        false,
	        null, ConvexHullPlugIn.getEnableCheck(checkFactory)
		);
	
	
	//======================================
	//featureInstaller.addMenuSeparator(MENU_TOOLS_ANALYSIS);
	featureInstaller
	.addMainMenuItem(
			overlayPlugIn,
			MENU_TOOLS_ANALYSIS,
			overlayPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
			.add(checkFactory.createAtLeastNLayersMustExistCheck(2)));
	
	//======================================
	//featureInstaller.addMenuSeparator(MENU_TOOLS_ANALYSIS);
	
	featureInstaller.addMainMenuItem(calculateAreasAndLengthsPlugIn,
			new String[] { MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
			calculateAreasAndLengthsPlugIn.getName() + "...", false, null,
			calculateAreasAndLengthsPlugIn.createEnableCheck(workbenchContext));
	
	new WarpingPlugIn().initialize(new PlugInContext(workbenchContext,
			null, null, null, null));
	
	new AffineTransformPlugIn().initialize(new PlugInContext(
			workbenchContext, null, null, null, null));
	
	new RandomTrianglesPlugIn().initialize(new PlugInContext(
			workbenchContext, null, null, null, null));
	new RandomArrowsPlugIn().initialize(new PlugInContext(workbenchContext,
			null, null, null, null));
	
}


private ExtractSegmentsPlugIn extractSegmentsPlugIn = new ExtractSegmentsPlugIn();
private LineNoderPlugIn lineNoderPlugIn = new LineNoderPlugIn();
private PolygonizerPlugIn polygonizerPlugIn = new PolygonizerPlugIn();    
private PrecisionReducerPlugIn precisionReducerPlugIn = new PrecisionReducerPlugIn();
private AffineTransformationPlugIn affineTransPlugIn = new AffineTransformationPlugIn();

private void configToolsEdit(final WorkbenchContext workbenchContext,
            final EnableCheckFactory checkFactory,
            FeatureInstaller featureInstaller) throws Exception {

	String MENU_TOOLS = MenuNames.TOOLS;
	String MENU_EDIT = MenuNames.TOOLS_EDIT_GEOMETRY;
	String[] MENU_TOOLS_EDIT = new String[] { MENU_TOOLS, MENU_EDIT};

    featureInstaller.addMainMenuItem(extractSegmentsPlugIn,
            MENU_TOOLS_EDIT,
                extractSegmentsPlugIn.getName() + "...", false, null,
                extractSegmentsPlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMainMenuItem(lineNoderPlugIn,
            MENU_TOOLS_EDIT,
                lineNoderPlugIn.getName() + "...", false, null,
                lineNoderPlugIn.createEnableCheck(workbenchContext));

    featureInstaller.addMainMenuItem(polygonizerPlugIn,
            MENU_TOOLS_EDIT,
                polygonizerPlugIn.getName() + "...", false, null,
                polygonizerPlugIn.createEnableCheck(workbenchContext));
    
    featureInstaller.addMainMenuItem(precisionReducerPlugIn,
    		MENU_TOOLS_EDIT,
            precisionReducerPlugIn.getName() + "...", false, null,
            precisionReducerPlugIn.createEnableCheck(workbenchContext));
            
    featureInstaller.addMainMenuItem(affineTransPlugIn,
          MENU_TOOLS_EDIT,
          affineTransPlugIn.getName() + "...", false, null,
          affineTransPlugIn.createEnableCheck(workbenchContext));

}

private ValidateSelectedLayersPlugIn validateSelectedLayersPlugIn = new ValidateSelectedLayersPlugIn();
private LayerStatisticsPlugIn layerStatisticsPlugIn = new LayerStatisticsPlugIn();
private FeatureStatisticsPlugIn featureStatisticsPlugIn = new FeatureStatisticsPlugIn();
private DiffGeometryPlugIn diffGeometryPlugIn = new DiffGeometryPlugIn();

private void configToolsQA(final WorkbenchContext workbenchContext,
		final EnableCheckFactory checkFactory,
		FeatureInstaller featureInstaller) throws Exception {
	
	String MENU_TOOLS = MenuNames.TOOLS;
	String MENU_QA = MenuNames.TOOLS_QA;
	String[] MENU_TOOLS_QA= new String[] { MENU_TOOLS, MENU_QA};
	
	featureInstaller
	.addMainMenuItem(validateSelectedLayersPlugIn,
			MENU_TOOLS_QA,
			validateSelectedLayersPlugIn.getName() + "...",
			false,
			null,
			new MultiEnableCheck()
			.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
			.add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1)));
	featureInstaller
	.addMainMenuItem(layerStatisticsPlugIn,
			MENU_TOOLS_QA,
			layerStatisticsPlugIn.getName(),
			false,
			null,
			new MultiEnableCheck()
			.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
			.add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1)));
	featureInstaller
	.addMainMenuItem(featureStatisticsPlugIn,
			MENU_TOOLS_QA,
			featureStatisticsPlugIn.getName(),
			false,
			null,
			new MultiEnableCheck()
			.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
			.add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1)));
	
	featureInstaller.addMainMenuItem(diffGeometryPlugIn,
	            MENU_TOOLS_QA,
	            diffGeometryPlugIn.getName() + "...",
	            false,
	            null,
	            new MultiEnableCheck()
	                    .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
	                    .add(checkFactory.createAtLeastNLayersMustExistCheck(2)));

}

public void configureDatastores(final WorkbenchContext context) throws Exception {

	context.getRegistry().
    createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,new PostgisDataStoreDriver());

    // update exit handler
    final ApplicationExitHandler oldApplicationExitHandler = context
            .getWorkbench().getFrame().getApplicationExitHandler();
    context.getWorkbench().getFrame().setApplicationExitHandler(
            new ApplicationExitHandler() {
                public void exitApplication(JFrame mainFrame) {
                    try {
                        ConnectionManager.instance(context)
                                .closeConnections();
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
    }

    private QuasimodeTool add(CursorTool tool, WorkbenchContext context) {
        return context.getWorkbench().getFrame().getToolBar().addCursorTool(
                tool).getQuasimodeTool();
    }

    private void configureToolBar(final WorkbenchContext workbenchContext,
            EnableCheckFactory checkFactory) {    	
        WorkbenchFrame frame = workbenchContext.getWorkbench().getFrame();
        frame.getToolBar().addPlugIn(NewTaskPlugIn.getIcon(),
        		newTaskPlugIn,
        		NewTaskPlugIn.createEnableCheck(workbenchContext),
				workbenchContext);                
        frame.getToolBar().addPlugIn(LoadDatasetPlugIn.getIcon(),
        		loadDatasetPlugIn,
        		LoadDatasetPlugIn.createEnableCheck(workbenchContext),
				workbenchContext);        
        frame.getToolBar().addSeparator();        
        add(new ZoomTool(), workbenchContext);
        add(new PanTool(), workbenchContext);
        frame.getToolBar().addSeparator();
        frame.getToolBar().addPlugIn(zoomToFullExtentPlugIn.getIcon(),
                zoomToFullExtentPlugIn,
                zoomToFullExtentPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addPlugIn(zoomToSelectedItemsPlugIn.getIcon(),
                zoomToSelectedItemsPlugIn,
                ZoomToSelectedItemsPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame
                .getToolBar()
                .addPlugIn(
                        zoomToFencePlugIn.getIcon(),
                        zoomToFencePlugIn,
                        new MultiEnableCheck()
                                .add(
                                        checkFactory
                                                .createWindowWithLayerViewPanelMustBeActiveCheck())
                                .add(checkFactory.createFenceMustBeDrawnCheck()),
                        workbenchContext);
        frame.getToolBar().addPlugIn(zoomPreviousPlugIn.getIcon(),
                zoomPreviousPlugIn,
                zoomPreviousPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addPlugIn(zoomNextPlugIn.getIcon(), zoomNextPlugIn,
                zoomNextPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addPlugIn(changeStylesPlugIn.getIcon(),
                changeStylesPlugIn,
                changeStylesPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addPlugIn(viewAttributesPlugIn.getIcon(),
                viewAttributesPlugIn,
                viewAttributesPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addSeparator();

        //Null out the quasimodes for [Ctrl] because the Select tools will
        // handle that case. [Jon Aquino]
        add(new QuasimodeTool(new SelectFeaturesTool()).add(
                new QuasimodeTool.ModifierKeySpec(true, false, false), null),
                workbenchContext);
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
        add(new MeasureTool(), workbenchContext);
        frame.getToolBar().addSeparator();
        frame.getToolBar().addPlugIn(undoPlugIn.getIcon(), undoPlugIn,
                undoPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addPlugIn(redoPlugIn.getIcon(), redoPlugIn,
                redoPlugIn.createEnableCheck(workbenchContext),
                workbenchContext);
        frame.getToolBar().addSeparator();
        workbenchContext.getWorkbench().getFrame().getOutputFrame().setButton(
                frame.getToolBar().addPlugIn(outputWindowPlugIn.getIcon(),
                        outputWindowPlugIn, new MultiEnableCheck(),
                        workbenchContext));
        //Last of all, add a separator because some plug-ins may add
        // CursorTools.
        //[Jon Aquino]
        frame.getToolBar().addSeparator();
    }

    private void configureEditingButton(final WorkbenchContext workbenchContext) {
        final JToggleButton toggleButton = new JToggleButton();
        workbenchContext.getWorkbench().getFrame().getToolBar().add(
                toggleButton,
                editingPlugIn.getName(),
                EditingPlugIn.ICON,
                AbstractPlugIn.toActionListener(editingPlugIn,
                        workbenchContext, new TaskMonitorManager()), null);
        workbenchContext.getWorkbench().getFrame().addComponentListener(
                new ComponentAdapter() {

                    public void componentShown(ComponentEvent e) {
                        //Can't #getToolbox before Workbench is thrown.
                        // Otherwise, get
                        //IllegalComponentStateException. Thus, do it inside
                        // #componentShown. [Jon Aquino]
                        editingPlugIn.getToolbox(workbenchContext)
                                .addComponentListener(new ComponentAdapter() {

                                    //There are other ways to show/hide the
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
     * Call each PlugIn's #initialize() method. Uses reflection to build a list
     * of plug-ins.
     *
     * @param workbenchContext
     *                   Description of the Parameter
     * @exception Exception
     *                        Description of the Exception
     */
    private void initializeBuiltInPlugIns(WorkbenchContext workbenchContext)
            throws Exception {
        Field[] fields = getClass().getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            Object field = null;

            try {
                field = fields[i].get(this);
            } catch (IllegalAccessException e) {
                Assert.shouldNeverReachHere();
            }

            if (!(field instanceof PlugIn)) {
                continue;
            }

            PlugIn plugIn = (PlugIn) field;
            plugIn.initialize(new PlugInContext(workbenchContext, null, null,
                    null, null));
        }
    }
}
