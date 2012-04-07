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
 * created:  		03.11.2005
 * 
 * Draw a circle for a given radius and a specified 
 * number of vertices per circle quarter<p>
 * 
 *****************************************************/

package org.openjump.core.ui.plugin.edittoolbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawCircleWithGivenRadiusTool;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DelegatingTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;


/**
 * Selects items of the actual layer
 * and informs about the number of selected items
 * 
 * @author sstein
 *
 */
public class DrawCircleWithGivenRadiusPlugIn extends AbstractPlugIn{
	
    private boolean circleButtonAdded = false;
    private String T1 = "diameter";
    private String T2 = "Points";
    private String sidebarstring ="";
	private double diameter = 50;
	private int points = 8; //points per cirlce quarter
	
    public void initialize(final PlugInContext context) throws Exception {

		//this.T1 = I18N.get("org.openjump.core.ui.plugin.edit.SelectItemsByCirlceFromSelectedLayersPlugIn.cirlce-diameter") + ":";
    	this.T1 = "Circle Diameter";
    	this.T2 = "Number of segments per circle quarter";
		this.sidebarstring = "Draw a circle by specifiying the radius, the number of points per circle quarter and the center position by mouse click";
		/**
	    context.getFeatureInstaller().addMainMenuItem(this,
		        new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
				"Draw Cirlce for Given Radius", 
				false, //icon
				null, //icon
	            createEnableCheck(context.getWorkbenchContext())); //enable check
	    **/
	      //add a listener so that when the toolbox dialog opens the constrained tools will be added
        //we can't just add the tools directly at this point since the toolbox isn't ready yet

        context.getWorkbenchContext().getWorkbench().getFrame().addComponentListener(
        new ComponentAdapter()
        { 
            public void componentShown(ComponentEvent e)
            {
                final ToolboxDialog toolBox = ((EditingPlugIn) context.getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext());
                toolBox.addComponentListener(new ComponentAdapter()
                {
                    
                    public void componentShown(ComponentEvent e)
                    {
                        addButton(context);
                    }
                    
                    public void componentHidden(ComponentEvent e)
                    {
                    }
                });
            }
        });  
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);        
        return new MultiEnableCheck()
						.add(checkFactory.createSelectedLayersMustBeEditableCheck());        
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
	    dialog.addIntegerField(T2,this.points, 8,T2);
	  }

	private void getDialogValues(MultiInputDialog dialog) {
	    this.diameter = dialog.getDouble(T1);
	    this.points = dialog.getInteger(T2); 
	  }
	
    
	public boolean execute(PlugInContext context) throws Exception{	    
        try
        {
        	/**
        	this.makeDialogThings(context);
        	Envelope viewportEnvelope = context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
        	double x = viewportEnvelope.getMinX() + viewportEnvelope.getWidth()/2;
        	double y = viewportEnvelope.getMinY() + viewportEnvelope.getHeight()/2;
        	Coordinate initCoords = new Coordinate(x,y);
        	TestDrawCircleByRadiusTool circleTool = new TestDrawCircleByRadiusTool(context, Math.abs(this.diameter), 
        										Math.abs(this.points), initCoords);
        	
            context.getLayerViewPanel().setCurrentCursorTool(circleTool);
            **/
            CursorTool circleTool = DrawCircleWithGivenRadiusTool.create((LayerNamePanelProxy) context.getActiveInternalFrame());
            context.getLayerViewPanel().setCurrentCursorTool(circleTool); 
            
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

    public void addButton(final PlugInContext pcontext)
    {
        if (!circleButtonAdded)
        {
            final ToolboxDialog toolbox = ((EditingPlugIn) pcontext.getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(pcontext.getWorkbenchContext());
            final DelegatingTool cursorTool = (DelegatingTool)DrawCircleWithGivenRadiusTool.create(toolbox.getContext());
            final QuasimodeTool quasimodeTool = new QuasimodeTool(cursorTool);
            quasimodeTool.add(new QuasimodeTool.ModifierKeySpec(true, false, false), null);
            quasimodeTool.add(new QuasimodeTool.ModifierKeySpec(true, true, false), null);
            toolbox.add(quasimodeTool, null);
            toolbox.finishAddingComponents();
            toolbox.validate();
            toolbox.getToolBar().getButton(quasimodeTool.getClass()).addMouseListener(new java.awt.event.MouseAdapter(){
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    try {
                        ((DrawCircleWithGivenRadiusTool)cursorTool.getDelegate()).makeDialogThings(pcontext.getWorkbenchContext().getLayerViewPanel());
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            circleButtonAdded = true;
        }
    }
     
}
