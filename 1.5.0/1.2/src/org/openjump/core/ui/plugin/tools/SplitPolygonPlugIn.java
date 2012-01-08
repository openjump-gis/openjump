/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.tools;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JComponent;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.plugin.util.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.SelectionManagerProxy;
import com.vividsolutions.jump.workbench.ui.plugin.analysis.GeometryFunction;
public class SplitPolygonPlugIn extends AbstractPlugIn implements ThreadedPlugIn
{
	  private static String UPDATE_SRC = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Update-the-polygon-with-result");
	  private static String ADD_TO_SRC = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Add-result-to-the-polygon-layer");
	  private static String CREATE_LYR = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Create-new-layer-for-result");
	  private static String sCutPolygon = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Cut-Polygon");
	  private static String sError = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Errors-found-while-executing-Cut-Polygon");
	  private static String sExecuting = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Executing-Difference-function");
	  private static String sMustSelect = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Must-select-one-polygon-and-one-linestring");
	  private static String sDescription = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Uses-the-selected-linestring-to-cut-the-selected-polygon-into-separate-sections");
	  
	  private MultiInputDialog dialog;
	  private Layer srcLayer;
	  private GeometryFunction differenceFunction = GeometryFunction.getFunction("Difference (Source-Mask)");
	  private GeometryFunction intersectionFunction = GeometryFunction.getFunction("Intersection");
	  private boolean createLayer = true;
	  private boolean updateSource = false;
	  private boolean addToSource = false;

	  public SplitPolygonPlugIn(){
	  }

	  public void initialize(PlugInContext context) throws Exception 
	  {
		  //-- load again in the correct language
		  UPDATE_SRC = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Update-the-polygon-with-result");
		  ADD_TO_SRC = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Add-result-to-the-polygon-layer");
		  CREATE_LYR = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Create-new-layer-for-result");
		  sCutPolygon = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Cut-Polygon");
		  sError = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Errors-found-while-executing-Cut-Polygon");
		  sExecuting = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Executing-Difference-function");
		  sMustSelect = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Must-select-one-polygon-and-one-linestring");
		  sDescription = I18N.get("org.openjump.core.ui.plugin.tools.CutPolygonPlugIn.Uses-the-selected-linestring-to-cut-the-selected-polygon-into-separate-sections");
		  
		  //-- [sstein 11 March 2007] it is a bit circumstantially to access a geometry function 
		  //   using i18n strings - we should introduce an unique ID
		  differenceFunction = GeometryFunction.getFunction(I18N.get("ui.plugin.analysis.GeometryFunction.difference-a-b"));
		  intersectionFunction = GeometryFunction.getFunction(I18N.get("ui.plugin.analysis.GeometryFunction.intersection"));
		  
			WorkbenchContext workbenchContext = context.getWorkbenchContext();
	        context.getFeatureInstaller().addMainMenuItem(this, 
	        		new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY }, 
	        		getName() + "...", 
	        		false, 
	        		null, 
	        		this.createEnableCheck(workbenchContext));
 	  }

	  public String getName(){
		  return this.sCutPolygon;
	  }
	  
	  public boolean execute(PlugInContext context) throws Exception 
	  {	  
		    dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
			setDialogValues(dialog, context);
		    GUIUtil.centreOnWindow(dialog);
		    dialog.setVisible(true);
		    if (! dialog.wasOKPressed()) { return false; }
		    getDialogValues(dialog);
		    return true;
	  }

	  public void run(TaskMonitor monitor, PlugInContext context)throws Exception 
	  {
		    monitor.allowCancellationRequests();
			Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
			Iterator i = selectedFeatures.iterator();
			Feature featureOne = (Feature)i.next();
			Feature featureTwo = (Feature)i.next();
			
			//this works because enable check ensures that one is polygon and the other is linestring
			Feature polyFeature = featureOne;
			Geometry linestring = featureTwo.getGeometry();
			
			if (linestring instanceof Polygon)
			{
				polyFeature = featureTwo;
				linestring = featureOne.getGeometry();
			}
			
			double bufferWidth = 0.01;
			Geometry buffer = linestring.buffer(bufferWidth);

			//find the poly layer since linestring can be on different layer
	        for (Iterator lyr = context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems().iterator();lyr.hasNext();) 
	        {
	        	Layer layer = (Layer) lyr.next();
	        	
	        	for (Iterator ftr = layer.getFeatureCollectionWrapper().getFeatures().iterator(); ftr.hasNext();)
	        	{
	        		if (polyFeature == ftr.next())
	        		{
	        			srcLayer = layer;
	        			break;
	        		}
	        	}
	        	
	        	if (srcLayer == layer) break;
	        }
		    monitor.report(sExecuting + "...");

		    Collection resultFeatures = new ArrayList();
		    Geometry result = null;
		    Geometry intersection = null;
		    
			try 
			{
			    Geometry geoms[] = new Geometry[2];
			    geoms[0] = polyFeature.getGeometry();
			    geoms[1] = buffer;
				result = differenceFunction.execute(geoms, new double[2]);
				
				geoms[1] = linestring;
				intersection = intersectionFunction.execute(geoms, new double[2]);
			}
			catch (RuntimeException ex) 
			{
				context.getWorkbenchFrame().warnUser(sError);
			}
		    
			Coordinate[] intersectionPts = intersection.getCoordinates();
			
		    if (result == null || result.isEmpty()) return;
		    
		    int lsNumPts = linestring.getNumPoints();
		    Coordinate[] lsCoords = linestring.getCoordinates();
		    
	        for (int j = 0; j < result.getNumGeometries(); j++) 
	        {
	            Feature fNew = polyFeature.clone(true);
	            
	            //snap the resulting geos to the cut line
	            Geometry geo = (Geometry)result.getGeometryN(j).clone();
	        	int numPts = geo.getNumPoints();
	        	Coordinate[] coords = geo.getCoordinates();
	        	
	        	for (int m = 0; m < numPts; m++)
	        	{
	        		for (int n = 0; n < lsNumPts - 1; n++)
	        		{
	        			Coordinate p0 = lsCoords[n];
	        			Coordinate p1 = lsCoords[n + 1];
	        			double distToLine = GeoUtils.getDistance(coords[m], p0, p1);
	        			if (Math.abs((distToLine) - (bufferWidth)) < 0.001)
	        			{
	        				//is close to buffer boundary
	        				Coordinate snapPt = getNearestSnapPoint(coords[m], intersectionPts);
	        				coords[m].x = snapPt.x;
	        				coords[m].y = snapPt.y;
	        			}
	        		}
	        	}
	        	CoordinateList coordList = new CoordinateList(coords, false); //gets rid of duplicates
				Polygon poly = new GeometryFactory().createPolygon( new GeometryFactory().createLinearRing(coordList.toCoordinateArray()), null);
	        	fNew.setGeometry(poly);
	            resultFeatures.add(fNew);
	        }
	        
		    if (createLayer) 
		    {
			      String outputLayerName = LayerNameGenerator.generateOperationOnLayerName(
				          sCutPolygon, srcLayer.getName());
				  FeatureCollection resultFC = new FeatureDataset(srcLayer.getFeatureCollectionWrapper().getFeatureSchema());
				  resultFC.addAll(resultFeatures);
				  String categoryName = StandardCategoryNames.RESULT;
				  context.getLayerManager().addCategory(categoryName);
				  Layer newLayer = context.addLayer(categoryName, outputLayerName, resultFC);
				  newLayer.setFeatureCollectionModified(true);
		    }
		    
		    else if (updateSource) 
		    {
		        final Collection undoableNewFeatures = resultFeatures;
		        final Feature undoablePolyFeatures = polyFeature;

		        UndoableCommand cmd = new UndoableCommand( getName() ) {
		            public void execute() {
		                srcLayer.getFeatureCollectionWrapper().remove(undoablePolyFeatures);
		                srcLayer.getFeatureCollectionWrapper().addAll(undoableNewFeatures);
		            }

		            public void unexecute() {
		                srcLayer.getFeatureCollectionWrapper().removeAll(undoableNewFeatures);
		                srcLayer.getFeatureCollectionWrapper().add(undoablePolyFeatures);
		            }
		        };
		        execute( cmd, context );
		    }
		    
		    else if (addToSource) 
		    {
		        final Collection undoableFeatures = resultFeatures;

		        UndoableCommand cmd = new UndoableCommand( getName() ) {
		            public void execute() {
		                srcLayer.getFeatureCollectionWrapper().addAll( undoableFeatures );
		            }

		            public void unexecute() {
		                srcLayer.getFeatureCollectionWrapper().removeAll( undoableFeatures );
		            }
		        };
		        execute( cmd, context );
		    }
	  }

	  private Coordinate getNearestSnapPoint(Coordinate coord, Coordinate[] intersectionPts)
	  {
		  Coordinate closestPt = (Coordinate)((Coordinate) intersectionPts[0]).clone();
		  double shortestDist = coord.distance(intersectionPts[0]);
		  
		  for (int i = 1; i < intersectionPts.length; i++)
		  {
			  if (coord.distance(intersectionPts[i]) < shortestDist)
			  {
				  closestPt = (Coordinate)((Coordinate) intersectionPts[i]).clone();
				  shortestDist = coord.distance(intersectionPts[i]);
			  }
		  }
		  return closestPt;
	  }
	  
	  private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
		    dialog.setSideBarDescription(sDescription);

		    final String OUTPUT_GROUP = "Match Type";
		    dialog.addRadioButton(CREATE_LYR, OUTPUT_GROUP, createLayer,CREATE_LYR);
		    
		    dialog.addRadioButton(UPDATE_SRC, OUTPUT_GROUP, updateSource,UPDATE_SRC);

		    dialog.addRadioButton(ADD_TO_SRC, OUTPUT_GROUP, addToSource,ADD_TO_SRC);
	  }

	  private void getDialogValues(MultiInputDialog dialog) 
	  {
		    createLayer = dialog.getBoolean(CREATE_LYR);
		    updateSource = dialog.getBoolean(UPDATE_SRC);
		    addToSource = dialog.getBoolean(ADD_TO_SRC);
	  }
	  
	  public EnableCheck onlyPolyAndLinestringMayBeSelected(final WorkbenchContext workbenchContext) {
	        return new EnableCheck() {
	            public String check(JComponent component) {
		           Collection selectedItems = ((SelectionManagerProxy) workbenchContext
	                            .getWorkbench()
	                            .getFrame()
	                            .getActiveInternalFrame())
	                            .getSelectionManager()
	                            .getSelectedItems();
		        int polyCount = 0;
		        int lsCount = 0;
		        
	            for (Iterator i = selectedItems.iterator(); i.hasNext();)
	            {
	            	Geometry geo = (Geometry) i.next();
	            	if (geo instanceof Polygon) polyCount++;
	            	if (geo instanceof LineString) lsCount++;
	            }
	            
	            if (polyCount == 1 && lsCount == 1) return null;
	            return sMustSelect;
	            }
	        };
	    }
	        
	  public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
	    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
	    return new MultiEnableCheck()
	        .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
	        .add(checkFactory.createExactlyNFeaturesMustBeSelectedCheck(2))
	        .add(onlyPolyAndLinestringMayBeSelected(workbenchContext));
	  	}  
}