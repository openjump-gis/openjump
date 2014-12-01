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

package com.vividsolutions.jump.workbench;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.driver.DriverManager;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerManagerProxy;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.*;

/**
 * Implementation of {@link WorkbenchContext} for the {@link JUMPWorkbench}.
 */
public class JUMPWorkbenchContext extends WorkbenchContext {
  private JUMPWorkbench workbench;

  public JUMPWorkbenchContext(JUMPWorkbench workbench) {
    this.workbench = workbench;
  }

  public JUMPWorkbench getWorkbench() {
    return workbench;
  }

  public Blackboard getBlackboard() {
    return workbench.getBlackboard();
  }

  public DriverManager getDriverManager() {
    return workbench.getDriverManager();
  }

  public ErrorHandler getErrorHandler() {
    return workbench.getFrame();
  }

  public Task getTask() {
    // in the very beginning there is no task e.g.
    // JUMPConfiguration.setup(WorkbenchContext)
    return getActiveTaskFrame() instanceof TaskFrame ? getActiveTaskFrame()
        .getTask() : null;
  }

  public LayerNamePanel getLayerNamePanel() {
    return getActiveTaskFrame() instanceof LayerNamePanelProxy ? 
        ((LayerNamePanelProxy) getActiveTaskFrame()).getLayerNamePanel() : null;
  }

  public LayerManager getLayerManager() {
    return getActiveTaskFrame() instanceof LayerManagerProxy ? getActiveTaskFrame()
        .getLayerManager() : null;
  }

  public LayerViewPanel getLayerViewPanel() {
    return getActiveTaskFrame() instanceof LayerViewPanelProxy ? getActiveTaskFrame()
        .getLayerViewPanel() : null;
  }

  // ask workbench to give us the active taskframe
  private TaskFrame getActiveTaskFrame() {
    return workbench.getFrame().getActiveTaskFrame();
  }
}
