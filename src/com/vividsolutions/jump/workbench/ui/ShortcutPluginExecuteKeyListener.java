package com.vividsolutions.jump.workbench.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.KeyStroke;

import org.openjump.core.CheckOS;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableChecked;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

public class ShortcutPluginExecuteKeyListener implements KeyListener {
  private HashMap<KeyStroke, PlugIn> keyStrokeToPluginMap = new LinkedHashMap();
  private HashMap<KeyStroke, EnableCheck> keyStrokeToCheckMap = new HashMap();
  private WorkbenchContext workbenchContext;

  public ShortcutPluginExecuteKeyListener(final WorkbenchContext wbc) {
    this.workbenchContext = wbc;
  }

  /**
   * Legacy method. Use add(KeyStroke key, final PlugIn plugin) instead.
   * @param keyCode
   * @param modifiers
   * @param plugin
   * @param enableCheck
   * @deprecated
   */
  public void add(final int keyCode, final int modifiers, final PlugIn plugin,
      final EnableCheck enableCheck) {
    KeyStroke key = keyStroke(keyCode, modifiers, true);
    keyStrokeToPluginMap.put(key,plugin);
    if (enableCheck!=null)
      keyStrokeToCheckMap.put(key, enableCheck);
  }

  public void add(KeyStroke key, final PlugIn plugin) {
    keyStrokeToPluginMap.put(key,plugin);
  }

  public boolean contains(KeyStroke key) {
    return keyStrokeToPluginMap.containsKey(key);
  }

  public boolean containsDefinition(KeyStroke key) {
    key = keyStroke(key.getKeyCode(), key.getModifiers(), true);
    return keyStrokeToPluginMap.containsKey(key);
  }

  public PlugIn getPlugIn(KeyStroke key) {
    // get plain w/o key() as it is to fetch assigned plugins for keys actually
    // pressed
    return keyStrokeToPluginMap.get(key);
  }

  public EnableCheck getEnableCheck(KeyStroke key) {
    return keyStrokeToCheckMap.get(key);
  }

  public final Set<KeyStroke> getAllKeyStrokes(){
    return keyStrokeToPluginMap.keySet();
  }

  public void keyTyped(KeyEvent e) {
  }

  public void keyReleased(KeyEvent e) {
    // our shortcuts are on pressed, so consume any competing key pressed event
    if ( contains(keyStroke(e.getKeyCode(), e.getModifiers(), false)) )
      e.consume();
  }

  public void keyPressed(KeyEvent e) {
    // System.out.println("SCPE src "+e.getSource());
    KeyStroke key = keyStroke(e.getKeyCode(), e.getModifiers(), false);
    PlugIn plugin = getPlugIn(key);
    // System.out.println(e.getKeyCode()
    // + ":" + e.getModifiers()
    // +"/"+plugInAndEnableCheck+"/"+keyCodeAndModifiersToPlugInAndEnableCheckMap.keySet());
    if (plugin == null) {
      return;
    }

    e.consume();

    EnableCheck enableCheck = getEnableCheck(key);
    if (enableCheck==null && plugin instanceof EnableChecked)
      enableCheck = ((EnableChecked)plugin).getEnableCheck();
 
    String msg = null;
    if (enableCheck != null && (msg = enableCheck.check(null)) != null) {
      workbenchContext.getWorkbench().getFrame().warnUser(msg);
      return;
    }
    // #toActionListener handles checking if the plugIn is a
    // ThreadedPlugIn, and making calls to UndoableEditReceiver if necessary.
    // [Jon Aquino 10/15/2003]
    AbstractPlugIn.toActionListener(plugin, workbenchContext,
        new TaskMonitorManager()).actionPerformed(null);
  }

  public String toString() {
    String out = "";
    for (Object e : keyStrokeToPluginMap.entrySet()) {
      Map.Entry entry = (Map.Entry) e;
      out += entry.getKey()
          + "/"
          + (entry.getKey().toString().contains("79") ? valueToString(entry
              .getValue()) : "") + ", ";
    }
    return out;
  }

  private String valueToString(Object value) {
    PlugIn p = (PlugIn) ((Object[]) value)[0];
    return p.getName();
  }

  /**
   * create a proper keystroke per platform. used to decorate menus with
   * accelerators.
   * 
   * @param stroke
   * @return stroke
   */
  public static KeyStroke getPlatformKeyStroke(KeyStroke stroke) {
    return KeyStroke.getKeyStroke(stroke.getKeyCode(),
        filterModifierDefinition(stroke.getModifiers()),
        stroke.isOnKeyRelease());
  }

  // we do not use them internally
  private static int filterModifierDownMasks(int modifiers) {
    int old = modifiers;
    modifiers = modifiers & ~KeyEvent.CTRL_DOWN_MASK;
    modifiers = modifiers & ~KeyEvent.SHIFT_DOWN_MASK;
    modifiers = modifiers & ~KeyEvent.ALT_DOWN_MASK;
    modifiers = modifiers & ~KeyEvent.ALT_GRAPH_DOWN_MASK;
    modifiers = modifiers & ~KeyEvent.META_DOWN_MASK;
    // System.out.println("spe mod "+old+"/"+modifiers);
    return modifiers;
  }

  private static int filterModifierDefinition(int modifiers) {
    // int old = modifiers;
    if (CheckOS.isMacOsx() && (modifiers & KeyEvent.CTRL_MASK) != 0) {
      // subtract all!!! Ctrl masks
      modifiers = modifiers & ~KeyEvent.CTRL_MASK;
      modifiers = modifiers & ~KeyEvent.CTRL_DOWN_MASK;
      // add Meta
      modifiers = modifiers | KeyEvent.META_MASK;
      // System.out.println(Integer.toBinaryString(old)+" -> "+Integer.toBinaryString(modifiers)+" ? "+Integer.toBinaryString(KeyEvent.META_MASK)+"/"+Integer.toBinaryString(Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    return modifiers;
  }

  // private static String key(int keyCode, int modifiers, boolean define) {
  // // Mac always uses CMD key instead of CTRL, which is preserved
  // // for left click contex
  // return keyCode + ":" + (define ? filterModifierDefinition(modifiers) :
  // filterModifierDownMasks(modifiers));
  // }

  private static KeyStroke keyStroke(int keyCode, int modifiers, boolean define) {
    modifiers = define ? filterModifierDefinition(modifiers)
        : filterModifierDownMasks(modifiers);
    return KeyStroke.getKeyStroke(keyCode, modifiers);
  }
}
