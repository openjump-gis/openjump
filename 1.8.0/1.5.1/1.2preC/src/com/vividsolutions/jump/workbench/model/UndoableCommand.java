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

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

/**
 * An action that can be rolled back. Similar to an UndoableEdit, but assumes
 * that the action is yet to be executed, whereas an UndoableEdit assumes that
 * the action has already been executed (i.e. it has a #redo method but not a 
 * #do method).
 * 
 * @see javax.swing.undo.UndoableEdit
 */
public abstract class UndoableCommand {
    private String name;

    public UndoableCommand(String name) {
        this.name = name;
    }
    
    /** Releases resources. */
    protected void dispose() {}

    /**
     * If there is an exception that leaves this UndoableCommand execution 
     * partially complete and non-unexecutable, be sure to call #reportIrreversibleChange()
     * on the UndoableEditReceiver (which can be obtained from the LayerManager).
     * @see UndoableEditReceiver#reportIrreversibleChange()
     */
    public abstract void execute();
    
    public abstract void unexecute();

    public UndoableEdit toUndoableEdit() {
        return new AbstractUndoableEdit() {
            public String getPresentationName() {
                return name;
            }

            public void redo() {
                execute();
                super.redo();
            }

            public void die() {
                dispose();
                super.die();
            }

            public void undo() {
                super.undo();
                unexecute();
            }
        };
    }
    public String getName() {
        return name;
    }

    public static final UndoableCommand DUMMY = new UndoableCommand("Dummy") {
        public void execute() {}
        public void unexecute() {}
    };
    
}
