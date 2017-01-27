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


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openjump.core.CheckOS;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridCell;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import de.latlon.deejump.plugin.style.CrossVertexStyle;

public class RasterQueryCursorTool extends NClickTool 
{
	
	/*
	   * Giuseppe Aruta 
	   * Simple plugin that allows to inspect raster cell value for DTM
	   * ver 0.1 2013_05_27
	   */
	
	
	private FeatureDrawingUtil featureDrawingUtil;
	 public static final String LAYER_NAME =I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.values");
    public static final String LAYER = I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.layer");
	 
	 private List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();
    private OpenJUMPSextanteRasterLayer rstLayer = null;
	private GridWrapperNotInterpolated gwrapper = null;
    
    private String lastClick = "-";
    private String previousClick = "-";
    private Coordinate currCoord;
    private String VALUE = I18N.get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.value");
    private String PREVIOUS_CLICK= I18N.get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.previous.value");
   private String INFO = I18N.get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.info");
    private String name;
    
    
    
    WorkbenchToolBar toolBar;
    
    public RasterQueryCursorTool() {
        //this.allowSnapping();
        super (1) ;
              
    }

    
    protected Shape getShape() throws NoninvertibleTransformException {
		//Don't want anything to show up when the user drags. [Jon Aquino]
		return null;
	}
    
    public Icon getIcon() 
    {
    	  return IconLoader.icon("information_16x16.png");
    	    }
    
    /* 
    public Icon getIcon() 
    {
        return  IconLoader.icon("Raster_Info.png");
    }

   
    public Cursor getCursor() 
    {
        for (int i = 0; i < savedCoordinates.size(); i++)
        {
            add((Coordinate)savedCoordinates.get(i));
        }
        return createCursor(IconLoader.icon("Raster_Info_cursor.gif").getImage());
        
    }

    */
    public Cursor getCursor() {
        // [ede 03.2103] linux currently support only 2 color cursors
        Image i = !CheckOS.isLinux() ? IconLoader.image("information_cursor.png")
            : IconLoader.image("information_cursor_2color.gif");
        return createCursor(i);
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
       

        //-- [sstein] now all the raster profile stuff
        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(this.getWorkbench().getContext(), RasterImageLayer.class);        
        if (rLayer==null){
            getPanel().getContext().warnUser(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }
        reportNothingToUndoYet();
    
        
        this.rstLayer = new OpenJUMPSextanteRasterLayer();
        this.rstLayer.create(rLayer);
        

 		
		this.rstLayer.setFullExtent(); // not sure why this needs to be done but it seems to 
									   this.rstLayer.getWindowGridExtent();
		//-- create a gridwrapper to access the cells		
		this.gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
        //this.calculateProfile(getCoordinates(), getWorkbench().getContext());
        //-- this was used for testing
        double rvalue = 0.0;
		Coordinate startCoord = (Coordinate)getCoordinates().get(0);
		GridCell cell = rstLayer.getLayerGridExtent().getGridCoordsFromWorldCoords(startCoord.x, startCoord.y);
		 rvalue = cell.getValue(); //can't use this, since the value will be zero, so I assume the cell 
									 //object is just a place holder for the coordinates
		
		rvalue = gwrapper.getCellValueAsDouble(cell.getX(), cell.getY(), 0); //get value for first band
	double Value = rvalue;
	this.name =rLayer.getName();
 		  this.previousClick = this.lastClick;
 	      if (Double.isNaN(rvalue))
 	        this.lastClick = "    ";
 	      else {
 	        this.lastClick = (Double.toString(rvalue));
 	      }
		 
 	      
 	     Geometry measureGeometry = null;  
 	    if (wasShiftPressed()){
 	    	pixelLayer().getFeatureCollectionWrapper().add(toFeature(measureGeometry,
 	    			pixelLayer().getFeatureCollectionWrapper().getFeatureSchema()));
 	    }
 	     	    
 	   else
       {
 	    	  // In questa maniera l'oggetto precedente  è cancellato //
 	         pixelLayer().getFeatureCollectionWrapper().clear();            
         		pixelLayer().getFeatureCollectionWrapper().add(toFeature(measureGeometry,
         				pixelLayer().getFeatureCollectionWrapper().getFeatureSchema()));
       }	
      	 
 	   getPanel().getContext().setStatusMessage("("+name+") "+VALUE+": " + lastClick + "   ["+PREVIOUS_CLICK+": " + previousClick + "]");
  		
 		
 		
    }
	
      
      private Feature toFeature(Geometry measureGeometry, FeatureSchema schema) throws NoninvertibleTransformException {
  	Feature feature = new BasicFeature(pixelLayer().getFeatureCollectionWrapper().getFeatureSchema());
  		feature.setGeometry(measureGeometry);
          feature.setAttribute("Pixel", lastClick);
          feature.setAttribute(LAYER, name);
          feature.setAttribute("X",new Double(getPoint().getCoordinate().x));
          feature.setAttribute("Y",new Double(getPoint().getCoordinate().y));
          feature.setAttribute("GEOM", getPoint());
  		return feature;
  		}
      
      private Layer pixelLayer() {
          Layer pixelLayer = getPanel().getLayerManager().getLayer(LAYER_NAME);
         // azimuthLayer.setSelectable(true);
         // azimuthLayer.setEditable(true);
          if (pixelLayer != null) {
       
  	return pixelLayer;
    }

     FeatureSchema schema = new FeatureSchema();
     schema.addAttribute("Pixel", AttributeType.DOUBLE);
     schema.addAttribute("X", AttributeType.DOUBLE);
     schema.addAttribute("Y", AttributeType.DOUBLE);
     schema.addAttribute(LAYER, AttributeType.STRING);
     schema.addAttribute("GEOM", AttributeType.GEOMETRY);
     new FeatureSchema(); 
     new FeatureDataset(schema);
              
              
              
             // schema.addAttribute("Points", AttributeType.GEOMETRY);
      
          FeatureCollection featureCollection = new FeatureDataset(schema);
          Layer layer;
          boolean firingEvents = getPanel().getLayerManager().isFiringEvents();
          getPanel().getLayerManager().setFiringEvents(false);
         
      	          try {layer = new Layer(LAYER_NAME, Color.red, featureCollection, getPanel().getLayerManager());
       
      	          
      	          
      	          layer.removeStyle(layer.getVertexStyle());
              	  layer.addStyle(new CrossVertexStyle());
                  layer.getBasicStyle().setLineColor(Color.black);
                  layer.getBasicStyle().setFillColor(Color.black);
                  layer.getBasicStyle().setLineWidth(1);
                  layer.getBasicStyle().setAlpha(255);
                  layer.getBasicStyle().setRenderingLine(true);
                  layer.getBasicStyle().setRenderingFill(false);
                  layer.getVertexStyle().setEnabled(false);
                  layer.getVertexStyle().setSize(1);
                  layer.setDrawingLast(true);
                  
                  LabelStyle labelStyle = layer.getLabelStyle();
                  labelStyle.setAttribute("Pixel");
             	  // labelStyle.setVerticalAlignment(LabelStyle.RIGHT_SIDE);
             	  labelStyle.setVerticalAlignment(LabelStyle.ABOVE_LINE);
             	 labelStyle.setHorizontalPosition(LabelStyle.RIGHT_SIDE);
             	   labelStyle.setHorizontalAlignment(LabelStyle.JUSTIFY_RIGHT);
             	   labelStyle.setEnabled(true);
                   labelStyle.setColor(Color.black);
                   labelStyle.setHeight(14);
                  labelStyle.setOutlineShowing(true);
                  labelStyle.setOutlineColor((Color.white));
             	  //labelStyle.setOutlineWidth(1);
                   labelStyle.setHidingOverlappingLabels(false);
                  labelStyle.setFont(layer.getLabelStyle().getFont().deriveFont(Font.BOLD, 16));
                   // labelStyle.setHideAtScale(true);
          	     
          	  
                  layer.setDrawingLast(true);
                 
                  
             } finally {
                  getPanel().getLayerManager().setFiringEvents(firingEvents);
              }
      
              getPanel().getLayerManager().addLayer(StandardCategoryNames.SYSTEM,
                  layer);
      
              return layer;
         }  
      
      
      
      protected Point getPoint()
    			throws NoninvertibleTransformException {
    			return new GeometryFactory().createPoint(
    				(Coordinate)getCoordinates().get(0));
    		}
   
      
        
  /*    
      
    
      public void mouseLocationChanged(MouseEvent e) 
      {
          try {      if (isShapeOnScreen()) {
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
      
  
      
    
      
      public void mouseMoved(MouseEvent e)
      {
          mouseLocationChanged(e);
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
    	        panel.getContext().setStatusMessage( 
    	            panel.format(distance)) ;
    	    }

  	    
    	    private double distance(List<Coordinate> coordinates) 
    	    {
    	        double distance = 0;

    	        RasterImageLayer rLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(this.getWorkbench().getContext(), RasterImageLayer.class);        
    	         
    	        reportNothingToUndoYet();
    	    
    	        
    	        this.rstLayer = new OpenJUMPSextanteRasterLayer();
    			this.rstLayer.create(rLayer);
    			this.rstLayer.setFullExtent(); // not sure why this needs to be done but it seems to 
    										   this.rstLayer.getWindowGridExtent();
    			//-- create a gridwrapper to access the cells		
    			this.gwrapper = new GridWrapperNotInterpolated(rstLayer, rstLayer.getLayerGridExtent());
    	        //this.calculateProfile(getCoordinates(), getWorkbench().getContext());
    	        //-- this was used for testing
    	        double rvalue = 0.0;
    			Coordinate startCoord = (Coordinate)getCoordinates().get(0);
    			GridCell cell = rstLayer.getLayerGridExtent().getGridCoordsFromWorldCoords(startCoord.x, startCoord.y);
    			 rvalue = cell.getValue(); //can't use this, since the value will be zero, so I assume the cell 
    										 //object is just a place holder for the coordinates
    			rvalue = gwrapper.getCellValueAsDouble(cell.getX(), cell.getY(), 0); //get value for first band
    		double Value = rvalue;

    	        return Value;
    	    }
    */
   public MultiEnableCheck createEnableCheck(
    	            final WorkbenchContext workbenchContext) {
    	        EnableCheckFactory checkFactory = new EnableCheckFactory(
    	                workbenchContext);

    	        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
    	                .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
    	                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
    	                    	               
    	    }
     
}