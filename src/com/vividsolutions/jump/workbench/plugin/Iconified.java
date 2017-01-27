package com.vividsolutions.jump.workbench.plugin;

import java.awt.Dimension;
import javax.swing.Icon;


/**
 * A set of methods to implement for plugins to be recognized as icon enabled.
 * We cannot define getIcon() as this is used all over the code in various 
 * forms (static, non-static, public, private).
 * Hence we define a more generic getter that additionally allows to define 
 * the dimension wanted.
 * 
 * @author ed
 * @since 01.2013
 *
 */
public interface Iconified {
  public Icon getIcon(Dimension dim);
  public Icon getIcon(int height);
}
