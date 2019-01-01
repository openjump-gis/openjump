
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

package org.openjump.util.python.pythonexampleclasses;

import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.Shape;
import java.util.List;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.EventListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.util.Iterator;

import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.*;

public class DrawCustomTool extends ConstrainedNClickTool {
    private FeatureDrawingUtil featureDrawingUtil;
    private int minClicks = 1;
    private int fireClicks = 2;
    private Icon icon = null;
    private String toolName = "Custom Tool";
    private String geoType = "Point";
    private FeedbackListener feedbackListener = null;
    private FinalDrawListener finalGeoListener = null;
    private ActivationListener activationListener = null;
    private DeActivationListener deActivationListener = null;
    
    public DrawCustomTool(FeatureDrawingUtil featureDrawingUtil)
    {
    	super();
    	drawClosed = false;
    	this.featureDrawingUtil = featureDrawingUtil;
    }
    
	public void activate(LayerViewPanel layerViewPanel) {
		super.activate(layerViewPanel);
	   	if (activationListener != null){
			WorkbenchContext wc = this.getWorkbench().getContext();
			CustomToolEvent toolEvent = new CustomToolEvent(this, null, wc);	   	 
			activationListener.handleActivation(toolEvent);		
	   	}
	}
	
	public void deactivate() {
		cancelGesture();
		super.deactivate();
	   	if (deActivationListener != null){
			WorkbenchContext wc = this.getWorkbench().getContext();
			CustomToolEvent toolEvent = new CustomToolEvent(this, null, wc);	   	 
			deActivationListener.handleDeActivation(toolEvent);		
	   	}
	}

   public void setFireClicks(int fireClicks)
    {
    	this.fireClicks = fireClicks;
    }
    
    public void setMinClicks(int minClicks)
    {
    	this.minClicks = minClicks;
    }
    
    public void setMaxClicks(int maxClicks)
    {
    	this.n = maxClicks; //number of clicks at which to finish the drawing (stored in super)
    }
    
    public void setIcon(Icon icon)
    {
    	this.icon = icon;
    }
    
    public void setToolName(String toolName)
    {
    	this.toolName = toolName;
    }
    
    public void setGeometryType(String geoType)
    {
    	boolean goodType = (geoType.equalsIgnoreCase("POINT") ||
    			geoType.equalsIgnoreCase("LINESTRING") ||
    			geoType.equalsIgnoreCase("POLYGON"));
    	if (goodType)
    		this.geoType = geoType;
    	else
    		throw new PyException(Py.ValueError, new PyString(geoType + ": invalid geometry type")); 
    }
    
    public void setStrokeWidth(int width)
    {
    	super.setStrokeWidth(width);
    }
    
	public interface FinalDrawListener extends EventListener
	{
	    public void finalDraw(CustomToolEvent event);
	}

	public interface FeedbackListener extends EventListener
	{
	    public void feedbackDraw(CustomToolEvent event);
	}

	public interface ActivationListener extends EventListener
	{
	    public void handleActivation(CustomToolEvent event);
	}
    
	public interface DeActivationListener extends EventListener
	{
	    public void handleDeActivation(CustomToolEvent event);
	}
	
	public void setFeedbackListener(FeedbackListener listener)
    {
    	feedbackListener = listener;
    }

    public void setFinalGeoListener(FinalDrawListener listener)
    {
    	finalGeoListener = listener;
    }

    public void setActivationListener(ActivationListener listener)
    {
    	activationListener = listener;
    }

    public void setDeActivationListener(DeActivationListener listener)
    {
    	deActivationListener = listener;
    }

   public String getName() 
    {
    	//Specify name explicitly, otherwise it will be "Draw Custom"
        return toolName;
    }

    public Icon getIcon() 
    {
    	if (icon == null)
    		return new ImageIcon(getClass().getResource("DrawLine.gif")); 
    	else
    		return icon;
    }

    protected void gestureFinished() throws Exception 
	{
        reportNothingToUndoYet();
        Geometry geo = getFinalGeometry();
        if (geo == null) return;
        execute(featureDrawingUtil.createAddCommand(geo,
                isRollingBackInvalidEdits(), getPanel(), this));
    }

    protected Shape getShape() throws NoninvertibleTransformException
    {
    	List coords = new ArrayList(getCoordinates());

        if (coords.size() >= fireClicks)
        {
        	coords.add(tentativeCoordinate);
        	coords = fireFeedbackEvent(coords);
            Point2D firstPoint = getPanel().getViewport().toViewPoint((Coordinate)coords.get(0));
            GeneralPath path = new GeneralPath();
            path.moveTo((float) firstPoint.getX(), (float) firstPoint.getY());

            for (int i = 1; i < coords.size(); i++) 
            {
                Coordinate nextCoordinate = (Coordinate) coords.get(i);
                Point2D nextPoint = getPanel().getViewport().toViewPoint(nextCoordinate);
                path.lineTo((int) nextPoint.getX(), (int) nextPoint.getY());
            }
            return path;
        }
        else
        {
        	return super.getShape();
        }
    }

    protected Geometry getFinalGeometry() throws NoninvertibleTransformException 
	{
    	boolean goodGeo = true;
    	Geometry geo = null;
    	
        if (getCoordinates().size() < minClicks) 
        {
        	getPanel().getContext().warnUser("Must have at least " + minClicks + " points");
            return null;
        }
    	
    	List coords = fireFinalGeoEvent(getCoordinates());
        IsValidOp isValidOp = null;
        
        if (geoType.equalsIgnoreCase("POINT"))
        {
        	if (coords.size() == 1)
        	{
        		geo = new GeometryFactory().createPoint((Coordinate)coords.get(0));
        		isValidOp = new IsValidOp((Point) geo);
        	}
        	else
        	{
        		geo = new GeometryFactory().createMultiPoint(toArray(coords));
        		isValidOp = new IsValidOp((MultiPoint) geo);
        	}
         }
        
        else if (geoType.equalsIgnoreCase("LINESTRING"))
        {
        	geo = new GeometryFactory().createLineString(toArray(coords));
        	isValidOp = new IsValidOp((LineString) geo);
        }
        
        else if (geoType.equalsIgnoreCase("POLYGON"))
        {
        	geo = new GeometryFactory().createPolygon(
        			new GeometryFactory().createLinearRing(toArray(coords)),
					null);
        	isValidOp = new IsValidOp((Polygon) geo);
        }
        else
        {
        	getPanel().getContext().warnUser(geoType + " not a valid type.");
        	return null;
        }
        
        if (!isValidOp.isValid()) 
        {
        	getPanel().getContext().warnUser(isValidOp.getValidationError().getMessage());
        	if (PersistentBlackboardPlugIn.get(getWorkbench().getContext())
					.get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false))
        	{
        		return null;
        	}
        }
        
        return geo;
    }

    protected List fireFeedbackEvent(List coordsIn)
    {
    	if (feedbackListener == null)
    	{
    		return fireFinalGeoEvent(coordsIn);
    	}
    	else
    	{
    		ArrayList coords = new ArrayList();
    		
    		for (Iterator i = coordsIn.iterator(); i.hasNext();)
    			coords.add(((Coordinate) i.next()).clone());
    		
    		WorkbenchContext wc = this.getWorkbench().getContext();
    		CustomToolEvent toolEvent = new CustomToolEvent(this, coords, wc);
    		feedbackListener.feedbackDraw(toolEvent);
    		coords = (ArrayList)toolEvent.getCoords();
    		//statusMessage = toolEvent.getStatusMessage();
    		return coords;
    	}
    }

    protected List fireFinalGeoEvent(List coordsIn)
    {
    	if (finalGeoListener == null)
    	{
    		return coordsIn;
    	}
    	else
    	{
    		ArrayList coords = new ArrayList();
    		
    		for (Iterator i = coordsIn.iterator(); i.hasNext();)
    			coords.add(((Coordinate) i.next()).clone());
    		WorkbenchContext wc = this.getWorkbench().getContext();
    		CustomToolEvent toolEvent = new CustomToolEvent(this, coords, wc);
    		finalGeoListener.finalDraw(toolEvent);
    		coords = (ArrayList)toolEvent.getCoords();
    		//statusMessage = toolEvent.getStatusMessage();
    		return coords;
    	}
    }

    public class CustomToolEvent extends EventObject
	{
    	private List localCoords;
    	private String statusMessage = "";
    	private WorkbenchContext wc;
    	CustomToolEvent(Object source, List coords, WorkbenchContext wc)
		{
    		super(source);
    		localCoords = coords;
    		this.wc = wc;
		}
    	
    	public WorkbenchContext getWc()
    	{
    		return wc;
    	}

    	public List getCoords()
    	{
    		return localCoords;
    	}
    	
    	public void setCoords(List coords)
    	{
    		localCoords = coords;
    	}
    	
    	public void setStatusMessage(String statusMessage)
    	{
    		this.statusMessage = statusMessage;
    	}
    	
    	public String getStatusMessage()
    	{
    		return statusMessage;
    	}
	}
}
