package com.vividsolutions.jump.task;

import com.vividsolutions.jump.JUMPException;

/**
 * a simple way to signal whatever was done down the line
 * was cancelled whereever this was thrown
 */
public class TaskCancelledException extends JUMPException {
  public TaskCancelledException() {
    super("");
  }
}
