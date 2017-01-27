package com.vividsolutions.jump.task;

public class TaskMonitorUtil {

  /**
   * set the title on nextgen {@link TaskMonitorV2}
   */
  public static void setTitle(TaskMonitor monitor, String title) {
    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2) monitor).setTitle(title);
  }

  /**
   * set message on autochecked monitor object
   */
  public static void report(TaskMonitor monitor, String message) {
    if (monitor != null)
      monitor.report(message);
  }

  public static void report(TaskMonitor monitor, int itemsDone, int totalItems,
      String itemDescription) {
    if (monitor != null)
      monitor.report(itemsDone, totalItems, itemDescription);
  }

  public static boolean isCancelRequested(TaskMonitor monitor){
    if (monitor != null)
      return monitor.isCancelRequested();
    return false;
  }
}
