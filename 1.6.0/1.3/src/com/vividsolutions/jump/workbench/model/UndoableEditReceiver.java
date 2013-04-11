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

package com.vividsolutions.jump.workbench.model;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.vividsolutions.jts.util.Assert;


/**
 * Receives UndoableEdits from PlugIns and CursorTools. Also provides access to
 * a Task's UndoManager.
 * <P>
 * In the documentation, the "receiving phase" refers to the time between the
 * calls to #start and #stop.
 * <P>
 * If there is an exception that leaves this UndoableCommand execution
 * partially complete and non-unexecutable, be sure to call
 * #reportIrreversibleChange()
 */
public class UndoableEditReceiver {
    private UndoManager undoManager = new UndoManager();
    private ArrayList newUndoableEdits = new ArrayList();
    /** Handle nested calls to UndoableEditReceiver */
    private int transactions = 0;
    private boolean nothingToUndoReported = false;
    private boolean irreversibleChangeReported = false;
    private boolean undoManagerCouldUndoAtStart = false;
    private ArrayList listeners = new ArrayList();
    
    public UndoableEditReceiver() {
    }

    public void startReceiving() {
        transactions++;
        setNothingToUndoReported(false);
        irreversibleChangeReported = false;
        undoManagerCouldUndoAtStart = undoManager.canUndo();
    }

    /**
     * Specifies that the undo history should not be modified at the end of
     * the current receiving phase, if neither #receive nor #reportIrreversibleChange
     * is called. If none of the three methods are called during the receiving
     * phase, an irreversible change is assumed to have occurred, and the
     * undo history will be truncated.
     */
    public void reportNothingToUndoYet() {
        Assert.isTrue(isReceiving());
        setNothingToUndoReported(true);
    }

    /**
     * Notifies this UndoableEditReceiver that something non-undoable has
     * happened. Be sure to call this if an Exception occurs during the execution
     * of an UndoableCommand, leaving it partially complete and non-unexecutable.
     */    
    public void reportIrreversibleChange() {
        Assert.isTrue(isReceiving());
        irreversibleChangeReported = true;
    }

    public void stopReceiving() {
        transactions--;
        try {
            if ((newUndoableEdits.isEmpty() && !wasNothingToUndoReported()) ||
                    irreversibleChangeReported) {
                undoManager.discardAllEdits();

                return;
            }

            for (Iterator i = newUndoableEdits.iterator(); i.hasNext();) {
                UndoableEdit undoableEdit = (UndoableEdit) i.next();
                undoManager.addEdit(undoableEdit);
            }
            newUndoableEdits.clear();            
        } finally {
            fireUndoHistoryChanged();

            if (undoManagerCouldUndoAtStart && !undoManager.canUndo()) {
                fireUndoHistoryTruncated();
            }
        }
    }

    private void fireUndoHistoryTruncated() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener listener = (Listener) i.next();
            listener.undoHistoryTruncated();
        }
    }

    private void fireUndoHistoryChanged() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener listener = (Listener) i.next();
            listener.undoHistoryChanged();
        }
    }

    public void add(Listener listener) {
        listeners.add(listener);
    }

    /**
     * If the currently executing PlugIn or AbstractCursorTool is not undoable,
     * it should simply not call this method; the undo history will be cleared.
     */
    public void receive(UndoableEdit undoableEdit) {
        Assert.isTrue(isReceiving());

        //Don't add the UndoableEdit to the UndoManager right away; the caller may
        //call #clearNewUndoableEdits. [Jon Aquino]
        newUndoableEdits.add(undoableEdit);
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    private void setNothingToUndoReported(boolean nothingToUndoReported) {
        this.nothingToUndoReported = nothingToUndoReported;
    }

    private boolean wasNothingToUndoReported() {
        return nothingToUndoReported;
    }

    public static interface Listener {
        public void undoHistoryChanged();

        public void undoHistoryTruncated();
    }
    public boolean isReceiving() {
        return transactions > 0;
    }

}
