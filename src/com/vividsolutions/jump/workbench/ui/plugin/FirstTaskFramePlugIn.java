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
package com.vividsolutions.jump.workbench.ui.plugin;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Iterator;

import org.openjump.core.ui.plugin.file.OpenFilePlugIn;
import org.openjump.core.ui.plugin.file.OpenProjectPlugIn;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

/**
 * Opens a TaskFrame when the Workbench starts up
 */
public class FirstTaskFramePlugIn extends AbstractPlugIn {// AbstractPlugIn {

  public FirstTaskFramePlugIn() {
  }

  private ComponentListener componentListener;

  @Override
  public void initialize(final PlugInContext context) throws Exception {

    final WorkbenchContext workbenchContext = context.getWorkbenchContext();

    componentListener = new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        // Two reasons wait until the frame is shown before adding the task
        // frame:
        // (1) Otherwise the task frame won't be selected (2) Otherwise
        // GUIUtil.setLocation
        // will throw an IllegalComponentStateException. [Jon Aquino]
        // UT skip this; see 1st if there is a filename available
        // if so, load it
        String filename = (String) context.getWorkbenchContext()
            .getBlackboard().get(JUMPWorkbench.INITIAL_PROJECT_FILE);
        File f;

        // load -project
        if (filename != null ) {// create empty task
          Logger.info("Found initial '-project' file: " + filename);
          f = new File(filename);

          try {
            // switch to new OpenProjectPlugIn [Matthias Scholz 11. Dec. 2011]
            OpenProjectPlugIn openProjectPlugIn = new OpenProjectPlugIn(
                workbenchContext, f);
            AbstractPlugIn.toActionListener(openProjectPlugIn,
                workbenchContext, new TaskMonitorManager()).actionPerformed(
                new ActionEvent(this, 0, ""));
          } catch (Exception ex) {
            String mesg = I18N.getMessage(this.getClass().getName()+".could-not-load-file-{0}", f);
            Logger.error(mesg);
            context.getWorkbenchFrame().warnUser(mesg);
          }
        }

        // load files from commandline
        Iterator files = context.getWorkbenchContext().getWorkbench().getCommandLine().getParams();
        while (files.hasNext()) {
          filename = (String) files.next();
          f = new File(filename);
          try {
            // try project
            if (SaveProjectAsPlugIn.JUMP_PROJECT_FILE_FILTER.accept(f)){
              OpenProjectPlugIn open = new OpenProjectPlugIn(
                  workbenchContext, f);
              AbstractPlugIn.toActionListener(open,
                  workbenchContext, new TaskMonitorManager()).actionPerformed(
                  new ActionEvent(this, 0, ""));
            }
            // else must be a data file
            else {
              OpenFilePlugIn open = new OpenFilePlugIn(
                  workbenchContext, f);
              AbstractPlugIn.toActionListener(open,
                  workbenchContext, new TaskMonitorManager()).actionPerformed(
                  new ActionEvent(this, 0, ""));
            }
          } catch (Exception e2) {
            String mesg = I18N.getMessage(this.getClass().getName()+".could-not-load-file-{0}", f);
            Logger.error(mesg);
          }
        }

        // always open at least one first task
        if (!(context.getWorkbenchFrame().getActiveTaskFrame() instanceof TaskFrame))
          context.getWorkbenchFrame().addTaskFrame();

        context.getWorkbenchFrame().removeComponentListener(componentListener);
      }
    };
    context.getWorkbenchFrame().addComponentListener(componentListener);
  }
}
