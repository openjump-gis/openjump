
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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.driver.AbstractOutputDriver;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.DriverDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;


public class SaveDatasetAsPlugIn extends ThreadedBasePlugIn {
    private DriverDialog saveDatasetDialog;
    private Layer layer;

    public SaveDatasetAsPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        saveDatasetDialog = new DriverDialog(context.getWorkbenchFrame(),
        		I18N.get("ui.plugin.SaveDatasetAsPlugIn.save-dataset"), true);
        saveDatasetDialog.initialize(context.getDriverManager().getOutputDrivers());
        GUIUtil.centreOnWindow(saveDatasetDialog);
    }
    
	public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
		return new MultiEnableCheck()
			.add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
			.add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
	}    
    
    public String getName() {
        return I18N.get("ui.plugin.SaveDatasetAsPlugIn.save-dataset-as-old");
    }    

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        layer = context.getSelectedLayer(0);
        saveDatasetDialog.setLayer(layer);
        saveDatasetDialog.setVisible(true);

        return saveDatasetDialog.wasOKPressed();
    }

    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        if (!saveDatasetDialog.wasOKPressed()) {
            return;
        }

        AbstractOutputDriver outputDriver = (AbstractOutputDriver) saveDatasetDialog.getCurrentDriver();
        monitor.report(I18N.get("ui.plugin.SaveDatasetAsPlugIn.saving")+" " + layer.getName() + "...");
        outputDriver.output(layer);
    }
}
