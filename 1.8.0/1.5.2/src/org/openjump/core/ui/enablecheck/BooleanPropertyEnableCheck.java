package org.openjump.core.ui.enablecheck;

import java.lang.reflect.Method;

import javax.swing.JComponent;

import com.vividsolutions.jump.workbench.plugin.EnableCheck;

public class BooleanPropertyEnableCheck implements EnableCheck {
  String disabledMessage = "";

  /** The check method. */
  private Method method;

  /** The object to invoke the method on. */
  private Object object;

  /** The expected value to be returned for the check to be enabled. */
  private boolean expectedValue;

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   */
  public BooleanPropertyEnableCheck(final Object object,
    final String checkMethodName) {
    this(object, checkMethodName, true, "");
  }

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   * @param expectedValue The expected value to be returned for the check to be
   *          enabled.
   */
  public BooleanPropertyEnableCheck(final Object object,
    final String checkMethodName, final boolean expectedValue) {
    this(object, checkMethodName, expectedValue, "");
  }

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   * @param expectedValue The expected value to be returned for the check to be
   *          enabled.
   * @param disabledMessage
   */
  public BooleanPropertyEnableCheck(final Object object,
    final String checkMethodName, final boolean expectedValue,
    String disabledMessage) {
    Class clazz = object.getClass();
    this.object = object;
    this.expectedValue = expectedValue;
    this.disabledMessage = disabledMessage;
    try {
      method = clazz.getMethod(checkMethodName, new Class[] {});
    } catch (Throwable e) {
      throw new IllegalArgumentException("Unable to get check method "
        + checkMethodName + " on " + clazz);
    }
  }

  public String check(JComponent component) {
    try {
      Boolean result = (Boolean)method.invoke(object, new Object[0]);
      if (result.booleanValue() == expectedValue) {
        return null;
      } else {
        return disabledMessage;
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }
}
