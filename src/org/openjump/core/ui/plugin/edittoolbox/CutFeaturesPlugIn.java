package org.openjump.core.ui.plugin.edittoolbox;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.CutFeaturesTool;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.CutPolygonTool;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

public class CutFeaturesPlugIn extends AbstractPlugIn {
	 
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
	            toolbox.add(new CutFeaturesTool(context));
	            toolbox.finishAddingComponents();
	            toolbox.validate();
	            CutPolygonButtonAdded = true;
	        }
	    }
	}


