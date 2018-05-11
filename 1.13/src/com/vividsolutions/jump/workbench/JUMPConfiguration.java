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

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.openjump.OpenJumpConfiguration;
import org.openjump.core.ui.plugin.layer.LayerableStylePlugIn;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureOptionsPanel;
import org.openjump.core.ui.plugin.tools.AdvancedMeasureTool;
import org.openjump.core.ui.plugin.tools.ZoomRealtimeTool;
import org.openjump.core.ui.plugin.view.SuperZoomPanTool;

import com.vividsolutions.jts.util.Assert;
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
import com.vividsolutions.jump.workbench.ui.plugin.ClearSelectionPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.NewTaskPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.OutputWindowPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.RedoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.UndoPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.ViewAttributesPlugIn;
import com.vividsolutions.jump.workbench.ui.renderer.LayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.WmsLayerRendererFactory;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.CircleLineStringEndpointStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.MetricsLineStringSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexIndexLineSegmentStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexXYLineSegmentStyle;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomNextPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomPreviousPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFencePlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToFullExtentPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;


/**
 * Initializes the Workbench with various menus and cursor tools. Accesses the
 * Workbench structure through a WorkbenchContext.
 */
public class JUMPConfiguration implements Setup {

  /**
   * Built-in plugins must be defined as instance variables, since they are
   * located for iniatialization via reflection on this class
   */

  private ClearSelectionPlugIn clearSelectionPlugIn = new ClearSelectionPlugIn();

  private EditingPlugIn editingPlugIn = EditingPlugIn.getInstance();

  private NewTaskPlugIn newTaskPlugIn = new NewTaskPlugIn();

  private LayerableStylePlugIn changeStylesPlugIn = new LayerableStylePlugIn();

  private UndoPlugIn undoPlugIn = new UndoPlugIn();

  private RedoPlugIn redoPlugIn = new RedoPlugIn();

  private ViewAttributesPlugIn viewAttributesPlugIn = new ViewAttributesPlugIn();

  private OutputWindowPlugIn outputWindowPlugIn = new OutputWindowPlugIn();

  private ZoomNextPlugIn zoomNextPlugIn = new ZoomNextPlugIn();

  private ZoomPreviousPlugIn zoomPreviousPlugIn = new ZoomPreviousPlugIn();

  private ZoomToFencePlugIn zoomToFencePlugIn = new ZoomToFencePlugIn();

  private ZoomToFullExtentPlugIn zoomToFullExtentPlugIn = new ZoomToFullExtentPlugIn();

  private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn = new ZoomToSelectedItemsPlugIn();


  // ////////////////////////////////////////////////////////////////////
  public void setup(WorkbenchContext workbenchContext) throws Exception {

    configureStyles(workbenchContext);

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
  }

  private void configureWMSQueryNamePopupMenu(
      final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
    JPopupMenu wmsLayerNamePopupMenu = workbenchContext.getWorkbench()
        .getFrame().getWMSLayerNamePopupMenu();
  }

  private void configureAttributePopupMenu(
      final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
  }

  private void configureLayerPopupMenu(final WorkbenchContext workbenchContext,
      FeatureInstaller featureInstaller, EnableCheckFactory checkFactory) {
  }

  private void configureLayerViewPanelPopupMenu(
      WorkbenchContext workbenchContext, EnableCheckFactory checkFactory,
      FeatureInstaller featureInstaller) {
  }

  private void configureMainMenus(final WorkbenchContext workbenchContext,
      final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
      throws Exception {

    /**
     * FILE ===================================================================
     */
    String[] fileMenuPath = new String[] { MenuNames.FILE };

    // add basic default entries
    featureInstaller.addMenuSeparator(fileMenuPath);
    // menu exit item
    workbenchContext.getWorkbench().getFrame().new ExitPlugin()
        .initialize(workbenchContext.createPlugInContext());

    /**
     * LAYER ==================================================================
     */
    configLayer(workbenchContext, checkFactory, featureInstaller);
  }


  private void configLayer(final WorkbenchContext workbenchContext,
      final EnableCheckFactory checkFactory, FeatureInstaller featureInstaller)
      throws Exception {
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
        return I18N.get("JUMPConfiguration.fence");
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
