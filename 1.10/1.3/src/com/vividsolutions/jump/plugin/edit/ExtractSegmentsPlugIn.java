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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import java.util.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.geom.*;
import com.vividsolutions.jump.task.*;

public class ExtractSegmentsPlugIn
    extends ThreadedBasePlugIn
{

  private static Collection toLineStrings(Collection segments)
  {
    GeometryFactory fact = new GeometryFactory();
    List lineStringList = new ArrayList();
    for (Iterator i = segments.iterator(); i.hasNext();) {
      LineSegment seg = (LineSegment) i.next();
      LineString ls = LineSegmentUtil.asGeometry(fact, seg);
      lineStringList.add(ls);
    }
    return lineStringList;
  }
  
  private static Collection toMergedLineStrings(Collection segments)
  {
    GeometryFactory fact = new GeometryFactory();
    LineMerger lineMerger = new LineMerger(); 
    for (Iterator i = segments.iterator(); i.hasNext();) {
      LineSegment seg = (LineSegment) i.next();
      lineMerger.add(LineSegmentUtil.asGeometry(fact, seg));
    }
    return lineMerger.getMergedLineStrings();
  }

  private MultiInputDialog dialog;
  private String layerName;
  private boolean uniqueSegmentsOnly;
  private boolean mergeResultingSegments;
  private int inputEdgeCount = 0;
  private int uniqueSegmentCount = 0;

  public ExtractSegmentsPlugIn() { }

  /**
   * Returns a very brief description of this task.
   * @return the name of this task
   */
  public String getName() { return I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extract-Segments"); }

  public void initialize(PlugInContext context) throws Exception
  {
      	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
  	        this,								//exe
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT}, 	//menu path
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

    monitor.report(I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracting-Segments"));

    Layer layer = dialog.getLayer(LAYER);
    FeatureCollection lineFC = layer.getFeatureCollectionWrapper();
    inputEdgeCount = lineFC.size();

    //UniqueSegmentsExtracter extracter = new UniqueSegmentsExtracter(monitor);
    SegmentsExtracter extracter = new SegmentsExtracter(monitor);
    extracter.add(lineFC);
    Collection uniqueFSList = uniqueSegmentsOnly ? extracter.getSegments(1,1)
                                                 : extracter.getSegments();
    uniqueSegmentCount = uniqueFSList.size();
    Collection linestringList = mergeResultingSegments ? toMergedLineStrings(uniqueFSList)
                                                       : toLineStrings(uniqueFSList);

    if (monitor.isCancelRequested()) return;
    createLayers(context, linestringList);
  }

  private void createLayers(PlugInContext context, Collection linestringList)
         throws Exception
  {

    FeatureCollection lineStringFC = FeatureDatasetFactory.createFromGeometry(linestringList);
    context.addLayer(
        StandardCategoryNames.RESULT,
        layerName + " " + I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracted-Segs"),
        lineStringFC);

    createOutput(context);

  }

  private void createOutput(PlugInContext context)
  {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1,
    		I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extract-Segments"));
    context.getOutputFrame().addField(I18N.get("ui.MenuNames.LAYER")+ ":", layerName);


    context.getOutputFrame().addText(" ");
    context.getOutputFrame().addField(
                                      I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Number-of-unique-segments-extracted"), "" + uniqueSegmentCount);
  }

  private final static String LAYER = I18N.get("ui.MenuNames.LAYER");

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("ExtractSegments.png")));
    dialog.setSideBarDescription(I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracts-all-unique-line-segments-from-a-dataset"));
    JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
    JCheckBox oneTimeCheckBox = dialog.addCheckBox(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Remove-doubled-segments"),false);
    JCheckBox mergeCheckBox = dialog.addCheckBox(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Merge-resulting-segments"),false);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    Layer layer = dialog.getLayer(LAYER);
    layerName = layer.getName();
    uniqueSegmentsOnly = dialog.getBoolean(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Remove-doubled-segments"));
    mergeResultingSegments = dialog.getBoolean(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Merge-resulting-segments"));
  }
}
