package org.openjump.core.ui.swing.wizard;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public abstract class AbstractWizardGroup implements WizardGroup {
  private String firstId;

  private String name;

  private List<WizardPanel> panels = new ArrayList<WizardPanel>();

  private Icon icon;

  public AbstractWizardGroup() {
  }

  public AbstractWizardGroup(String name, Icon icon, String firstId) {
    this.name = name;
    this.icon = icon;
    this.firstId = firstId;
  }

  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
  }
  
  public String getFirstId() {
    return firstId;
  }

  public Icon getIcon() {
    return icon;
  }

  public String getName() {
    return name;
  }

  public void addPanel(WizardPanel panel) {
    panels.add(panel);
  }

  public void addPanel(int index, WizardPanel panel) {
    panels.add(index, panel);
  }

  public void removePanel(WizardPanel panel) {
    panels.remove(panel);
  }

  public void removeAllPanels() {
    panels.clear();
  }

  public List<WizardPanel> getPanels() {
    return panels;
  }
}
