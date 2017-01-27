
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

package com.vividsolutions.jump.workbench.driver;

import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;


//<<TODO:DESIGN>> Convert to interface. There's little reason for this code to remain
//an abstract class. [Jon Aquino]

/**
 * Adds/saves a Layer from/to a data source, such as a file. Subclasses should
 * call #setReader and #setDriverProperties on the layers they create
 * @deprecated Use DataSourceQueryChooser instead
 */
public abstract class AbstractDriver {
    protected DriverManager driverManager;
    protected ErrorHandler errorHandler;

    public AbstractDriver() {
    }

    public void initialize(DriverManager driverManager,
        ErrorHandler errorHandler) {
        this.driverManager = driverManager;
        this.errorHandler = errorHandler;
    }

    /**
     * Brief description of the data source to which this driver connects.
     * Displayed in the DriverDialog "Format" combobox.
     */
    public abstract String toString();

    public abstract AbstractDriverPanel getPanel();

    //<<TODO:INVESTIGATE>> Drivers are not consistent in their use of
    //DriverManager#getShared[Open/Save]BasicFileDriverPanel(). Some use it;
    //some don't. [Jon Aquino]
}
