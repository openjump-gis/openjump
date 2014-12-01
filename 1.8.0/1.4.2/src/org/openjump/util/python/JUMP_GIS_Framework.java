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

package org.openjump.util.python;

import org.python.core.*;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.feature.Feature;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Field;
import javax.swing.JOptionPane;


public class JUMP_GIS_Framework implements ClassDictInit//, GIS_Framework
{

	public static WorkbenchContext workbenchContext = null;
	private static boolean logInited = false;
	
//	public JUMP_GIS_Framework(WorkbenchContext workbenchContext)
	public static void setWorkbenchContext(WorkbenchContext workContext)
	{
		workbenchContext = workContext;
	}
	
	// this Class gives us control of a module's Jython __dict__ attribute
	public static void classDictInit(PyObject dict) 
	{
		//TODO: make this work
	    final Field fields[] =
	    	JUMP_GIS_Framework.class.getFields();
	    String doc = new String();
	    for (int i = 0; i < fields.length; ++i) {
	    	doc = doc + "\n" + fields[i];
	    }
	    dict.__setitem__("__doc__", new PyString(doc));
	    dict.__delitem__("classDictInit");
	    dict.__delitem__("setWorkbenchContext");
	}

	public static Collection getLayers()
	{
		return (Collection) workbenchContext.getLayerNamePanel().getLayerManager().getLayers();
	}
	
	public static Collection getLayersWithSelectedItems()
	{
		Collection layers = workbenchContext.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
		ArrayList al = new ArrayList();
		for (Iterator i = layers.iterator(); i.hasNext();)
			al.add(i.next());
		return al;
	}
	
	public static Collection getSelectedLayers()
	{
		return (Collection) workbenchContext.getLayerNamePanel().selectedNodes(Layer.class);
	}
	
	public static Collection getSelectedFeatures()
	{
		return (Collection) workbenchContext.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
	}

	public static Collection featuresOnLayer(Layer layer)
	{
		try
		{
			Collection features = workbenchContext.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer);
			ArrayList al = new ArrayList();
			for (Iterator i = features.iterator(); i.hasNext();)
				al.add(i.next());
			return al;
		}
		catch (Exception ex)
		{			
			throw new PyException(Py.ValueError, new PyString("Invalid Layer")); 
		}
	}
	
	public static Collection filterFeatures(Collection features, String[] types)
	{
	    boolean selectEmpty = false;
	    boolean selectPoint = false;
	    boolean selectMultiPoint = false;
	    boolean selectLineString = false;
	    boolean selectLinearRing = false;
	    boolean selectMultiLineString = false;
	    boolean selectPolygon = false;
	    boolean selectMultiPolygon = false;
	    boolean selectGeometryCollection = false;
		Collection filteredFeatures = new ArrayList();

		try
		{
		for (int i = 0; i < types.length; i++)
		{
			if (types[i] != null)
			{
				if      (types[i].equalsIgnoreCase("POINT")) selectPoint = true;
				else if (types[i].equalsIgnoreCase("MULTIPOINT")) selectMultiPoint = true;
				else if (types[i].equalsIgnoreCase("LINESTRING")) selectLineString = true;
				else if (types[i].equalsIgnoreCase("LINEARRING")) selectLinearRing = true;
				else if (types[i].equalsIgnoreCase("MULTILINESTRING")) selectMultiLineString = true;
				else if (types[i].equalsIgnoreCase("POLYGON")) selectPolygon = true;
				else if (types[i].equalsIgnoreCase("MULTIPOLYGON")) selectMultiPolygon = true;
				else if (types[i].equalsIgnoreCase("GEOMETRYCOLLECTION")) selectGeometryCollection = true;
				else if (types[i].equalsIgnoreCase("EMPTY")) selectEmpty = true;
			}
		}
		}
		catch (Exception ex)
		{
			throw new PyException(Py.ValueError, new PyString("Invalid String in list")); 
		}
		
		try
		{
        for (Iterator i = features.iterator(); i.hasNext();)
        {
        	Feature feature = (Feature) i.next();
        	if (feature != null)
        	{
        		if (feature instanceof Feature)
        		{
        			boolean addFeature = false;
        			Geometry geo = feature.getGeometry();
        			
        			if (selectPoint && (geo instanceof Point)) addFeature = true;
        			else if (selectMultiPoint && (geo instanceof MultiPoint)) addFeature = true;
        			else if (selectLineString && (geo instanceof LineString)) addFeature = true;
        			else if (selectLinearRing && (geo instanceof LinearRing)) addFeature = true;
        			else if (selectMultiLineString && (geo instanceof MultiLineString)) addFeature = true;
        			else if (selectPolygon && (geo instanceof Polygon)) addFeature = true;
        			else if (selectMultiPolygon && (geo instanceof MultiPolygon)) addFeature = true;
        			else if (selectGeometryCollection && (geo instanceof GeometryCollection)) addFeature = true;
        			else if (selectEmpty && geo.isEmpty()) addFeature = true;
        			
        			if (addFeature) filteredFeatures.add(feature); 
        		}
        	}
        }
		}
		catch (Exception ex)
		{
			throw new PyException(Py.ValueError, new PyString("Invalid Feature in list")); 
		}
        return filteredFeatures;
	}
	
	public static void clearSelection()
	{
		workbenchContext.getLayerViewPanel().getSelectionManager().clear();
	}
	
	public static void selectFeatures(Collection features)
	{
		try
		{
		LayerViewPanel layerViewPanel = workbenchContext.getLayerViewPanel();

		Collection layers = (Collection) workbenchContext.getLayerNamePanel().getLayerManager().getLayers();

		for (Iterator j = layers.iterator(); j.hasNext();) 
        {
            Layer layer = (Layer) j.next();
            layerViewPanel.getSelectionManager().getFeatureSelection().selectItems(layer, features);
        }
		}
		catch (Exception ex)
		{
			throw new PyException(Py.ValueError, new PyString("Invalid Feature in list")); 
		}
	}
	
	public static void warnUser(String statusMessage)
	{
		workbenchContext.getLayerViewPanel().getContext().warnUser(statusMessage);
	}
	
	public static void showMessage(String message)
	{
		JOptionPane.showMessageDialog(workbenchContext.getLayerViewPanel(), message);
	}
	
	public static int confirmMessage(String message, String title)
	{
		return JOptionPane.showConfirmDialog(workbenchContext.getLayerViewPanel(), message, title, JOptionPane.OK_CANCEL_OPTION);
	}
	
	public static void logMessage(String message, boolean startNewPage)
	{
		if ((!logInited) || (startNewPage))
		{
			logInited = true;
			workbenchContext.getWorkbench().getFrame().getOutputFrame().createNewDocument();
		}
		workbenchContext.getWorkbench().getFrame().getOutputFrame().addText(message);
	}
	
	public static int getInput(String[] map, String title)
	{
        MultiInputDialog dialog = new MultiInputDialog(workbenchContext.getWorkbench().getFrame(), title, true);
        for (int i = 0; i < map.length; i+=2)
        	dialog.addTextField(map[i], map[i+1], 10, null, "");
        dialog.setVisible(true);
        boolean okClicked = dialog.wasOKPressed();
        if (okClicked)
        {
        	for (int i = 0; i < map.length; i+=2)
        		map[i+1] = dialog.getText(map[i]);
        	return 0;
        }
        return 2; //return value for cancel
	}
	
	public static void repaint()
	{
		workbenchContext.getLayerViewPanel().repaint();
	}
}
