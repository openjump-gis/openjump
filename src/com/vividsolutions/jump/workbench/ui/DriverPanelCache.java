
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

import java.util.Hashtable;
import java.util.Iterator;


/**
 * A cache of the state of a DriverPanel. Used to restore a DriverPanel to
 * a past state, to minimize re-typing for the user. Different DriverPanels
 * can even use each other's cached values, whenever the cache keys match.
 * For each DriverDialog, one DriverPanelCache is associated with one Layer.
 */

//<<TODO:DEFECT>> Not sure if this is still working. Didn't it also solve the
//following problem: when the user chooses a different file format from the combobox,
//the filename he entered gets blanked out? The problem has come back. [Jon Aquino]
public class DriverPanelCache {
    /**
     * This value is set by the DriverDialog. It always exists.
     */
    public static final String DRIVER_CACHE_KEY = "DRIVER";

    /**
     * Most AbstractDriverPanels will set this value, but some do not (e.g. those
     * that retrieve data from the web instead of a file). Thus, it almost always
     * exists.
     */
    public static final String FILE_CACHE_KEY = "FILE";
    private Hashtable map = new Hashtable();

    public DriverPanelCache() {
    }

    /**
     * @return the specied value, or null if no such key exists
     */
    public Object get(String cacheKey) {
        return map.get(cacheKey);
    }

    public void put(String cacheKey, Object cachedValue) {
        map.put(cacheKey, cachedValue);
    }

    public void addAll(DriverPanelCache otherCache) {
        for (Iterator i = otherCache.map.keySet().iterator(); i.hasNext();) {
            String otherCacheKey = (String) i.next();
            map.put(otherCacheKey, otherCache.get(otherCacheKey));
        }
    }
}
