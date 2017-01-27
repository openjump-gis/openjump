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
package com.vividsolutions.jump.util;

import java.util.Comparator;
import java.util.TreeMap;

import com.vividsolutions.jts.util.Assert;

public class Range {
    private boolean includingMin;
    private boolean includingMax;
    private Object max;
    private Object min;
    
    public String toString() {
        return min + " - " + max;
    }
    
    public Range() {
        this(new NegativeInfinity(), false, new PositiveInfinity(), false);
    }
    
    //NegativeInfinity and PositiveInfinity are classes so they can be
    //serialized via Java2XML. [Jon Aquino]
    public static final class NegativeInfinity {
        public String toString() {
            // Empty string, for range display in TreeLayerNamePanel. [Jon Aquino 2005-07-25]
            return "";
        }
    }
    
    public static final class PositiveInfinity {
        public String toString() {
            return "";
        }
    }    

    public Range(
        Object min,
        boolean includingMin,
        Object max,
        boolean includingMax) {
        Assert.isTrue(!(min.equals(max) && (!includingMin || !includingMax)));            
        this.min = min;
        this.max = max;
        this.includingMin = includingMin;
        this.includingMax = includingMax;
    }
    public boolean equals(Object obj) {
        return Range.RANGE_COMPARATOR.compare(this, obj) == 0;
    }
    public boolean isIncludingMax() {
        return includingMax;
    }

    public boolean isIncludingMin() {
        return includingMin;
    }

    public Object getMax() {
        return max;
    }

    public Object getMin() {
        return min;
    }

    public void setIncludingMax(boolean b) {
        includingMax = b;
    }

    public void setIncludingMin(boolean b) {
        includingMin = b;
    }

    public void setMax(Object object) {
        max = object;
    }

    public void setMin(Object object) {
        min = object;
    }
    
    
    private static final Comparator INFINITY_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof PositiveInfinity || o2 instanceof NegativeInfinity) {
                return +1;
            }
            if (o1 instanceof NegativeInfinity|| o2 instanceof PositiveInfinity) {
                return -1;
            }
            return ((Comparable) o1).compareTo(o2);
        }
    };

    public static final Comparator RANGE_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            Range range1 =
                o1 instanceof Range
                    ? (Range) o1
                    : new Range(o1, true, o1, true);
            Range range2 =
                o2 instanceof Range
                    ? (Range) o2
                    : new Range(o2, true, o2, true);
            int max1ComparedToMin2 =
                INFINITY_COMPARATOR.compare(range1.getMax(), range2.getMin());
            if (max1ComparedToMin2 < 0
                || (max1ComparedToMin2 == 0
                    && (!range1.isIncludingMax() || !range2.isIncludingMin()))) {
                return -1;
            }
            int min1ComparedToMax2 =
                INFINITY_COMPARATOR.compare(range1.getMin(), range2.getMax());
            if (min1ComparedToMax2 > 0
                || (min1ComparedToMax2 == 0
                    && (!range1.isIncludingMin() || !range2.isIncludingMax()))) {
                return +1;
            }
            return 0;
        }
    };    

    //Trivial, but necessary for Java2XML serialization. [Jon Aquino]
    public static class RangeTreeMap extends TreeMap {
        public RangeTreeMap() {
            super(RANGE_COMPARATOR);
        }
    }

}
