package org.openjump.core.ui.plugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.UndoableEditReceiver;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

/**
 * Default implementation of PlugIn, with useful functions for auto-generating a
 * name, converting a PlugIn into an ActionListener (for use with JButtons, for
 * example), and supporting undo.
 */
public abstract class AbstractUiPlugIn extends AbstractPlugIn implements ActionListener {
  /** The icon for the plug-in. */
  private Icon icon;

  /** The name for the plug-in. */
  private String name;

  /** The workbench context. */
  protected WorkbenchContext workbenchContext;

  /** The tool-tip for the plug-in. */
  private String toolTip;

  protected EnableCheck enableCheck = new MultiEnableCheck();

  public AbstractUiPlugIn() {
  }

  public AbstractUiPlugIn(final String name) {
    this.name = name;
  }

  public AbstractUiPlugIn(final Icon icon) {
    this.icon = icon;
  }

  public AbstractUiPlugIn(final String name, final String toolTip) {
    this.name = name;
    this.toolTip = toolTip;
  }

  public AbstractUiPlugIn(final String name, final Icon icon) {
    this.name = name;
    this.icon = icon;
  }

  public AbstractUiPlugIn(final String name, final Icon icon, final String toolTip) {
    this.name = name;
    this.icon = icon;
    this.toolTip = toolTip;
  }

  /**
   * Method to be overridden by implementations to initialize the plug-in.
   * Plug-ins must invoke super.initialize().
   * 
   * @param context The plug-in context.
   */
  public void initialize(final PlugInContext context) throws Exception {
    this.workbenchContext = context.getWorkbenchContext();
  }

  /**
   * Method to be overridden by implementations to execute the plug-in.
   * 
   * @param context The plug-in context.
   */
  public boolean execute(final PlugInContext context) throws Exception {
    return true;
  }

  /**
   * Indicates that this plug-in either (1) is undoable but hasn't modified the
   * system yet or (2) does not modify the system. In either case, the undo
   * history will be preserved. If this method is not called, then this plug-in
   * will be assumed to be non-undoable, and the undo history will be truncated.
   */
  protected void reportNothingToUndoYet(PlugInContext context) {
    LayerManager layerManager = context.getLayerManager();
    if (layerManager != null) {
      layerManager.getUndoableEditReceiver().reportNothingToUndoYet();
    }
  }

  protected boolean isRollingBackInvalidEdits(PlugInContext context) {
    return PersistentBlackboardPlugIn.get(context.getWorkbenchContext()).get(
      EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
  }

  /**
   * Wrap the plug-in as an ActionListener.
   * 
   * @param e The action event.
   */
  public void actionPerformed(final ActionEvent e) {
    try {
      JUMPWorkbench workbench = workbenchContext.getWorkbench();
      WorkbenchFrame frame = workbench.getFrame();
      if (workbench != null) {
        frame.setStatusMessage("");
        frame.log(I18N.get("plugin.AbstractPlugIn.executing") + " " + getName());
      }

      PlugInContext plugInContext = workbenchContext.createPlugInContext();
      LayerManager layerManager = workbenchContext.getLayerManager();
      UndoableEditReceiver undoableEditReceiver = null;
      if (layerManager != null) {
        undoableEditReceiver = layerManager.getUndoableEditReceiver();
        if (undoableEditReceiver != null) {
          undoableEditReceiver.startReceiving();
        }
      }

      try {
        boolean executeComplete = execute(plugInContext);

        if (executeComplete && this instanceof ThreadedPlugIn) {
          new TaskMonitorManager().execute((ThreadedPlugIn)this, plugInContext);
        }
      } finally {
        if (undoableEditReceiver != null) {
          undoableEditReceiver.stopReceiving();
        }
      }

      if (workbench != null) {
        frame.log(I18N.get("plugin.AbstractPlugIn.done-current-committed-memory")
          + frame.getMBCommittedMemory() + " MB");
      }
    } catch (Throwable t) {
      ErrorHandler errorHandler = workbenchContext.getErrorHandler();
      errorHandler.handleThrowable(t);
    }
  }

  public EnableCheck getEnableCheck() {
    return enableCheck;
  }

  /**
   * Get the icon for the plug-in.
   * 
   * @return The icon.
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Get the name of the plug-in. If a name was not specified ask super class.
   * 
   * @return The plug-in name.
   */
  public String getName() {
    return name!=null ? name : super.getName();
  }

  /**
   * Get the tool-tip for the plug-in.
   * 
   * @return The tool-tip.
   */
  public String getToolTip() {
    return toolTip;
  }

//  /**
//   * Create a name using the I18N String using the class name or by adding
//   * spaces between the words in the class name without the PlugIn suffix.
//   * 
//   * @param plugInClass The plug-in's class.
//   * @return The plug-in's name.
//   */
//  public static String createName(final Class plugInClass) {
//    try {
//      return I18N.get(plugInClass.getName());
//    } catch (java.util.MissingResourceException e) {
//      return StringUtil.toFriendlyName(plugInClass.getName(), "PlugIn");
//    }
//  }

  /**
   * @param workbenchContext the workbenchContext to set
   */
  protected void setWorkbenchContext(WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
  }

//  /**
//   * Get the String representation of the plug-in.
//   * 
//   * @return The string.
//   */
//  public String toString() {
//    return getName();
//  }
}
