package com.vividsolutions.jump.workbench.ui.wizard;

/**
 * The CancelNextException is used by {@link WizardPanel}s to abort moving to
 * the next panel in the {@link WizardPanel#exitingToRight()} method if an error
 * occurred. The panel is responsible for displaying an appropriate message.
 * 
 * @author Paul Austin
 */
public class CancelNextException extends RuntimeException {

  public CancelNextException() {
  }

  public CancelNextException(String message, Throwable cause) {
    super(message, cause);
  }

  public CancelNextException(String message) {
    super(message);
  }

  public CancelNextException(Throwable cause) {
    super(cause);
  }

}
