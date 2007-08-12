
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
 * www.ashs.isa.com
 */


package org.openjump.core.ui.plugin.view;

import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.openjump.core.geomutils.GeoUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.ToolTipWriter;

public class MapToolTipPlugIn extends AbstractPlugIn
{
    private class GeoData
    {
        public String type;
        public double distance;
        public int side;
        public double length;
        public double angle;
        public double area;
        public void set(GeoData geoData)
        {
            this.type = geoData.type;
            this.distance = geoData.distance;
            this.side = geoData.side;
            this.length = geoData.length;
            this.angle = geoData.angle;
            this.area = geoData.area;
        }
    }
    
    PlugInContext gContext;
	final static String sErrorSeeOutputWindow =I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Error-See-Output-Window");
	final static String sPoint =I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Point");
	final static String sSide =I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.Side");	
	final static String sLength =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.length");
	final static String sAngle =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.angle");
	final static String sDegrees =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.degrees");
	final static String sNoData =I18N.get("org.openjump.core.ui.plugin.view.MapToolTipPlugIn.No-Data");
	final static String sArea =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.area");
	
    private MouseMotionAdapter mouseMotionAdapter =
    new MouseMotionAdapter()
    {
        public void mouseMoved(MouseEvent e)
        {
        	if (gContext.getWorkbenchContext().getLayerViewPanel() == null) { return; } //[Jon Aquino 2005-08-04]
            ToolTipWriter toolTipWriter = new ToolTipWriter(gContext.getWorkbenchContext().getLayerViewPanel());
            toolTipWriter.setEnabled(gContext.getWorkbenchContext().getLayerViewPanel().getToolTipWriter().isEnabled());
            String fid = toolTipWriter.write("{FID}", e.getPoint());

            if (fid != null)
            {
                String toolTipText = getData(Integer.parseInt(fid), e.getPoint());
                gContext.getWorkbenchContext().getLayerViewPanel().setToolTipText(toolTipText);
            }
        }
    };
               
    public void initialize(final PlugInContext context) throws Exception
    {
        gContext = context;                
        context.getWorkbenchFrame().getDesktopPane().addContainerListener(
        new ContainerListener()
        {
            public void componentAdded(ContainerEvent e)
            {                              
                Component child = e.getChild();
                if (child.getClass().getName().equals("com.vividsolutions.jump.workbench.ui.TaskFrame"))
                {
                    ((TaskFrame)child).getLayerViewPanel().addMouseMotionListener(mouseMotionAdapter);
            	}
            }
            
            public void componentRemoved(ContainerEvent e)
            {
                Component child = e.getChild();
                if (child.getClass().getName().equals("com.vividsolutions.jump.workbench.ui.TaskFrame"))
                {
                    ((TaskFrame)child).getLayerViewPanel().removeMouseMotionListener(mouseMotionAdapter);
            	}
            }
        });
    }
    
    public boolean execute(PlugInContext context) throws Exception
    {
        try
        {
            return true;
        }
        catch (Exception e)
        {
            context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            context.getWorkbenchFrame().getOutputFrame().addText("MapToolTipPlugIn Exception:" + e.toString());
            return false;
        }
    }
    
    private String getData(int fID, Point2D mouseLocation)
    {
        int maxLinesOfData = 10;
        String dataText = "<html>";
        LayerViewPanel panel = gContext.getWorkbenchContext().getLayerViewPanel();
        if (panel == null) {return "";}
        LayerManager layerManager = panel.getLayerManager();
        List layerList = layerManager.getVisibleLayers(false);
        for (Iterator i = layerList.iterator(); i.hasNext();)
        {
            Layer layer = (Layer) i.next();
            FeatureSchema featureSchema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            int numAttribs = featureSchema.getAttributeCount();
            FeatureCollectionWrapper featureCollection = layer.getFeatureCollectionWrapper();
            List featureList = featureCollection.getFeatures();
            
            //for each layer iterate thru featureList
            for (Iterator j = featureList.iterator(); j.hasNext();)
            {
                Feature feature = (Feature) j.next();
                int fid = feature.getID();
                
                if (fid == fID)
                {
                    Geometry geo = feature.getGeometry();
                    try
                    {
                        Coordinate coord = panel.getViewport().toModelCoordinate(mouseLocation);
                        dataText += getGeoData(geo, coord);
                    } catch (NoninvertibleTransformException e)
                    {
                        //let it go
                    }
                    
                    int NumLinesOfData = 1;
                    
                    for (int num = 0; num < numAttribs; num++)
                    {
                        AttributeType type = featureSchema.getAttributeType(num);
                        
                        if (type == AttributeType.STRING)
                        {
                            String name = featureSchema.getAttributeName(num);
                            String data = feature.getString(name).trim();
                            if ((!data.equals("")) && (NumLinesOfData < maxLinesOfData))
                            {
                                dataText += "<br>" + name + ": " + data;
                                NumLinesOfData++;
                            }
                        }
                    }
                    return dataText + "</html>";
                }
            }
        }
        dataText += sNoData + "</html>";
        return dataText;
    }
    
    private String getGeoData(Geometry geo, Coordinate mousePt)
    {
        GeoData geoData = getClosest(geo, mousePt);
        DecimalFormat df2 = new DecimalFormat("##0.0#");
        DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        String geoText = "";
        if (geoData.area > 0)
        	geoText = geoData.type + ": " + sSide + ": " + geoData.side + ", " + sLength + ": " + df3.format(geoData.length) + ", " + sAngle + ": " + df2.format(geoData.angle) + " " + sDegrees + ", " + sArea + ": " + df2.format(geoData.area);
    	else	
    		geoText = geoData.type + ": " + sSide + ": " + geoData.side + ", " + sLength + ": " + df3.format(geoData.length) + ", " + sAngle +": " + df2.format(geoData.angle) + " " + sDegrees;
        if (geoData.type.equals("Point")) geoText = "Point";
     return geoText;
    }  
    
    private GeoData getClosest(Geometry geo, Coordinate mousePt)
    {
        GeoData geoData;
        
        if ((geo.getGeometryType().equals("GeometryCollection")) ||
            (geo.getGeometryType().equals("MultiPoint")) ||
            (geo.getGeometryType().equals("MultiLineString")) ||
            (geo.getGeometryType().equals("MultiPolygon")))
        {
            geoData = getClosest(((GeometryCollection)geo).getGeometryN(0), mousePt);
            for (int i = 1; i < ((GeometryCollection)geo).getNumGeometries(); i++)
            {
                GeoData geoData2 = getClosest(((GeometryCollection)geo).getGeometryN(i), mousePt);
                if (geoData2.distance < geoData.distance)
                {
                    geoData.set(geoData2);
                }
            }
        }
        else
        {
            geo.getCoordinates();
            CoordinateList coords = new CoordinateList();
            UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
            geo.apply(filter);
            coords.add( filter.getCoordinates() ,false);
            
            //need to do this since UniqueCoordinateArrayFilter keeps the poly from being closed
            if ((geo instanceof Polygon) || (geo instanceof LinearRing))
            {
                coords.add(coords.getCoordinate(0));
            }
            
            int maxIndex = coords.size() - 1;
            int side = 1;
            double length = 0;
            double angle = 0;
            Coordinate p0;
            Coordinate p1;
            double distToClosestSide = mousePt.distance(coords.getCoordinate(0));
            
            if (coords.size() > 1)
            {
                p0 = coords.getCoordinate(0);
                p1 = coords.getCoordinate(1);
                length = p0.distance(p1);
                angle = GeoUtils.getBearing180(p0, p1);
                distToClosestSide = GeoUtils.getDistance(mousePt, p0, p1);
            }
            
            for (int i = 1; i < maxIndex; i++)
            {
                p0 = coords.getCoordinate(i);
                p1 = coords.getCoordinate(i+1);
                double distToSide = GeoUtils.getDistance(mousePt, p0, p1);
                
                if (distToSide < distToClosestSide)
                {
                    side = i + 1;
                    length = p0.distance(p1);
                    angle = GeoUtils.getBearing180(p0, p1);
                    distToClosestSide = distToSide;
                }
            }
            geoData = new GeoData();
            geoData.type = geo.getGeometryType();
            geoData.side = side;
            geoData.length = length;
            geoData.angle = angle;
            geoData.distance = distToClosestSide;
            geoData.area = geo.getArea();
        }
        return geoData;
    }
 }

// this code shows how to add an ItemListener to a menu item
// so that you know when a menu item changes
//    private ItemListener itemListener =
//    new ItemListener()
//    {
//        public void itemStateChanged(ItemEvent e)
//        {
////                   String chk = context.getFeatureInstaller().menuBar().getMenu(2).getItem(13).getText();
//            java.awt.Toolkit.getDefaultToolkit().beep();
//        }
//    };

// add this to the initializer
//        context.getFeatureInstaller().menuBar().getMenu(2).getItem(13).addItemListener(itemListener);
