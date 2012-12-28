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

  public void add(final int keyCode, int modifiers, final PlugIn plugIn,
      final EnableCheck enableCheck) {

    // Mac always uses CMD key instead of CTRL, which is preserved 
    // for left click context menu, right click emulation
    if ( CheckOS.isMacOsx() && (modifiers & KeyEvent.CTRL_MASK)!=0 ){
      // subtract Ctrl
      modifiers -= KeyEvent.CTRL_MASK;
      // add Meta
      modifiers += KeyEvent.META_MASK;
    }

    keyCodeAndModifiersToPlugInAndEnableCheckMap.put(keyCode + ":" + modifiers,
        new Object[] { plugIn, enableCheck });
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyReleased(KeyEvent e) {
    //System.out.println("SCPE src "+e.getSource());
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
    if (enableCheck != null && (msg=enableCheck.check(null)) != null) {
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
