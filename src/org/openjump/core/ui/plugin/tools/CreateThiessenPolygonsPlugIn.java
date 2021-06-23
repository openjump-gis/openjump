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
 * created:  05.06.2006
 * last modified:
 * 
 * @author sstein
 * 
 * description:
 *  creates voronoi regions/thiessen polygons from a set of points.
 * The Delauney algorithm used for the triangulation is by L. Paul Chew and
 * his free demonstration java-applet.<p>
 * @see <a href="http://www.cs.cornell.edu/Info/People/chew/Delaunay.html">chew</a>
 *
 *****************************************************/

package org.openjump.core.ui.plugin.tools;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.openjump.core.graph.delauneySimplexInsert.DTriangulationForJTS;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.quadtree.Quadtree;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.tools.OverlayEngine;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 *	Creates voronoi regions from a set of points.
 * 	The Delauney algorithm used for the triangulation is by L. Paul Chew and
 * 	his free demonstration java-applet.<p>
 * 	http://www.cs.cornell.edu/Info/People/chew/Delaunay.html
 *  
 * @author sstein
 *
 **/
public class CreateThiessenPolygonsPlugIn extends AbstractPlugIn implements ThreadedPlugIn{

    private String sName = "Create Thiessen Polygons";
    private String CLAYER = "select point layer";
    private String BLAYER = "background layer to delineate the thiessen polygon size";
    private String sUseBGD = "use background layer";
    
    private String sideBarText = "Creates a Delaunay triangulation and returns the Voronoi regions.";
    private String msgCreateDG = "create triangulation";
    private String msgCreatePolys = "create polygons from voronoi edges";
    private String msgAddAttributesPolys = "add attributes from points";
    private String msgMultiplePointsInPoly = "Error: found multiple points in polygon";    
    //--
    private String msgNoPoint = "no point geometry";
    private Layer itemlayer = null;
    private Layer bckgrdlayer = null;
    private PlugInContext pcontext = null;
    private boolean useBackground = false;
    //--
	private MultiInputDialog dialog;
	private JCheckBox checkbox;
	private JComboBox layerComboBoxBackground;
    
    public void initialize(PlugInContext context) throws Exception {
    		
    		this.CLAYER = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.select-point-layer");
       		this.BLAYER = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.background-layer-to-estimate-the-thiessen-polygon-size");
      		this.sUseBGD = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.use-background-layer");    		 
       		this.sName = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn");
    	    this.sideBarText = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.Creates-a-Delaunay-triangulation-and-returns-the-Voronoi-regions");
    	    this.msgCreateDG = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.create-triangulation");
    	    this.msgCreatePolys = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.create-polygons-from-voronoi-edges");
    	    this.msgNoPoint =I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.no-point-geometry");
    	    this.msgAddAttributesPolys = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.add-attributes-from-points");
    	    this.msgMultiplePointsInPoly = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.CreateThiessenPolygonsPlugIn.Error-found-multiple-points-in-polygon");
    	    	
    		this.pcontext = context;
    	    
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuPlugin(
	    	        this,								//exe
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS}, 	//menu path
	                this.sName + "..." /*+ "{pos:2}"*/, //name methode .getName received by AbstractPlugIn
	                false,			//checkbox
	                null,			//icon
	                createEnableCheck(context.getWorkbenchContext())); //enable check
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
	public boolean execute(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	        
 		dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), this.sName, true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);	    
	    return true;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {
	    dialog.setSideBarDescription(this.sideBarText);	    
    	JComboBox addLayerComboBoxBuild = dialog.addLayerComboBox(this.CLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
    	checkbox = dialog.addCheckBox(this.sUseBGD, this.useBackground);
		checkbox.addItemListener(new MethodItemListener());
		//-- add Background-Layer DropDown .. and enable or disable dependent on checkbox values
    	layerComboBoxBackground = dialog.addLayerComboBox(this.BLAYER, context.getCandidateLayer(0), null, context.getLayerManager());
		layerComboBoxBackground.setEnabled(this.useBackground);
	  }

	private void getDialogValues(MultiInputDialog dialog) {
    	this.itemlayer = dialog.getLayer(this.CLAYER);
    	this.bckgrdlayer = dialog.getLayer(this.BLAYER);
    	this.useBackground = dialog.getBoolean(this.sUseBGD);
	  }
	
	private void updateUIForMethod(){ 	
		//-- if use of background, LAYER selection needs to work
		boolean val=checkbox.isSelected();
		layerComboBoxBackground.setEnabled(val);		  	
		dialog.validate();
	}
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{            		
    	    this.createGraph(context, monitor);
    	    System.gc();    		
    	}
	
	private boolean createGraph(PlugInContext context, TaskMonitor monitor) throws Exception{
	    System.gc(); //flush garbage collector
	    // --------------------------	    
	    //-- get selected items
	    final Collection features = this.itemlayer.getFeatureCollectionWrapper().getFeatures();
	    //-- get objects from background layer (assuming there is only one
	    List bkg = this.bckgrdlayer.getFeatureCollectionWrapper().getFeatures();
	    //--
	    ArrayList points = new ArrayList();
	    Quadtree qtree = new Quadtree(); //-- tree used later to transfer attributes
	    for (Iterator iter = features.iterator(); iter.hasNext();) {
            Feature f = (Feature) iter.next();
            Geometry g = f.getGeometry();
            if(g instanceof Point){
            	if(this.useBackground){
            		// check if point is in one of the polygons of the background-layer
            		boolean isInside = false;
            		for (Iterator iterator = bkg.iterator(); iterator.hasNext();) {
						Feature ftemp = (Feature) iterator.next();
						if (ftemp.getGeometry().covers(g)){
							isInside = true;
						}
					}
            		if(isInside){
    	                points.add(f.getGeometry());
    	                qtree.insert(g.getEnvelopeInternal(), f);
            		}
            	}
            	else{
	                points.add(f.getGeometry());
	                qtree.insert(g.getEnvelopeInternal(), f);
	            }
            }
            else{
                context.getWorkbenchFrame().warnUser(this.msgNoPoint);
            }
        }
	    if (points.size() > 0){
		    monitor.report(this.msgCreateDG);
		    DTriangulationForJTS tri = null;
		    if (this.useBackground == true){
		    	FeatureCollection fc = this.bckgrdlayer.getFeatureCollectionWrapper();
		    	Envelope env = fc.getEnvelope(); 
		    	tri = new DTriangulationForJTS(points, env);
		    }
		    else{
		    	tri = new DTriangulationForJTS(points);
		    }
		    
		    //ArrayList nodes = tri.drawAllSites();	    
		    //FeatureCollection myCollA = FeatureDatasetFactory.createFromGeometry(nodes);	    
			//context.addLayer(StandardCategoryNames.WORKING, "sites", myCollA);
			
			//ArrayList nodes2 = tri.getInitialSimmplexAsJTSPoints();
		    //FeatureCollection myCollD = FeatureDatasetFactory.createFromGeometry(nodes2);	    
			//context.addLayer(StandardCategoryNames.WORKING, "cornerpoints", myCollD);
			
		    //ArrayList edges = tri.drawAllVoronoi(); 
		    //FeatureCollection myCollB = FeatureDatasetFactory.createFromGeometry(edges);	    
		    //context.addLayer(StandardCategoryNames.WORKING, "voronoi edges", myCollB);
		    
		    //ArrayList bbox = new ArrayList(); 
		    //bbox.add(tri.getThiessenBoundingBox());
		    //FeatureCollection myCollE = FeatureDatasetFactory.createFromGeometry(bbox);	    
		    //context.addLayer(StandardCategoryNames.WORKING, "bbox", myCollE);
			
		    monitor.report(this.msgCreatePolys);
		    ArrayList polys = tri.getThiessenPolys();
		    //FeatureCollection myCollC = FeatureDatasetFactory.createFromGeometry(polys);
		    
		    monitor.report(this.msgAddAttributesPolys);
		    //-- add attributes
		    FeatureCollection myCollC = this.transferAttributes(this.itemlayer.getFeatureCollectionWrapper().getFeatureSchema(),
		    		qtree, polys);
		    //-- clip to background polygon
		    if (this.useBackground){
		    OverlayEngine oe = new OverlayEngine();
		        FeatureCollection a = myCollC;
		        FeatureCollection b = this.bckgrdlayer.getFeatureCollectionWrapper();
		        AttributeMapping mapping = new AttributeMapping(a.getFeatureSchema(), new FeatureSchema());
		        FeatureCollection overlay = oe.overlay(a, b, mapping, monitor);
		        myCollC = overlay;
		    }
		    //--
			context.addLayer(StandardCategoryNames.WORKING, "Thiessen polygons", myCollC);
	    }
	    else{
	    	context.getWorkbenchFrame().warnUser(this.msgNoPoint);
	    }
		return true;        		
	}
	
	public FeatureDataset transferAttributes(FeatureSchema fs, Quadtree treeWithFeatures, ArrayList thiessenGeoms){
	    FeatureDataset fd = new FeatureDataset(fs);
		//-- walk through list of polygons and find points that are inside
	    for (Iterator iterator = thiessenGeoms.iterator(); iterator.hasNext();) {
			Geometry poly = (Geometry) iterator.next();
			Feature newFeature = new BasicFeature(fs);
			newFeature.setGeometry(poly);
			//-- find points near by
			Collection candidates = treeWithFeatures.query(poly.getEnvelopeInternal());
			//-- find the candidate points that are inside the thiessen poly
			//   this should only be one
			int pointsInside = 0;
			for (Iterator iterator2 = candidates.iterator(); iterator2.hasNext();) {
				Feature pt = (Feature) iterator2.next();			
				if (poly.contains(pt.getGeometry())){
					//-- create a copy of feature without geometry
					//   and add new thiessen poly geom
					newFeature = pt.clone(false);
					newFeature.setGeometry(poly);
					//-- do some tests
					pointsInside = pointsInside +1;
					if (pointsInside > 1){
						//-- too much points => no unique identification
						//	 this actually should not happen, but one never knows
						this.pcontext.getWorkbenchFrame().warnUser(this.msgMultiplePointsInPoly + ": " + pointsInside);
						//-- reset attributes to zero (i.e. create a new feature)
						if (pointsInside == 2){//=2 to do this only once
							newFeature = new BasicFeature(fs);
							newFeature.setGeometry(poly);
						}
					}
				}
			}
			//-- add thiessen poly (with or without attributes)
			fd.add(newFeature);
		}
	    return  fd;
	}
    
	private class MethodItemListener implements ItemListener{
		
		public void itemStateChanged(ItemEvent e) {
			updateUIForMethod();
		}
	}
	
}
