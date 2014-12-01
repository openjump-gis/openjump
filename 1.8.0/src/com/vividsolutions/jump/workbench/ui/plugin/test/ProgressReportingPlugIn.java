
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

package com.vividsolutions.jump.workbench.ui.plugin.test;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;


public class ProgressReportingPlugIn extends ThreadedBasePlugIn {
    private final static int MS_PER_SUBTASK = 3000;
    private final static int SUBTASK_COUNT = 5;
    private final static int SUBSUBTASK_COUNT = 1000;

    public ProgressReportingPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(this,
            new String[] { "Tools", "Test" }, getName(), false, null, null);
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) {
        monitor.allowCancellationRequests();
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().addHeader(1, "Header 1");
        context.getOutputFrame().addHeader(2, "Header 2");
        context.getOutputFrame().addHeader(3, "Header 3");
        context.getOutputFrame().addHeader(4, "Header 4");
        context.getOutputFrame().addHeader(5, "Header 5");

        for (int i = 1; i <= SUBTASK_COUNT; i++) {
            if (monitor.isCancelRequested()) {
                break;
            }

            monitor.report("Doing Subtask " + i);
            context.getOutputFrame().addField("Progress:", String.valueOf(i),
                "tasks");

            for (int j = 1; j <= SUBSUBTASK_COUNT; j++) {
                monitor.report(j, SUBSUBTASK_COUNT, "subsubtasks");

                try {
                    Thread.sleep(MS_PER_SUBTASK / SUBSUBTASK_COUNT);
                } catch (InterruptedException e) {
                    Assert.shouldNeverReachHere();
                }
            }
        }
    }
}
