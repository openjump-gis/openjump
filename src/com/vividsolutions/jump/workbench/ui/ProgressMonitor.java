package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;

import javax.swing.*;
import java.awt.*;

public abstract class ProgressMonitor extends JPanel implements TaskMonitor {

  private final Component component;

  public ProgressMonitor(Component component) {
    this.component = component;
    setLayout(new BorderLayout());
    add(component, BorderLayout.CENTER);
    setOpaque(false);
  }

  protected Component getComponent() {
    return component;
  }

  protected abstract void addText(String s);

  public void report(String description) {
    addText(description);
  }

  public void report(int itemsDone, int totalItems, String itemDescription) {
    addText(itemsDone + " / " + totalItems + " " + itemDescription);
  }

  public void report(Exception exception) {
    addText(StringUtil.toFriendlyName(exception.getClass().getName()));
  }

  public void allowCancellationRequests() {
  }

  public boolean isCancelRequested() {
    return false;
  }
}
