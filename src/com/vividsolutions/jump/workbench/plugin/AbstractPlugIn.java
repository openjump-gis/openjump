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

package com.vividsolutions.jump.workbench.plugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.model.UndoableEditReceiver;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

/**
 * Default implementation of PlugIn, with useful functions for auto-generating a
 * name, converting a PlugIn into an ActionListener (for use with JButtons, for
 * example), and supporting undo.
 */
public abstract class AbstractPlugIn implements PlugIn, ShortcutEnabled, EnableChecked, Iconified, Recordable {

  protected int shortcutModifiers = 0;
  protected int shortcutKeys = 0;
  private String name;

  // [mmichaud 2014-10-01] add some methods for macro plugin recorder
  private Map<String,Object> parameters;
  private PlugInContext context = null;

  public void addParameter(String name, Object value) {
      if (parameters == null) parameters = new HashMap<>();
      parameters.put(name, value);
  }

  public Object getParameter(String name) {
      if (parameters == null) return null;
      return parameters.get(name);
  }

  public Boolean getBooleanParam(String name) {
      if (parameters == null) return null;
      return (Boolean)parameters.get(name);
  }

  public Integer getIntegerParam(String name) {
      if (parameters == null) return null;
      return (Integer)parameters.get(name);
  }

  public Double getDoubleParam(String name) {
      if (parameters == null) return null;
      return (Double)parameters.get(name);
  }

  public String getStringParam(String name) {
      if (parameters == null) return null;
      return (String)parameters.get(name);
  }

  public void setParameters(Map<String,Object> map) {
      parameters = map;
  }

  public Map<String,Object> getParameters() {
      return parameters;
  }
  // [mmichaud 2014-10-01] end

  protected void execute(UndoableCommand command, PlugInContext context) {
    execute(command, context.getLayerViewPanel());
  }

  public AbstractPlugIn() {
  }

  public AbstractPlugIn(String name) {
    this.name = name;
  }

  public void initialize(PlugInContext context) throws Exception {
    this.context  = context;
  }

  /**
   * Execute the PlugIn.
   * @param context context of this PlugIn
   * @return true if the PlugIn has been executed
   * @throws Exception if an Exception occurs during execution
   */
  public boolean execute(PlugInContext context) throws Exception {
    return true;
  }

  /**
   * Indicates that this plug-in either (1) is undoable but hasn't modified the
   * system yet or (2) does not modify the system. In either case, the undo
   * history will be preserved. If this method is not called, then this plug-in
   * will be assumed to be non-undoable, and the undo history will be truncated.
   * @param context plugin context
   */
  protected void reportNothingToUndoYet(PlugInContext context) {
    // The LayerManager can be null if for example there are no TaskFrames
    // and
    // the user selects File / New Task. When we get to this point,
    // LayerManager
    // will be null. [Jon Aquino]
    if (context.getLayerManager() == null) {
      return;
    }
    context.getLayerManager().getUndoableEditReceiver()
        .reportNothingToUndoYet();
  }

  protected boolean isRollingBackInvalidEdits(PlugInContext context) {
    return PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
        .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
  }

  /*
   * ShortCutEnabled implementation.
   */
  public boolean isShortcutEnabled() {
    return shortcutKeys>0;
  }

  /*
   * ShortCutEnabled implementation.
   */
  public final int getShortcutModifiers() {
    return shortcutModifiers;
  }

  /*
   * ShortCutEnabled implementation.
   */
  public void setShortcutModifiers(int shortcutModifiers) {
    this.shortcutModifiers = shortcutModifiers;
  }

  /*
   * ShortCutEnabled implementation.
   */
  public final int getShortcutKeys() {
    return shortcutKeys;
  }

  /*
   * ShortCutEnabled implementation.
   */
  public void setShortcutKeys(int shortcutKeys) {
    this.shortcutKeys = shortcutKeys;
  }

  /*
   * ShortCutEnabled implementation.
   */
  public KeyStroke getShortcutKeyStroke() {
    return getShortcutKeys() > 0 ? KeyStroke.getKeyStroke(getShortcutKeys(),
        getShortcutModifiers()) : null;
  }

  /**
   * Use reflection to find an EnableCheck object defined by old method
   * createEnableCheck in this plugin.
   * @return the EnableCheck defined the old way
   */
  public EnableCheck getEnableCheck() {
    //System.out.println("ap look for "+this.getName());
    // find old method
    try {
      Method m = null;
      Class<?> c = this.getClass();
      do {
        try {
          //System.out.println("ap check "+c);
          m = c.getDeclaredMethod("createEnableCheck", WorkbenchContext.class);
        } catch (NoSuchMethodException e) {}
      } while (m==null && (c=c.getSuperclass())!=null);
      if (m != null) {
        m.setAccessible(true);
        PlugInContext pc = getContext();
        if (pc==null) throw new IllegalArgumentException(getName());
        return (EnableCheck) m.invoke(this, pc.getWorkbenchContext());
      }
    } catch (SecurityException|IllegalArgumentException|IllegalAccessException|InvocationTargetException e) {
      Logger.error(e);
    }
    // or return unimplemented
    return null;
  }

  public Icon getIcon(int height) {
    return getIcon(new Dimension(height, height));
  }

  /**
   * Use reflection to find the icon defined the old way by method
   * getIcon or by attribute ICON.
   * @param dim dimension of the icon
   * @return the Icon of this PlugIn
   */
  public Icon getIcon(Dimension dim) {
    Icon icon = null;
    Class c = this.getClass();
    // find old method
    try {
      Method m = null;
      do {
        try {
          //System.out.println("ap check "+c);
          m = c.getDeclaredMethod("getIcon");
        } catch (NoSuchMethodException e) {}
      } while (m==null && (c=c.getSuperclass())!=null);
      if (m != null) {
        m.setAccessible(true);
        icon = (Icon) m.invoke(this);
      }
    } catch (SecurityException e) {
    } catch (IllegalArgumentException e) {
    } catch (IllegalAccessException e) {
    } catch (InvocationTargetException e) {
    }
    // find old field
    if (icon==null) {
      c = this.getClass();
      try {
        Field f = c.getDeclaredField("ICON");
        if (Icon.class.isAssignableFrom(f.getType()))
          icon = (Icon) f.get(this);
      } catch (NoSuchFieldException e) {
      } catch (SecurityException e) {
      } catch (IllegalArgumentException e) {
      } catch (IllegalAccessException e) {
      }
      
    }
    // resize if requested (currently only one via height param)
    if (icon instanceof ImageIcon && dim != null
        && icon.getIconHeight() != dim.height) {
      icon = GUIUtil.resize((ImageIcon) icon, dim.height);
    }
    
    return icon;
  }

  /**
   * @return the class name, minus "PlugIn", with spaces inserted at the
   *         appropriate point before each uppercase+lowercase and
   *         lowercase+uppercase combination.
   */
  public String getName() {
    return name == null ? createName(getClass()) : name;
  }

  public String toString() {
    return getName();
  }
  
  public static String createName(Class<? extends PlugIn> plugInClass) {
    try {
      return I18N.getInstance().get(plugInClass.getName());
    } catch (java.util.MissingResourceException e) {
      // No I18N for the PlugIn so log it, but don't stop
      Logger.error(e.getMessage() + " " + plugInClass.getName());
      return StringUtil.toFriendlyName(plugInClass.getName(), "PlugIn");
    }
  }

  /**
   * @param plugIn the plugin
   * @param workbenchContext context of the application
   * @param taskMonitorManager
   *          can be null if you do not wish to use the Task Monitor
   *          progress-reporting framework
   * @return an ActionListener for this PlugIn
   */

  public static ActionListener toActionListener(final PlugIn plugIn,
      final WorkbenchContext workbenchContext,
      final TaskMonitorManager taskMonitorManager) {
    return new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //System.out.println("ap toaction "+e);
        try {
          if (workbenchContext.getWorkbench() != null) {
            workbenchContext.getWorkbench().getFrame().setStatusMessage("");
            Logger.info(I18N.getInstance().get("plugin.AbstractPlugIn.executing") + " " + plugIn.getName());
          }

          PlugInContext plugInContext = workbenchContext.createPlugInContext();
          // Cache the UndoableEditReceiver, because the "topmost"
          // layer manager before the edit may be different from the
          // topmost layer manager after (e.g. NewTaskPlugIn). [Jon Aquino]

          UndoableEditReceiver undoableEditReceiver = workbenchContext
              .getLayerManager() != null ? workbenchContext.getLayerManager()
              .getUndoableEditReceiver() : null;
          if (undoableEditReceiver != null) {
            undoableEditReceiver.startReceiving();
          }

          try {
            boolean executeComplete = plugIn.execute(plugInContext);
            if (workbenchContext.getBlackboard().get(StartMacroPlugIn.MACRO_STARTED, false)) {
                if (plugIn instanceof StartMacroPlugIn || plugIn instanceof StopMacroPlugIn) {}
                else {
                    ((Macro)workbenchContext.getBlackboard().get("Macro")).addProcess((Recordable)plugIn);
                }
            }
            if (plugIn instanceof ThreadedPlugIn && executeComplete) {
              taskMonitorManager
                  .execute((ThreadedPlugIn) plugIn, plugInContext);
            }
          } finally {
            if (undoableEditReceiver != null) {
              undoableEditReceiver.stopReceiving();
            }
          }

          if (workbenchContext.getWorkbench() != null) {
            Logger.info(I18N.getInstance().get("plugin.AbstractPlugIn.done-current-committed-memory")
                    + workbenchContext.getWorkbench().getFrame()
                    .getMBCommittedMemory() + " MB");
          }
        } catch (Throwable t) {
          workbenchContext.getErrorHandler().handleThrowable(t);
        }
      }
    };
  }

  public static void execute(final UndoableCommand command,
      LayerManagerProxy layerManagerProxy) {
    // Used to do nothing if command or panel were null, but that seems to me
    // now like a dangerous thing to do. So I've taken it out, and hopefully will
    // receive a complaint from someone about a NullPointerException. When I find out
    // why, I'll be sure to document the reason! [Jon Aquino]
    boolean exceptionOccurred = true;
    try {
      command.execute();
      exceptionOccurred = false;
    } finally {
      // Funny logic because I want to avoid adding a throws clause to this
      // method, so that existing code will not break [Jon Aquino 12/5/2003]
      if (exceptionOccurred) {
        layerManagerProxy.getLayerManager().getUndoableEditReceiver()
            .getUndoManager().discardAllEdits();
      }
    }
    // [2013-01-20 mmichaud] undoableEdits created by classes derived from
    // AbstractPlugIn listen to LayerEvent to cancel the action and to free
    // resources if the layer has been removed. 
    final UndoableEdit undoableEdit = command.toUndoableEdit();
    if (command.getLayer() != null) {
        final LayerManager layerManager = command.getLayer().getLayerManager();
        if (layerManager != null) {
            LayerListener listener = new LayerListener(){
              public void categoryChanged(CategoryEvent e) {}
              public void featuresChanged(FeatureEvent e) {}
              public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.REMOVED &&
                        e.getLayerable() == command.getLayer()) {
                  undoableEdit.die();
                  layerManager.removeLayerListener(this);
                }
              }
            };
            layerManager.addLayerListener(listener);
        }
    }
    layerManagerProxy.getLayerManager().getUndoableEditReceiver()
        .receive(command.toUndoableEdit());
  }

  protected PlugInContext getContext() {
    if (context == null) throw new RuntimeException("Add super.initialize() to your AbstractPlugIn.initialize() implementation!\n"+this.getClass().getName());
    return context;
  }

  protected WorkbenchContext getWorkbenchContext() {
    return getContext().getWorkbenchContext();
  }
}