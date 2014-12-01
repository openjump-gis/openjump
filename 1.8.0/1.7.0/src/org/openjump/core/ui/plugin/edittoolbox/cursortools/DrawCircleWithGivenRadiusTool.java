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
 * 					28.01.2006 new MultiInputDialog without cancel button
 * 
 * description:
 *  draws a circle for a given radius (showing the circle on mouse endpoint) 
 * 
 *****************************************************/
package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
//import org.openjump.core.ui.MultiInputDialogWithoutCancel;

public class DrawCircleWithGivenRadiusTool extends NClickTool{
	
	//---- from DragTool -----------------
    /** Modify using #setDestination */
    protected Coordinate modelDestination = null;
    //------------------------------------
    private FeatureDrawingUtil featureDrawingUtil;    
    private Shape selectedFeaturesShape;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private static double radius= 50.0; //in m
    private int points = 8;
    private static double tolerance = 0.1;
    //private String T1 = "Circle Radius";
	private String T1 = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Cirlce-Radius") + ":";
	//private String T2 = "Number of segments per circle quarter";
	private String T2 = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Number-of-segments-per-circle-quarter") + ":";
	//private String name = "Draw circle with given radius and center.";
	private String name = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Draw-circle-with-given-radius-and-center");
	//private String sidebarstring = "Draws a circle by specifying the radius, the number of segments per circle quarter and the center position by mouse click";
	private String sidebarstring = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Draws-a-circle-by-specifiying-the-radius-and-the-circle-accuracy-and-the-centre-position-by-mouse-click");
	private String sAccuracy = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.Circle-Accuracy");
	private String sReset = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool.too-small-tolerance-reset-to-300-segments");
	    
	private DrawCircleWithGivenRadiusTool(FeatureDrawingUtil featureDrawingUtil) {
		super(1);
		this.featureDrawingUtil = featureDrawingUtil;
        setStroke(
                new BasicStroke(
                    1,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,
                    0,
                    new float[] { 3, 3 },
                    0));
        this.allowSnapping();       
	}
		
    public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
        FeatureDrawingUtil featureDrawingUtil = new FeatureDrawingUtil(layerNamePanelProxy);
        
        return featureDrawingUtil.prepare(
        		new DrawCircleWithGivenRadiusTool(featureDrawingUtil), true);
    }

    /******************  events ********************************/
	protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();
        
    	Point p = new GeometryFactory().createPoint(this.getModelDestination());
    	//-- calculate number of points per quarter
    	double tmp = Math.acos((DrawCircleWithGivenRadiusTool.radius-DrawCircleWithGivenRadiusTool.tolerance)/DrawCircleWithGivenRadiusTool.radius);
    	if (tmp != 0){
    		int pts = (int)Math.floor((Math.PI/tmp)/4.0);
    		if(pts < 3){
    			pts = 3;
    		}
    		if(pts > 300){
    			pts = 300;
    			AbstractCursorTool.workbenchFrame(this.getPanel()).warnUser(sReset);
    		}
    		this.points=pts;
    	}    	 
    	//-------------------
    													//points-1 = segments
    	Geometry circle = BufferOp.bufferOp(p,DrawCircleWithGivenRadiusTool.radius,this.points-1);
    	this.checkCircle(circle);
    	
    	if (circle instanceof Polygon){ 
            featureDrawingUtil.drawRing(
                    (Polygon)circle,
                    isRollingBackInvalidEdits(),
                    this,
                    getPanel());       
    	}    	
    }
	
    protected boolean checkCircle(Geometry circle) throws NoninvertibleTransformException {
        IsValidOp isValidOp = new IsValidOp(circle);

        if (!isValidOp.isValid()) {
            getPanel().getContext().warnUser(isValidOp.getValidationError()
                                                      .getMessage());

            if (getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
                return false;
            }
        }

        return true;
    }
    
    public void activate(LayerViewPanel layerViewPanel){
    	super.activate(layerViewPanel);
    	try{
	    	Envelope viewportEnvelope = layerViewPanel.getViewport().getEnvelopeInModelCoordinates();
	    	double x = viewportEnvelope.getMinX() + viewportEnvelope.getWidth()/2;
	    	double y = viewportEnvelope.getMinY() + viewportEnvelope.getHeight()/2;
	    	Coordinate initCoords = new Coordinate(x,y);
	    	this.calculateCircle(initCoords, layerViewPanel);
	    	redrawShape();
    	}
    	catch(Exception e){
    		System.out.println(e);
    	}
    }

        
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    
    
    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("DrawCircleByRadius.gif"));
    }

	/**
	 * overwritten super method to show the circle on any mouse move
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
    
    /******************  other methods ********************************/
    
    /**
     * changed to get circle around mouse pointer
     */
    protected Shape getShape(){
    	this.calculateCircle(this.modelDestination, this.getPanel());
		return this.selectedFeaturesShape; 
    }   
     
	/**
	 * called from constructor and by mouse move event<p>
	 * calculates a cirle around the mouse pointer and converts it to a java shape  
	 * @param middlePoint coordinates of the circle
	 */
    private void calculateCircle(Coordinate middlePoint, LayerViewPanel panel){
        //--calculate circle;
    	Point p = new GeometryFactory().createPoint(middlePoint);
    	Geometry buffer = p.buffer(DrawCircleWithGivenRadiusTool.radius);   	

    	Geometry[] geomArray = new Geometry[1];
    	geomArray[0] = buffer;
    	GeometryCollection gc = geometryFactory.createGeometryCollection(geomArray);
    	try{
    		this.selectedFeaturesShape = panel.getJava2DConverter().toShape(gc);
    	}
    	catch(NoninvertibleTransformException e){
    		System.out.println("DrawCircleWithGivenRadiusTool:Exception " + e);
    	}
    }
 
	public boolean makeDialogThings(LayerViewPanel panel) throws Exception {
		LayerViewPanelContext context = (LayerViewPanelContext)panel.getContext();
		WorkbenchFrame fr = (WorkbenchFrame)(context);
	    MultiInputDialog dialog = new MultiInputDialog(fr, getName(), true);
	    dialog.setCancelVisible(false);
	    setDialogValues(dialog);
	    GUIUtil.centreOnWindow(dialog);
	    dialog.setVisible(true);
	    if (! dialog.wasOKPressed()) { return false; }
	    getDialogValues(dialog);
	    return true;	
	}
	
    private void setDialogValues(MultiInputDialog dialog) {    	
	    dialog.setSideBarDescription(this.sidebarstring);
	    dialog.addDoubleField(T1,DrawCircleWithGivenRadiusTool.radius,7,T1); 
	    dialog.addDoubleField(this.sAccuracy,DrawCircleWithGivenRadiusTool.tolerance, 7,this.sAccuracy);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        DrawCircleWithGivenRadiusTool.radius = dialog.getDouble(T1);
        DrawCircleWithGivenRadiusTool.tolerance = dialog.getDouble(this.sAccuracy); 
    }
	
    //----------- from drag tool ------------//
       
    protected void setViewDestination(Point2D destination) throws NoninvertibleTransformException {
        this.setModelDestination(getPanel().getViewport().toModelCoordinate(destination));
    }    

    protected void setModelDestination(Coordinate destination) {
        this.modelDestination = snap(destination);
    }    
}
