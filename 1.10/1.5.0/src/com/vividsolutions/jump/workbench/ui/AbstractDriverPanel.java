
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

package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import com.vividsolutions.jts.util.Assert;


/**
 * UI for AbstractDrivers. Must supply OK and Cancel buttons (for example, using
 * OKCancelPanel).
 */

//JBuilder displays this component as a "Red Bean". There's a trick to
//displaying it -- see test.AbstractDriverPanelProxy and
//http://www.visi.com/~gyles19/fom-serve/cache/97.html. [Jon Aquino]
public abstract class AbstractDriverPanel extends JPanel {
    BorderLayout borderLayout = new BorderLayout();


    public AbstractDriverPanel() {
        //Don't let JBuilder create a jbInit method and call it here.
        //It will cause a tricky bug:
        //  1. The subclass' jbInit will be called
        //  2. A NullPointerException will occur because the subclass' jbInit
        //     will perform operations on instance variables that have not yet been
        //     initialized.
        //[Jon Aquino]
    }

    /**
     * Adds an ActionListener that should be notified when the user presses
     * this panel's OK or Cancel button.
     */
    public abstract void addActionListener(ActionListener l);

    public abstract void removeActionListener(ActionListener l);

    public abstract boolean wasOKPressed();

    public boolean isInputValid() {
        return null == getValidationError();
    }

    /**
     * Attempts to restore as many panel values as possible to a previous state,
     * to save the user some typing. Subclasses overriding this method should
     * call the superclass method first.
     * @param cache not null
     */
    public void setCache(DriverPanelCache cache) {
        Assert.isTrue(cache != null);
    }

    /**
     * Returns the current panel values. Subclasses overriding this method should
     * call the superclass method first. DriverDialog takes care of merging
     * the returned cache with the original cache.
     */
    public DriverPanelCache getCache() {
        return new DriverPanelCache();
    }

    /**
     * @return null if there are no validation errors; otherwise, an error message
     */
    public String getValidationError() {
        return null;
    }
}
