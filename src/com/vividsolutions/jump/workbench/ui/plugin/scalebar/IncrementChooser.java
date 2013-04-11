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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.MathUtil;


/**
 * Chooses a good size for the scale-bar increments.
 */
public class IncrementChooser {
    public IncrementChooser() {
    }

    /**
     * @return a Quantity whose value will be a multiple of 10
     */
    public RoundQuantity chooseGoodIncrement(Collection units,
        double idealIncrement) {
        return goodIncrement(goodUnit(units, idealIncrement), idealIncrement);
    }

    /**
     * @return the Unit that is the fewest orders of magnitude away from the ideal
     * increment, preferably smaller than the ideal increment.
     */
    private Unit goodUnit(Collection units, double idealIncrement) {
        Unit goodUnit = (Unit) Collections.min(units);

        for (Iterator i = units.iterator(); i.hasNext();) {
            Unit candidateUnit = (Unit) i.next();

            if (candidateUnit.getModelValue() > idealIncrement) {
                continue;
            }

            if (distance(candidateUnit.getModelValue(), idealIncrement) < distance(
                        goodUnit.getModelValue(), idealIncrement)) {
                goodUnit = candidateUnit;
            }
        }

        return goodUnit;
    }

    private double distance(double a, double b) {
        return Math.abs(MathUtil.orderOfMagnitude(a) -
            MathUtil.orderOfMagnitude(b));
    }

    /**
     * @return an amount of the form 1 x 10^n, 2 x 10^n or 5 x 10^n that is closest to the
     * ideal increment without exceeding it.
     */
    private RoundQuantity goodIncrement(Unit unit, double idealIncrement) {
        RoundQuantity mantissa1Candidate = new RoundQuantity(1,
                (int) Math.floor(MathUtil.orderOfMagnitude(idealIncrement) -
                    MathUtil.orderOfMagnitude(unit.getModelValue())), unit);
        // MD - hack to get around Nan exception
        if (Double.isNaN(idealIncrement))
          idealIncrement = mantissa1Candidate.getModelValue();
        Assert.isTrue(mantissa1Candidate.getModelValue() <= idealIncrement, "unit=" + unit.getModelValue() + ", ideal increment=" + idealIncrement);

        RoundQuantity mantissa2Candidate = new RoundQuantity(2,
                mantissa1Candidate.getExponent(), unit);
        RoundQuantity mantissa5Candidate = new RoundQuantity(5,
                mantissa1Candidate.getExponent(), unit);

        if (mantissa5Candidate.getModelValue() <= idealIncrement) {
            return mantissa5Candidate;
        }

        if (mantissa2Candidate.getModelValue() <= idealIncrement) {
            return mantissa2Candidate;
        }

        return mantissa1Candidate;
    }

}
