package com.vividsolutions.jump.util;

public class Timer {
  /**
   *  a helper method to measure time frames in milliseconds 
   * @param i time in milliseconds
   * @return milliseconds since time
   */
  public static long milliSecondsSince( long i ){
    return System.currentTimeMillis() - i;
  }

  /**
   * a helper method to nicely format the above output e.g. 12046ms -> 12.05s
   * @param i milliseconds
   * @return eg. "1.05" for a difference of 1047ms
   */
  public static String secondsSinceString( long i ){
    return String.format("%.2f", milliSecondsSince(i)/1000f);
  }
}
