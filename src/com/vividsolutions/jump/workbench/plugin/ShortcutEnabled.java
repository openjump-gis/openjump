package com.vividsolutions.jump.workbench.plugin;

import javax.swing.KeyStroke;


/**
 * A set of method to implement for plugins to be recognized as shortcut enabled.
 * 
 * @author ed
 *
 */
public interface ShortcutEnabled {

  boolean isShortcutEnabled();
  
  /**
   * Convenience Method for getShortcutKeys() and getShortcutModifiers()
   * @return the KeyStroke associated to the shortcut
   */
  KeyStroke getShortcutKeyStroke();

  int getShortcutModifiers();

  void setShortcutModifiers(int shortcutModifiers);

  int getShortcutKeys();

  void setShortcutKeys(int shortcutKeys);
}
