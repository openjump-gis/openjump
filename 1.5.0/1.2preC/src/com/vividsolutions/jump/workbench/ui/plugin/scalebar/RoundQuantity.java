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

package com.vividsolutions.jump.workbench.ui.plugin.scalebar;

import java.text.DecimalFormat;


/**
 * Numbers with one or two significant digits, like 1 x 10^3, or 5 x 10^2.
 */
public class RoundQuantity {
    private Unit unit;
    private int mantissa;
    private int exponent;

    public RoundQuantity(int mantissa, int exponent, Unit unit) {
        this.mantissa = mantissa;
        this.exponent = exponent;
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    public int getMantissa() {
        return mantissa;
    }
    
    public String toString() {
        return getAmountString() + " " + getUnit();
    }
    
    public String getAmountString() {
        if (getMantissa() == 0) {
            return "0";
        }

        if ((0 <= getExponent()) && (getExponent() <= 3)) {
            return new DecimalFormat("#").format(getAmount());
        }

        if ((-4 <= getExponent()) && (getExponent() < 0)) {
            return new DecimalFormat("#.####").format(getAmount());
        }

        return getMantissa() + "E" + getExponent();
    }    

    public int getExponent() {
        return exponent;
    }

    public double getAmount() {
        return mantissa * Math.pow(10, exponent);
    }

    public double getModelValue() {
        return getAmount() * unit.getModelValue();
    }
}
