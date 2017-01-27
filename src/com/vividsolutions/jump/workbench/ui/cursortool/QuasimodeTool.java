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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.KeyStroke;

import org.openjump.core.CheckOS;
import org.openjump.core.ui.plugin.edittoolbox.cursortools.RotateSelectedItemTool;

import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.DeleteVertexTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.InsertVertexTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.MoveSelectedItemsTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.MoveVertexTool;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;

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
  // switch defaults on/off
  private boolean useDefaults = false;
  // default tools - keep one instance only per default shortcut
  private static CursorTool zoom = new ZoomTool();
  private static CursorTool pan = new PanTool();
  private static CursorTool selectFeaturesTool = new SelectFeaturesTool() {
    protected boolean selectedLayersOnly() {
      return false;
    }
  };
  // create default tools
  private static CursorTool info = new FeatureInfoTool();
  private static CursorTool insVertex = new InsertVertexTool(EnableCheckFactory.getInstance());
  private static CursorTool delVertex = new DeleteVertexTool(EnableCheckFactory.getInstance());
  private static CursorTool movVertex = new MoveVertexTool(EnableCheckFactory.getInstance());
  private static CursorTool moveItem = new MoveSelectedItemsTool(EnableCheckFactory.getInstance());
  private static CursorTool rotateItem = new RotateSelectedItemTool(EnableCheckFactory.getInstance());
  // add default tools, keep adding order for documentation later
  private static HashMap<ModifierKeySpec,CursorTool> defaultToolsMap= new LinkedHashMap();
  static {
    addDefaultTool(new ModifierKeySpec(false, false, true), zoom);
    // KNOWN ISSUE: shortcut is used by Ubuntu
    addDefaultTool(new ModifierKeySpec(false, true, true), pan);
    // using Ctrl+Shift we can actually add to the selection or deselect
    selectFeaturesTool = addDefaultTool(new ModifierKeySpec(true, false, false),
        selectFeaturesTool);
    addDefaultTool(new ModifierKeySpec(true, true, false),
        selectFeaturesTool);
    addDefaultTool(new ModifierKeySpec(true, false, true), info);
    // add edit vertex modes 
    addDefaultTool(new ModifierKeySpec(new int[]{KeyEvent.VK_A}), insVertex);
    addDefaultTool(new ModifierKeySpec(new int[]{KeyEvent.VK_X}), delVertex);
    addDefaultTool(new ModifierKeySpec(new int[]{KeyEvent.VK_V}), movVertex);
    // move item
    addDefaultTool(new ModifierKeySpec(new int[]{KeyEvent.VK_M}), moveItem);
    // rotate (use identical instance for setting rotation center)
    rotateItem = addDefaultTool(
        new ModifierKeySpec(new int[] { KeyEvent.VK_R }), rotateItem);
    addDefaultTool(new ModifierKeySpec(new int[] { KeyEvent.VK_R,
        KeyEvent.VK_SHIFT }), rotateItem);
  }


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

  public CursorTool getDefaultTool() {
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

  public QuasimodeTool remove(ModifierKeySpec keySpec) {
    keySpecToToolMap.remove(keySpec);
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
      buf.append(keys + "=" + ((CursorTool)keySpecToToolMap.get(keys)) + "\n");
    }
    if (useDefaults){
      for (ModifierKeySpec keys : (Set<ModifierKeySpec>)defaultToolsMap.keySet()) {
        buf.append(keys + "=" + ((CursorTool)defaultToolsMap.get(keys)) + "\n");
      }
    }
    return getDefaultTool().getName()+"\n"+buf;
  }
  
  
  
  /*
   * cancel gestures of all quasimode tools.
   * @see com.vividsolutions.jump.workbench.ui.cursortool.DelegatingTool#cancelGesture()
   */
  public void cancelGesture() {
    // cancel tools
    for (CursorTool ct : keySpecToToolMap.values()) {
      ct.cancelGesture();
    }
    // cancel default quasimodes if enabled
    if (useDefaults) {
      for (CursorTool ct : defaultToolsMap.values()) {
        ct.cancelGesture();
      }
    }
  }

  private CursorTool getTool(Collection<Integer> keys) {
    // tools override defaults
    ModifierKeySet ks = new ModifierKeySet(keys);
    CursorTool tool = keySpecToToolMap.get(ks);
    // fetch defaults if enabled
    if (useDefaults && tool == null)
      tool = defaultToolsMap.get(ks);
    //System.out.println("keys: " + keys +" is "+(tool!=null?tool+"\n"+this:null));
    return tool;
  }

  private HashMap keyTimeMap = new HashMap();
  
  private KeyListener keyListener = new KeyListener() {
    private Collection previous = null;

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      keyTimeMap.put(e.getKeyCode(), 0/*System.currentTimeMillis()*/);
      keyStateChanged(e);
    }

    public void keyReleased(KeyEvent e) {
      keyTimeMap.remove(e.getKeyCode());
      keyStateChanged(e);
    }

    // one method to rule them all
    private void keyStateChanged(KeyEvent e) {
      Set keys = keyTimeMap.keySet();
      // filter out duplicate events (e.g. key stays pressed)
      if (previous != null && previous.equals(keys))
        return;
      previous = new Vector(keys);
      //System.out.println(e.getKeyCode()+"/"+e.getKeyModifiersText(e.getModifiers())+"/"+e.getKeyText(e.getKeyCode()));
      setTool(keys);
    }
    
  };

  public KeyListener getKeyListener(){
    return keyListener;
  }

  /*
   * Cleans up the list of pressed keys by verifying the attached time stamp
   * and deleting all entries older than 1 second
   * 
   * This prevents keeping entries where the app lost focus with the key 
   * pressed and java does not receive key released event.
   */
  private void revalidateQuasiMode(){
    // TEST: don't overengineer, simply reset remembered keys
    //       the key listener will switch if necessary
    keyTimeMap.clear();
    
//    Iterator it = keyTimeMap.entrySet().iterator();
//    while (it.hasNext()) {
//        Map.Entry pair = (Map.Entry)it.next();
//        //System.out.println(pair.getKey()+"="+((Long)pair.getValue()-System.currentTimeMillis()));
//        if ((Long)pair.getValue() < System.currentTimeMillis()-1000)
//          it.remove(); // avoids a ConcurrentModificationException
//    }
    setTool(keyTimeMap.keySet());
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

    // standard null tool is the default tool
    if (tool == null)
      tool = getDefaultTool();//dummytool;
    
    //System.out.println("qmt selected "+tool.getName());
    
    // if delegate tool didn't change, do nothing
    //System.out.println(tool+" == "+getDelegate()+" is "+(tool.equals(getDelegate())));
    if (tool.equals(getDelegate())){
      return;
    }
    
    super.deactivate();
    setDelegate(tool);
    super.activate(panel);

    panel.setCursor(getCursor());
    panel.setCurrentCursorTool(this);
  }

  private LayerViewPanel panel;
  private WorkbenchFrame frame;

  public void activate(final LayerViewPanel panel) {
    if (panel==null) return;
    this.panel = panel;

    // check if keys are still pushed
    revalidateQuasiMode();

    super.activate(panel);

    // attach our keylistener
    panel.getWorkBenchFrame().addEasyKeyListener(keyListener);
  }

  public void deactivate() {
    super.deactivate();
    panel.getWorkBenchFrame().removeEasyKeyListener(keyListener);
  }

  // same as below, just does not alter key assignments but represents pressed keys instead
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
      // Mac always uses CMD key instead of CTRL, which is preserved for left
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
      if (obj instanceof ModifierKeySpec) {
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
      }else if (obj instanceof KeyStroke){
        KeyStroke other = (KeyStroke) obj;
        return other.equals(toKeyStroke());
      }
      
      return false;
    }
    
    public KeyStroke toKeyStroke(){
      Iterator iter = this.iterator();
      // iterate over pressed keys, generate
      int modifiers = 0, keys = 0;
      while (iter.hasNext()) {
        Integer keyval = (Integer) iter.next();
        if (keyval==KeyEvent.VK_SHIFT){
          modifiers |= KeyEvent.SHIFT_MASK;
        }else if (keyval==KeyEvent.VK_CONTROL){
          modifiers |= KeyEvent.CTRL_MASK;
        }else if (keyval==KeyEvent.VK_META){
          modifiers |= KeyEvent.META_MASK;
        }else if (keyval==KeyEvent.VK_ALT){
          modifiers |= KeyEvent.ALT_MASK;
        }else if (keyval==KeyEvent.VK_ALT_GRAPH){
          modifiers |= KeyEvent.ALT_GRAPH_MASK;
        }else{
          keys |= keyval;
        }
      }

      return KeyStroke.getKeyStroke(keys, modifiers);
    }

    public String toString() {
      String out = "";
      Iterator iter = this.iterator();
      // iterate over pressed keys, generate
      while (iter.hasNext()) {
        Integer keyval = (Integer) iter.next();
        String keyDesc = KeyEvent.getKeyText(keyval);
        out += out.length()>0? "+"+keyDesc : keyDesc;
      }
      return out;
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



  public static QuasimodeTool createWithDefaults(CursorTool tool) {
    QuasimodeTool quasimodeTool = tool instanceof QuasimodeTool ? (QuasimodeTool) tool
        : new QuasimodeTool(tool);
    
    quasimodeTool.useDefaults(true);
    
    return quasimodeTool;
  }

  /*
   * manually add a default quasimode tool.
   */
  private static CursorTool addDefaultTool(ModifierKeySpec key, CursorTool tool) {
    tool = (tool.isRightMouseButtonUsed() || tool instanceof LeftClickFilter) ? tool
        : new LeftClickFilter(tool);
    defaultToolsMap.put(key, tool);
    return tool;
  }
  
  /*
   * switch using default quasimodes on/off
   */
  public void useDefaults( boolean onoff ){
    useDefaults = onoff;
  }
  
  /*
   * retrieve a quasimode tool by it's shortcut
   */
  public static CursorTool getDefaultKeyboardShortcutTool(ModifierKeySpec key){
    return defaultToolsMap.get(key);
  }

  /*
   * retrieve a set of all registered quasimode tools
   */
  public static Set<ModifierKeySpec> getDefaultKeyboardShortcuts(){
//    HashSet<KeyStroke> set = new HashSet<KeyStroke>();
//    for (ModifierKeySpec spec : defaultToolsMap.keySet()) {
//      set.add(spec.toKeyStroke());
//    }
    return defaultToolsMap.keySet();
  }
}
