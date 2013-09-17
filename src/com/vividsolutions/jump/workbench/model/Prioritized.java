package com.vividsolutions.jump.workbench.model;

/**
 * an Interface to implement priority
 */
public interface Prioritized {
  public static int NOPRIORITY = Integer.MAX_VALUE;
  
  public int getPriority();
}
