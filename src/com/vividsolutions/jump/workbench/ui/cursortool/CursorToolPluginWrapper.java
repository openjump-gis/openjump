package com.vividsolutions.jump.workbench.ui.cursortool;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

/**
 * a simple wrapper to reuse a cursor tool as a plugin w/in the ui.
 */
public class CursorToolPluginWrapper implements PlugIn {

  CursorTool tool;

  /**
   * create a plugin instance for the given cursor tool
   * 
   * make sure the cursor tool respects shortcuts by wrapping it into
   * {@link com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool}
   * 
   * @param cursorTool
   */
  public CursorToolPluginWrapper(CursorTool cursorTool) {
    super();
    final QuasimodeTool quasimodeTool = cursorTool instanceof QuasimodeTool ? (QuasimodeTool) cursorTool
        : QuasimodeTool.createWithDefaults(cursorTool);
    this.tool = quasimodeTool;
  }

  @Override
  public void initialize(PlugInContext context) throws Exception {
  }

  @Override
  public boolean execute(PlugInContext context) throws Exception {
    LayerViewPanel lvp = context.getLayerViewPanel();

    if (lvp != null) {
      lvp.setCurrentCursorTool(tool);
      return true;
    }
    return false;
  }

  @Override
  public String getName() {
    return tool.getName();
  }

  public ImageIcon getIcon() {
    // we really only use ImageIcons anyway for CursorTools
    Icon icon = tool.getIcon();
    if (icon instanceof ImageIcon)
      return ((ImageIcon) icon);
    return null;
  }

}
