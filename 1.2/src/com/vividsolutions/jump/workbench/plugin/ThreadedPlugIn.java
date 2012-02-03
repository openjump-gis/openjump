
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

import com.vividsolutions.jump.task.TaskMonitor;


/**
 * A long-running PlugIn that keeps the GUI responsive (but only partially so,
 * as a modal progress dialog will be in the way).
 * <P>
 * First, #execute is called on the AWT event dispatch thread (the "GUI
 * thread"). Here, the user can be prompted with a dialog.
 * <P>
 * Then, #run(TaskMonitor) is called, on a new thread. Here, a long operation
 * can be performed. The TaskMonitor can be used to report progress (messages
 * will appear on a modal progress dialog). Because the thread is not the GUI
 * thread, the GUI will remain responsive.
 * <P>
 * Thus, to run a PlugIn on a separate thread with progress reporting:
 * <UL>
 * <LI>implement ThreadedPlugIn
 * <LI>put any GUI prompting in #execute. If the user chooses to cancel,
 * return false.
 * <LI>put the time-consuming task in #run(TaskMonitor)
 * <LI>add the ThreadedPlugIn using FeatureInstaller
 * </UL>
 */
public interface ThreadedPlugIn extends PlugIn {
    /**
     * Runs the task. This method will be executed in a separate thread, so that
     * the GUI remains responsive (but only partially so, as a modal progress
     * dialog will be in the way). Don't call GUI classes in this method as it is not 
     * executed on the GUI thread.
     * @param monitor context to which this task can report its progress and
     * check whether a party has requested its cancellation
     */
    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception;
}
