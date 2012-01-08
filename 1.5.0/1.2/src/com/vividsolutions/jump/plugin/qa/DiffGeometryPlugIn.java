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


package com.vividsolutions.jump.plugin.qa;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.qa.diff.*;
import com.vividsolutions.jump.util.ColorUtil;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;

/**
 * Computes both raw segment diffs and geometry diffs
 * for two input layers.
 *
 * @author Martin Davis
 */
public class DiffGeometryPlugIn
  extends ThreadedBasePlugIn
  {

  private String sLayer = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Layer");
  private String LAYER1 = sLayer + " 1";
  private String LAYER2 = sLayer + " 2";
  private String MATCH_SEGMENTS = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-Segments");
  private String MATCH_GEOMETRY = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-entire-Geometry");
  private String EXACT_COORD_ORDER = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Test-for-identical-Start-point-and-Orientation");
  private String USE_TOLERANCE = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-using-Distance-Tolerance");
  private String DISTANCE_TOL = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Distance-Tolerance");
  private String SPLIT_COMPONENTS = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-components-of-MultiGeometries");

  private String sSegmentDiffs = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Segment-Diffs"); 
  private String sUnmGeoms = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Unmatched-Geometries-in-Layer");
  private String sUnmSegms = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Unmatched-Segments-in-Geometry-Diffs-in-Layer");
  //note: further strings below
  
  private MultiInputDialog dialog;
  private Layer layer1, layer2;
  private boolean matchGeometry = false;
  private boolean useTolerance = false;
  private double distanceTolerance = 1.0;
  private boolean testExactCoordinateOrder = false;
  private boolean splitIntoComponents = false;

  public DiffGeometryPlugIn() { }

/*
  public void initialize(PlugInContext context) throws Exception {
    context.getFeatureInstaller().addMainMenuItem(this, new String[] {"QA"},
        getName() + "...", false, null, new MultiEnableCheck()
        .add(context.getCheckFactory().createWindowWithLayerViewPanelMustBeActiveCheck())
        .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(2)));
  }
*/

  public String getName(){
  	return I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Calculate-Geometry-Differences"); 
  }
  
  public boolean execute(PlugInContext context) throws Exception {
  	
  	//[sstein, 16.07.2006] set again to obtain correct language
    sLayer = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Layer");
    LAYER1 = sLayer + " 1";
    LAYER2 = sLayer + " 2";
    MATCH_SEGMENTS = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-Segments");
    MATCH_GEOMETRY = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-entire-Geometry");
    EXACT_COORD_ORDER = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Test-for-identical-Start-point-and-Orientation");
    USE_TOLERANCE = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-using-Distance-Tolerance");
    DISTANCE_TOL = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Distance-Tolerance");
    SPLIT_COMPONENTS = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-components-of-MultiGeometries");

    sSegmentDiffs = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Segment-Diffs"); 
    sUnmGeoms = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Unmatched-Geometries-in-Layer");
    sUnmSegms = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Unmatched-Segments-in-Geometry-Diffs-in-Layer");
    
    dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (! dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    //perform(dialog, context);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context)
       throws Exception
  {
    FeatureCollection[] diffFC = new FeatureCollection[] {
        layer1.getFeatureCollectionWrapper(),
        layer2.getFeatureCollectionWrapper() };

    if (matchGeometry) {
      DiffGeometryComponents diff = new DiffGeometryComponents(layer1.getFeatureCollectionWrapper(),
          layer2.getFeatureCollectionWrapper(),
          monitor);
      diff.setNormalize(! testExactCoordinateOrder);
      diff.setSplitIntoComponents(splitIntoComponents);
      if (useTolerance) {
        diff.setMatcher(new BufferGeometryMatcher(distanceTolerance));
      }
      diffFC = diff.diff();
    }
    monitor.report(I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Computing-Segment-Diffs"));
    FeatureCollection[] diffSegFC = diffSegments(diffFC, monitor);

    createLayers(context, diffFC, diffSegFC);
    createOutput(context, diffFC, diffSegFC);
  }

  public FeatureCollection[] diffSegments(FeatureCollection[] diffFC, TaskMonitor monitor)
       throws Exception
  {
    FeatureCollection[] diffSegFC;
    if (! useTolerance) {
      DiffSegments diff = new DiffSegments(monitor);
      diff.setSegments(0, diffFC[0] );
      diff.setSegments(1, diffFC[1] );

      diffSegFC = new FeatureCollection[2];
      diffSegFC[0] = diff.computeDiffEdges(0);
      diffSegFC[1] = diff.computeDiffEdges(1);
    }
    else {
      DiffSegmentsWithTolerance diff = new DiffSegmentsWithTolerance(
          diffFC[0],
          diffFC[1],
          distanceTolerance);

      diffSegFC  = diff.diff();
    }
    return diffSegFC;
  }

  /**
   * Sets the style for a diff geometry layer.
   * @param lyr
   * @param fillColor
   * @param lineColor
   */
  public static void setDiffGeometryStyle(Layer lyr, Color fillColor, Color lineColor)
  {
    lyr.getBasicStyle().setRenderingFill(true);
    lyr.getBasicStyle().setFillColor(fillColor);
    lyr.setSynchronizingLineColor(false);
    lyr.getBasicStyle().setAlpha(200);
    lyr.getBasicStyle().setLineWidth(1);
    lyr.getBasicStyle().setLineColor(lineColor);
    lyr.getVertexStyle().setEnabled(false);
  }

  private void createLayers(PlugInContext context,
                            FeatureCollection[] diffFC,
                            FeatureCollection[] diffSegFC)
  {
    // segment diffs
    Layer segLyr = context.addLayer(StandardCategoryNames.QA,
                                    sSegmentDiffs + " - " + layer1.getName(), diffSegFC[0]);
    LayerStyleUtil.setLinearStyle(segLyr, Color.red, 2, 4);
    segLyr.fireAppearanceChanged();

    Layer segLyr2 = context.addLayer(StandardCategoryNames.QA,
    								sSegmentDiffs + " - " + layer2.getName(), diffSegFC[1]);
    LayerStyleUtil.setLinearStyle(segLyr2, Color.blue, 2, 4);
    segLyr2.fireAppearanceChanged();

    if (matchGeometry) {
      // Geometry diffs
      Layer lyr = context.addLayer(StandardCategoryNames.QA,
      								sSegmentDiffs + " - " + layer1.getName(), diffFC[0]);
      setDiffGeometryStyle(lyr, ColorUtil.PALE_RED, Color.red);
      lyr.fireAppearanceChanged();

      Layer lyr2 = context.addLayer(StandardCategoryNames.QA,
      								sSegmentDiffs + " - " + layer2.getName(), diffFC[1]);
      setDiffGeometryStyle(lyr2, ColorUtil.PALE_BLUE, Color.blue);
      lyr2.fireAppearanceChanged();
    }
  }

  private void createOutput(PlugInContext context,
                            FeatureCollection[] diffFC,
                            FeatureCollection[] diffSegFC)
 {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1, getName());
    context.getOutputFrame().addField(sLayer +" 1: ", layer1.getName() );
    context.getOutputFrame().addField(sLayer +" 2: ", layer2.getName() );
    if (useTolerance) {
      context.getOutputFrame().addField(DISTANCE_TOL + ":",
                                        "" + distanceTolerance);
    }
    if (testExactCoordinateOrder) {
      context.getOutputFrame().addField(EXACT_COORD_ORDER + ":",
                                        (new Boolean(testExactCoordinateOrder)).toString() );
    }

    if (matchGeometry) {
      context.getOutputFrame().addText(" ");
      context.getOutputFrame().addField(
          "# " + sUnmGeoms + " 1: ", "" + diffFC[0].size());
      context.getOutputFrame().addField(
          "# " + sUnmGeoms + " 2: ", "" + diffFC[1].size());
    }

    context.getOutputFrame().addText(" ");
    context.getOutputFrame().addField(
        "# " + sUnmSegms + " 1: ", "" + diffSegFC[0].size());
    context.getOutputFrame().addField(
        "# " + sUnmSegms + " 2: ", "" + diffSegFC[1].size());
  }

  private JRadioButton matchSegmentsRB;
  private JRadioButton matchGeometryRB;
  private JCheckBox matchGeometryCheckbox;
  private JCheckBox splitComponentsCheckbox;
  private JCheckBox exactOrderCheckbox;
  private JCheckBox useToleranceCheckbox;
  private JTextField distanceTextField;

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("DiffSegments.png")));
    dialog.setSideBarDescription(
    		I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Finds-differences-between-the-Segments-or-Geometries-in-two-layers")
            + "  " + I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Matching-can-be-either-exact-or-within-a-Distance-Tolerance"));

    // Set initial layer values to the first and second layers in the layer list.
    // In #initialize we've already checked that the number of layers >= 2.
    dialog.addLayerComboBox(LAYER1, context.getLayerManager().getLayer(0),
        context.getLayerManager());
    dialog.addLayerComboBox(LAYER2, context.getLayerManager().getLayer(1),
        context.getLayerManager());

    final String MATCH_TYPE_GROUP = I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Match-Type");
    matchSegmentsRB = dialog.addRadioButton(MATCH_SEGMENTS, MATCH_TYPE_GROUP, ! matchGeometry,
    		I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Matches-using-segments-only"));
    matchGeometryRB = dialog.addRadioButton(MATCH_GEOMETRY, MATCH_TYPE_GROUP, matchGeometry,
    		I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Matches-using-full-geometry-topology,-not-just-segments"));
    matchSegmentsRB.addItemListener(new StateItemListener());
    matchGeometryRB.addItemListener(new StateItemListener());

    splitComponentsCheckbox = dialog.addCheckBox(SPLIT_COMPONENTS, splitIntoComponents,
    		I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Matches-individual-components-of-MultiGeometries"));
    exactOrderCheckbox = dialog.addCheckBox(
            EXACT_COORD_ORDER,
            false,
			I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Requires-coordinate-lists-in-matching-geometries-to-have-identical-start-points-and-ring-orientation"));
    dialog.addLabel("");
    useToleranceCheckbox = dialog.addCheckBox(
            USE_TOLERANCE,
            useTolerance,
			I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Matches-geometries-if-all-points-are-within-the-Distance-Tolerance-of-the-other-Geometry"));
    distanceTextField = dialog.addDoubleField(DISTANCE_TOL, distanceTolerance, 8,
    		I18N.get("jump.plugin.qa.DiffGeometryPlugIn.Specifies-how-close-geometries-must-be-to-match"));
    useToleranceCheckbox.addItemListener(new StateItemListener());

    updateUI();
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer1 = dialog.getLayer(LAYER1);
    layer2 = dialog.getLayer(LAYER2);
    matchGeometry = dialog.getBoolean(MATCH_GEOMETRY);
    useTolerance = dialog.getBoolean(USE_TOLERANCE);
    distanceTolerance = dialog.getDouble(DISTANCE_TOL);
    splitIntoComponents = dialog.getBoolean(SPLIT_COMPONENTS);
    testExactCoordinateOrder = dialog.getBoolean(EXACT_COORD_ORDER);
  }

  private void updateUI()
  {
    boolean matchGeometry = dialog.getBoolean(MATCH_GEOMETRY);
    splitComponentsCheckbox.setEnabled(matchGeometry);
    exactOrderCheckbox.setEnabled(matchGeometry);

    boolean useDistance = dialog.getBoolean(USE_TOLERANCE);
    distanceTextField.setEnabled(useDistance);
    // this has the effect of making the background gray (disabled)
    distanceTextField.setOpaque(useDistance);
  }

  private class StateItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateUI();
    }
  }
}
