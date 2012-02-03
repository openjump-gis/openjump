/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 */
/*****************************************************
 * created:  		20.05.2005
 * last modified:  	22.05.2005
 * 
 * description:
 *    selects items within a cirlce of the actual selected layers
 *    and informs about the number of selected items<p>
 * 	  uses selectItemsByCircle class for user interaction and
 * 	  determination of selection
 * 
 *****************************************************/

package org.openjump.core.ui.plugin.edit;

import org.openjump.core.ui.plugin.edit.helpclassesselection.SelectItemsByCircleTool;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;


/**
 * @description:
 *    selects items of the actual layer
 *    and informs about the number of selected items
 * 
 * @author sstein
 *
 */
public class SelectItemsByCircleFromSelectedLayersPlugIn extends AbstractPlugIn{
	
    private String T1 = "diameter";
    private String sidebarstring ="";
	private double diameter = 50;
	
    public void initialize(PlugInContext context) throws Exception {

		this.T1 = I18N.get("org.openjump.core.ui.plugin.edit.SelectItemsByCirlceFromSelectedLayersPlugIn.cirlce-diameter") + ":";
		this.sidebarstring = I18N.get("org.openjump.core.ui.plugin.edit.SelectItemsByCirlceFromSelectedLayersPlugIn.select-items-within-a-circle-from-currently-selected-layers");
		
	    context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
	        new String[]
			{MenuNames.EDIT},
			I18N.get("org.openjump.core.ui.plugin.edit.SelectItemsByCirlceFromSelectedLayersPlugIn.select-items-by-cirlce-from-selected-layers")+"{pos:7}", 
			false, //icon
			null, //icon
            createEnableCheck(context.getWorkbenchContext())); //enable check
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);        
        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));        
    }

	public boolean makeDialogThings(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	    MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), getName(), true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);
	        return true;	
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	  {    	
	    dialog.setSideBarDescription(this.sidebarstring);
	    dialog.addDoubleField(T1,this.diameter,7,T1);    	
	  }

	private void getDialogValues(MultiInputDialog dialog) {
	    this.diameter = dialog.getDouble(T1);

	  }
	
    
	public boolean execute(PlugInContext context) throws Exception{	    
        try
        {
        	this.makeDialogThings(context);
        	Envelope viewportEnvelope = context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
        	double x = viewportEnvelope.getMinX() + viewportEnvelope.getWidth()/2;
        	double y = viewportEnvelope.getMinY() + viewportEnvelope.getHeight()/2;
        	Coordinate initCoords = new Coordinate(x,y);
            SelectItemsByCircleTool sit = new SelectItemsByCircleTool(context, this.diameter, initCoords);
            context.getLayerViewPanel().setCurrentCursorTool(sit);
            
            //-- if an toolbar item should be added use the following? 
            /**
            QuasimodeTool tool = new QuasimodeTool(sit).add(
                    new QuasimodeTool.ModifierKeySpec(true, false, false), null);
            WorkbenchContext wbcontext = context.getWorkbenchContext();
            wbcontext.getWorkbench().getFrame().getToolBar().addCursorTool(tool).getQuasimodeTool();
            **/     
        }
        catch (Exception e)
        {
            context.getWorkbenchFrame().warnUser("SelecItemsByCircleTool Exception:" + e.toString());
            return false;
        }

		System.gc();		
	    return true;
	}

     
}
