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

package com.vividsolutions.jump.workbench.ui.toolbox;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;

/**
 * An always-on-top modeless dialog with a WorkbenchToolBar (for CursorTools,
 * PlugIns, and other buttons). Takes care of unpressing the CursorTools (if
 * necessary) when the dialog is closed (and pressing the first CursorTool on
 * the main toolbar).
 */

// Must use modeless dialog rather than JInternalFrame; otherwise, if we
// implement it as an internal frame, when we click it, the original TaskFrame
// will deactivate, thus deactivating the current CursorTool. [Jon Aquino]
// not sure this is necessary anymore as the logic of determining the active
// taskframe has been reworked, still it works, so leave it in peace [ede 01.2013]
public class ToolboxDialog extends JDialog {
    private ArrayList pluginsTools = new ArrayList();

    public ToolboxDialog(final WorkbenchContext context) {
      super(context.getWorkbench().getFrame(), "", false);
      jbInit();
      this.context = context;
      setResizable(true);
      setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      this.addComponentListener(new ComponentAdapter() {
        public void componentHidden(ComponentEvent e) {
          if (buttons.contains(context.getWorkbench().getFrame().getToolBar().getSelectedCursorToolButton())) {
            ((AbstractButton) context.getWorkbench().getFrame().getToolBar().getButtonGroup().getElements()
                .nextElement()).doClick();
          }
        }
      });
    }

    private WorkbenchContext context;

    private JPanel northPanel = new JPanel();
    private JPanel centerPanel = new JPanel();
    private JPanel southPanel = new JPanel();

    private JPanel toolbarsPanel = new JPanel();
    private JSeparator northPanelSeparator = new JSeparator(JSeparator.HORIZONTAL);

    private GridLayout gridLayout1 = new GridLayout();

    private void jbInit() {
      getContentPane().setLayout(new BorderLayout());

      northPanel.setLayout(new BorderLayout());
      getContentPane().add(northPanel, BorderLayout.NORTH);
      centerPanel.setLayout(new BorderLayout());
      getContentPane().add(centerPanel, BorderLayout.CENTER);
      southPanel.setLayout(new BorderLayout());
      getContentPane().add(southPanel, BorderLayout.SOUTH);

      toolbarsPanel.setLayout(gridLayout1);
      // pad toolbar left/right a bit
      toolbarsPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
      gridLayout1.setColumns(1);

      // add toolbar panel to the north panel
      northPanel.add(toolbarsPanel, BorderLayout.CENTER);
      // separate toolbar visually from the center panel
      northPanel.add(northPanelSeparator, BorderLayout.SOUTH);
      // only shown if there are buttons registered
      this.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
          northPanelSeparator.setVisible(pluginsTools.size()>0);
        }
      });
    }

    public JPanel getNorthPanel() {
      return northPanel;
    }

    public JPanel getSouthPanel() {
      return southPanel;
    }

    public JPanel getCenterPanel() {
      return centerPanel;
    }

    /**
     * [ede 01.2013] disabled and replaced with ComponentListener above Call this
     * method after all the CursorTools have been added.
     */
    public void finishAddingComponents() {
    }

    public void setVisible(boolean visible) {
      if (visible && !locationInitializedBeforeMakingDialogVisible) {
        // here comes a hack
        addComponentListener(new ComponentListener() {
          public void componentShown(ComponentEvent e) {
            // we assume all plugins registered before us, so they will
            // install before us also, so we pack and unregister ourself
            pack();
            removeComponentListener(this);
          }

          public void componentResized(ComponentEvent e) {
          }

          public void componentMoved(ComponentEvent e) {
          }

          public void componentHidden(ComponentEvent e) {
          }
        });

        // #initializeLocation was called in #finishAddingComponents,
        // but the Workbench may have moved since then, so call
        // #initializeLocation again just before making the dialog
        // visible. [Jon Aquino 2005-03-14]
        pack();
        initializeLocation();
        locationInitializedBeforeMakingDialogVisible = true;
      }
      // TODO: change this strange programming, see above
      // weird shit.. Plugins register as ComponentListeners to us, so
      // they can add buttons as soon as we are shown [ede 01.2013]
      // this leads to the phenomenon that on shown we see the editTooolbar
      // extending while plugins add their tools to it and pack() it after
      // each entry in finishAddingComponents()
      super.setVisible(visible);
    }

    public AbstractButton getButton(Class cursorToolClass) {
        for (Iterator i = toolBars.iterator(); i.hasNext();) {
            WorkbenchToolBar toolBar = (WorkbenchToolBar) i.next();
            AbstractButton button = toolBar.getButton(cursorToolClass);
            if (button != null) {
                return button;
            }
        }
        return null;
    }

    public WorkbenchToolBar getToolBar() {
        if (toolBars.isEmpty()) {
            addToolBar();
        }
        return (WorkbenchToolBar) toolBars.get(toolBars.size() - 1);
    }

    public WorkbenchContext getContext() {
        return context;
    }

    public WorkbenchToolBar.ToolConfig add(CursorTool tool) {
        return add(tool, null);
    }

    /**
     * @param enableCheck
     *            null to leave unspecified
     */
    public WorkbenchToolBar.ToolConfig add(CursorTool tool,
            EnableCheck enableCheck) {
        WorkbenchToolBar.ToolConfig config = getToolBar().addCursorTool(tool);
        JToggleButton button = config.getButton();
        getToolBar().setEnableCheck(button,
                enableCheck != null ? enableCheck : new MultiEnableCheck());
        registerButton(button, enableCheck);
        
        // add to internal list
        pluginsTools.add(tool);
        return config;
    }

    public void addPlugIn(PlugIn plugIn, EnableCheck enableCheck, Icon icon) {
        registerButton(getToolBar().addPlugIn(icon, plugIn, enableCheck,
                context), enableCheck);
        // add to internal list
        pluginsTools.add(plugIn);
    }

    public List getPluginsTools(){
      return new ArrayList(pluginsTools);
    }
    
    private void registerButton(AbstractButton button, EnableCheck enableCheck) {
        buttons.add(button);
    }

    private ArrayList<AbstractButton> buttons = new ArrayList();

    public List<AbstractButton> getButtons() {
        return buttons;
    }

    private ArrayList<WorkbenchToolBar> toolBars = new ArrayList();

    public List<WorkbenchToolBar> getToolBars() {
        return toolBars;
    }

    public void addToolBar() {
        toolBars.add(new WorkbenchToolBar(context, context.getWorkbench()
                .getFrame().getToolBar().getButtonGroup()));
        getToolBar().setBorder(null);
        getToolBar().setFloatable(false);
        gridLayout1.setRows(toolBars.size());
        toolbarsPanel.add(getToolBar());
    }

    private boolean locationInitializedBeforeMakingDialogVisible = false;

    private void initializeLocation() {
        GUIUtil.setLocation(this, initialLocation, context.getWorkbench()
                .getFrame().getDesktopPane());
    }

    private GUIUtil.Location initialLocation = new GUIUtil.Location(0, false,
            0, false);

    public void updateEnabledState() {
        for (Iterator i = toolBars.iterator(); i.hasNext();) {
            WorkbenchToolBar toolBar = (WorkbenchToolBar) i.next();
            toolBar.updateEnabledState();
        }
    }

    public void setInitialLocation(GUIUtil.Location location) {
        initialLocation = location;
    }

}