package org.openjump.core.ui.plugin.file;

import java.awt.Component;
import java.util.Map;

import javax.swing.JScrollPane;

import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class ComponentWizardPanel extends JScrollPane implements WizardPanel {

  private String title;
  private String id;

  public ComponentWizardPanel(String title, String id, Component component) {
    this.title = title;
    this.id = id;
    setViewportView(component);
  }

  public void add(InputChangedListener listener) {
  }

  public void enteredFromLeft(Map dataMap) {
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return id;
  }

  public String getInstructions() {
    return null;
  }

  public String getNextID() {
    return null;
  }

  public String getTitle() {
    return title;
  }

  public boolean isInputValid() {
    return true;
  }

  public void remove(InputChangedListener listener) {
  }

}
