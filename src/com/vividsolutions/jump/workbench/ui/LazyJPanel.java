package com.vividsolutions.jump.workbench.ui;

import java.awt.Graphics;

import javax.swing.JPanel;

/**
 * wrapper class to defer ui init until panel is drawn
 */
public abstract class LazyJPanel extends JPanel {
  // init only once
  private boolean lazyInitCalled = false;

  // wait for constructor to run lazy init
  private boolean isConstructorFinished = false;

  protected LazyJPanel() {
    isConstructorFinished = true;
  }

  public void paint(Graphics g) {
    callLazyInit();
    super.paint(g);
  }

  public void paintAll(Graphics g) {
    callLazyInit();
    super.paintAll(g);
  }

  public void paintComponents(Graphics g) {
    callLazyInit();
    super.paintComponents(g);
  }

  public void repaint() {
    callLazyInit();
    super.repaint();
  }

  public void repaint(long l) {
    callLazyInit();
    super.repaint(l);
  }

  public void repaint(int i1, int i2, int i3, int i4) {
    callLazyInit();
    super.repaint(i1, i2, i3, i4);
  }

  public void repaint(long l, int i1, int i2, int i3, int i4) {
    callLazyInit();
    super.repaint(l, i1, i2, i3, i4);
  }

  public void update(Graphics g) {
    callLazyInit();
    super.update(g);
  }

  public synchronized final void callLazyInit() {
    if ((lazyInitCalled == false) && (getParent() != null)) {
      lazyInit();
      lazyInitCalled = true;
      validate();
    }
  }

  /**
   * implements the delayed gui init code
   */
  abstract protected void lazyInit();
}