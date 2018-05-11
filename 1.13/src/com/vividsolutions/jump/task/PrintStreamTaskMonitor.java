
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

package com.vividsolutions.jump.task;

import java.io.PrintStream;

import com.vividsolutions.jump.util.StringUtil;


/**
 * A TaskMonitor that reports its output to a PrintStream.
 * User can control the level of output reported, as well as whether timing
 * information is logged as well.
 */
public class PrintStreamTaskMonitor implements TaskMonitor {
    private PrintStream stream;
    private boolean isLoggingSubtasks = false;

    public PrintStreamTaskMonitor(PrintStream stream) {
        this.stream = stream;
    }

    public PrintStreamTaskMonitor() {
        stream = System.out;
    }

    public void setLoggingSubtasks(boolean isLoggingSubtasks) {
        this.isLoggingSubtasks = isLoggingSubtasks;
    }

    public void report(String description) {
        stream.println(description);
    }

    public void report(Exception exception) {
        stream.println(StringUtil.stackTrace(exception));
    }

    public void report(int subtasksDone, int totalSubtasks,
        String subtaskDescription) {
        if (isLoggingSubtasks) {
            stream.println(subtasksDone + " / " + totalSubtasks + " " +
                subtaskDescription);
        }
    }

    public void allowCancellationRequests() {
    }

    public boolean isCancelRequested() {
        return false;
    }
}
