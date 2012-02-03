package org.openjump.core.ui.enablecheck;

import java.lang.reflect.Method;

import javax.swing.JComponent;

import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;

public class PropertyEnableCheck implements EnableCheck {
  String disabledMessage = "";

  /** The check method. */
  private Method method;

  /** The object to invoke the method on. */
  private Object object;

  /** The expected value to be returned for the check to be enabled. */
  private Object expectedValue;

  private ErrorHandler errorHandler;

  private boolean invert;

  public PropertyEnableCheck(Object object, String checkMethodName,
    Object expectedValue, boolean invert, ErrorHandler errorHandler) {
    this(object, checkMethodName, expectedValue, errorHandler);
    this.invert = invert;
  }

  /**
   * @param object The object to invoke the method on.
   * @param checkMethodName The name of the check method which returns a boolean
   *          value.
   * @param expectedValue The expected value to be returned for the check to be
   *          enabled.
   */
  public PropertyEnableCheck(final Object object, final String checkMethodName,
    final Object expectedValue, ErrorHandler errorHandler) {
    Class clazz = object.getClass();
    this.object = object;
    this.expectedValue = expectedValue;
    this.errorHandler = errorHandler;
    try {
      method = clazz.getMethod(checkMethodName, new Class[] {});
    } catch (Throwable e) {
      errorHandler.handleThrowable(e);
    }
  }

  public String check(JComponent component) {
    try {
      Object result = method.invoke(object, new Object[0]);
      if (result == expectedValue) {
        return getResult(true);
      } else if (result == null) {
        return getResult(false);
      } else if (expectedValue == null) {
        return getResult(false);
      } else if (result.equals(expectedValue)) {
        return getResult(true);
      } else {
        return getResult(false);
      }
    } catch (Throwable e) {
      errorHandler.handleThrowable(e);
      return e.getMessage();
    }
  }

  private String getResult(boolean equal) {
    if (equal || invert) {
      return null;
    } else {
      return disabledMessage;
    }
  }
}
