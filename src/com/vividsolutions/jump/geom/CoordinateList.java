
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

package com.vividsolutions.jump.geom;

import java.util.*;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * A CoordinateList is a list of Coordinates.
 * It prevents duplicate consecutive coordinates from appearing in the list.
 */
public class CoordinateList extends ArrayList {
    private final static Coordinate[] coordArrayType = new Coordinate[0];

    //List coordList = new ArrayList();
    public CoordinateList() {
    }

    public Coordinate getCoordinate(int i) {
        return (Coordinate) get(i);
    }

    public boolean add(Object obj) {
        add((Coordinate) obj);

        return true;
    }

    public void add(Coordinate coord) {
        // don't add duplicate coordinates
        if (size() >= 1) {
            Coordinate last = (Coordinate) get(size() - 1);

            if (last.equals(coord)) {
                return;
            }
        }

        super.add(coord);
    }

    public boolean addAll(Collection coll) {
        boolean isChanged = false;

        for (Iterator i = coll.iterator(); i.hasNext();) {
            add((Coordinate) i.next());
            isChanged = true;
        }

        return isChanged;
    }

    /**
     * Ensure this coordList is a ring, by adding the start point if necessary
     */
    public void closeRing() {
        if (size() > 0) {
            add(get(0));
        }
    }

    public Coordinate[] toCoordinateArray() {
        return (Coordinate[]) toArray(coordArrayType);
    }
}
