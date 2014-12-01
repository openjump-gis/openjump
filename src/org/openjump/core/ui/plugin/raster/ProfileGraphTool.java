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

package org.openjump.core.ui.plugin.raster;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;
import org.openjump.core.ui.plot.Plot2DPanelOJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProfileGraphTool extends MultiClickTool 
{
    private final static String sDistance = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.Distance");
    private final static String sMeters = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.meters");
    private final static String sFeet = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.feet");
    
    private List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();
    private Coordinate currCoord;
    private OpenJUMPSextanteRasterLayer rstLayer = null;
	private GeometryFactory gf = new GeometryFactory();
	private FeatureCollection resultFC = null;
	private FeatureSchema resultFSchema = null;
	private double dDist = 0,dHorzDist = 0;
	private double m_dLastX, m_dLastY, m_dLastZ;
	private int nPoints = 0;
	
	//private FeatureDatasetFactory fdf = new FeatureDatasetFactory();
    //private GridWrapperNotInterpolated gwrapper = null;
    
    public ProfileGraphTool() 
    {
        this.allowSnapping();
        //-- do on init        
        this.resultFSchema = new FeatureSchema();
        this.resultFSchema.addAttribute("geometry", AttributeType.GEOMETRY);
        this.resultFSchema.addAttribute("X", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("Y", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("Z", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("PlaneDist", AttributeType.DOUBLE);
        this.resultFSchema.addAttribute("TerrainDist", AttributeType.DOUBLE);
        this.resultFC = new FeatureDataset(this.resultFSchema);
    }

    public Icon getIcon() 
    {
        return new ImageIcon(getClass().getResource("RulerM_F.gif"));
    }

    public Cursor getCursor() 
    {
        for (int i = 0; i < savedCoordinates.size(); i++)
        {
            add((Coordinate)savedCoordinates.get(i));
        }
        return createCursor(new ImageIcon(getClass().getResource("RulerCursorM_F.gif")).getImage());
    }

    public void mouseLocationChanged(MouseEvent e) 
    {
        try {
            if (isShapeOnScreen()) {
                ArrayList<Coordinate> currentCoordinates = new ArrayList<Coordinate>(getCoordinates());
                currentCoordinates.add(getPanel().getViewport().toModelCoordinate(e.getPoint()));
                display(currentCoordinates, getPanel());
            }

            currCoord = snap(e.getPoint());
            super.mouseLocationChanged(e);
        } 
        catch (Throwable t) 
        {
            getPanel().getContext().handleThrowable(t);
        }
    }

    public void mousePressed(MouseEvent e) 
    {
        super.mousePressed(e);
        savedCoordinates = new ArrayList<Coordinate>(getCoordinates());
    }
    
      protected void gestureFinished() throws NoninvertibleTransformException{
        reportNothingToUndoYet();
        savedCoordinates.clear();
        
        //Status bar is cleared before #gestureFinished is called. So redisplay
        //the length. [Jon Aquino]
        display(getCoordinates(), getPanel());

        //-- [sstein] now all the raster profile stuff
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(this.getWorkbench().getContext(), RasterImageLayer.class);        
        if (rLayer==null){
            getPanel().getContext().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }
		this.rstLayer = new OpenJUMPSextanteRasterLayer();
        // [mmichaud 2013-05-25] false : this is a temporary image not a file based image
		this.rstLayer.create(rLayer, false);
		this.rstLayer.setFullExtent(); // not sure why this needs to be done but it seems to 
									   // be necessary (otherwise I get an NPE when 
		                               // doing this.rstLayer.getWindowCellSize())		
        GridExtent extent = this.rstLayer.getWindowGridExtent(); // not sure if this needs to be done - but it was in the Sextante class
		//-- clear the resultFC
		this.resultFC.clear();
		this.nPoints = 0;
		//-- create a gridwrapper to access the cells		
		//this.gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
        this.calculateProfile(getCoordinates(), getWorkbench().getContext());
        /* //-- this was used for testing
        double rvalue = 0.0;
		Coordinate startCoord = (Coordinate)getCoordinates().get(0);
		GridCell cell = rstLayer.getLayerGridExtent().getGridCoordsFromWorldCoords(startCoord.x, startCoord.y);
		// rvalue = cell.getValue(); //can't use this, since the value will be zero, so I assume the cell 
									 //object is just a place holder for the coordinates
		rvalue = gwrapper.getCellValueAsDouble(cell.getX(), cell.getY(), 0); //get value for first band
		//--output
		getPanel().getContext().setStatusMessage("starting point value: " + rvalue);
		*/
    }

    private void display(List<Coordinate> coordinates, LayerViewPanel panel)
        throws NoninvertibleTransformException 
    {
        display(distance(coordinates), panel);
    }
    
    private void display(double distance, LayerViewPanel panel)
    {
        DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        String distString = df3.format(distance / 0.3048);
        panel.getContext().setStatusMessage(sDistance+ ": " +
            panel.format(distance) + " " + sMeters + " " + " = " + distString + " feet");
    }

    private double distance(List<Coordinate> coordinates) 
    {
        double distance = 0;

        for (int i = 1; i < coordinates.size(); i++) 
        {
            distance += ((Coordinate) coordinates.get(i - 1)).distance((Coordinate) coordinates.get(i));
        }

        if ((currCoord != null) && (coordinates.size() > 1))
        {
            distance -= ((Coordinate) coordinates.get(coordinates.size() - 2)).distance((Coordinate) coordinates.get(coordinates.size() - 1));
            distance += ((Coordinate) coordinates.get(coordinates.size() - 2)).distance(currCoord);
        }

        return distance;
    }
    
    private void calculateProfile(List<Coordinate> coordinates, WorkbenchContext context){
    	
		//-- create a linestring
		Coordinate[] coords = new Coordinate[coordinates.size()];
		int i = 0;
		for (Iterator iterator = coordinates.iterator(); iterator.hasNext();) {
			Coordinate c = (Coordinate) iterator.next();
			coords[i] = c; 
			i++;
		}
		LineString line = gf.createLineString(coords);
		this.processLine(line);
		PlugInContext pc = context.createPlugInContext();
		if((this.resultFC != null) && (this.resultFC.size()>0)){
			pc.addLayer(StandardCategoryNames.RESULT, I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.profile-pts"), this.resultFC);
		}
		//-- graph stuff
		ShowProfile myScorePlot = new ShowProfile(this.resultFC);

	    	Plot2DPanelOJ plot = myScorePlot.getPlot();
    	
	        // FrameView fv = new FrameView(plot);
	        // -- replace the upper line by:
	        JInternalFrame frame = new JInternalFrame(I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot"));
	        frame.setLayout(new BorderLayout());
	        frame.add(plot, BorderLayout.CENTER);
	        frame.setClosable(true);
	        frame.setResizable(true);
	        frame.setMaximizable(true);
	        frame.setSize(450, 450);
	        frame.setVisible(true);
	        
	        context.getWorkbench().getFrame().addInternalFrame(frame);
	        
    }
    
	private void processLine(Geometry line){

		double x,y,x2,y2;
		Coordinate[] coords = line.getCoordinates();

		for (int i = 0; i < coords.length - 1; i++){
			x = coords[i].x;
			y = coords[i].y;
			x2 = coords[i + 1].x;
			y2 = coords[i + 1].y;
			processSegment(x,y,x2,y2);
		}

	}

	private void processSegment(double x, double y, double x2, double y2){

		double	dx, dy, d, n;

		dx	= Math.abs(x2 - x);
		dy	= Math.abs(y2 - y);

		if( dx > 0.0 || dy > 0.0 ){
			if( dx > dy ){
				dx	/= this.rstLayer.getWindowCellSize();
				n	 = dx;
				dy	/= dx;
				dx	 = this.rstLayer.getWindowCellSize();
			}
			else{
				dy	/= this.rstLayer.getWindowCellSize();
				n	 = dy;
				dx	/= dy;
				dy	 = this.rstLayer.getWindowCellSize();
			}

			if(x2 < x ){
				dx = -dx;
			}

			if( y2 < y ){
				dy = -dy;
			}

			for(d=0.0; d<=n; d++, x+=dx, y+=dy){
				addPoint(x,y);
			}
		}

	}

	private void addPoint(double x, double y){

		double z;
		double dDX, dDY, dDZ;

		z = this.rstLayer.getValueAt(x, y);

		if( this.nPoints == 0 ){
			dDist =  0.0;
			dHorzDist	= 0.0;
		}
		else{
			dDX = x - m_dLastX;
			dDY = y - m_dLastY;
			if (this.rstLayer.isNoDataValue(z) ||this.rstLayer.isNoDataValue(m_dLastZ)){
				dDZ = 0.0;
			}
			else{
				dDZ = z - m_dLastZ;
			}
			dDist += Math.sqrt(dDX * dDX + dDY * dDY);
			dHorzDist += Math.sqrt(dDX * dDX + dDY * dDY + dDZ * dDZ);
		}

		m_dLastX = x;
		m_dLastY= y;
		m_dLastZ = z;

		this.nPoints++;

		Point geometry = new GeometryFactory().createPoint(new Coordinate(x,y));
		Feature fpoint = new BasicFeature(this.resultFSchema);
		fpoint.setGeometry(geometry);
		fpoint.setAttribute("X", new Double(x));
		fpoint.setAttribute("Y", new Double(y));
		fpoint.setAttribute("Z", new Double(z));
		fpoint.setAttribute("PlaneDist", new Double(dDist));
		fpoint.setAttribute("TerrainDist", new Double(dHorzDist));
		
		/*//--graph stuff
		if (!this.rstLayer.isNoDataValue(z)){
			serie.add(dDist, z);
		}
		*/
		this.resultFC.add(fpoint);

	}
	
} 
    final class ShowProfile extends JFrame{

    	Plot2DPanelOJ plot = null;

    	public ShowProfile(FeatureCollection fc){

    		// Build a 2D data set	    
    		double[][] datas1 = new double [fc.size()][2];
    		for (int j = 0; j < fc.size(); j++) {
    			Feature f = (Feature)fc.getFeatures().get(j);
    			datas1[j][0] = (Double)f.getAttribute("PlaneDist");
    			datas1[j][1] = (Double)f.getAttribute("Z");			
    		}
    		// Build the 2D scatterplot of the datas in a Panel
    		// LINE, SCATTER, BAR, QUANTILE, STAIRCASE, (HISTOGRAMM?)		
    		Plot2DPanelOJ plot2dA = new Plot2DPanelOJ();
    		plot2dA.addLinePlot("graph",datas1);
    		//plot2dA.addScatterPlot("pts",datas1);
    		//====================
    		plot2dA.setAxisLabel(0,I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance"));
    		plot2dA.setAxisLabel(1,I18N.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values"));
    		// Display a Frame containing the plot panel
    		//new FrameView(plot2dA);		
    		this.plot = plot2dA;

    	}   

    	public Plot2DPanelOJ getPlot(){
    		return this.plot;
    	}
}
