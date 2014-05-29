
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

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Envelope;


public class ZoomHistory {
    private LayerViewPanel layerViewPanel;
    private ArrayList envelopes = new ArrayList();
    private int currentIndex = -1;
    private boolean adding = true;

    public ZoomHistory(LayerViewPanel layerViewPanel) {
        this.layerViewPanel = layerViewPanel;
    }

    public void setAdding(boolean adding) {
        this.adding = adding;
    }

    public void add(Envelope envelope) {
        if (!adding) {
            return;
        }

        envelopes.subList(currentIndex + 1, envelopes.size()).clear();
        envelopes.add(envelope);
        currentIndex = envelopes.size() - 1;
    }

    public Envelope next() {
        currentIndex++;

        return getCurrentEnvelope();
    }

    public Envelope prev() {
        currentIndex--;

        return getCurrentEnvelope();
    }

    private Envelope getCurrentEnvelope() {
        return (Envelope) envelopes.get(currentIndex);
    }

    public boolean hasPrev() {
        return currentIndex > 0;
    }

    public boolean hasNext() {
        return currentIndex < (envelopes.size() - 1);
    }
}
