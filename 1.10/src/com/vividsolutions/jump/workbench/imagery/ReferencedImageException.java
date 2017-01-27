package com.vividsolutions.jump.workbench.imagery;

import com.vividsolutions.jump.JUMPException;

public class ReferencedImageException extends JUMPException {

  public ReferencedImageException(String message, Throwable cause) {
    super(message, cause);
  }

  public ReferencedImageException(String message) {
    super(message);
  }

  public ReferencedImageException(Throwable cause) {
    super(cause);
  }

}
