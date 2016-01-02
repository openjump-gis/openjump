package com.vividsolutions.jump.task;


public class TaskMonitorUtil {

  /**
   * set the title on nextgen {@link TaskMonitorV2}
   * @param monitor
   * @param title
   */
  public static void setTitle( TaskMonitor monitor, String title ){
    if (monitor instanceof TaskMonitorV2)
      ((TaskMonitorV2)monitor).setTitle(title);
  }
}
