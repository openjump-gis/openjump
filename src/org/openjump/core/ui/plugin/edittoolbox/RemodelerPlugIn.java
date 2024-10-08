package org.openjump.core.ui.plugin.edittoolbox;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.RemodelerTool;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class RemodelerPlugIn extends AbstractPlugIn {

  private boolean ModelerButtonAdded = false;

  /* (non-Javadoc)
   * @see com.vividsolutions.jump.workbench.plugin.AbstractPlugIn#initialize(com.vividsolutions.jump.workbench.plugin.PlugInContext)
   */
  public void initialize(final PlugInContext context) throws Exception {
    super.initialize(context);

    //add a listener so that when the toolbox dialog opens the constrained tools will be added
    //we can't just add the tools directly at this point since the toolbox isn't ready yet

    context.getWorkbenchContext().getWorkbench().getFrame().addComponentListener(
            new ComponentAdapter() {
              public void componentShown(ComponentEvent e) {
                final ToolboxDialog toolBox = ((EditingPlugIn) context
                        .getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY))
                        .getToolbox(context.getWorkbenchContext());
                toolBox.addComponentListener(new ComponentAdapter() {
                  public void componentShown(ComponentEvent e) {
                    addButton(context);
                  }
                  public void componentHidden(ComponentEvent e) {}
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
    if (!ModelerButtonAdded)
    {
      final ToolboxDialog toolbox = ((EditingPlugIn) context
              .getWorkbenchContext().getBlackboard().get(EditingPlugIn.KEY))
              .getToolbox(context.getWorkbenchContext());

      // Add a new bar in the Toolbox
      toolbox.addToolBar();
      toolbox.add(new RemodelerTool(context.getWorkbenchContext()));
      toolbox.finishAddingComponents();
      toolbox.validate();
      ModelerButtonAdded = true;
    }
  }
}
