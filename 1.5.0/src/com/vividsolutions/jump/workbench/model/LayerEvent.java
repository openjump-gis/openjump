
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

import com.vividsolutions.jts.util.Assert;

/**
 * An addition, removal, or modification of a Layer.
 * @see Layer
 * @see LayerEventType
 */
public class LayerEvent {
    private Layerable layerable;
    private LayerEventType type;
    private Category category;
    private int layerableIndex;

    public LayerEvent(Layerable layerable, LayerEventType type,
        Category category, int layerIndex) {
        Assert.isTrue(category != null);
        Assert.isTrue(layerable != null);
        Assert.isTrue(type != null);
        this.layerable = layerable;
        this.type = type;
        this.category = category;
        this.layerableIndex = layerIndex;
    }

    public LayerEventType getType() {
        return type;
    }

    public Layerable getLayerable() {
        return layerable;
    }

    public Category getCategory() {
        return category;
    }

    public int getLayerableIndex() {
        return layerableIndex;
    }
}
