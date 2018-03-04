package com.vividsolutions.jump.task;

public interface RefreshRated {
  /**
   * retrieve the interval for updating the ui components
   * @return interval in milliseconds
   */
  public int getRefreshRate();
  
  /**
   * set the interval for updating the ui components
   */
  public void setRefreshRate(int millisecondDelay);
}
