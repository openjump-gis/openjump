package org.openjump.core.ui.plugin;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

public abstract class ThreadedBasePlugIn extends AbstractPlugIn implements
  ThreadedPlugIn {

  private TaskMonitorManager taskMonitorManager = new TaskMonitorManager();

  public ThreadedBasePlugIn() {
  }

  public ThreadedBasePlugIn(String name, Icon icon, String toolTip) {
    super(name, icon, toolTip);
  }

  public ThreadedBasePlugIn(String name, Icon icon) {
    super(name, icon);
  }

  public ThreadedBasePlugIn(String name, String toolTip) {
    super(name, toolTip);
  }

  public ThreadedBasePlugIn(String name) {
    super(name);
  }

  public ThreadedBasePlugIn(ImageIcon icon) {
    super(icon);
  }
}
