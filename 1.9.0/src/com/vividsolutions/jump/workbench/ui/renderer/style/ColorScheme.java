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
package com.vividsolutions.jump.workbench.ui.renderer.style;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.CollectionMap;
import com.vividsolutions.jump.util.FileUtil;

/**
 * The colour schemes were taken from the following sources:
 * <ul>
 * <li>
 * Visual Mining, Inc. "Charts--Color Palettes" 2003.
 * Available from http://chartworks.com/resources/palettes.html.
 * Internet; accessed 24 April 2003.
 * <li>
 * Brewer, Cindy and Harrower, Mark. "ColorBrewer".
 * Available from http://www.personal.psu.edu/faculty/c/a/cab38/ColorBrewerBeta2.html.
 * Internet; accessed 24 April 2003.
 * </ul>
 */
public class ColorScheme {

    public static ColorScheme create(String name) {
        Assert.isTrue(nameToColorsMap().keySet().contains(name));
        return new ColorScheme(name, nameToColorsMap().getItems(name));
    }

    private static ArrayList<String> rangeColorSchemeNames;

    private static ArrayList<String> discreteColorSchemeNames;

    private static void load() {
        try {
            rangeColorSchemeNames = new ArrayList<String>();
            discreteColorSchemeNames = new ArrayList<String>();
            nameToColorsMap = new CollectionMap();
            InputStream inputStream =
                ColorScheme.class.getResourceAsStream("ColorScheme.txt");
            try {
                for (Object line : FileUtil.getContents(inputStream)) {
                    add((String)line);
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            Assert.shouldNeverReachHere(e.toString());
        }
    }

    private static void add(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        String name = tokenizer.nextToken();
        boolean range = tokenizer.nextToken().equals("range");
        (range ? rangeColorSchemeNames : discreteColorSchemeNames).add(name);
        while (tokenizer.hasMoreTokens()) {
            String hex = tokenizer.nextToken();
            Assert.isTrue(hex.length() == 6, hex);
            nameToColorsMap().addItem(name, Color.decode("#" + hex));
        }
    }

    private static CollectionMap nameToColorsMap() {
        if (nameToColorsMap == null) {
            load();
        }
        return nameToColorsMap;
    }

    private static CollectionMap nameToColorsMap;

    public static Collection rangeColorSchemeNames() {
        if (rangeColorSchemeNames == null) {
            load();
        }
        return Collections.unmodifiableList(rangeColorSchemeNames);
    }

    public static Collection discreteColorSchemeNames() {
        if (discreteColorSchemeNames == null) {
            load();
        }
        return Collections.unmodifiableList(discreteColorSchemeNames);
    }    

    final private String name;

    public ColorScheme(String name, Collection colors) {
        this.name = name;
        this.colors = new ArrayList<Color>(colors);
    }

    private int lastColorReturned = -1;

    public int getLastColorReturned() {
        return lastColorReturned;
    }

    public void setLastColorReturned(int lastColorReturned) {
        this.lastColorReturned = lastColorReturned;
    }

    final private List<Color> colors;

    public Color next() {
        lastColorReturned++;
        if (lastColorReturned >= colors.size()) {
            lastColorReturned = 0;
        }
        return colors.get(lastColorReturned);
    }

    public List<Color> getColors() {
        return Collections.unmodifiableList(colors);
    }

    public String getName() {
        return name;
    }

}
