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
 * created:  		by Vivid Solutions
 * last modified:  	22.05.2005 by sstein
 * 
 * description:
 *  - defines the mouse behaviour (shows a circle on mouse endpoint)
 *  - makes the item selection for the circle
 * 
 *****************************************************/
package org.openjump.core.ui.plugin.edit.helpclassesselection;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class SelectItemsByCircleTool extends DragTool {
    private Shape selectedFeaturesShape;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private List verticesToSnap = null;
    private PlugInContext context = null;
    private double diameter=0; //in m
    private Point mp = null;

    public SelectItemsByCircleTool(PlugInContext context, double diameter, Coordinate initCoo) {
    	this.context =context;
    	this.diameter = diameter;
        setStroke(
            new BasicStroke(
                1,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[] { 3, 3 },
                0));
        this.allowSnapping();       
        this.calculateCircle(initCoo);
        

    }

    /******************  events ********************************/
    protected void gestureFinished() throws java.lang.Exception {
        reportNothingToUndoYet();
        //System.out.println("gesture finnished!");               
    }
   
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    
    

    public Icon getIcon() {
        return IconLoader.icon("");
    }

	/**
	 * overwritten super method to show teh circle on any mouse move
	 */
    public void mouseMoved(MouseEvent e){
        try {
            setViewDestination(e.getPoint());
            redrawShape();
            } 
        catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }
    

    /**
     * overwrite super method since mouseReleased is not needed any more
     */
    public void mouseReleased(MouseEvent e) {
    	
    }

    /**
     * partly overwrite method to do the things i like to
     */
    public void mousePressed(MouseEvent e) {
        try {
        	//-- do now the selection and finnish the cursor tool? 
            //System.out.println("mouse pressed");
                    	
        	
        	Point p = new GeometryFactory().createPoint(this.getModelDestination());
        	this.mp = p;
        	Geometry circle = p.buffer(this.diameter);   	
            this.selectItems(circle);
            /*****************************
             * 
             * hei Jon!
             *  here some code is needed to end the mouse behaviour
             *  and return to normal mouse functionality (like zoom or selection)
             * 
             *****************************/

            /*((AbstractButton) context.getWorkbenchContext()
                    .getWorkbench()
                    .getFrame()
                    .getToolBar()
                    .getButtonGroup()
                    .getElements()
                    .nextElement()).doClick();
           */
            
           //-- A does not work: (modelsource = null) does select a second time ???
           //SelectFeaturesTool sft = new SelectFeaturesTool();
           //context.getLayerViewPanel().setCurrentCursorTool(sft);

           //-- B does work
            
           Enumeration buttons = context.getWorkbenchContext().getWorkbench().
		   		getFrame().getToolBar().getButtonGroup().getElements();
           
           AbstractButton myButton = (AbstractButton)buttons.nextElement();
           //System.out.println(myButton.getClass());
           int j = 0;
           while(buttons.hasMoreElements()){
           		j++;
           		myButton = (AbstractButton)buttons.nextElement();
           		if (j ==1){ //j==1 is move button
           			myButton.doClick();
           		}
                //System.out.println(myButton.getClass());
           }
           
           fireGestureFinished();      	
        } 
        catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    /******************  other methods ********************************/
    
    /**
     * (C) Vivid Solutions
     */
    private Collection verticesToSnap() {
        //Lazily initialized because not used if there are no snapping policies. [Jon Aquino]
        //Envelope viewportEnvelope = getPanel().getViewport().getEnvelopeInModelCoordinates();
    	
    	/***** sstein: commented out (see below in setModelDestination)
    	Envelope viewportEnvelope = this.context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
    	
    	if (verticesToSnap == null) {
            verticesToSnap = new ArrayList();
            for (Iterator i = getPanel().getSelectionManager().getSelectedItems().iterator();
                i.hasNext();
                ) {
                Geometry selectedItem = (Geometry) i.next();
                Coordinate[] coordinates = selectedItem.getCoordinates();

                for (int j = 0; j < coordinates.length; j++) {
                    if (viewportEnvelope.contains(coordinates[j])) {
                        verticesToSnap.add(coordinates[j]);
                    }
                }
            }

            if (verticesToSnap.size() > 100) {
                Collections.shuffle(verticesToSnap);
                verticesToSnap = verticesToSnap.subList(0, 99);
            }
        }
        ****/
        return verticesToSnap;
    }

    /**
     * changed to get circle around mouse pointer
     */
    protected Shape getShape() throws Exception {
    	this.calculateCircle(this.modelDestination);
		return this.selectedFeaturesShape; 
    }

    /**
     * (C) Vivid Solutions
     */
    protected void setModelDestination(Coordinate modelDestination) {
    	
    	/** sstein: commented out 
    	 * since getModelSource() produces error if verticesToSnap() is not null 
    	 * 
        for (Iterator i = verticesToSnap().iterator(); i.hasNext();) {
            Coordinate vertex = (Coordinate) i.next();
            Coordinate displacement = CoordUtil.subtract(vertex, getModelSource());
            Coordinate snapPoint = snap(CoordUtil.add(modelDestination, displacement));            
            if (getSnapManager().wasSnapCoordinateFound()) {
                this.modelDestination = CoordUtil.subtract(snapPoint, displacement);
                return;
            }
        }
        */
        this.modelDestination = modelDestination;

    }

     
	/**
	 * called from constructor and by mouse move event<p>
	 * calculates a cirle around the mouse pointer and converts it to a java shape  
	 * @param middlePoint coordinates of the circle
	 */
    private void calculateCircle(Coordinate middlePoint){
        //--calcualte circle;
    	Point p = new GeometryFactory().createPoint(middlePoint);
    	this.mp = p;
    	Geometry buffer = p.buffer(this.diameter);   	

    	Geometry[] geomArray = new Geometry[1];
    	geomArray[0] = buffer;
    	GeometryCollection gc = geometryFactory.createGeometryCollection(geomArray);
    	try{
    		this.selectedFeaturesShape = this.context.getLayerViewPanel().getJava2DConverter().toShape(gc);
    	}
    	catch(NoninvertibleTransformException e){
    		System.out.println("SelectItemsByCircleTool:Exception " + e);
    	}
    }
    
    /**
     * called on mouse click<p>
     * selects all the items from the selected layer 
     * wich are not(!) disjoint with circle 
     */
    private void selectItems(Geometry circle){
		int count = 0;
		Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
		for (int i = 0; i < selectedLayers.length; i++) {
			Layer actualLayer = selectedLayers[i]; 		
			FeatureCollection fc = context.getSelectedLayer(i).getFeatureCollectionWrapper().getWrappee();
			Collection features = new ArrayList();
			for (Iterator iter = fc.iterator(); iter.hasNext();) {
				Feature element = (Feature) iter.next();
				if(!circle.disjoint(element.getGeometry())){
					features.add(element);
					count++;
				}
			}
			context.getLayerViewPanel().getSelectionManager().getFeatureSelection().selectItems(actualLayer, features);			
		}		
	    final Collection myf = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
		//context.getWorkbenchFrame().setTimeMessage("Items: " + count + ", selected items:" + myf.size());		
		context.getWorkbenchFrame().setTimeMessage(
				I18N.get("org.openjump.core.ui.plugin.edit.helpclassesselection.SelectItemsByCircleTool.layer-items") + ": " + 
				count + 
				", " +
				I18N.get("org.openjump.core.ui.plugin.edit.helpclassesselection.SelectItemsByCircleTool.selected-items") +
				": " +
				myf.size());

    }
}
