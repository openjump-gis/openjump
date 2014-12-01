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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AbstractSelection;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

public class DeleteEmptyGeometriesPlugIn extends AbstractPlugIn
{
    private WorkbenchContext workbenchContext;
    private MultiInputDialog dialog;
    private boolean exceptionThrown = false;
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
    protected AbstractSelection selection;
    
    String sDeleteEmptyGeometries=I18N.get("org.openjump.core.ui.plugin.tools.DeleteEmptyGeometriesPlugIn.Delete-Empty-Geometries-in-Selection");

    public void initialize(PlugInContext context) throws Exception
    {     
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuItemWithJava14Fix(
        		this, 
				new String[] { MenuNames.TOOLS, MenuNames.TOOLS_QA }, 
				sDeleteEmptyGeometries + "...", 
				false, 
				null, 
				this.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        String sDeleteEmptyGeometries=I18N.get("org.openjump.core.ui.plugin.tools.DeleteEmptyGeometriesPlugIn.Delete-Empty-Geometries-in-Selection");
        
        reportNothingToUndoYet(context);
        ArrayList featuresToDelete = new ArrayList();
        Collection layers = context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
        
        for (Iterator j = layers.iterator(); j.hasNext();)
        {
            Layer layer = (Layer) j.next();
            
            Collection selectedFeatures = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer);
            for (Iterator i = selectedFeatures.iterator(); i.hasNext();)
            {
                Feature feature = (Feature) i.next();
                if (feature.getGeometry().isEmpty())
                    featuresToDelete.add(feature);
            }
            layer.getFeatureCollectionWrapper().removeAll(featuresToDelete);
            featuresToDelete.clear();
        }
        return true;
    }
        
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) 
    {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                                     .add(checkFactory.createAtLeastNFeaturesMustHaveSelectedItemsCheck(1));
    }    
}
