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

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jump.geom.*;
import com.vividsolutions.jump.task.*;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

public class ExtractSegmentsPlugIn extends AbstractThreadedUiPlugIn {
    
  private final static String LAYER = I18N.get("ui.MenuNames.LAYER");

  private static FeatureCollection toLineStrings(Collection segments) {
    FeatureSchema schema = new FeatureSchema();
    schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    GeometryFactory fact = new GeometryFactory();
    //List lineStringList = new ArrayList();
    FeatureDataset dataset = new FeatureDataset(schema);
    for (Iterator i = segments.iterator(); i.hasNext();) {
      LineSegment seg = (LineSegment) i.next();
      LineString ls = LineSegmentUtil.asGeometry(fact, seg);
      //lineStringList.add(ls);
      BasicFeature f = new BasicFeature(schema);
      f.setGeometry(ls);
      dataset.add(f);
    }
    return dataset;
  }

  private static FeatureCollection toLineStrings(Collection segments, Map<LineSegment,List<Feature>> map) {
    assert map != null;
    assert map.size() > 0 : "no segment/feature map";
    assert map.get(map.keySet().iterator().next()) != null : "first segment does not map any feature";
    assert map.get(map.keySet().iterator().next()).size() > 0 : "first segment does not map any feature";
    FeatureSchema schema = map.get(map.keySet().iterator().next()).get(0).getSchema();
    GeometryFactory fact = new GeometryFactory();
    List lineStringList = new ArrayList();
    FeatureDataset dataset = new FeatureDataset(schema);
    for (Iterator i = segments.iterator(); i.hasNext();) {
      LineSegment seg = (LineSegment) i.next();
      List<Feature> features = map.get(seg);
      LineString ls = LineSegmentUtil.asGeometry(fact, seg);
      for (Feature f : features) {
          Feature bf = (Feature)f.clone(false);
          bf.setGeometry(ls);
          dataset.add(bf);
      }
      //lineStringList.add(ls);
    }
    return dataset;
  }
  
  private static FeatureCollection toMergedLineStrings(Collection segments) {
    FeatureSchema schema = new FeatureSchema();
    schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
    GeometryFactory fact = new GeometryFactory();
    LineMerger lineMerger = new LineMerger(); 
    for (Iterator i = segments.iterator(); i.hasNext();) {
      LineSegment seg = (LineSegment) i.next();
      lineMerger.add(LineSegmentUtil.asGeometry(fact, seg));
    }
    FeatureDataset dataset = new FeatureDataset(schema);
    for (Object o : lineMerger.getMergedLineStrings()) {
        BasicFeature bf = new BasicFeature(schema);
        bf.setGeometry((Geometry)o);
        dataset.add(bf);
    }
    return dataset;
  }

  //private MultiInputDialog dialog;
  private String layerName;
  private boolean removeDoubledSegments     = false;
  private boolean makeDoubledSegmentsUnique = false;
  private boolean mergeResultingSegments    = false;
  private boolean keepAllSegments           = true;
  private boolean keepAttributes            = false;
  private int inputEdgeCount                = 0;
  private int uniqueSegmentCount            = 0;

  public ExtractSegmentsPlugIn() { }

  /**
   * Returns a very brief description of this task.
   * @return the name of this task
   */
  public String getName() { 
      return I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extract-Segments"); 
  }

  public void initialize(PlugInContext context) throws Exception {
      	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
  		featureInstaller.addMainMenuItem(
			new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT},
            this, 
            new JMenuItem(getName() + "..."),
            createEnableCheck(context.getWorkbenchContext()),
            -1);  
  }
  
  public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
      return new MultiEnableCheck()
          .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
          .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {
    MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    
    monitor.allowCancellationRequests();

    monitor.report(I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracting-Segments"));

    Layer layer = context.getLayerManager().getLayer(layerName);
    FeatureCollection lineFC = layer.getFeatureCollectionWrapper();
    inputEdgeCount = lineFC.size();
    if (inputEdgeCount == 0) {
        context.getWorkbenchFrame().warnUser("jump.plugin.edit.ExtractSegmentsPlugIn.No-edge-to-process");
        return;
    }
    //UniqueSegmentsExtracter extracter = new UniqueSegmentsExtracter(monitor);
    SegmentsExtracter extracter = new SegmentsExtracter(monitor);
    Collection<LineSegment> segmentList = null;
    FeatureCollection result = null;
    if (removeDoubledSegments || makeDoubledSegmentsUnique) {
        extracter.normalizeSegments();
        extracter.add(lineFC);
        segmentList = removeDoubledSegments ?
                extracter.getSegments(1,1)
                : extracter.getSegments();
        if (mergeResultingSegments) result = toMergedLineStrings(segmentList);
        else result = toLineStrings(segmentList);
    } else if (keepAllSegments) {
        if (keepAttributes) extracter.keepSource();
        extracter.add(lineFC);
        if (keepAttributes) {
            segmentList = extracter.getSegments();
            result = toLineStrings(segmentList, extracter.getSegmentSource());
        } else {
            segmentList = extracter.getAllSegments();
            result = toLineStrings(segmentList);
        }
    }
    uniqueSegmentCount = segmentList.size();
    if (monitor.isCancelRequested()) return;
    createLayers(context, result);
  }

  private void createLayers(PlugInContext context, FeatureCollection result)
         throws Exception {
    //FeatureCollection lineStringFC = FeatureDatasetFactory.createFromGeometry(linestringList);
    context.addLayer(
        StandardCategoryNames.RESULT,
        layerName + " " + I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracted-Segs"),
        result);
    createOutput(context);
  }

  private void createOutput(PlugInContext context) {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1,
    		I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extract-Segments"));
    context.getOutputFrame().addField(I18N.get("ui.MenuNames.LAYER")+ ":", layerName);

    context.getOutputFrame().addText(" ");
    context.getOutputFrame().addField(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Number-of-unique-segments-extracted"), 
        "" + uniqueSegmentCount);
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("ExtractSegments.png")));
    dialog.setSideBarDescription(I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Extracts-all-unique-line-segments-from-a-dataset"));
    final JComboBox layerComboBox = dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
    final JRadioButton removeDoubleSegmentsCheckBox = dialog.addRadioButton(
            I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Remove-doubled-segments"),
            "group1", removeDoubledSegments, null);
    final JRadioButton makeDoubleSegmentsUniqueCheckBox = dialog.addRadioButton(
            I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Make-doubled-segments-unique"),
            "group1", makeDoubledSegmentsUnique, null);
    final JRadioButton keepAllSegmentsCheckBox = dialog.addRadioButton(
              I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Keep-all-segments"),
            "group1", keepAllSegments, null);
    final JCheckBox mergeCheckBox = dialog.addCheckBox(
              I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Merge-resulting-segments"),mergeResultingSegments);
    mergeCheckBox.setEnabled(removeDoubledSegments || makeDoubledSegmentsUnique);
    final JCheckBox keepAttributesCheckBox = dialog.addCheckBox(
              I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Keep-attributes"),keepAttributes);
    keepAttributesCheckBox.setEnabled(keepAllSegments);

    removeDoubleSegmentsCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean a = removeDoubleSegmentsCheckBox.isSelected();
        boolean b = makeDoubleSegmentsUniqueCheckBox.isSelected();
        boolean c = keepAllSegmentsCheckBox.isSelected();
        mergeCheckBox.setEnabled(a || b);
        keepAttributesCheckBox.setEnabled(c);
      }
    });
    makeDoubleSegmentsUniqueCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean a = removeDoubleSegmentsCheckBox.isSelected();
        boolean b = makeDoubleSegmentsUniqueCheckBox.isSelected();
        boolean c = keepAllSegmentsCheckBox.isSelected();
        mergeCheckBox.setEnabled(a || b);
        keepAttributesCheckBox.setEnabled(c);
      }
    });
    keepAllSegmentsCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean a = removeDoubleSegmentsCheckBox.isSelected();
        boolean b = makeDoubleSegmentsUniqueCheckBox.isSelected();
        boolean c = keepAllSegmentsCheckBox.isSelected();
        mergeCheckBox.setEnabled(a || b);
        keepAttributesCheckBox.setEnabled(c);
      }
    });
  }

  private void getDialogValues(MultiInputDialog dialog) {
    Layer layer = dialog.getLayer(LAYER);
    layerName = layer.getName();
    removeDoubledSegments = dialog.getBoolean(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Remove-doubled-segments"));
    makeDoubledSegmentsUnique = dialog.getBoolean(
              I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Make-doubled-segments-unique"));
    keepAllSegments = dialog.getBoolean(
            I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Keep-all-segments"));
    mergeResultingSegments = dialog.getBoolean(
        I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Merge-resulting-segments"));
    keepAttributes = dialog.getBoolean(
              I18N.get("jump.plugin.edit.ExtractSegmentsPlugIn.Keep-attributes"));
  }
}
