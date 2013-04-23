package com.vividsolutions.jump.workbench.plugin;

import javax.swing.KeyStroke;


/**
 * A set of method to implement for plugins to be recognized as shortcut enabled.
 * 
 * @author ed
 *
 */
public interface MultiShortcutEnabled {
  public PlugIn[] getShortcutEnabledPlugins();
}
