package com.vividsolutions.jump.task;

import java.util.HashMap;
import java.util.Hashtable;

public class TaskMonitorV2Util extends TaskMonitorUtil{

  /**
   * set the title on nextgen {@link TaskMonitorV2}
   */
  public static void setTitle(TaskMonitor monitor, String title) {
    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2) monitor).setTitle(title);
  }

  public static void report(TaskMonitor monitor, long itemsDone, long totalItems,
      String itemDescription) {
    if (monitor == null)
      return;
    
    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2) monitor).report(itemsDone, totalItems, itemDescription);
    else
      monitor.report((int)itemsDone, (int)totalItems, itemDescription);
  }

}
