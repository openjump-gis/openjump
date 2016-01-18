package com.vividsolutions.jump.workbench.plugin;

import com.vividsolutions.jump.workbench.WorkbenchContext;


/**
 * a method to mark plugins as enable checked. this is new. older plugins only
 * implemented a static createEnableCheck() method.
 * 
 * @author ed
 * 
 */
public interface EnableChecked {
  public EnableCheck getEnableCheck();
}
