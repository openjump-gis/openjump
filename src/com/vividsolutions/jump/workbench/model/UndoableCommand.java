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
    private Layer layer;
    private boolean canceled = false;

    /**
     * UndoableCommand with a name to be shown in the user interface.
     */
    public UndoableCommand(String name) {
        this.name = name;
    }
    
    /**
     * UndoableCommand with a name and a layer parameter.
     * The layer parameter is used to neutralize the undoableEdit if the layer
     * is removed from the LayerManager (neutralization uses significant
     * attribute).
     */
    public UndoableCommand(String name, Layer layer) {
        this(name);
        setLayer(layer);
    }
    
    public void setLayer(Layer layer) {
        this.layer = layer;
    }
    
    public Layer getLayer() {
        return layer;
    }
    
    /** 
     * Releases resources and make the edit action unsignificant, so that
     * it will be ignored in the undo chain.
     * If a UndoableCommand subclasse has resources to be released,
     * oveload this method and call super.dispose() set canceled to true.
     */
    protected void dispose() {
        layer = null;
        canceled = true;
    }
    
    public boolean isCanceled() {
        return canceled;
    } 

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
                if (isCanceled()) return;
                execute();
                super.redo();
            }

            public void die() {
                dispose();
                super.die();
            }

            public void undo() {
                if (isCanceled()) return;
                super.undo();
                unexecute();
            }
            
            public boolean isSignificant() {
                return !isCanceled();
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
