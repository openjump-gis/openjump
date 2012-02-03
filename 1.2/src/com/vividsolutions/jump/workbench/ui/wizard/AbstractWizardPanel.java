package com.vividsolutions.jump.workbench.ui.wizard;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;

public class AbstractWizardPanel extends JPanel implements WizardPanel {
  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private String id;

  private String nextId;

  private String instructions;

  private String title;

  private Map data;

  public AbstractWizardPanel() {
    id = getClass().getName();
    title = I18N.get(id);
    instructions = I18N.get(id + ".instructions");
  }

  public AbstractWizardPanel(final String id, final String title,
    final String instructions) {
    this.id = id;
    this.title = title;
    this.instructions = instructions;
  }

  public AbstractWizardPanel(final String id, final String nextId,
    final String title, final String instructions) {
    this.id = id;
    this.nextId = nextId;
    this.title = title;
    this.instructions = instructions;
  }

  public void add(final InputChangedListener listener) {
    listeners.add(listener);
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

  protected void fireInputChanged() {
    for (InputChangedListener listener : listeners) {
      listener.inputChanged();
    }
  }

  public void enteredFromLeft(final Map data) {
    this.data = data;
  }

  public Map getData() {
    return data;
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return id;
  }

  public String getInstructions() {
    return instructions;
  }

  public String getNextID() {
    return nextId;
  }

  protected void setNextID(final String nextId) {
    this.nextId = nextId;
  }

  public String getTitle() {
    return title;
  }

  public boolean isInputValid() {
    return true;
  }

}
