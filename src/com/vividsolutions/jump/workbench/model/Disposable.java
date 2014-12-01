package com.vividsolutions.jump.workbench.model;

/**
 * Interface describing a disposable object. executing dispose() should release
 * all memory used by the implementing object.
 */
public interface Disposable {
  public void dispose();
}
