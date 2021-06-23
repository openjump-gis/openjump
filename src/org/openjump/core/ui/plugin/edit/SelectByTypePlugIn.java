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

package org.openjump.core.ui.plugin.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AbstractSelection;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class SelectByTypePlugIn extends AbstractPlugIn
{
    private WorkbenchContext workbenchContext;
    private boolean selectEmpty = false;
    private boolean selectPoint = false;
    private boolean selectMultiPoint = false;
    private boolean selectLineString = false;
    private boolean selectLinearRing = false;
    private boolean selectMultiLineString = false;
    private boolean selectPolygon = false;
    private boolean selectMultiPolygon = false;
    private boolean selectGeometryCollection = false;
    private boolean selectedLayersOnly = true;
    private boolean excludeEmptyGeometries = false;
    protected AbstractSelection selection;

    private final static String sSelectByGeometryType = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectByTypePlugIn.Select-by-Geometry-Type");
    private final static String sSelectOnlyTheseTypes = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectByTypePlugIn.Select-only-these-types");
    private final static String sEmptyGeometries = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectByTypePlugIn.Empty-Geometries");
    private final static String sOnSelectedLayersOnly = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectByTypePlugIn.On-selected-layers-only");
    private final static String sExcludeEmptyGeometries = I18N.getInstance().get("org.openjump.core.ui.plugin.edit.SelectByTypePlugIn.Exclude-empty-geometries");
	
    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuPlugin(this,
        		new String[] { MenuNames.EDIT, MenuNames.SELECTION }, 
        		sSelectByGeometryType + "...", 
				false, null, getEnableCheck());
    }
    
    public String getName(){
    	return sSelectByGeometryType;
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        reportNothingToUndoYet(context);
        MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        
        if (! dialog.wasOKPressed()) {return false;}
        
        getDialogValues(dialog);
        LayerViewPanel layerViewPanel = context.getWorkbenchContext().getLayerViewPanel();
        List<Feature> selectedFeatures = new ArrayList<>();
        
        layerViewPanel.getSelectionManager().clear();
        Collection<Layer> layers;
        
        if (selectedLayersOnly)
            layers = context.getWorkbenchContext().getLayerableNamePanel().selectedNodes(Layer.class);
        else
            layers = context.getLayerManager().getLayers();
            
        for (Layer layer : layers)
        {
            selectedFeatures.clear();
            
            if (layer.isVisible())
            {
                FeatureCollection featureCollection = layer.getFeatureCollectionWrapper();
                for (Iterator i = featureCollection.iterator(); i.hasNext();)
                {
                    Feature feature = (Feature) i.next();
                    if (selectFeature(feature))
                    {
                        selectedFeatures.add(feature);
                    }
                }
            }
            if (selectedFeatures.size() > 0)
                layerViewPanel.getSelectionManager().getFeatureSelection().selectItems(layer, selectedFeatures);
        }        
        return true;
    }
      
    private boolean selectFeature(Feature feature)
    {
        Geometry geo = feature.getGeometry();

        if (geo.isEmpty() && excludeEmptyGeometries) return false;
		else if (selectPoint && (geo instanceof Point)) return true;
		else if (selectMultiPoint && (geo instanceof MultiPoint)) return true;
		else if (selectLineString && (geo instanceof LineString)) return true;
		else if (selectLinearRing && (geo instanceof LinearRing)) return true;
		else if (selectMultiLineString && (geo instanceof MultiLineString)) return true;
		else if (selectPolygon && (geo instanceof Polygon)) return true;
		else if (selectMultiPolygon && (geo instanceof MultiPolygon)) return true;
		else if (selectGeometryCollection && (geo instanceof GeometryCollection)) return true;
		else if (selectEmpty && geo.isEmpty()) return true;        
        return false;
    }
    
    private void setDialogValues(MultiInputDialog dialog)
    {
        dialog.addLabel(sSelectOnlyTheseTypes);
        dialog.addCheckBox(sEmptyGeometries, selectEmpty);
        dialog.addCheckBox("Point", selectPoint);
        dialog.addCheckBox("MultiPoint", selectMultiPoint);
        dialog.addCheckBox("LineString", selectLineString);
        dialog.addCheckBox("LinearRing", selectLinearRing);
        dialog.addCheckBox("MultiLineString", selectMultiLineString);
        dialog.addCheckBox("Polygon", selectPolygon);
        dialog.addCheckBox("MultiPolygon", selectMultiPolygon);
        dialog.addCheckBox("GeometryCollection", selectGeometryCollection);
        dialog.addCheckBox(sExcludeEmptyGeometries, excludeEmptyGeometries);
        dialog.addCheckBox(sOnSelectedLayersOnly, selectedLayersOnly);
    }
    
    private void getDialogValues(MultiInputDialog dialog)
    {
        selectEmpty = dialog.getCheckBox(sEmptyGeometries).isSelected();
        selectPoint = dialog.getCheckBox("Point").isSelected();
        selectMultiPoint = dialog.getCheckBox("MultiPoint").isSelected();
        selectLineString = dialog.getCheckBox("LineString").isSelected();
        selectLinearRing = dialog.getCheckBox("LinearRing").isSelected();
        selectMultiLineString = dialog.getCheckBox("MultiLineString").isSelected();
        selectPolygon = dialog.getCheckBox("Polygon").isSelected();
        selectMultiPolygon = dialog.getCheckBox("MultiPolygon").isSelected();
        selectGeometryCollection = dialog.getCheckBox("GeometryCollection").isSelected();
        excludeEmptyGeometries = dialog.getCheckBox(sExcludeEmptyGeometries).isSelected();
        selectedLayersOnly = dialog.getCheckBox(sOnSelectedLayersOnly).isSelected();
    }

    @Override
    public MultiEnableCheck getEnableCheck()
    {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
        		.add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }    
}
