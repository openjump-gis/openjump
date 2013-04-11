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
package com.vividsolutions.jump.workbench.ui.cursortool.editing;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.openjump.core.ui.plugin.edittoolbox.cursortools.ScaleSelectedItemsTool;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.NodeLineStringsTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectFeaturesTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectLineStringsTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SelectPartsTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SplitLineStringTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.OptionsPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;

public class EditingPlugIn extends ToolboxPlugIn {
  private static EditingPlugIn instance = null;
  
  /**
   * please use getInstance() to get unique runtime instance.
   */
  private EditingPlugIn() {
    super();
  }

  public String getName() {
    return I18N.get("ui.cursortool.editing.EditingPlugIn.editing-toolbox");
  }

  // public static ImageIcon ICON =
  // IconLoaderFamFam.icon("page_white_wrench.png");
  public static ImageIcon ICON = IconLoader.icon("EditingToolbox.gif");

  public static final String KEY = EditingPlugIn.class.getName();

  private JButton optionsButton;
  
  private WorkbenchContext wbc;

  public void initialize(PlugInContext context) throws Exception {
    wbc = context.getWorkbenchContext();
    wbc.getWorkbench().getBlackboard().put(KEY, this);
    optionsButton = new JButton(
        I18N.get("ui.cursortool.editing.EditingPlugIn.options"));
  }

  protected void initializeToolbox(ToolboxDialog toolbox) {
    // add easylistener to switch QuasiModeTools also when focus is still on Toolbox
    wbc.getWorkbench().getFrame().addEasyKeyListenerToComp(toolbox);
    
    // The auto-generated title "Editing Toolbox" is too long to fit. [Jon
    // Aquino]
    toolbox.setTitle(I18N.get("ui.cursortool.editing.EditingPlugIn.editing"));
    toolbox.setResizable(false);
    toolbox.setInitialLocation(new GUIUtil.Location(20, true, 20, false));
    
    EnableCheckFactory checkFactory = new EnableCheckFactory(
        toolbox.getContext());
    // Null out the quasimodes for [Ctrl] because the Select tools will handle
    // that case. [Jon Aquino]
    toolbox.add(new SelectFeaturesTool());
    toolbox.add(new SelectPartsTool());
    toolbox.add(new SelectLineStringsTool());
    toolbox.add(new MoveSelectedItemsTool(checkFactory));

    toolbox.addToolBar();
    toolbox.add(DrawRectangleTool.create(toolbox.getContext()));
    toolbox.add(DrawPolygonTool.create(toolbox.getContext()));
    toolbox.add(DrawLineStringTool.create(toolbox.getContext()));
    toolbox.add(DrawPointTool.create(toolbox.getContext()));

    toolbox.addToolBar();
    CursorTool insVertex = new InsertVertexTool(checkFactory);
    CursorTool delVertex = new DeleteVertexTool(checkFactory);
    CursorTool movVertex = new MoveVertexTool(checkFactory);
    toolbox.add(insVertex);
    toolbox.add(delVertex);
    toolbox.add(movVertex);

    // -- [sstein: 11.12.2006] added here to fill toolbox
    toolbox.add(new ScaleSelectedItemsTool(checkFactory));

    toolbox.addToolBar();
    toolbox.add(new SnapVerticesTool(checkFactory));
    toolbox.add(new SnapVerticesToSelectedVertexTool(checkFactory));
    toolbox.add(new SplitLineStringTool());
    toolbox.add(new NodeLineStringsTool());

    optionsButton.addActionListener(AbstractPlugIn.toActionListener(
        new OptionsPlugIn(), toolbox.getContext(), null));
    optionsButton.setIcon(IconLoader.icon("fugue/wrench-screwdriver.png"));
    toolbox.getCenterPanel().add(optionsButton, BorderLayout.CENTER);

  }

  public static EditingPlugIn getInstance(){
    if (instance==null)
      instance=new EditingPlugIn();
    return instance;
  }
  
}