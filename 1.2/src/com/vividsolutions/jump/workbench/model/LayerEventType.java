
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

/**
 * Whether a Layer was added, removed, or modified.
 */
public final class LayerEventType {
    public final static LayerEventType ADDED = new LayerEventType("ADDED");
    public final static LayerEventType REMOVED = new LayerEventType("REMOVED");

    /** Metadata includes the name of the layer. */
    public final static LayerEventType METADATA_CHANGED = new LayerEventType(
            "METADATA_CHANGED");

    /** The data changed or a style changed. */
    public final static LayerEventType APPEARANCE_CHANGED = new LayerEventType(
            "APPEARANCE_CHANGED");
    public final static LayerEventType VISIBILITY_CHANGED = new LayerEventType(
            "VISIBILITY_CHANGED");
    private String name;

    private LayerEventType(String name) {
        this.name = name;
    }
    public String toString() {
        return name;
    }
}
