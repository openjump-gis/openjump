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

import java.util.ArrayList;
import java.util.Collection;


/**
 * This will eventually implement an interface called MeasurementSystem.
 */
public class MetricSystem {
    private double modelUnitsPerMetre;

    public MetricSystem(double modelUnitsPerMetre) {
        this.modelUnitsPerMetre = modelUnitsPerMetre;
    }

    public Collection createUnits() {
        ArrayList units = new ArrayList();

        //Leave out the lesser known units, like fm. [Jon Aquino]
        units.add(new Unit("km", 1E3 * modelUnitsPerMetre));
        units.add(new Unit("m", 1E0 * modelUnitsPerMetre));
        units.add(new Unit("mm", 1E-3 * modelUnitsPerMetre));
        units.add(new Unit("um", 1E-6 * modelUnitsPerMetre));
        units.add(new Unit("nm", 1E-9 * modelUnitsPerMetre));
        units.add(new Unit("pm", 1E-12 * modelUnitsPerMetre));

        return units;
    }
}
