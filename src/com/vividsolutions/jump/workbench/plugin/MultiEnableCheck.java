
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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComponent;


//<<TODO:DOC>> Create a package comment saying that the classes in this package
//are the framework for context-sensitive enabling of menus. Give an example
//of the use of this framework. [Jon Aquino]
/**
 * A sequence of EnableChecks treated as one.
 */
public class MultiEnableCheck implements EnableCheck {
    private ArrayList enableChecks = new ArrayList();

    /**
     * Create a new MultiEnableCheck
     */
    public MultiEnableCheck() {
    }

    public String check(JComponent component) {
        for (Iterator i = enableChecks.iterator(); i.hasNext();) {
            EnableCheck enableCheck = (EnableCheck) i.next();
            String errorMessage = enableCheck.check(component);

            if (errorMessage != null) {
                return errorMessage;
            }
        }

        return null;
    }

    /**
     *@return    this, to allow "method chaining"
     */
    public MultiEnableCheck add(EnableCheck enableCheck) {
        enableChecks.add(enableCheck);

        return this;
    }
}
