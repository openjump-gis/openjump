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

package com.vividsolutions.jump.workbench.ui.renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;


/**
 * Limits the number of executing threads to a specified maximum.
 */
public class ThreadQueue {
    private volatile int runningThreads = 0;

    //Vector is thread-safe. [Jon Aquino]
    private Vector queuedRunnables = new Vector();
    private int maxRunningThreads;

    public ThreadQueue(final int maxRunningThreads) {
        this.maxRunningThreads = maxRunningThreads;
    }

    public void clear() {
        queuedRunnables.clear();
    }

    private void processQueue() {
        while (!queuedRunnables.isEmpty() &&
                (runningThreads < maxRunningThreads) && enabled) {
            setRunningThreads(getRunningThreads()+1);
            new Thread((Runnable) queuedRunnables.remove(0)).start();
            // I wonder if it would improve performance to put a little
            // delay here. This loop seems pretty tight. Then again, I haven't
            // worked with this code in several months, so I might be mistaken.
            // [Jon Aquino 2005-03-2]
        }
    }
    
    private void setRunningThreads(int runningThreads) {
        this.runningThreads = runningThreads;
        if (runningThreads == 0) { fireAllRunningThreadsFinished(); }
    }
    
    public void add(final Runnable runnable) {
        queuedRunnables.add(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    setRunningThreads(getRunningThreads() - 1);
                    processQueue();
                }
            }
        });
        processQueue();
    }
    public int getRunningThreads() {
        return runningThreads;
    }
    
    private ArrayList listeners = new ArrayList();
    public void add(Listener listener) { listeners.add(listener); }
    public void remove(Listener listener) { listeners.remove(listener); }    
    public static interface Listener {
        public void allRunningThreadsFinished();
    }
    private void fireAllRunningThreadsFinished() {
    	//new ArrayList to avoid ConcurrentModificationException [Jon Aquino]
        for (Iterator i = new ArrayList(listeners).iterator(); i.hasNext(); ) {
            Listener listener = (Listener) i.next();
            listener.allRunningThreadsFinished();
        }
    }

    public void dispose() {
        enabled = false;
    }
    
    private volatile boolean enabled = true;

}
