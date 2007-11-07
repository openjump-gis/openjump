package org.openjump.core.ui.plugin;

import java.util.List;

import javax.swing.Icon;

import org.openjump.core.ui.swing.wizard.WizardGroup;

import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class AbstractWizardPlugin extends AbstractThreadedUiPlugIn {

  private WizardGroup wizard;

  private WizardDialog dialog;

  public AbstractWizardPlugin() {
  }

  public AbstractWizardPlugin(Icon icon) {
    super(icon);
  }

  public AbstractWizardPlugin(String name, Icon icon, String toolTip) {
    super(name, icon, toolTip);
  }

  public AbstractWizardPlugin(String name, Icon icon) {
    super(name, icon);
  }

  public AbstractWizardPlugin(String name, String toolTip) {
    super(name, toolTip);
  }

  public AbstractWizardPlugin(String name) {
    super(name);
  }

  public boolean execute(PlugInContext context) throws Exception {
    Registry registry = workbenchContext.getRegistry();

    WorkbenchFrame workbenchFrame = context.getWorkbenchFrame();
    dialog = new WizardDialog(workbenchFrame, getName(),
      context.getErrorHandler());

    wizard.initialize(workbenchContext, dialog);
    List<WizardPanel> panels = wizard.getPanels();
    String firstId = wizard.getFirstId();
    if (panels.isEmpty() || firstId == null) {
      return true;
    }

    dialog.init(panels);
    dialog.setCurrentWizardPanel(firstId);
    dialog.pack();
    dialog.setVisible(true);
    if (dialog.wasFinishPressed()) {
      return true;
    } else {
      return false;
    }
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    wizard.run(dialog, monitor);
  }

  public WizardGroup getWizard() {
    return wizard;
  }

  protected void setWizard(WizardGroup wizard) {
    this.wizard = wizard;
  }

}
