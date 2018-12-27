/*
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
 */ 
package org.openjump.core.ui.plugin.datastore.postgis;

import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.datastore.AddDataStoreLayerWizard;
import org.openjump.core.ui.plugin.file.OpenWizardPlugIn;

/**
 * This plugin is a write-only driver for a data source backed by an PostGIS 
 * database.
 */
public class SaveToPostGISPlugIn implements PlugIn {
  
    //public static boolean DEBUG = true;
    private PostGISSaveDataSourceQueryChooser saveChooser;
  
    /**
     * Initializes the plugin by creating the data source and data source query choosers.
     * @see PlugIn#initialize(com.vividsolutions.jump.workbench.plugin.PlugInContext)
     */
    public void initialize(PlugInContext context) {
        SaveToPostGISDataSource dataSource = new SaveToPostGISDataSource(context.getWorkbenchContext());
        //dataSource.setWorkbenchContext(context.getWorkbenchContext());
        saveChooser = new PostGISSaveDataSourceQueryChooser(dataSource, context);

        DataSourceQueryChooserManager.get(
          context.getWorkbenchContext().getWorkbench().getBlackboard()
        ).addSaveDataSourceQueryChooser(saveChooser);

        //PostGISOpenWizard postGISOpenWizard = new PostGISOpenWizard(context.getWorkbenchContext());
        //OpenWizardPlugIn.addWizard(context.getWorkbenchContext(), postGISOpenWizard);
    }

    /**
     * This function always returns false.
     */
    public boolean execute(PlugInContext context) {
        return false;
    }
  
    /**
     * @see PlugIn#getName()
     */
    public String getName() { 
        return "PostGIS Writer"; 
    }
}