// license Licence CeCILL http://www.cecill.info/

package org.openjump.core.ui.plugin.edittoolbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.openjump.core.ui.plugin.edittoolbox.cursortools.CutPolygonTool;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;


/**
* CutPolygonSIGLEPlugIn is an editing tool to cut a polygon using the geometry
* of another polygon.
* To use it, select a Polygon A, click the CutPolygonSIGLEPlugIn in the editing
* toolbox, and draw a polygon B. Result will be the polygon A - B.
* @author ERWAN BOCHER Laboratoire RESO UMR CNRS 6590, Olivier Bonnefont, Ugo Tadei
* @see <a href="http://www.projet-sigle.org">projet-sigle</a>
* @version 2006-05-18
* 
*/
public class CutPolygonSIGLEPlugIn extends AbstractPlugIn
{
    private boolean CutPolygonButtonAdded = false;
    
    /* (non-Javadoc)
     * @see com.vividsolutions.jump.workbench.plugin.AbstractPlugIn#initialize(com.vividsolutions.jump.workbench.plugin.PlugInContext)
     */
    public void initialize(final PlugInContext context) throws Exception
    {
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
    
    public boolean execute(PlugInContext context) throws Exception
    {
        return true;
    }
    
    public void addButton(final PlugInContext context)
    {
        if (!CutPolygonButtonAdded)
        {
            final ToolboxDialog toolbox = ((EditingPlugIn) context.getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext());
            
			// Add a new bar in the Toolbox
            toolbox.addToolBar();
            toolbox.add(CutPolygonTool.create(toolbox.getContext()));
            toolbox.finishAddingComponents();
            toolbox.validate();
            CutPolygonButtonAdded = true;
        }
    }
}


