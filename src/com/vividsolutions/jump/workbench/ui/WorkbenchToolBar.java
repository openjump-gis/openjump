
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

package com.vividsolutions.jump.workbench.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

import de.soldin.awt.WrapLayout;

/**
 * Makes it easy to add CursorTools and PlugIns as toolbar buttons.
 * An "EnableCheck" is used to specify whether to enable or disable the
 * CursorTool buttons. Moreover, CursorTools are added as mutually exclusive
 * toggle buttons (that is, JToggleButtons in a ButtonGroup). When a CursorTool
 * button is pressed, the current CursorTool is unregistered and the new one
 * is registered with the LayerViewPanel.
 * <P>
 * Set the cursor-tool-enable-check to use context-sensitive enabling of toolbar
 * buttons.
 * <P>
 * Set the task-monitor-manager to report the progress of threaded plug-ins.
 */
public class WorkbenchToolBar extends EnableableToolBar {
    private HashMap cursorToolClassToButtonMap = new HashMap();


    private LayerViewPanelProxy layerViewPanelProxy;
    private TaskMonitorManager taskMonitorManager = null;

    public AbstractButton getButton(Class cursorToolClass) {
        Assert.isTrue(CursorTool.class.isAssignableFrom(cursorToolClass));
        return (AbstractButton) cursorToolClassToButtonMap.get(cursorToolClass);
    }

    // By default, CursorTool buttons are always enabled. [Jon Aquino]
    private EnableCheck cursorToolEnableCheck = new EnableCheck() {
            public String check(JComponent component) {
                return null;
            }
        };

    private ButtonGroup cursorToolButtonGroup;

    public WorkbenchToolBar(LayerViewPanelProxy layerViewPanelProxy) {
        this(layerViewPanelProxy, new ButtonGroup());
    }

    public WorkbenchToolBar(LayerViewPanelProxy layerViewPanelProxy, ButtonGroup cursorToolButtonGroup) {
        this.cursorToolButtonGroup = cursorToolButtonGroup;
        this.layerViewPanelProxy = layerViewPanelProxy;
        setLayout(new WrapLayout(WrapLayout.LEFT, 0, 0));
    }

    public void setCursorToolEnableCheck(EnableCheck cursorToolEnableCheck) {
        this.cursorToolEnableCheck = cursorToolEnableCheck;
    }

    public void setTaskMonitorManager(TaskMonitorManager taskMonitorManager) {
        this.taskMonitorManager = taskMonitorManager;
    }
    
    public ToolConfig addCursorTool(final CursorTool cursorTool) {
        return addCursorTool(cursorTool.getName(), cursorTool,
                new JToggleButton() {
                    public String getToolTipText(MouseEvent event) {
                        //Get tooltip text dynamically [Jon Aquino 11/13/2003]
                    return cursorTool.getName();
                }
                });
    }

	/**
	 * Add's a CursorTool with an own JToggleButton.
	 * This is useful, if you want to add CursorTool with an own JToggleButton
	 * implementation, such a DropDownToggleButton.
	 * 
	 */
	public ToolConfig addCursorTool(final CursorTool cursorTool, JToggleButton button) {
		button.setToolTipText(cursorTool.getName());
        return addCursorTool(cursorTool.getName(), cursorTool, button);
    }
    
    public static class ToolConfig {
        private JToggleButton button;
        private QuasimodeTool quasimodeTool;
        public ToolConfig(JToggleButton button, QuasimodeTool quasimodeTool) {
            this.button = button;
            this.quasimodeTool = quasimodeTool;
        }
        public JToggleButton getButton() {
            return button;
        }

        public QuasimodeTool getQuasimodeTool() {
            return quasimodeTool;
        }

    }

    public ToolConfig addCursorTool(String tooltip, final CursorTool cursorTool) {
        JToggleButton button = new JToggleButton();
        return addCursorTool(tooltip, cursorTool, button);
    }
    
    private ToolConfig addCursorTool(String tooltip, final CursorTool cursorTool,
        JToggleButton button) {
      cursorToolButtonGroup.add(button);
      cursorToolClassToButtonMap.put(cursorTool.getClass(), button);
  
      final QuasimodeTool quasimodeTool = cursorTool instanceof QuasimodeTool ? 
          (QuasimodeTool) cursorTool : QuasimodeTool.createWithDefaults(cursorTool);
      add(button, tooltip, cursorTool.getIcon(), new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // It's null when the Workbench starts up. [Jon Aquino]
          // Or the active frame may not have a LayerViewPanel. [Jon Aquino]
          if (layerViewPanelProxy.getLayerViewPanel() != null)
            layerViewPanelProxy.getLayerViewPanel().setCurrentCursorTool(
                quasimodeTool);
          // <<TODO:DESIGN>> We really shouldn't create a new LeftClickFilter on
          // each
          // click of the tool button. Not a big deal though. [Jon Aquino]
        }
      }, cursorToolEnableCheck);
      if (cursorToolButtonGroup.getButtonCount() == 1) {
        cursorToolButtonGroup.setSelected(button.getModel(), true);
        reClickSelectedCursorToolButton();
      }
      return new ToolConfig(button, quasimodeTool);
    }

    public ButtonGroup getButtonGroup() { return cursorToolButtonGroup; }

    public JToggleButton getSelectedCursorToolButton() {
        for (Enumeration e = cursorToolButtonGroup.getElements();
                e.hasMoreElements();) {
            JToggleButton button = (JToggleButton) e.nextElement();

            if (button.getModel() == cursorToolButtonGroup.getSelection()) {
                return button;
            }
        }

        Assert.shouldNeverReachHere();

        return null;
    }

    public void reClickSelectedCursorToolButton() {
        if (cursorToolButtonGroup.getButtonCount() == 0) {
            return;
        }
        getSelectedCursorToolButton().doClick();
    }

    //<<TODO:REFACTOR>> This method duplicates code in FeatureInstaller, with the
    //result that when the latter was updated (to handle ThreadedPlugIns), the
    //changes were left out from the former. [Jon Aquino]
    public JButton addPlugIn(Icon icon, final PlugIn plugIn,
        EnableCheck enableCheck, WorkbenchContext workbenchContext) {
        JButton button = new JButton();
        add(button, plugIn.getName(), icon,
            AbstractPlugIn.toActionListener(plugIn, workbenchContext,
                taskMonitorManager), enableCheck);

        return button;
    }
    
    public JButton addPlugIn(final int index, final PlugIn plugIn,
      final Icon icon, final EnableCheck enableCheck,
      final WorkbenchContext workbenchContext) {
      JButton button = new JButton();
      ActionListener listener = AbstractPlugIn.toActionListener(plugIn,
        workbenchContext, taskMonitorManager);
      add(index, button, plugIn.getName(), icon, listener, enableCheck);
      return button;
  }
}
