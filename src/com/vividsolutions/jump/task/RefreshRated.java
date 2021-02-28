package com.vividsolutions.jump.task;

public interface RefreshRated {
  /**
   * retrieve the interval for updating the ui components
   * @return interval in milliseconds
   */
  int getRefreshRate();
  
  /**
   * set the interval for updating the ui components
   */
  void setRefreshRate(int millisecondDelay);
}
