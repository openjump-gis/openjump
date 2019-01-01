
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

package com.vividsolutions.jump.util;

import com.vividsolutions.jts.util.Stopwatch;


public class DebugTimer {

    private static DebugTimer timer = new DebugTimer();
    private final String blankStr;
    private final int TIME_LEN;
    private Stopwatch sw = null;

    public DebugTimer() {
        sw = new Stopwatch();
        sw.start();
        blankStr = "          ";
        TIME_LEN = blankStr.length();
    }

    public static void startStatic(String msg) {
        timer.start(msg);
    }

    public static void logEventStatic(String msg) {
        timer.logEvent(msg);
    }

    public void start(String msg) {
        System.out.println("Started    " + msg);
        sw.start();
    }

    public void logEvent(String msg) {
        String elapsedStr = formatTime(sw.getTimeString());
        System.out.println("Elapsed: " + elapsedStr + "    " + msg);
        sw.start();
    }

    public String formatTime(String timeStr) {
        if (timeStr.length() < TIME_LEN) {
            String filled = blankStr + timeStr;
            int start = filled.length() - TIME_LEN;

            return filled.substring(start);
        }
        // don't pad if it's already longer
        return timeStr;
    }

}
