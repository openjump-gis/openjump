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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class PinEqualCoordinatesStyle implements Style {
    public boolean isEnabled() {
        return enabled;
    }
    
    public void initialize(Layer layer) {
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }    
    
    public void paint(Feature f, Graphics2D g, Viewport viewport)
        throws Exception {
        paintGeometry(f.getGeometry(), g, viewport);
    }    

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    private boolean enabled = true;


    /**
     * Parameterless constructor for Java2XML persistence. [Jon Aquino]
     */
    public PinEqualCoordinatesStyle() {
        this(Color.black);
    }

    public PinEqualCoordinatesStyle(Color color) {
        setColor(color);
    }

    private Color color;

    protected void paintGeometry(Geometry geometry, Graphics2D graphics, Viewport viewport)
        throws Exception {
        if (geometry.isEmpty()) {
            return;
        }
        if (!coordinatesEqual(geometry)) {
            return;
        }
        graphics.setColor(color);
        Point2D viewCentre = viewport.toViewPoint(geometry.getCoordinate());
        graphics.drawImage(image, (int)viewCentre.getX()-9, (int)viewCentre.getY()-19, null);
    }
    
    private static Image image = IconLoader.icon("GreenPinPushedIn.gif").getImage();

    public static boolean coordinatesEqual(Geometry geometry) {
        //Coordinates may be expensive to build (e.g. GeometryCollections) , 
        //so build it once. [Jon Aquino]
        Coordinate[] coordinates = geometry.getCoordinates();
        for (int i = 1; i < coordinates.length; i++) {
            if (!coordinates[i].equals(coordinates[0])) {
                return false;
            }
        }
        return true;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

}
