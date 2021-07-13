package com.vividsolutions.jump.workbench.plugin;

import java.awt.Dimension;
import javax.swing.Icon;


/**
 * A set of methods to implement for plugins to be recognized as icon enabled.
 * {@link AbstractPlugIn} does implement a generic implementation
 */
public interface Iconified {
  Icon getIcon(Dimension dim);
  Icon getIcon(int height);
  Icon getIcon();
}
