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
  
  public void run(WizardDialog dialog, TaskMonitor monitor);
}
