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

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.geom.precision.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.util.*;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class PrecisionReducerPlugIn
    extends ThreadedBasePlugIn
{

  private static final double EXAMPLE_VALUE = 1234567.123123123123;

  private final static String LAYER = I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Layer");
  private final static String DECIMAL_PLACES = I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Decimal-Places");
  private final static String SCALE_FACTOR = I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Scale-Factor");

  private MultiInputDialog dialog;
  private JTextField decimalPlacesField;
  private JTextField scaleFactorField;
  private JLabel exampleLabel;
  private String layerName;
  private int decimalPlaces = 0;
  private int scaleFactor = 1;

  public PrecisionReducerPlugIn() { }

  /**
   * Returns a very brief description of this task.
   * @return the name of this task
   */
  public String getName() { return I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Precision-Reducer"); }

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
          .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
          .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
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

    monitor.report(I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Reducing-Precision") + "...");

    Layer layer = dialog.getLayer(LAYER);
    FeatureCollection fc = layer.getFeatureCollectionWrapper();

    List[] bad = reducePrecision(fc, monitor);
    layer.fireAppearanceChanged();

    if (monitor.isCancelRequested()) return;

    if (bad[0].size() > 0) {
      Layer lyr = context.getLayerManager().addLayer(StandardCategoryNames.QA,
      		I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Invalid-Input-Geometries"), FeatureDatasetFactory.createFromGeometry(bad[0]));
      LayerStyleUtil.setLinearStyle(lyr, Color.red, 2, 0);
      lyr.fireAppearanceChanged();

      Layer lyr2 = context.getLayerManager().addLayer(StandardCategoryNames.QA,
      		I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Invalid-Reduced-Geometries"), FeatureDatasetFactory.createFromGeometry(bad[1]));
      lyr2.getBasicStyle().setFillColor( ColorUtil.GOLD);
      lyr2.getBasicStyle().setLineColor( Layer.defaultLineColor(ColorUtil.GOLD));
      lyr2.fireAppearanceChanged();
    }
  }

  private NumberPrecisionReducer createNumberPrecisionReducer()
  {
    double sf = scaleFactor;
    // scaleFactor and decimalPlaces should be in synch, but if they are not use decimalPlaces
    if (scaleFactor != NumberPrecisionReducer.scaleFactorForDecimalPlaces(decimalPlaces))
      sf = NumberPrecisionReducer.scaleFactorForDecimalPlaces(decimalPlaces);

    return new NumberPrecisionReducer(sf);
  }

  /**
   * @return an array of two Lists.
   * The first contains the geometries which reduced to invalid geometries.
   * The second contains the invalid geometries created
   */
  private List[] reducePrecision(FeatureCollection fc, TaskMonitor monitor)
  {
    List[] bad = { new ArrayList(), new ArrayList() };
    int total = fc.size();
    int count = 0;
    for (Iterator i = fc.iterator(); i.hasNext(); ) {
      monitor.report(count++, total, I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.features"));

      Feature f = (Feature) i.next();
      Geometry g = f.getGeometry();
      Geometry g2 = (Geometry) g.clone();
      GeometryPrecisionReducer pr = new GeometryPrecisionReducer(createNumberPrecisionReducer());
      pr.reduce(g2);
      if (g2.isValid()) {
        f.setGeometry(g2);
      }
      else {
        bad[0].add(g.clone());
        bad[1].add(g2);
      }
    }
    return bad;
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("PrecisionReducer.png")));
    dialog.setSideBarDescription(I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Reduces-the-precision-of-the-coordinates-in-a-layer")
    );
    String fieldName = LAYER;
    JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());

    scaleFactorField = dialog.addIntegerField(SCALE_FACTOR, scaleFactor, 8,
    		I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.The-scale-factor-to-multiply-by-before-rounding-(-Negative-for-left-of-decimal-point-,-0-if-not-used-)"));
    scaleFactorField.getDocument().addDocumentListener(new ScaleFactorDocumentListener());

    decimalPlacesField = dialog.addIntegerField(DECIMAL_PLACES, decimalPlaces, 4,
    		I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.The-number-of-decimal-places-to-round-to-(-Negative-for-left-of-decimal-point-)"));
    decimalPlacesField.getDocument().addDocumentListener(new DecimalPlacesDocumentListener());

    dialog.addLabel("");
    dialog.addLabel(I18N.get("ui.plugin.edit.PrecisionReducerPlugIn.Example") + "  " + EXAMPLE_VALUE);
    exampleLabel = dialog.addLabel("");

    updateExample();
  }

  private int parseValidInt(String text)
  {
    int i = 0;
    try {
      i = Integer.parseInt(text);
    }
    catch (NumberFormatException ex) {
      // leave decPlaces value as 0
    }
    return i;
  }
  private void decimalPlacesChanged()
  {
    decimalPlaces = parseValidInt(decimalPlacesField.getText());
    double sf = NumberPrecisionReducer.scaleFactorForDecimalPlaces(decimalPlaces);
    scaleFactorField.setText("" + (int) sf);
    updateExample();
  }

  private void scaleFactorChanged()
  {
    scaleFactor = parseValidInt(scaleFactorField.getText());
    // can't update decimalPlaces because it will cause an event cycle
    //decimalPlacesField.setText("");
    updateExample();
  }

  private void updateExample()
  {
    NumberPrecisionReducer cpr = new NumberPrecisionReducer(scaleFactor);
    double exampleOutput = cpr.reducePrecision(EXAMPLE_VALUE);
    exampleLabel.setText("      ==>  " + exampleOutput);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    Layer layer = dialog.getLayer(LAYER);
    layerName = layer.getName();
    decimalPlaces = dialog.getInteger(DECIMAL_PLACES);
    scaleFactor = dialog.getInteger(SCALE_FACTOR);
  }

  class DecimalPlacesDocumentListener implements DocumentListener {
    public void insertUpdate(DocumentEvent e) {
      decimalPlacesChanged();
    }
    public void removeUpdate(DocumentEvent e) {
      decimalPlacesChanged();
    }
    public void changedUpdate(DocumentEvent e) {
      decimalPlacesChanged();
    }
  }
  class ScaleFactorDocumentListener implements DocumentListener {
    public void insertUpdate(DocumentEvent e) {
      scaleFactorChanged();
    }
    public void removeUpdate(DocumentEvent e) {
      scaleFactorChanged();
    }
    public void changedUpdate(DocumentEvent e) {
      scaleFactorChanged();
    }
  }

}
