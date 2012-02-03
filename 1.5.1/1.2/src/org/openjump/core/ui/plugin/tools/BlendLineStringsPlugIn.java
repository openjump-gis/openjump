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

package org.openjump.core.ui.plugin.tools;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class BlendLineStringsPlugIn extends AbstractPlugIn {
    
	private WorkbenchContext workbenchContext;
    
	private String sToolTipText = "huhu!  :)";
    private String sTheBlendTolerance = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.The-blend-tolerance");
    private String sNew = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.New");
    private String TOLERANCE = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Tolerance");
    private String sName = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Blend-LineStrings");
    
    private double blendTolerance = 0.1;

    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuItem(this, new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY }, sName, false, null, this.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
		sToolTipText = "huhu!  :)";
		sTheBlendTolerance = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.The-blend-tolerance");
		sNew = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.New");
		TOLERANCE = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Tolerance");
		sName = I18N.get("org.openjump.core.ui.plugin.tools.BlendLineStringsPlugIn.Blend-LineStrings");
    	
        reportNothingToUndoYet(context);
        
        MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        //context.getLayerViewPanel().setToolTipText(sToolTipText);
        Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(); 
        Feature currFeature = ((Feature) selectedFeatures.iterator().next());
        Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
        LayerManager layerManager = context.getLayerManager();
        FeatureDataset newFeatures = new FeatureDataset(currFeature.getSchema());       
        Vector inputLS = new Vector(selectedFeatures.size());
        
        for (Iterator j = selectedFeatures.iterator(); j.hasNext();)
        {
            Feature f = (Feature) j.next();
            Geometry geo = f.getGeometry();
            
            if (geo instanceof LineString)
            {
                inputLS.add(geo);
            }
        }
        
        Vector outputLS = new Vector(inputLS.size()); //contains all the blended linestrings
        
        while (inputLS.size() > 0)
        {
            //start a new blended linestring
            LineString ls = (LineString)inputLS.get(0);
            CoordinateList blendedCoords = new CoordinateList(ls.getCoordinates());
            inputLS.removeElementAt(0);
            //sequence through remaining input linestrings
            //and find those which can be added to either
            //the beginning or end of the current blended coordinate list
            int currIndex = 0; //index of current linestring in input vector
            while (currIndex < inputLS.size())
            {
                ls = (LineString)inputLS.get(currIndex);
                CoordinateList lsCoords = new CoordinateList(ls.getCoordinates());
                if (blended(blendedCoords, lsCoords))
                {
                    inputLS.removeElementAt(currIndex);
                    currIndex = 0; //start at top since some that were rejected before might add to new string
                }
                else
                {
                    currIndex++;
                }
            }
            
            outputLS.add(new GeometryFactory().createLineString(blendedCoords.toCoordinateArray()));
        }
                           
        for (Iterator i = outputLS.iterator(); i.hasNext();)
        {
            Feature newFeature = (Feature) currFeature.clone();
            newFeature.setGeometry((LineString) i.next());
            newFeatures.add(newFeature);
        }
        
        layerManager.addLayer(selectedCategories.isEmpty()
        ? StandardCategoryNames.WORKING
        : selectedCategories.iterator().next().toString(),
        layerManager.uniqueLayerName(sNew),
        newFeatures);
        
        layerManager.getLayer(0).setFeatureCollectionModified(true);
        layerManager.getLayer(0).setEditable(true);
        
        return true;
    }
    
    private boolean blended(CoordinateList blendedCoords, CoordinateList lsCoords)
    {
        Coordinate start = blendedCoords.getCoordinate(0);
        Coordinate end = blendedCoords.getCoordinate(blendedCoords.size()-1);
        Coordinate first = lsCoords.getCoordinate(0);
        Coordinate last = lsCoords.getCoordinate(lsCoords.size()-1);
        if (start.distance(first) < blendTolerance)
        {
            for (int i = 1; i < lsCoords.size(); i++)
            {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        }
        else if (start.distance(last) < blendTolerance)
        {
            for (int i = lsCoords.size()-2; i >= 0; i--)
            {
                blendedCoords.add(0, lsCoords.getCoordinate(i));
            }
        }
        else if (end.distance(first) < blendTolerance)
        {
            for (int i = 1; i < lsCoords.size(); i++)
            {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }
        }
        else if (end.distance(last) < blendTolerance)
        {
            for (int i = lsCoords.size()-2; i >= 0; i--)
            {
                blendedCoords.add(lsCoords.getCoordinate(i));
            }            
        }
        else
        {
            return false;
        }
        return true;
    }
    
      private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
      {
        dialog.addDoubleField(TOLERANCE, blendTolerance, 6, sTheBlendTolerance);
      }

      private void getDialogValues(MultiInputDialog dialog) {
        blendTolerance = dialog.getDouble(TOLERANCE);
      }

    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
            .add(checkFactory.createOnlyOneLayerMayHaveSelectedFeaturesCheck())
            .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(2));
    }    
}
