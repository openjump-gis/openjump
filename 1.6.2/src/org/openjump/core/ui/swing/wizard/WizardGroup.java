package org.openjump.core.ui.swing.wizard;

import java.util.List;

import javax.swing.Icon;

import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public interface WizardGroup {
  public String getName();

  public Icon getIcon();
  
  public List<WizardPanel> getPanels();
  
  public String getFirstId();
  
  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog);
  
  // [mmichaud - 2011-07-14] let the TaskMonitorManager a chance to manage
  // exceptions thrown by a plugin like OpenWizardPlugIn which is running a
  // class implementing WizardGroup.
  // There has been many arguments against the over-use of checked exception
  // but it's better to follow the general design of legacy OpenJUMP code.
  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception;
}
