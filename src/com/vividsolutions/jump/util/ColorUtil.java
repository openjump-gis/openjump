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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import org.openjump.core.rasterimage.styler.ColorUtils;

import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;

public class ColorUtil {
    public static final Color GOLD = new Color(255, 192, 0, 150);
    public static final Color PALE_BLUE = new Color(153, 204, 255, 150);
    public static final Color PALE_RED = new Color(255, 204, 204, 150);

    //ColorBrewer range color
    static String[] color2s = { "#08306b", "#00441b", "#7f2704", "#3f007d",
            "#67000d", "#000000" };

    static String[] color1s = { "#f7fbff", "#f7fcf5", "#fff5eb", "#fcfbfd",
            "#fff5f0", "#ffffff" };

    /**
     * Giving an interval, this method creates a random colorschema that fills the entire interval,
     * eg. a feature collection, with a random color set (ColorBrewer).
     * This method extends color range to more that maximum defined in the ColorScheme.txt 
     * (12 intervals) [Giuseppe Aruta 2019-1-12]
     * @param intervals
     * @return com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme class
     * @throws Exception
     */
    public static ColorScheme createRandomColorSchema(int intervals)
            throws Exception {

        final ColorUtils colorUtils = new ColorUtils();
        final ArrayList<Color> arrayColor = new ArrayList<Color>();
        final Color startColor = Color.decode(color1s[new Random()
                .nextInt(color1s.length)]);
        arrayColor.add(startColor);
        final Color endColor = Color.decode(color2s[new Random()
                .nextInt(color2s.length)]);
        for (int c = 1; c < intervals; c++) {

            final double cellRelDistance = (double) c / (double) (intervals);

            final Color color = colorUtils.interpolateColor(startColor,
                    endColor, cellRelDistance);
            arrayColor.add(color);
        }
        arrayColor.add(endColor);

        final ColorScheme colorScheme = new ColorScheme("test", arrayColor);
        return colorScheme;
    }

    /**
     * Giving an interval, and two colors, this method creates a random colorschema that fills 
     * the entire interval, eg. a feature collection, with a set of color between the 
     * two ones 
     * This method extends color range to more that maximum defined in the ColorScheme.txt 
     * (12 intervals) [Giuseppe Aruta 2019-1-12]
     * @param int intervals
     * @param Color startColor
     * @param Color endColor
     * @return com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme class
     * @throws Exception
     */

    public static ColorScheme createColorSchema(int intervals,
            Color startColor, Color endColor) throws Exception {

        final ColorUtils colorUtils = new ColorUtils();
        final ArrayList<Color> arrayColor = new ArrayList<Color>();

        arrayColor.add(startColor);

        for (int c = 1; c < intervals; c++) {

            final double cellRelDistance = (double) c / (double) (intervals);

            final Color color = colorUtils.interpolateColor(startColor,
                    endColor, cellRelDistance);
            arrayColor.add(color);
        }
        arrayColor.add(endColor);

        final ColorScheme colorScheme = new ColorScheme("test", arrayColor);
        return colorScheme;
    }

}
