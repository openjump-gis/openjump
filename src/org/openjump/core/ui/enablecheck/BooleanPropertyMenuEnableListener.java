package org.openjump.core.ui.enablecheck;

import java.lang.reflect.Method;

import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class BooleanPropertyMenuEnableListener implements MenuListener {
  String disabledToolTip;

  /** The check method. */
  private Method method;

  /** The object to invoke the method on. */
  private Object object;

  /** The expected value to be returned for the check to be enabled. */
  private boolean expectedValue;

  private String enabledToolTip;

  private JMenuItem menuItem;

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   */
  public BooleanPropertyMenuEnableListener(final JMenuItem menuItem,
    final Object object, final String checkMethodName) {
    this(menuItem, object, checkMethodName, true, null, null);
  }

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   */
  public BooleanPropertyMenuEnableListener(final JMenuItem menuItem,
    final Object object, final String checkMethodName,
    final String enabledToolTip, final String disabledToolTip) {
    this(menuItem, object, checkMethodName, true, enabledToolTip,
      disabledToolTip);
  }

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   * @param expectedValue The expected value to be returned for the check to be
   *          enabled.
   */
  public BooleanPropertyMenuEnableListener(final JMenuItem menuItem,
    final Object object, final String checkMethodName,
    final boolean expectedValue, final String enabledToolTip,
    final String disabledToolTip) {
    this.menuItem = menuItem;
    Class clazz = object.getClass();
    this.object = object;
    this.expectedValue = expectedValue;
    this.enabledToolTip = enabledToolTip;
    this.disabledToolTip = disabledToolTip;
    try {
      method = clazz.getMethod(checkMethodName, new Class[] {});
    } catch (Throwable e) {
      throw new IllegalArgumentException("Unable to get check method "
        + checkMethodName + " on " + clazz);
    }
  }

  public void menuItemShown(final JMenuItem menuItem) {
  }

  public void menuCanceled(MenuEvent e) {
  }

  public void menuDeselected(MenuEvent e) {
  }

  public void menuSelected(MenuEvent event) {
    try {
      Boolean result = (Boolean)method.invoke(object, new Object[0]);
      if (result.booleanValue() == expectedValue) {
        menuItem.setEnabled(true);
        menuItem.setToolTipText(enabledToolTip);
      } else {
        menuItem.setEnabled(false);
        menuItem.setToolTipText(disabledToolTip);
      }
    } catch (Throwable e) {
      menuItem.setEnabled(false);
      e.printStackTrace();
      menuItem.setToolTipText(e.getMessage());
    }
  }
}
