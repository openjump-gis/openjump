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

import javax.swing.JPopupMenu;

import org.openjump.core.ccordsys.srid.EnsureAllLayersHaveSRIDStylePlugIn;
import org.openjump.core.ui.plugin.customize.BeanToolsPlugIn;
import org.openjump.core.ui.plugin.edit.ReplicateSelectedItemsPlugIn;
import org.openjump.core.ui.plugin.edit.SelectAllLayerItemsPlugIn;
import org.openjump.core.ui.plugin.edit.SelectByTypePlugIn;
import org.openjump.core.ui.plugin.edit.SelectItemsByCircleFromSelectedLayersPlugIn;
import org.openjump.core.ui.plugin.edit.SelectItemsByFenceFromSelectedLayersPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.ConstrainedMoveVertexPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawCircleWithGivenRadiusPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedArcPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedCirclePlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedLineStringPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.DrawConstrainedPolygonPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.RotateSelectedItemPlugIn;
import org.openjump.core.ui.plugin.edittoolbox.SelectOneItemPlugIn;
import org.openjump.core.ui.plugin.file.SaveImageAsSVGPlugIn;
import org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn;
import org.openjump.core.ui.plugin.layer.ChangeLayerableNamePlugIn;
import org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn;
import org.openjump.core.ui.plugin.layer.ToggleVisiblityPlugIn;
import org.openjump.core.ui.plugin.mousemenu.EditSelectedSidePlugIn;
import org.openjump.core.ui.plugin.mousemenu.MoveAlongAnglePlugIn;
import org.openjump.core.ui.plugin.mousemenu.RotatePlugIn;
import org.openjump.core.ui.plugin.mousemenu.SaveDatasetsPlugIn;
import org.openjump.core.ui.plugin.queries.SimpleQueryPlugIn;
import org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn;
import org.openjump.core.ui.plugin.tools.ConvexHullPlugIn;
import org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn;
import org.openjump.core.ui.plugin.tools.CutPolygonPlugIn;
import org.openjump.core.ui.plugin.tools.DeleteEmptyGeometriesPlugIn;
import org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn;
import org.openjump.core.ui.plugin.tools.JoinWithArcPlugIn;
import org.openjump.core.ui.plugin.tools.LineSimplifyJTS15AlgorithmPlugIn;
import org.openjump.core.ui.plugin.tools.MeasureM_FPlugIn;
import org.openjump.core.ui.plugin.tools.MergeTwoSelectedPolygonsPlugIn;
import org.openjump.core.ui.plugin.tools.ReducePointsISAPlugIn;
import org.openjump.core.ui.plugin.view.InstallKeyPanPlugIn;
import org.openjump.core.ui.plugin.view.MapToolTipPlugIn;
import org.openjump.core.ui.plugin.view.ShowFullPathPlugIn;
import org.openjump.core.ui.plugin.view.ShowScalePlugIn;
import org.openjump.core.ui.plugin.view.ZoomToScalePlugIn;
import org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn;
import org.openjump.core.ui.style.decoration.ArrowLineStringMiddlepointStyle;
import org.openjump.sigle.plugin.geoprocessing.layers.SpatialJoinPlugIn;
import org.openjump.sigle.plugin.geoprocessing.oneLayer.topology.PlanarGraphPlugIn;
import org.openjump.sigle.plugin.joinTable.JoinTablePlugIn;
import org.openjump.sigle.plugin.replace.ReplaceValuePlugIn;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.BeanShellPlugIn;

import de.fho.jump.pirol.plugins.EditAttributeByFormula.EditAttributeByFormulaPlugIn;
import de.latlon.deejump.plugin.SaveLegendPlugIn;
import de.latlon.deejump.plugin.manager.ExtensionManagerPlugIn;
import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;

/**
 * @description:
 *   This class loads all openjump plugins.
 *   The method loadOpenJumpPlugIns() is called from 
 *   com.vividsolutions.jump.workbench.JUMPConfiguaration. 

 * @author sstein
 *
 */
public class OpenJumpConfiguration{

	public static void loadOpenJumpPlugIns(final WorkbenchContext workbenchContext)throws Exception {
		
		
		/*-----------------------------------------------
		 *  add here first the field which holds the plugin
		 *  and afterwards initialize it for the menu
		 *-----------------------------------------------*/
		
		/***********************
		 *  menu FILE
		 **********************/
		SaveImageAsSVGPlugIn imageSvgPlugin= new SaveImageAsSVGPlugIn();
		imageSvgPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		/***********************
		 *  menu EDIT
		 **********************/
		SelectItemsByFenceFromSelectedLayersPlugIn selectItemsFromLayersPlugIn = new SelectItemsByFenceFromSelectedLayersPlugIn();
		selectItemsFromLayersPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		SelectItemsByCircleFromSelectedLayersPlugIn selectItemsFromCirclePlugIn = new SelectItemsByCircleFromSelectedLayersPlugIn();
		selectItemsFromCirclePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		SelectAllLayerItemsPlugIn selectAllLayerItemsPlugIn = new SelectAllLayerItemsPlugIn();
		selectAllLayerItemsPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		ReplicateSelectedItemsPlugIn replicatePlugIn = new ReplicateSelectedItemsPlugIn();
		replicatePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		SelectByTypePlugIn mySelectByGeomTypePlugIn = new SelectByTypePlugIn();
		mySelectByGeomTypePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));		
		
		/***********************
		 *  menu VIEW
		 **********************/
		
		ZoomToWMSPlugIn myZoomToWMSPlugIn = new ZoomToWMSPlugIn();
		myZoomToWMSPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		ZoomToScalePlugIn myZoomToScalePlugIn = new ZoomToScalePlugIn();
		myZoomToScalePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		ShowScalePlugIn myShowScalePlugIn = new ShowScalePlugIn();
		myShowScalePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
				
		MapToolTipPlugIn myMapTipPlugIn= new MapToolTipPlugIn();
		myMapTipPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
			
		//-- deeJUMP function by LAT/LON [01.08.2006 sstein]		
		LayerStyle2SLDPlugIn mySytle2SLDplugIn= new LayerStyle2SLDPlugIn();
		mySytle2SLDplugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		//-- to install in Toolbar
		//mySytle2SLDplugIn.install(new PlugInContext(workbenchContext, null, null, null, null));

		//--this caused problems with the postgis plugin [sstein]
		//  TODO: the problem has been solved (using try/catch) but still class has to be
		//        changed using LayerListener LayerEventType.ADDED event instead of 
		//		  layerSelectionChanged() from LayerNamePanelListener
		ShowFullPathPlugIn myFullPathPlugin = new ShowFullPathPlugIn();
		myFullPathPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		
		/***********************
		 *  menu LAYER
		 **********************/
		
		ToggleVisiblityPlugIn myToggleVisPlugIn = new ToggleVisiblityPlugIn();
		myToggleVisPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		AddSIDLayerPlugIn myMrSIDPlugIn= new AddSIDLayerPlugIn();
		myMrSIDPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		ChangeSRIDPlugIn myChangeSRIDPlugIn= new ChangeSRIDPlugIn();
		myChangeSRIDPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		
		/***********************
		 *  menu TOOLS
		 **********************/
				
		/**** ANALYSIS ****/
		JoinAttributesSpatiallyPlugIn mySpatialJoin = new JoinAttributesSpatiallyPlugIn();
		mySpatialJoin.initialize(new PlugInContext(workbenchContext, null, null, null, null));	

		//-- SIGLE PlugIn
		PlanarGraphPlugIn coveragePlugIn = new PlanarGraphPlugIn();
		coveragePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/**** GENERATE ****/
		ConvexHullPlugIn myConvHullPlugIn = new ConvexHullPlugIn();
		myConvHullPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		CreateThiessenPolygonsPlugIn myThiessenPlugin = new CreateThiessenPolygonsPlugIn();
		myThiessenPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));				
		
		/**** QUERY ****/
		SimpleQueryPlugIn mySimpleQueryPlugIn = new SimpleQueryPlugIn();
		mySimpleQueryPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/**** QA ****/
		DeleteEmptyGeometriesPlugIn myDelGeomPlugin= new DeleteEmptyGeometriesPlugIn(); 
		myDelGeomPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
	
		/**** EDIT_GEOMETRY ****/		
		JoinWithArcPlugIn myJoinWithArcPlugIn= new JoinWithArcPlugIn();
		myJoinWithArcPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		BlendLineStringsPlugIn myLSBlender= new BlendLineStringsPlugIn();
		myLSBlender.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		MergeTwoSelectedPolygonsPlugIn twopolymerger = new MergeTwoSelectedPolygonsPlugIn();
		twopolymerger.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		CutPolygonPlugIn cutpoly = new CutPolygonPlugIn();
		cutpoly.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/**** EDIT_ATTIBUTES *****/
		ReplaceValuePlugIn myRepVal = new ReplaceValuePlugIn();
		myRepVal.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		EditAttributeByFormulaPlugIn formulaEdit = new EditAttributeByFormulaPlugIn();
		formulaEdit.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		SpatialJoinPlugIn spatialJoinPlugIn = new SpatialJoinPlugIn();
		spatialJoinPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/**** GENERALIZATION ****/
		ReducePointsISAPlugIn mySimplifyISA = new ReducePointsISAPlugIn();
		mySimplifyISA.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		LineSimplifyJTS15AlgorithmPlugIn jtsSimplifier = new LineSimplifyJTS15AlgorithmPlugIn();
		jtsSimplifier.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/**** tools main ****/
		
		//-- [sstein] do this to avoid that the programming menu is created after 
		//   MeasureM_FPlugIn is added to the tools menu
        // [Michael Michaud 2007-03-23] : put programming plugins in MenuNames.CUSTOMIZE menu
		/*
        PlugInContext pc = new PlugInContext(workbenchContext, null, null, null, null);
		FeatureInstaller fi = pc.getFeatureInstaller();
		JMenu menuTools = fi.menuBarMenu(MenuNames.TOOLS);
		fi.createMenusIfNecessary(menuTools, new String[]{MenuNames.TOOLS_PROGRAMMING});
        */
        
		//-- deeJUMP function by LAT/LON [05.08.2006 sstein]
        // [Michael Michaud 2007-03-23] move the plugin to the CUSTOMIZE menu (see here after)
		/*
        ExtensionManagerPlugIn extensionManagerPlugIn = new ExtensionManagerPlugIn();
		extensionManagerPlugIn.install(new PlugInContext(workbenchContext, null, null, null, null));
        */
        
		MeasureM_FPlugIn myFeetPlugIn = new MeasureM_FPlugIn();
		myFeetPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		/***********************
		 *  menu vector geoprocessing
		 *  previously used by Projet-sigle.org   
		 ***********************/
				
		//-- Two layers 
				
		//-- One Layer
		
		//-- Topology

        /***********************
		 *  menu CUSTOMIZE [added byte Michael Michaud on 2007-03-04]
		 **********************/
        //-- deeJUMP function by LAT/LON [05.08.2006 sstein]	
		ExtensionManagerPlugIn extensionManagerPlugIn = new ExtensionManagerPlugIn();
		extensionManagerPlugIn.install(new PlugInContext(workbenchContext, null, null, null, null));
        
        //-- [michael michaud] move from JUMPConfiguration
        BeanShellPlugIn beanShellPlugIn = new BeanShellPlugIn();
        beanShellPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
        
        //-- [michael michaud] add Larry's BeanToolsPlugIn
		BeanToolsPlugIn beanTools = new BeanToolsPlugIn();
		beanTools.initialize(new PlugInContext(workbenchContext, null, null, null, null));
        
		/***********************
		 *  menu WINDOW
		 **********************/

		/***********************
		 *  menu HELP
		 **********************/

		/***********************
		 *  Right click menus
		 **********************/		
		JPopupMenu popupMenu = LayerViewPanel.popupMenu();
		popupMenu.addSeparator();        

		MoveAlongAnglePlugIn myMoveAlongAnglePlugin = new MoveAlongAnglePlugIn();
		myMoveAlongAnglePlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		RotatePlugIn myRotatePlugin = new RotatePlugIn();
		myRotatePlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		EditSelectedSidePlugIn myEditSidePlugin = new EditSelectedSidePlugIn();
		myEditSidePlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
				
		SaveDatasetsPlugIn mySaveDataSetPlugIn = new SaveDatasetsPlugIn();
		mySaveDataSetPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		//-- deeJUMP plugin
	  	SaveLegendPlugIn saveLegend = new SaveLegendPlugIn();
	  	saveLegend.initialize(new PlugInContext(workbenchContext, null, null, null, null));
        
	  	//-- SIGLE plugin
		JoinTablePlugIn joinTablePlugIn = new JoinTablePlugIn();
		joinTablePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		ChangeLayerableNamePlugIn changeLayerableNamePlugIn = new ChangeLayerableNamePlugIn();
		changeLayerableNamePlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		/***********************
		 *  EDITing toolbox
		 **********************/

		DrawConstrainedPolygonPlugIn myConstrainedPolygonPlugIn = new DrawConstrainedPolygonPlugIn();
		myConstrainedPolygonPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));

		DrawConstrainedLineStringPlugIn myConstrainedLSPlugIn = new DrawConstrainedLineStringPlugIn();
		myConstrainedLSPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		DrawConstrainedCirclePlugIn myConstrainedCPlugIn = new DrawConstrainedCirclePlugIn();
		myConstrainedCPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		DrawConstrainedArcPlugIn myConstrainedArcPlugIn = new DrawConstrainedArcPlugIn();
		myConstrainedArcPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
				
		ConstrainedMoveVertexPlugIn myCMVPlugIn = new ConstrainedMoveVertexPlugIn();
		myCMVPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		RotateSelectedItemPlugIn myRotateSIPlugIn = new RotateSelectedItemPlugIn();
		myRotateSIPlugIn.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		SelectOneItemPlugIn mySelectOnePlugin= new SelectOneItemPlugIn();
		mySelectOnePlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		DrawCircleWithGivenRadiusPlugIn drawCirclePlugin = new DrawCircleWithGivenRadiusPlugIn();
		drawCirclePlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		//--  now initialized in #EditingPlugIn.java to fill toolbox
		/*
		ScaleSelectedItemsPlugIn myScaleItemsPlugin = new ScaleSelectedItemsPlugIn();
		myScaleItemsPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		*/		
		
		/***********************
		 *  others
		 **********************/
		
		// takes care of keyboard navigation
		new InstallKeyPanPlugIn().initialize( new PlugInContext(workbenchContext, null, null, null, null) );
			
		//-- enables to store the SRID = EPSG code as style for every Layer
		//   since it is stored as style it should be saved in the project file 
		EnsureAllLayersHaveSRIDStylePlugIn ensureLayerSRIDPlugin = new EnsureAllLayersHaveSRIDStylePlugIn();
		ensureLayerSRIDPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		
		/***********************
		 *  Decoration
		 ***********************/
		
		workbenchContext.getWorkbench().getFrame().addChoosableStyleClass(ArrowLineStringMiddlepointStyle.NarrowSolidMiddle.class);
		
		/***********************
		 *  Set Defaults
		 ***********************/
		//-- disable drawing of invalid polygons by default (can be changed during work in EditOptionsPanel)
		workbenchContext.getBlackboard().put(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, true);
		
		/***********************
		 *  testing
		 **********************/
		/*
		ProjectionPlugIn projectionPlugin = new ProjectionPlugIn();
		projectionPlugin.initialize(new PlugInContext(workbenchContext, null, null, null, null));
		*/

	}
}
