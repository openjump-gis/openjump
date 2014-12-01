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

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


public class ConvexHullPlugIn extends AbstractPlugIn implements ThreadedPlugIn {
    
    private String LAYER = I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Source-Layer");
    private MultiInputDialog dialog;

    public ConvexHullPlugIn() {
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
      categoryName = value;
    }
    
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuItem(
            this,
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
            new JMenuItem(this.getName() + "..."),
            createEnableCheck(context.getWorkbenchContext())); 
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

     public boolean execute(PlugInContext context) throws Exception {
     	//[sstein, 16.07.2006] put here again for language settings
        LAYER = I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Source-Layer");
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes. [Jon Aquino]
        initDialog(context);
        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return false;
        }

        return true;
    }

    public String getName(){
    	return I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Convex-Hull-on-Layer");
    }
     
    private void initDialog(PlugInContext context) {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Convex-Hull-on-Layer"), true);

        //dialog.setSideBarImage(IconLoader.icon("Overlay.gif"));
        dialog.setSideBarDescription(
        		 I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Creates-a-new-layer-containing-the-convex-hull-of-all-the-features-in-the-source-layer"));
        String fieldName = LAYER;
        JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName, context.getCandidateLayer(0), null, context.getLayerManager());
        GUIUtil.centreOnWindow(dialog);
    }

    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception 
    {
        FeatureCollection a = dialog.getLayer(LAYER).getFeatureCollectionWrapper();
        FeatureCollection hullFC = convexHhull(monitor, a);
        
        if (hullFC == null) return;
        
        context.getLayerManager().addCategory(categoryName);
        context.addLayer(categoryName, I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Convex-Hull"), hullFC);
    }

    private FeatureCollection convexHhull(TaskMonitor monitor, FeatureCollection fc) {
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("ui.plugin.analysis.ConvexHullPlugIn.Computing-Convex-Hull") + "...");

        int size = fc.size();
        GeometryFactory geomFact = null;
        
        if (size == 0) return null;
        int count = 0;
        Geometry[] geoms = new Geometry[size];
        
        for (Iterator i = fc.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            Geometry geom = f.getGeometry();
            if (geom == null)
            	continue;
        	if (geomFact == null) 
        		geomFact = geom.getFactory();

            geoms[count++] = geom;
        }
        GeometryCollection gc = geomFact.createGeometryCollection(geoms);
        Geometry hull = gc.convexHull();
        List hullList = new ArrayList();
        hullList.add(hull);

        return FeatureDatasetFactory.createFromGeometry(hullList);
    }
}
