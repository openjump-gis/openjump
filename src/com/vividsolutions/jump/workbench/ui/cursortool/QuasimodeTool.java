/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openjump.core.CheckOS;
import org.openjump.core.ui.plugin.view.SuperZoomPanTool;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;

/**
 * Delegates to different CursorTools depending on whether various modifier keys
 * are pressed (Ctrl, Shift, Alt). The term "quasimode" refers to a mode that is
 * only in existence as long as a key is held down -- the mode vanishes as soon
 * as the key is released. For more information, see the book
 * "Humane Interfaces" by Jef Raskin.
 */
public class QuasimodeTool extends DelegatingTool {
  private HashMap<ModifierKeySpec,CursorTool> keySpecToToolMap;
  private DummyTool dummytool = new DummyTool();

  // a tool that has delegates enabled via KEY events
  public QuasimodeTool(CursorTool defaultTool) {
    this(defaultTool, null);
  }

  private QuasimodeTool(CursorTool defaultTool, final HashMap<ModifierKeySpec,CursorTool> keyMap) {
    // set first delegate
    super(defaultTool);
    this.keySpecToToolMap = (keyMap != null) ? new HashMap<ModifierKeySpec,CursorTool>(keyMap)
        : new HashMap<ModifierKeySpec,CursorTool>();
    setDefaultTool(defaultTool);
  }

  private CursorTool getDefaultTool() {
    return (CursorTool) keySpecToToolMap.get(ModifierKeySpec.DEFAULT);
  }

  public QuasimodeTool add(ModifierKeySpec keySpec, CursorTool tool) {
    //System.out.println("add: " + (keySpec != null ? keySpec : "null") + " = " + (tool != null ? tool.getName() : "null"));
    if (tool != null && keySpec != null)
      keySpecToToolMap.put(keySpec, (tool.isRightMouseButtonUsed() ? tool
          : new LeftClickFilter(tool)));
    //System.out.println("add: " + (keySpec != null ? keySpec : "null") + " = " + (tool != null ? tool.getName() : "null")+"\nRES: "+keySpecToToolMap);
    return this;
  }

  private void setDefaultTool(CursorTool tool) {
    add(ModifierKeySpec.DEFAULT, tool);
  }

  // returns a clone with a new default tool
  public QuasimodeTool cloneAndSetDefaultTool(CursorTool defaultTool) {
    QuasimodeTool clone = new QuasimodeTool(defaultTool, keySpecToToolMap);
    return clone;
  }

  public String toString(){
    StringBuffer buf = new StringBuffer();
    for (ModifierKeySpec keys : (Set<ModifierKeySpec>)keySpecToToolMap.keySet()) {
      buf.append(keys + "=" + ((CursorTool)keySpecToToolMap.get(keys)).getName() + "\n");
    }
    return getDefaultTool().getName()+"\n"+buf;
  }
  
  private CursorTool getTool(Set<Integer> keys) {
    CursorTool tool = (CursorTool) keySpecToToolMap.get(new ModifierKeySet(
        keys));
    //System.out.println("keys: " + keys +" is "+(tool!=null?tool+"\n"+this:null));
    return tool;
  }

  private KeyListener keyListener = new KeyListener() {
    private HashSet keys = new HashSet();
    private HashSet previous = null;

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      keys.add(e.getKeyCode());
      keyStateChanged(e);
    }

    public void keyReleased(KeyEvent e) {
      keys.remove(e.getKeyCode());
      keyStateChanged(e);
    }

    // one method to rule them all
    private void keyStateChanged(KeyEvent e) {
      // filter out duplicate events (e.g. key stays pressed)
      if (previous != null && previous.equals(keys))
        return;
      previous = (HashSet) keys.clone();
      //System.out.println(e.getKeyCode()+"/"+e.getKeyModifiersText(e.getModifiers())+"/"+e.getKeyText(e.getKeyCode()));
      setTool(keys);
    }
    
//    public String toString(){
//      return getName()+" KeyListener";
//    }
  };

  public KeyListener getKeyListener(){
    return keyListener;
  }

  // /*
  // * [sstein: 17.Mar2007] added the Listener, to be able to remove the
  // listener
  // * if the cursortool is deactivated. Otherwise the listener still exists and
  // * the mousepointer is "flickering" through all previously used mouse-tools
  // * The modifications (see also #activate and #deactivate) are proposed by
  // Bob and Larry
  // * [ede 12.2012] commented, see reasoning above in activate()
  // */
  // private WindowAdapter windowListener = new WindowAdapter()
  // {
  // public void windowActivated(WindowEvent e) {
  // super.windowActivated(e);
  // setTool(new KeyEvent(panel, KeyEvent.KEY_PRESSED,
  // 0, 0, KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));
  // }
  // };

  private void setTool(Set<Integer> keys) {
    CursorTool tool = getTool(keys);
    // only remove tool on unknown modifier combinations
    // else use default tool, allows draw e.g. to use backspace key
    if (tool == null)
      for (Integer integer : keys) {
        if (integer != KeyEvent.VK_SHIFT && integer != KeyEvent.VK_CONTROL
            && integer != KeyEvent.VK_META && integer != KeyEvent.VK_ALT) {
          tool = getDefaultTool();
          break;
        }
      }
    // standard null tool is DummyTool
    if (tool == null)
      tool = dummytool;
    
    setDelegate(tool);
    super.activate(panel);
    panel.setCurrentCursorTool(this);
  }

  private LayerViewPanel panel;
  private WorkbenchFrame frame;

  public void activate(final LayerViewPanel panel) {
    if (panel==null) return;
    
    this.panel = panel;
    super.activate(panel);
    panel.setCurrentCursorTool(this);
    panel.getWorkBenchFrame().addEasyKeyListener(keyListener);
    // Cache WorkbenchFrame because in JDK 1.3 when I minimize an internal
    // frame, SwingUtilities#windowForComponent returns null for that frame.
    // A Swing bug. [Jon Aquino]
    //frame = AbstractCursorTool.workbenchFrame(panel);
    //if (frame != null) {
      //frame.addEasyKeyListener(keyListener);
      // Workaround for the following:
      // * Use WorkbenchFrame#addKeyboardShortcut for a plug-in that
      // pops up a dialog (or could pop up an error dialog). Assign it to
      // Ctrl-A,
      // for example.
      // * Press Ctrl-A. The Ctrl quasimode happens. But also the dialog pops
      // up.
      // * Release Ctrl. Close the dialog. Note that the cursor shows we're
      // still
      // in the Ctrl quasimode! This is because the dialog consumed the
      // key-up event.
      // So we're working around this by clearing the quasimode when the
      // WorkbenchFrame is activated (e.g. when a dialog is closed). [Jon
      // Aquino]

      // -- [sstein : ] deactivated and repalced by line above see comment
      // on windowListener above
      // [ede 12.2012] deactivated as it interferes with QuasiModeTools switched
      // by key pressed while toolbox is still active, tool got switched but was
      // reset to the default tool by the next line
      // frame.addWindowListener(windowListener);

      // Need to do the following so that the delegate gets set to the
      // LeftClickFilter
      // This fixes a problem where the selection was being lost on a right
      // click
      // when using the selector tool on the tool bar.
      // The following code is executed during windowActivated event in
      // WindowAdapter windowListener above.
      // This is why the problem fixed itself when returning to the window after
      // bringing
      // another application forward.
      // This event was done when clicking the selection tool on the toolbox so
      // that is why it was always working.
      // [ede 12.2012] deactivated, keep tool even if we loose focus
      // setTool(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0,
      // KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));

    //}
  }

  public void deactivate() {
    // [ede 12.2012] deactivated, as it makes no sense not to deactivate the
    // tool if ALT is not pressed
    // if (!altKeyDown)
    // {
    super.deactivate();
    panel.getWorkBenchFrame().removeEasyKeyListener(keyListener);
//    if (frame != null) {
//      frame.removeEasyKeyListener(keyListener);
//      // [ede 12.2012] deactivated, keep tool even if we loose focus
//      // frame.removeWindowListener(windowListener);
//    }
    // }
  }

  // same as below, just does not alter key assignments but mirrors actually pressed keys instead
  public static class ModifierKeySet extends ModifierKeySpec {
    public ModifierKeySet(Collection<Integer> keys) {
      super();
      addAllRaw(keys);
    }
  }

  public static class ModifierKeySpec extends HashSet<Integer> {
    public static final ModifierKeySpec DEFAULT = new ModifierKeySpec();

    // default key spec, no keys pressed
    private ModifierKeySpec() {
      super();
    }

    public ModifierKeySpec(int[] keys) {
      super();
      for (int i : keys) {
        add(new Integer(i));
      }
    }

    public ModifierKeySpec(Collection<Integer> keys) {
      super();
      addAll(keys);
    }

    public ModifierKeySpec(boolean needsControl, boolean needsShift,
        boolean needsAltOrMeta) {
      super();
      if (needsControl)
        add(KeyEvent.VK_CONTROL);
      if (needsShift)
        add(KeyEvent.VK_SHIFT);
      if (needsAltOrMeta)
        add(KeyEvent.VK_ALT);
    }

    @Override
    public boolean add(Integer e) {
      // OsX always uses CMD key instead of CTRL, which is preserved for left
      // click context menu, right click emulation
      if (e == KeyEvent.VK_CONTROL && CheckOS.isMacOsx()){
        e = new Integer(KeyEvent.VK_META);
      }
      return super.add((Integer) e);
    }

    protected void addAllRaw(Collection<Integer> c) {
      Iterator<Integer> e = c.iterator();
      while (e.hasNext()) {
        super.add(e.next());
      }
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof ModifierKeySpec)) {
        return false;
      }
      ModifierKeySpec other = (ModifierKeySpec) obj;
      // System.out.println("vgl1: "+other+"\nvgl2: "+this);
      Iterator iter = this.iterator();
      while (iter.hasNext()) {
        Integer keyval = (Integer) iter.next();
        if (!other.contains(keyval)) {
          // wrong shortcut dude
          return false;
        }
      }
      // arrived here? all is well
      return true;
    }
  }

  // public static class ModifierKeySpecOld {
  // public ModifierKeySpecOld(boolean needsControl, boolean needsShift,
  // boolean needsAltOrMeta) {
  // this.needsControl = needsControl;
  // this.needsShift = needsShift;
  // this.needsAltOrMeta = needsAltOrMeta;
  // }
  //
  // private boolean needsShift, needsAltOrMeta, needsControl;
  //
  // public int hashCode() {
  // // Map will be small anyway. [Jon Aquino]
  // return 0;
  // }
  //
  // public boolean equals(Object obj) {
  // if (!(obj instanceof ModifierKeySpecOld)) {
  // return false;
  // }
  // ModifierKeySpecOld other = (ModifierKeySpecOld) obj;
  // return needsControl == other.needsControl
  // && needsShift == other.needsShift
  // && needsAltOrMeta == other.needsAltOrMeta;
  // }
  // }

  // keep one instance only per default shortcut
  private static CursorTool zoom = new SuperZoomPanTool();
  private static CursorTool pan = new PanTool();
  private static SelectFeaturesTool selectFeaturesTool = new SelectFeaturesTool() {
    protected boolean selectedLayersOnly() {
      return false;
    }
  };
  private static CursorTool info = new FeatureInfoTool();

  public static QuasimodeTool addStandardQuasimodes(CursorTool tool) {
    QuasimodeTool quasimodeTool = tool instanceof QuasimodeTool ? (QuasimodeTool) tool
        : new QuasimodeTool(tool);
    quasimodeTool.add(new ModifierKeySpec(false, false, true), zoom);
    // disabled cause shortcut is used by Ubuntu, use combined SuperZoomPanTool instead
    //quasimodeTool.add(new ModifierKeySpec(false, true, true), pan);

    // using Shift we can actually add to the selction or deselct
    quasimodeTool.add(new ModifierKeySpec(true, false, false),
        selectFeaturesTool);
    quasimodeTool.add(new ModifierKeySpec(true, true, false),
        selectFeaturesTool);
    // [sstein 7.Nov.2011] we do not want the tool on MacOSX as the mouse
    // pointer change
    // interferes with the Edit Feature Context menu
    // [ede 12.2012] disabled to see what the real issue is here
    //               OSX now by default only accepts META_KEY instead of ALT
    // if (CheckOS.isMacOsx() == false) {
    quasimodeTool.add(new ModifierKeySpec(true, false, true), info);
    // }
    return quasimodeTool;
  }

}
