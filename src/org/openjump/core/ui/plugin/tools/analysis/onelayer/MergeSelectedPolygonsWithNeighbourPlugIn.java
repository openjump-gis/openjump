/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */

/*****************************************************
 * created:  		06.June.2010
 * last modified:  	
 * 
 * description: 
 *  Merges selected polygons with neighboring polygons, either with the one that is
 *  largest of all neighbors, or the one wiht which it has the longest common boundary.
 *  Note, the function may return multi-polygons if the polygons to merge have only 
 *  one point in common.
 *****************************************************/

package org.openjump.core.ui.plugin.tools.analysis.onelayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JRadioButton;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.apitools.objecttyperoles.PirolFeatureCollection;
import org.openjump.core.graph.polygongraph.PolygonGraph;
import org.openjump.core.graph.polygongraph.PolygonGraphEdge;
import org.openjump.core.graph.polygongraph.PolygonGraphNode;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


/**
 * @author sstein
 * 
 **/
public class MergeSelectedPolygonsWithNeighbourPlugIn extends ThreadedBasePlugIn{


	private String sMergeTwoPolys = "Merge Selected Polygons with Neighbours";
	private String sFeaturesFromDifferentLayer = "Error: Features from different layers!";
	private String sSidebar = "Merges selected polygons with neighboring polygons, either with the one that is largest of " +
			"all neighbors, or the one with which it has " +
			"the longest common boundary. Note, the function may return multi-polygons if " +
			"the polygons to merge have only one point in common.";
	boolean useArea = true;
	boolean useBorder = false;
	String sUseArea = "merge with neighbor that has the largest area";
	String sUseBoder = "merge with neighbor with the longest common edge";
	String sChoseMergeMethod = "Please chose the merge method:";
	String sMerged ="merged";
	String sSearchingForMergeCandidates = "Searching for merge candidates...";
	String sMergingPolygons = "Merging polygons...";
	final static String sMERGEMETHOD = "MERGE METHOD";
	
    private MultiInputDialog dialog;
    private JRadioButton buttonSelectMergeTypeUseArea = null;
    private JRadioButton buttonSelectMergeTypeUseBorder = null;
	
    public void initialize(PlugInContext context) throws Exception {
    	
    	sMergeTwoPolys = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merge-Selected-Polygons-with-Neighbours");
    	sFeaturesFromDifferentLayer = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.features-from-different-layers");
    	sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.description");
    	sUseArea = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-that-has-the-largest-area");
    	sUseBoder = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-with-the-longest-common-edge");
    	sChoseMergeMethod = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Please-chose-the-merge-method");
    	sMerged = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merged");
    	sSearchingForMergeCandidates = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Searching-for-merge-candidates");
    	sMergingPolygons = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merging-polygons");
    	
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuItem(
    	        this,								//exe
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY}, 	//menu path
                this.getName() + "...", //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check        
    }
    
    public String getName() {
        return sMergeTwoPolys;
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
    }
    
	public boolean execute(PlugInContext context) throws Exception{
		
        initDialog(context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        else{
        	this.getDialogValues(dialog); 
        }
        return true;    
	}
    
	private void initDialog(PlugInContext context) {
    	
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
        dialog.setSideBarDescription(sSidebar);
        final String METHODGROUP = sMERGEMETHOD;
        dialog.addLabel(sChoseMergeMethod);
        buttonSelectMergeTypeUseArea = dialog.addRadioButton(sUseArea, METHODGROUP, this.useArea, sUseArea);
        buttonSelectMergeTypeUseBorder = dialog.addRadioButton(sUseBoder, METHODGROUP, this.useBorder, sUseBoder);
        GUIUtil.centreOnWindow(dialog);
    }	    
  
    private void getDialogValues(MultiInputDialog dialog) {
    	this.useArea = dialog.getBoolean(this.sUseArea);
    	this.useBorder = dialog.getBoolean(this.sUseBoder);
      }
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{             
    	
    	monitor.allowCancellationRequests();
		// get the selected features
	    Collection<Feature> features = context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
	    // get the layers
	    Collection<Layer> layers = context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
	    if(layers.size() > 1){
	    	context.getWorkbenchFrame().warnUser(sFeaturesFromDifferentLayer);
	    	return;
	    }
	    else{
	    	// parse the features and mark them as selected in the geometry
	    	for (Iterator iterator = features.iterator(); iterator.hasNext();) {
				Feature ftemp = (Feature) iterator.next();
				ftemp.getGeometry().setUserData(new Boolean(true));
			}
	    	// now modify the layer and add two attributes: one for the feature to be merged
	    	// and one for the feature to be merging candidate
	    	Iterator iter = layers.iterator();
	    	Layer firstLayer = (Layer)iter.next();
	    	//make a copy of the FC first
	    	FeatureCollection input = FeatureCollectionTools.cloneFeatureCollection(firstLayer.getFeatureCollectionWrapper());
	    	FeatureSchema originalFeatureSchema = input.getFeatureSchema();
	    	PirolFeatureCollection fcN = FeatureCollectionTools.addAttributeToFeatureCollection(input, "mergeid", AttributeType.INTEGER, new Integer(0));
	    	fcN = FeatureCollectionTools.addAttributeToFeatureCollection(fcN, "selected", AttributeType.INTEGER, new Integer(0));
	    	fcN = FeatureCollectionTools.addAttributeToFeatureCollection(fcN, "toMergeWithFID", AttributeType.INTEGER, new Integer(0));
			FeatureDataset fcAssigned = new FeatureDataset(fcN.getFeatureSchema());
			
	    	// put all features in a tree for faster search and set the mergeID attributes, as well as mark the which should be merged
	    	int i = 0; int fcount = 88888888;
	    	Quadtree qTree = new Quadtree(); 
	    	ArrayList<Feature> selectedF = new ArrayList<Feature>();
	    	
	    	for (Iterator iterator = fcN.iterator(); iterator.hasNext();) {
				Feature f = (Feature) iterator.next();
				fcount++;
				f.setAttribute("mergeid", new Integer(fcount));
				Object userdata = f.getGeometry().getUserData();
				if (userdata != null){
					i++;
					Boolean isSelected = (Boolean)userdata;
					if(isSelected){
						//-- set the attribute
						f.setAttribute("selected", new Integer(i));
					}
					selectedF.add(f);
				}
				// I also add the features that are selected 
				// because some selected features may be the only ones touching 
				qTree.insert(f.getGeometry().getEnvelopeInternal(), f);
			}
	    	//-- reset the userdata entry otherwise it will be stored for eternity
	    	for (Iterator iterator = features.iterator(); iterator.hasNext();) {
				Feature ftemp = (Feature) iterator.next();
				ftemp.getGeometry().setUserData(null);
			}
	    	// find the polygons to merge with
	    	int sizeS = selectedF.size(); int counterS = 0;
	    	for (Iterator iterator = selectedF.iterator(); iterator.hasNext();) {
				
	    		monitor.report(counterS, sizeS, sSearchingForMergeCandidates);
	    		if(monitor.isCancelRequested()){
	    			return;
	    		}
				
				Feature ftemp = (Feature) iterator.next();
				Geometry gtemp = ftemp.getGeometry();
				Collection candidates = qTree.query(gtemp.getEnvelopeInternal());
				if(this.useArea){
					/*********************************************************
					 * we are interested in the polygon with the biggest area
					 * hence a normal intersection test should be ok
					 ********************************************************/
					double area = -1.0; 
					Feature f2merge = null;
					for (Iterator iterator2 = candidates.iterator(); iterator2.hasNext();) {
						Feature ft = (Feature) iterator2.next();
						// Fix bug 3060942 don't try to union f with itself !
						if (ft.getID() == ftemp.getID()) continue;
						if(gtemp.intersects(ft.getGeometry())){
							double tarea = ft.getGeometry().getArea();
							if(tarea > area){
								f2merge = ft;
								area = tarea;
							}
						}
					}
					//-- now set the with which Feature it should be merged, but check first if
					//   it has already a merge candidate
					int val = (Integer)f2merge.getAttribute("toMergeWithFID");
					if(val != 0){//it has already a merge candidate, so set value to be negative
						int selectedVal = (Integer)ftemp.getAttribute("selected");
						if(selectedVal >0){
							//if it is one of the selected ones
							//then we leave the original mergeid value in the field
						}
						else{
							//otherwise we set
							f2merge.setAttribute("toMergeWithFID", new Integer(-1));
						}
					}
					else{//set the polygon to merge with
						f2merge.setAttribute("toMergeWithFID", (Integer)ftemp.getAttribute("selected"));
					}
					ftemp.setAttribute("toMergeWithFID", (Integer)f2merge.getAttribute("mergeid"));
				}//end (useArea)
				else{
					/*******************************************************************
					 * we are interested in the polygon with the longest common boundary
					 * hence we use the polygon graph
					 ******************************************************************/
					PolygonGraph pg = new PolygonGraph(candidates, null);
					PolygonGraphEdge longestEdge = null; double maxlength = -1;
					for (Iterator iterator2 = pg.nodes.iterator(); iterator2.hasNext();) {
						PolygonGraphNode node = (PolygonGraphNode) iterator2.next();
						//-- get our previously selected polygon
						if(node.realWorldObject.getID() == ftemp.getID()){
							//-- get the the poly with the longest common edge
							for (Iterator iterator3 = node.edges.iterator(); iterator3.hasNext();) {
								PolygonGraphEdge tedge = (PolygonGraphEdge) iterator3.next();
								//we may have several lines for the case that another polygon is in between...
								ArrayList<Geometry> lines = tedge.getBoundaries();
								double length = 0;
								for (Iterator iterator4 = lines.iterator(); iterator4.hasNext();) {
									Geometry line = (Geometry) iterator4.next();
									length = length + line.getLength();
								}
								if(length > maxlength){
									maxlength = length;
									longestEdge = tedge;
								}
							}
						}//end if selected polygon
					}
					// get the feature with the longest shared edge
					Feature fToMerge = null;
					if(longestEdge.node1.realWorldObject.getID() == ftemp.getID()){
						fToMerge = longestEdge.node2.realWorldObject;
					}
					else{
						fToMerge = longestEdge.node1.realWorldObject;
					}
					//-- now set with which Feature it should be merged, but check first if
					//   it has already a merge candidate
					int val = (Integer)fToMerge.getAttribute("toMergeWithFID");
					if(val != 0){//it has already a merge candidate, so set value to be negative
						int selectedVal = (Integer)ftemp.getAttribute("selected");
						if(selectedVal >0){
							//if it is one of the selected ones
							//then we leave the original mergeid value in the field
						}
						else{
							//otherwise we set
							fToMerge.setAttribute("toMergeWithFID", new Integer(-1));
						}
					}
					else{//set the polygon to merge with
						fToMerge.setAttribute("toMergeWithFID", (Integer)ftemp.getAttribute("selected"));
					}
					ftemp.setAttribute("toMergeWithFID", (Integer)fToMerge.getAttribute("mergeid"));
				}
				counterS++;
			}// end loop over all selected features to find the ones to merge
	    	
			//-- so, we figured who has to be merged with whom - but we also should be able
			// to identify those ones polygons that are to be merged with a polygon that
			// needs to be merged too - here some output first
	    	
			List allfeat = qTree.queryAll();
			fcAssigned.addAll(allfeat); 
			//-- un-comment line below for debugging
			//context.addLayer(StandardCategoryNames.RESULT, firstLayer.getName() + "_featuresWithAssigments", fcAssigned);
			
			//-- sorting things out, and do a copy so we do not mess up things
			FeatureDataset resultFList = new FeatureDataset(fcAssigned.getFeatureSchema());
			FeatureDataset selectedFList = new FeatureDataset(fcAssigned.getFeatureSchema());
			FeatureDataset mergeFList = new FeatureDataset(fcAssigned.getFeatureSchema());
			for (Iterator iterator = allfeat.iterator(); iterator.hasNext();) {
				Feature fti = (Feature) iterator.next();
				Feature ft = fti.clone(true);
				int selectedVal = (Integer)ft.getAttribute("selected");
				int toMergeWithVal = (Integer)ft.getAttribute("toMergeWithFID");
				if((selectedVal == 0) && (toMergeWithVal == 0)){
					resultFList.add(ft);
				}
				else if(selectedVal > 0){
					selectedFList.add(ft);
					//add them also to the merge list (since the selected features may be a merge target too)
					mergeFList.add(ft);
				}
				else{
					mergeFList.add(ft);
				}
			}
			//-- do the merge
			int size = selectedFList.size(); int counter = 0;
	    	for (Iterator iterator = selectedFList.iterator(); iterator.hasNext();) {
				
	    		monitor.report(counter, size, sMergingPolygons);
	    		if(monitor.isCancelRequested()){
	    			return;
	    		}
				
	    		Feature ftemp = (Feature) iterator.next();
				int toMergeWithVal = (Integer)ftemp.getAttribute("toMergeWithFID");
				Feature mpoly = getPolyToMerge(toMergeWithVal, mergeFList);
				if (mpoly != null){
					//Do union with targetPoly and
					Collection<Geometry> polygons = new ArrayList<Geometry>();
					polygons.add(mpoly.getGeometry()); 
					polygons.add(ftemp.getGeometry());
					Geometry gp = UnaryUnionOp.union(polygons);
					mpoly.setGeometry(gp);
					// remove the selected poly from the mergeList
					// there should be no need to remove and add the new 
					// poly, since we worked with references only
					mergeFList = removeFromList(mergeFList, ftemp);
				}
				else{
					//just return this without any merge
					resultFList.add(ftemp);
					mergeFList = removeFromList(mergeFList, ftemp);
				}
	    		counter++;
	    	}
	    	//add the remaining merged polygons
	    	resultFList.addAll(mergeFList.getFeatures());
	    	{// remove the additional attributes
		    	FeatureDataset removedAttrFc = new FeatureDataset(originalFeatureSchema);
		    	for (Iterator iterator = resultFList.iterator(); iterator.hasNext();) {
					Feature ftemp = (Feature) iterator.next();
					Feature fnew = FeatureCollectionTools.copyFeatureAndSetFeatureSchema(ftemp, originalFeatureSchema);
					removedAttrFc.add(fnew);
		    	}
		    	resultFList = removedAttrFc;
	    	}
			context.addLayer(StandardCategoryNames.RESULT, firstLayer.getName() + "_" + sMerged, resultFList);
			
	    }//end else - layer size
    	//context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
    }	

	private Feature getPolyToMerge(int featureID, FeatureCollection mergeFList) {
		Feature foundFeature = null;
		for (Iterator iterator = mergeFList.iterator(); iterator.hasNext();) {
			Feature ftemp = (Feature) iterator.next();
			int val = (Integer)ftemp.getAttribute("mergeid");
			if(val == featureID){
				int selectedVal = (Integer)ftemp.getAttribute("selected");
				if(selectedVal == 0){
					foundFeature = ftemp;
				}
				else{// selecteVaL != null, so this poly will be merged as well
					// search for the feature this one should be merged with
					int toMergeWithVal = (Integer)ftemp.getAttribute("toMergeWithFID");
					foundFeature = getPolyToMerge(toMergeWithVal, mergeFList);
				}
			}
		}
		return foundFeature;
	}

	private FeatureDataset removeFromList(FeatureDataset mergeFList, Feature fToDelete) {
		FeatureDataset fdnew = new FeatureDataset(mergeFList.getFeatureSchema());
		int valToDelete = (Integer)fToDelete.getAttribute("mergeid");
		for (Iterator iterator = mergeFList.iterator(); iterator.hasNext();) {
			Feature ftemp = (Feature) iterator.next();
			int valCur = (Integer)ftemp.getAttribute("mergeid");
			if(valToDelete == valCur){
				// don't do anything
			}
			else{
				fdnew.add(ftemp);
			}
		}
		
		return fdnew;
	}
    
}
