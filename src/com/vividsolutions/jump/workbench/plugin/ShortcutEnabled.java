package com.vividsolutions.jump.workbench.plugin;

import javax.swing.KeyStroke;


/**
 * A set of method to implement for plugins to be recognized as shortcut enabled.
 * 
 * @author ed
 *
 */
public interface ShortcutEnabled {
  public boolean isShortcutEnabled();
  
  /**
   * Convenience Method for getShortcutKeys() and getShortcutModifiers()
   */
  public KeyStroke getShortcutKeyStroke();
  
  public int getShortcutModifiers();

  public void setShortcutModifiers(int shortcutModifiers);

  public int getShortcutKeys();

  public void setShortcutKeys(int shortcutKeys);
}
