/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Stefan Steiniger
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
 * Stefan Steiniger
 * perriger@gmx.de
 * 
 */

package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.ChangeCoordinateSystemPlugIn;

public class ProjectionPlugIn extends AbstractPlugIn
{
    private WorkbenchContext workbenchContext;
    
    //final static String sDeleteEmptyGeometries=I18N.get("org.openjump.core.ui.plugin.tools.DeleteEmptyGeometriesPlugIn.Delete-Empty-Geometries-in-Selection");
    final static String sName="test projections";

    public void initialize(PlugInContext context) throws Exception
    {        
        workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuItem(
        		new ChangeCoordinateSystemPlugIn(), 
				new String[] {MenuNames.TOOLS}, 
				sName, 
				false, 
				null, 
				this.createEnableCheck(workbenchContext));
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {

        return true;
    }
        
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) 
    {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createTaskWindowMustBeActiveCheck());
    }    
}
