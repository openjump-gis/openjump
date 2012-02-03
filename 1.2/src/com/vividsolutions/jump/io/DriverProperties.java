
/*
 * DataProperties.java
 *
 * Created on June 3, 2002, 1:23 PM
 */
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

package com.vividsolutions.jump.io;

import java.util.Properties;


/**
 * Object to store a bunch of key/value pairs used by the input/output drivers/classes. <br>
 *
 * dp = new DriverProperties()   <br>
 * dp.set('DefaultValue','c:\me.shp') <br>
 * <br>
 * is the same as:<br>
 * <br>
 * dp = new DriverProperties('c:\me.shp')<br>
 *<br>
 * NOTE: dp.get('DefaultValue') is available via the parent class <br>
 *       Typically one uses 'DefaultValue' or 'InputFile' or 'OutputFile'
 */
public class DriverProperties extends Properties {
    /** Creates new DataProperties */
    public DriverProperties() {
    }

    /**
     *constructor that will autoset the key 'DefaultValue'
     *
     *@param defaultValue value portion for the the key 'DefaultValue'
     **/
    public DriverProperties(String defaultValue) {
        this.set("DefaultValue", defaultValue);
    }

    /**
     * Sets a key/value pair in the object. <br>
     *  It returns the object so you can cascade sets: <br>
     *  dp.set ('a','value1') <br>
     *     .set('b','value2') <br>
     *  ...
     *@param key key name
     *@param value key's value
     */
    public DriverProperties set(String key, String value) {
        setProperty(key, value);

        return this;
    }
}
