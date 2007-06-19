
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

package com.vividsolutions.jump.workbench.ui.plugin;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.driver.AbstractInputDriver;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.DriverDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


//Note: this class is wrapped by AddLayerToCategoryPlugIn.
//[Jon Aquino]
//[sstein] the class is not used anymore, now we use the class 
//         from plugin.datastore package
public class LoadDatasetPlugIn extends ThreadedBasePlugIn {
    protected DriverDialog addLayerDialog;
    private String categoryName;

    public LoadDatasetPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        addLayerDialog = new DriverDialog(context.getWorkbenchFrame(),
        		I18N.get("ui.plugin.LoadDatasetPlugIn.load-dataset"), true);
        addLayerDialog.initialize(context.getDriverManager().getInputDrivers());
        GUIUtil.centreOnWindow(addLayerDialog);
    }

    public boolean execute(PlugInContext context) throws Exception {
        return addLayer(StandardCategoryNames.WORKING);
    }

    public boolean addLayer(String categoryName) throws Exception {
        addLayerDialog.setTitle(I18N.get("ui.plugin.LoadDatasetPlugIn.load-dataset-to")+" " + categoryName +
            " "+I18N.get("ui.plugin.LoadDatasetPlugIn.category"));
        addLayerDialog.setVisible(true);
        this.categoryName = categoryName;

        return addLayerDialog.wasOKPressed();
    }
    
    public String getName() {
        return I18N.get("ui.plugin.LoadDatasetPlugIn.load-dataset-old");
    }


    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        AbstractInputDriver inputDriver = (AbstractInputDriver) addLayerDialog.getCurrentDriver();
        monitor.report(I18N.get("ui.plugin.LoadDatasetPlugIn.loading"));
        inputDriver.input(context.getLayerManager(), categoryName);
    }
    
}
