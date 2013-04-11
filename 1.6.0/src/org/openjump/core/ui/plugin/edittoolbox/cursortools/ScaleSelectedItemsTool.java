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
 * Stefan Steiniger
 * perriger@gmx.de
 *
 */
/*****************************************************
 * created:  		11.12.2005
 * last modified:  	28.01.2006 cursor change on mouse over corner
 * 
 * author: sstein
 * 
 * description:
 *  - scales selected items (using the bounding box) 
 * 
 *****************************************************/
package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.cursortool.ShortcutsDescriptor;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool.ModifierKeySpec;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.snap.SnapManager;

public class ScaleSelectedItemsTool extends DragTool implements ShortcutsDescriptor{
	
    private String scaleSelectedItems = "Scale Selected Items"; 
    private String sScaleFactor = "scale factor";    

    private EnableCheckFactory checkFactory;
    private Shape selectionBBoxShape;
    private Geometry originalItemsAsLines=null;
    private Geometry outlineItems=null;
    //private Shape selectedItemsShape=null;
    private Shape outlineItemsShape=null;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private List verticesToSnap = null;
    private Coordinate centerCoord;
    private Geometry originalBBox = null;
    private double xscale = 0.0;
    private double yscale = 0.0;
    private Coordinate mousePos = null;
    private Coordinate center = null;
    DecimalFormat df2 = new DecimalFormat("##0.0#");
    boolean startScaling = false;
    double toleranceFactor = 2.0;
    private BasicStroke originalStroke = null;
    boolean somethingChanged = false;
    int style = 1;
	Cursor cursor2 = createCursor(IconLoader.icon("MoveVertexCursor.gif").getImage());
	Cursor cursor1 = null;
    
    public ScaleSelectedItemsTool(EnableCheckFactory checkFactory) {
        
    	this.scaleSelectedItems =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.ScaleSelectedItemsTool.Scale-Selected-Items");
    	this.sScaleFactor =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.ScaleSelectedItemsTool.scale-factor");    
    	
    	this.checkFactory = checkFactory;
        
        this.originalStroke =  new BasicStroke(
                1,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[] { 3, 3 },
                0);        
        setStroke(this.originalStroke);
        this.style=1;
        allowSnapping();    	
        this.cursor1 = this.getCursor();
    }

    protected void gestureFinished() throws java.lang.Exception { 	
    	//System.out.println("gesture finished");    	
    	if (this.startScaling == true){
	        reportNothingToUndoYet();
	        ArrayList transactions = new ArrayList();
	        for (Iterator i = getPanel().getSelectionManager().getLayersWithSelectedItems().iterator();
	            i.hasNext();
	            ) {
	            Layer layerWithSelectedItems = (Layer) i.next();
	            transactions.add(createTransaction(layerWithSelectedItems));
	        }
	        EditTransaction.commit(transactions);
	        this.startScaling=false;
	        //-- calc new box
    		this.originalBBox = createSelectedItemsBoundingBox();
    		this.selectionBBoxShape = getPanel().getJava2DConverter().toShape(this.originalBBox);
    		this.outlineItemsShape = this.selectionBBoxShape; //-- do this only to show inital box 

	    	/*
	    	//-- visualize box
	        Polygon box = (Polygon)this.originalBBox.clone(); //cloned!
	    	PolygonScale.scalePolygon(box,xscale, yscale,center.x,center.y);
	    	ArrayList geoms = new ArrayList();
	    	geoms.add(box);
	    	FeatureCollection myFC = FeatureDatasetFactory.createFromGeometry(geoms);
	    	getPanel().getLayerManager().addLayer(StandardCategoryNames.WORKING, "fbox", myFC);
	    	*/
	    	this.startScaling = false;
	    	somethingChanged = true;
    	}
    }
    
    /**
    private EditTransaction createTransactionPolygon(Layer layer) {
        EditTransaction transaction =
            EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                    Geometry item = (Geometry) j.next();                      
                    if(item instanceof Polygon){ 
                    	PolygonScale.scalePolygon((Polygon)item,xscale, yscale, center.x,center.y);
                    }
                    else{
                    	getPanel().getContext().warnUser("no polygon!");
                    }                    
                }
                return geometryWithSelectedItems;
            }
        }, getPanel(), getPanel().getContext(), getName(), layer, isRollingBackInvalidEdits(), false);
        return transaction;
    }  
    **/
    
    private EditTransaction createTransaction(Layer layer) {
        EditTransaction transaction =
            EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                    Geometry item = (Geometry) j.next();
                    scale(item);
                }
                return geometryWithSelectedItems;
            }
        }, getPanel(), getPanel().getContext(), getName(), layer, isRollingBackInvalidEdits(), false);
        return transaction;
    }  

    private void scale(Geometry geometry) {
        geometry.apply(new CoordinateFilter() {
            public void filter(Coordinate coordinate) {
                coordinate.x=center.x+xscale*(coordinate.x-center.x);
                coordinate.y=center.y+yscale*(coordinate.y-center.y);
              }
        });
    }
    
    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("ScalePolygon.gif"));        
    }

    public String getName() {
        return scaleSelectedItems;
    }

    public void activate(LayerViewPanel panel){
    	super.activate(panel);
    	try{
    		this.originalBBox = createSelectedItemsBoundingBox();
    		this.selectionBBoxShape = getPanel().getJava2DConverter().toShape(this.originalBBox);
    		this.outlineItemsShape = this.selectionBBoxShape; //-- do this only to show inital box 
    												   //   later we will replace it by the item geometries
            this.setStroke(this.originalStroke);
    		this.setColor(Color.RED);
            this.style=1;
    		somethingChanged = true;
    	}
    	catch (Throwable t) {
    		getPanel().getContext().handleThrowable(t);
    	}
    }
    
    public void mousePressed(MouseEvent e) {
    	//System.out.println("mouse pressed");
        this.setStroke(this.originalStroke);
		this.setColor(Color.RED);
        this.style=1;
        //---------------
    	try{
    		this.setMousePos(getPanel().getViewport().toModelCoordinate(e.getPoint())); //-- includes snap
    		} 
    	catch (Throwable t) {
    		getPanel().getContext().handleThrowable(t);
    	}    	
    	//-- check if mouse is withing a certain distance of the boundingBox
		double tolerance = SnapManager.getToleranceInPixels(this.getWorkbench().getBlackboard()) / this.getPanel().getViewport().getScale();
		//-- calc a buffer around the corner points.. so that only near to corner points scaling is activated
		LineString ls =  (LineString)this.originalBBox.getBoundary();
		MultiPoint mps = new GeometryFactory().createMultiPoint(ls.getCoordinates());
		Geometry buffergeom = mps.buffer(tolerance*this.toleranceFactor);
		Point mousep = new GeometryFactory().createPoint(this.mousePos);
		/*
	        //-- visualize
	        ArrayList geoms = new ArrayList();
	        geoms.add(buffergeom);
	        FeatureCollection myFC = FeatureDatasetFactory.createFromGeometry(geoms);
	        getPanel().getLayerManager().addLayer(StandardCategoryNames.WORKING, "buffer", myFC);
        */
    	if (buffergeom.contains(mousep)){    		
	        try {
	            if (!check(checkFactory.createAtLeastNFeaturesMustBeSelectedCheck(1))) {
	                return;
	            }
	
	            if (!check(checkFactory.createSelectedItemsLayersMustBeEditableCheck())) {
	                return;
	            }
	            verticesToSnap = null;                     
	            super.mousePressed(e);
	            this.setSelectedItemsOutlines(); //creates a geometry of all polygon items
	    		this.originalBBox = createSelectedItemsBoundingBox();
	    		//-- union should be done with the transformed polygon to linestring!
	    		//   boundary delivers the next lower dimension (OGC Simple Features spec)
	    		LineString lsBBox = (LineString)this.originalBBox.getBoundary();
	    		this.outlineItems = this.originalItemsAsLines.union(lsBBox); //clone    		
	    		//-- test: use this if drawing the bounding box only (=> change therfore #getShape() as well)
	    		//this.selectionBBoxShape = getPanel().getJava2DConverter().toShape(this.originalBBox);
	    		this.outlineItemsShape = getPanel().getJava2DConverter().toShape(this.outlineItems);
	            //-- set centroid on first press
	            this.center = this.getFarestPoint(this.originalBBox, this.mousePos); //clone since it should not change!
	            /* 
	            //-- visualize
	            ArrayList geoms = new ArrayList();
	            geoms.add(new GeometryFactory().createPoint(this.center));
	            FeatureCollection myFC = FeatureDatasetFactory.createFromGeometry(geoms);
	            getPanel().getLayerManager().addLayer(StandardCategoryNames.WORKING, "CenterSet", myFC);
	            */
	            
	            this.startScaling = true;	
	            somethingChanged = true;
	        } catch (Throwable t) {
	            getPanel().getContext().handleThrowable(t);
	        }
    	}
    }    

    public void mouseDragged(MouseEvent e){
    	if (startScaling == true){
	    	super.mouseDragged(e);
	        try {    	
		    	this.setMousePos(getPanel().getViewport().toModelCoordinate(e.getPoint())); //-- includes snap
		        double dxMouse=Math.abs(this.center.x- this.mousePos.x);        
		        double dyMouse=Math.abs(this.center.y- this.mousePos.y);
		        this.xscale = dxMouse / this.originalBBox.getEnvelopeInternal().getWidth();
		        this.yscale = dyMouse / this.originalBBox.getEnvelopeInternal().getHeight();
		        //-- attention: key must be pressed before mouse button is pressed
		        //   otherwise it wont be recognized
		        if (e.isShiftDown()){
		        	//System.out.println("key pressed");
		        	this.yscale=this.xscale;
		        }
		        getPanel().getContext().setStatusMessage(sScaleFactor+ " x: " + df2.format(xscale) + "  " + sScaleFactor + " y: " + df2.format(yscale));
		        /*
		        //-- reset shape of selectedFeatureShape = bbox
		        Polygon box = (Polygon)this.originalBBox.clone(); //cloned!
		    	PolygonScale.scalePolygon(box,xscale, yscale,center.x,center.y);	    	
		       	this.selectionBBoxShape = getPanel().getJava2DConverter().toShape(box);
		       	*/
		        Geometry geoms = (Geometry)this.outlineItems.clone();
		        this.scale(geoms);
		        this.outlineItemsShape = getPanel().getJava2DConverter().toShape(geoms);
		        somethingChanged = true;
	        }
	        catch (Throwable t) {
	        	getPanel().getContext().handleThrowable(t);
	        }
    	}
    }
    /* //overwrite method
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }
    */
    
    public void mouseMoved(MouseEvent e){
    	try{
    		this.setMousePos(getPanel().getViewport().toModelCoordinate(e.getPoint())); //-- includes snap
    		double tolerance = SnapManager.getToleranceInPixels(this.getWorkbench().getBlackboard()) / this.getPanel().getViewport().getScale();
    		//-- getBoundary().getBoundary schould return the corner points
    		//Geometry buffergeom = this.originalBBox.getBoundary().buffer(tolerance*this.toleranceFactor);
    		//-- calc a buffer around the corner points.. so that only near to corner points scaling is activated
    		LineString ls =  (LineString)this.originalBBox.getBoundary();
    		MultiPoint mps = new GeometryFactory().createMultiPoint(ls.getCoordinates());
    		Geometry buffergeom = mps.buffer(tolerance*this.toleranceFactor);
    		Point mousep = new GeometryFactory().createPoint(this.mousePos);
        	if (buffergeom.contains(mousep)){
        		//-- this does not work
        		if (this.style == 1){        		
	        		//this.setStroke(new BasicStroke(2));
	                this.getPanel().setCursor(this.cursor2);
			        this.style = 2;
			        this.somethingChanged = true;
        		}
        	}        	
        	else{
        		if ((this.style == 2) /*|| (this.isShapeOnScreen()== true)*/){
	                //this.setStroke(this.originalStroke);
	                this.getPanel().setCursor(this.cursor1);	                
			        this.style = 1;
			        this.somethingChanged = true;			        
        		}
        	}
        	if(somethingChanged == true){
    			this.redrawShape(); 	
    			somethingChanged = false;
    		}
    	}
        catch (Throwable t) {
        	getPanel().getContext().handleThrowable(t);
        }
    }

    private Geometry createSelectedItemsBoundingBox(){
    //private Shape createSelectedItemsBoundingBox() throws NoninvertibleTransformException {
        Collection selectedGeos = (getPanel().getSelectionManager().getSelectedItems());
        double xmin=0, xmax=0, ymin=0, ymax=0;
        int count = 0;
        for (Iterator iter = selectedGeos.iterator(); iter.hasNext();) {
			Geometry element = (Geometry) iter.next();
			if (count == 0){
				xmin=element.getEnvelopeInternal().getMinX();
				xmax=element.getEnvelopeInternal().getMaxX();
				ymin=element.getEnvelopeInternal().getMinY();
				ymax=element.getEnvelopeInternal().getMaxY();
			}
			else{				
				if(element.getEnvelopeInternal().getMinX() < xmin){
					xmin=element.getEnvelopeInternal().getMinX();
				}
				if(element.getEnvelopeInternal().getMaxX() > xmax){
					xmax=element.getEnvelopeInternal().getMaxX();
				}
				if(element.getEnvelopeInternal().getMinY() < ymin){
					ymin=element.getEnvelopeInternal().getMinY();
				}
				if(element.getEnvelopeInternal().getMaxY() > ymax){
					ymax=element.getEnvelopeInternal().getMaxY();
				}
			}
			count++;
		}
        
        Coordinate[] coords = new Coordinate[]{new Coordinate(xmin,ymin), 
        		new Coordinate(xmin,ymax), new Coordinate(xmax, ymax), 
				new Coordinate(xmax,ymin), new Coordinate(xmin,ymin)};
        LinearRing ring = new GeometryFactory().createLinearRing(coords);
        Geometry geo = new GeometryFactory().createPolygon(ring, null);
        this.centerCoord = geo.getCentroid().getCoordinate(); 
        return geo;
    }


    private void setSelectedItemsOutlines() throws NoninvertibleTransformException {
        Collection selectedGeos = (getPanel().getSelectionManager().getSelectedItems());
        Geometry geo = null;
        for (Iterator iter = selectedGeos.iterator(); iter.hasNext();) {
			Geometry element = (Geometry) iter.next();
			if (geo==null){
				if (element instanceof Polygon){
					geo=element.getBoundary();
				}
				else{
					geo=(Geometry)element.clone();
				}
			}
			else{
				//-- boundary delivers the next lower dimension geometry (polys=> lines)
				//   specified by OGC simple features
				if (element instanceof Polygon){
					geo = geo.union(element.getBoundary());
				}
				else{
					geo = geo.union((Geometry)element.clone());
				}
			}
		}
        this.originalItemsAsLines = geo; 
    }


    /**
     * changed to show bounding box or geometries
     */
    protected Shape getShape(){
    	return this.outlineItemsShape;
		//return this.selectionBBoxShape; 		
    }
    
    public void deactivate(MouseEvent e){
    	super.deactivate();
    	this.cleanup((Graphics2D)getPanel().getGraphics());
    }
    
    protected void setMousePos(Coordinate destination) {
        this.mousePos = snap(destination);
   }   
    
   private Coordinate getFarestPoint(Geometry box, Coordinate point){
   	Coordinate farestp = null;
   	double maxDist=0, dist = 0;
   	Coordinate[] coords = box.getCoordinates();
   	for(int i=0; i < coords.length; i++){
   		dist = GeoUtils.distance(coords[i], point);   			
   		if (dist > maxDist){
   			maxDist = dist;
   			farestp = (Coordinate)coords[i].clone();
   		}
   	}
   	return farestp;
   }
   
   public Map<ModifierKeySpec, String> describeShortcuts() {
     Map map = new HashMap();
     map.put(new ModifierKeySpec(new int[] { KeyEvent.VK_SHIFT }),
         I18N.get(this.getClass().getName() + ".keep-aspect-ratio"));
     return map;
   }
}
