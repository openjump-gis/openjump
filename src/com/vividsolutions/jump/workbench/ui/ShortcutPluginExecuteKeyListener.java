package com.vividsolutions.jump.workbench.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

import org.openjump.core.CheckOS;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

public class ShortcutPluginExecuteKeyListener implements KeyListener {
  private HashMap keyCodeAndModifiersToPlugInAndEnableCheckMap = new HashMap();
  private WorkbenchContext workbenchContext;

  public ShortcutPluginExecuteKeyListener(final WorkbenchContext wbc) {
    this.workbenchContext = wbc;
  }

  public boolean contains(int keyCode, int modifiers) {
    return keyCodeAndModifiersToPlugInAndEnableCheckMap.keySet().contains(
        key(keyCode, modifiers, false));
  }

  public boolean containsDefinition(int keyCode, int modifiers) {
    return keyCodeAndModifiersToPlugInAndEnableCheckMap.keySet().contains(
        key(keyCode, modifiers, true));
  }
  
  private String key(int keyCode, int modifiers, boolean define) {
    // Mac always uses CMD key instead of CTRL, which is preserved
    // for left click context menu, right click emulation
    if (define && CheckOS.isMacOsx() && (modifiers & KeyEvent.CTRL_MASK) != 0) {
      // subtract Ctrl
      modifiers -= KeyEvent.CTRL_MASK;
      // add Meta
      modifiers += KeyEvent.META_MASK;
    }
    return keyCode + ":" + modifiers;
  }

  public Object[] get(int keyCode, int modifiers) {
    // get plain w/o key() as it is to fetch assigned plugins for keys actually
    // pressed
    return (Object[]) keyCodeAndModifiersToPlugInAndEnableCheckMap.get(key(
        keyCode, modifiers, false));
  }

  public void add(final int keyCode, int modifiers, final PlugIn plugIn,
      final EnableCheck enableCheck) {
    keyCodeAndModifiersToPlugInAndEnableCheckMap.put(key(keyCode, modifiers, true),
        new Object[] { plugIn, enableCheck });
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyReleased(KeyEvent e) {
    // System.out.println("SCPE src "+e.getSource());
    Object[] plugInAndEnableCheck = (Object[]) keyCodeAndModifiersToPlugInAndEnableCheckMap
        .get(e.getKeyCode() + ":" + e.getModifiers());
    // System.out.println(e.getKeyCode()
    // + ":" + e.getModifiers()
    // +"/"+plugInAndEnableCheck+"/"+keyCodeAndModifiersToPlugInAndEnableCheckMap.keySet());
    if (plugInAndEnableCheck == null) {
      return;
    }

    PlugIn plugIn = (PlugIn) plugInAndEnableCheck[0];
    EnableCheck enableCheck = (EnableCheck) plugInAndEnableCheck[1];
    String msg = null;
    if (enableCheck != null && (msg = enableCheck.check(null)) != null) {
      workbenchContext.getWorkbench().getFrame().warnUser(msg);
      return;
    }
    // #toActionListener handles checking if the plugIn is a
    // ThreadedPlugIn,
    // and making calls to UndoableEditReceiver if necessary. [Jon
    // Aquino 10/15/2003]
    AbstractPlugIn.toActionListener(plugIn, workbenchContext,
        new TaskMonitorManager()).actionPerformed(null);
  }

  public void keyPressed(KeyEvent e) {
  }

}
