package com.vividsolutions.jump.io;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorSupport;

public abstract class AbstractJUMPWriter implements JUMPWriter, TaskMonitorSupport {

  private TaskMonitor taskMonitor;

  @Override
  abstract public void write(FeatureCollection featureCollection,
      DriverProperties dp) throws Exception;

  public void setTaskMonitor(TaskMonitor taskMonitor) {
    this.taskMonitor = taskMonitor;
  }

  public TaskMonitor getTaskMonitor() {
    return taskMonitor;
  }
}
