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
package com.vividsolutions.jump.plugin.edit;


import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import java.util.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.polygonize.*;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.ui.*;

public class LineNoderPlugIn
    extends ThreadedBasePlugIn
{
  private boolean useSelected = false;

  private MultiInputDialog dialog;
  private String layerName;

  private GeometryFactory fact = new GeometryFactory();

  public LineNoderPlugIn() { }

  /**
   * Returns a very brief description of this task.
   * @return the name of this task
   */  
  public String getName() { return I18N.get("jump.plugin.edit.LineNoderPlugIn.Node-Lines"); }
  
  public void initialize(PlugInContext context) throws Exception
  {
      	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
  	        this,								//exe
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY}, 	//menu path
              this.getName() + "...", //name methode .getName recieved by AbstractPlugIn 
              false,			//checkbox
              null,			//icon
              createEnableCheck(context.getWorkbenchContext())); //enable check  
  }
  
  public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
      return new MultiEnableCheck()
          .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
          .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {
    dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
       throws Exception
  {
    monitor.allowCancellationRequests();

    Polygonizer polygonizer = new Polygonizer();
//    polygonizer.setSplitLineStrings(splitLineStrings);

    monitor.report(I18N.get("jump.plugin.edit.LineNoderPlugIn.Noding"));

    Layer layer = dialog.getLayer(SRC_LAYER);

    Collection inputFeatures = getFeaturesToProcess(layer, context);

    Collection lines = getLines(inputFeatures);

    monitor.report(I18N.get("jump.plugin.edit.LineNoderPlugIn.Noding-input-lines"));
    Geometry nodedGeom = nodeLines((List) lines);
    Collection nodedLines = toLines(nodedGeom);

    if (monitor.isCancelRequested()) return;
    createLayer(context, nodedLines);
  }

  private Collection getFeaturesToProcess(Layer lyr, PlugInContext context){
    if (useSelected)
      return context.getLayerViewPanel()
                        .getSelectionManager().getFeaturesWithSelectedItems(lyr);
    return lyr.getFeatureCollectionWrapper().getFeatures();
  }

  private Collection getLines(Collection inputFeatures)
  {
    List linesList = new ArrayList();
    LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
    for (Iterator i = inputFeatures.iterator(); i.hasNext(); ) {
      Feature f = (Feature) i.next();
      Geometry g = f.getGeometry();
      g.apply(lineFilter);
    }
    return linesList;
  }

  /**
   * Nodes a collection of linestrings.
   * Noding is done via JTS union, which is reasonably effective but
   * may exhibit robustness failures.
   *
   * @param lines the linear geometries to node
   * @return a collection of linear geometries, noded together
   */
  private Geometry nodeLines(Collection lines)
  {
    Geometry linesGeom = fact.createMultiLineString(fact.toLineStringArray(lines));

    Geometry unionInput  = fact.createMultiLineString(null);
    // force the unionInput to be non-empty if possible, to ensure union is not optimized away
    Geometry minLine = extractPoint(lines);
    if (minLine != null)
      unionInput = minLine;

    Geometry noded = linesGeom.union(unionInput);
    return noded;
  }

  private static List toLines(Geometry geom)
  {
    List linesList = new ArrayList();
    LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
    geom.apply(lineFilter);
    return linesList;
  }

  private Geometry extractPoint(Collection lines)
  {
    int minPts = Integer.MAX_VALUE;
    Geometry point = null;
    // extract first point from first non-empty geometry
    for (Iterator i = lines.iterator(); i.hasNext(); ) {
      Geometry g = (Geometry) i.next();
      if (! g.isEmpty()) {
        Coordinate p = g.getCoordinate();
        point = g.getFactory().createPoint(p);
      }
    }
    return point;
  }

  private void createLayer(PlugInContext context, Collection nodedLines)
         throws Exception
  {
    FeatureCollection polyFC = FeatureDatasetFactory.createFromGeometry(nodedLines);
    context.addLayer(
        StandardCategoryNames.RESULT,
        layerName + " " +I18N.get("jump.plugin.edit.LineNoderPlugIn.Noded-Lines"),
        polyFC);
  }

  private final static String SRC_LAYER = I18N.get("jump.plugin.edit.LineNoderPlugIn.Line-Layer");
  private final static String SELECTED_ONLY = GenericNames.USE_SELECTED_FEATURES_ONLY;
  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("Polygonize.png")));
    dialog.setSideBarDescription(I18N.get("jump.plugin.edit.LineNoderPlugIn.Nodes-the-lines-in-a-layer"));
    String fieldName = SRC_LAYER;
    JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
    dialog.addCheckBox(SELECTED_ONLY, useSelected);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    Layer layer = dialog.getLayer(SRC_LAYER);
    layerName = layer.getName();
    useSelected = dialog.getBoolean(SELECTED_ONLY);
  }
}
