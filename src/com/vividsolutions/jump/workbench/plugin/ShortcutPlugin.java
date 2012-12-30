package com.vividsolutions.jump.workbench.plugin;


/**
 * A set of method to implement for plugins to be recognized as shortcut enabled.
 * 
 * @author ed
 *
 */
public interface ShortcutPlugin {
  public int getShortcutModifiers();

  public void setShortcutModifiers(int shortcutModifiers);

  public int getShortcutKeys();

  public void setShortcutKeys(int shortcutKeys);

  public boolean registerShortcut();

  public boolean unregisterShortcut();
}
