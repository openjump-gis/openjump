package com.vividsolutions.jump.workbench.ui;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.ui.renderer.style.ChoosableStyle;

public class MultiRecursiveKeyListener extends RecursiveKeyListener {
  private HashSet listeners = new HashSet();

  public MultiRecursiveKeyListener(Component component) {
    super(component);
  }

  public MultiRecursiveKeyListener(Component component, KeyListener l) {
    this(component);
    addKeyListener(l);
  }

  public void addKeyListener(KeyListener l){
    Assert.isTrue(l!=null);
    listeners.add(l);
  }

  public void removeKeyListener(KeyListener l){
    Assert.isTrue(l!=null);
    listeners.remove(l);
  }

  public void keyTyped(KeyEvent e) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      KeyListener l = (KeyListener)i.next();
      l.keyTyped(e);
    }
  }

  public void keyPressed(KeyEvent e) {
    for (Iterator i = new ArrayList(listeners).iterator(); i.hasNext();) {
      KeyListener l = (KeyListener)i.next();
      l.keyPressed(e);
    }
  }

  public void keyReleased(KeyEvent e) {
    for (Iterator i = new ArrayList(listeners).iterator(); i.hasNext();) {
      KeyListener l = (KeyListener)i.next();
      l.keyReleased(e);
    }
  }

}
