/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
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
 */

package org.openjump.core.ui.plugin.edittoolbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.NoteTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

/**
 * This PlugIn creates a planar graph from features displayed in view and
 * creates a polygon from the area containing the user click. If this area is 
 * not closed, the PlugIn start again with the whole datasets (visible datasets
 * only). If there is still no closed area around the click position, a warning
 * is thrown in the status bar.
 * @author Micha&euml;l Michaud
 */
public class FillPolygonPlugIn extends AbstractPlugIn {
    
    private boolean fillPolygonButtonAdded = false;
    
    public void initialize(final PlugInContext context) throws Exception {
        //add a listener so that when the toolbox dialog opens the constrained tools will be added
        //we can't just add the tools directly at this point since the toolbox isn't ready yet
        
        context.getWorkbenchContext().getWorkbench().getFrame().addComponentListener(
        new ComponentAdapter() { 
            public void componentShown(ComponentEvent e) {
                final ToolboxDialog toolBox =
                    ((EditingPlugIn)context.getWorkbenchContext()
                                           .getBlackboard()
                                           .get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext());
                
                toolBox.addComponentListener(new ComponentAdapter() {
                    
                    public void componentShown(ComponentEvent e) {
                        addButton(context);
                    }
                    
                    public void componentHidden(ComponentEvent e) {
                    }
                });
            }
        });        
    }
  
    public boolean execute(PlugInContext context) throws Exception {
        return true;
    }
    
    public void addButton(final PlugInContext context) {
        if (!fillPolygonButtonAdded) {
            final ToolboxDialog toolbox = 
                ((EditingPlugIn) context.getWorkbenchContext()
                                        .getBlackboard().get(EditingPlugIn.KEY))
                                        .getToolbox(context.getWorkbenchContext());
            //toolbox.addToolBar();
            toolbox.add(new FillPolygonTool(context.getWorkbenchContext()));
            toolbox.finishAddingComponents();
            toolbox.validate();
            fillPolygonButtonAdded = true;
        }
    }
}

