package org.openjump.core.ui.plugin.edittoolbox;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectMultiItemsTool;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.SelectOneItemTool;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Created by UMichael on 31/05/2015.
 */
public class SelectMultiItemsPlugIn extends AbstractPlugIn {

    private boolean selectMultiItemsButtonAdded = false;

    public void initialize(final PlugInContext context) throws Exception {

        //add a listener so that when the toolbox dialog opens the constrained tools will be added
        //we can't just add the tools directly at this point since the toolbox isn't ready yet

        context.getWorkbenchContext().getWorkbench().getFrame().addComponentListener(
                new ComponentAdapter() {
                    public void componentShown(ComponentEvent e){
                        final ToolboxDialog toolBox = ((EditingPlugIn) context.getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext());
                        toolBox.addComponentListener(new ComponentAdapter() {

                            public void componentShown(ComponentEvent e)
                            {
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
        if (!selectMultiItemsButtonAdded) {
            final ToolboxDialog toolbox = ((EditingPlugIn) context.getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY)).getToolbox(context.getWorkbenchContext());
            toolbox.addToolBar();
            toolbox.add(new SelectMultiItemsTool());
            toolbox.finishAddingComponents();
            toolbox.validate();
            selectMultiItemsButtonAdded = true;
        }
    }
}
