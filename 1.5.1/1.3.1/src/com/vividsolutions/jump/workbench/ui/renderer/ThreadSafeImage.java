
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

package com.vividsolutions.jump.workbench.ui.renderer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;

import com.vividsolutions.jump.workbench.ui.LayerViewPanel;


/**
 * Reading and writing can be done on separate threads.
 */
public class ThreadSafeImage implements Cloneable {
    private Image image = null;
    private Graphics2D graphics = null;
    private LayerViewPanel panel;
    private GraphicsState dummyGraphicsState = new GraphicsState() {
            public void restore(Graphics2D g) {
            }
        };

    public ThreadSafeImage(LayerViewPanel panel) {
        this.panel = panel;
    }

    private Image getImage() {
        if (image == null) {
            image = panel.createBlankPanelImage();
        }

        return image;
    }

    private Graphics2D getGraphics() {
        if (graphics == null) {
            graphics = (Graphics2D) getImage().getGraphics();
            //Not sure if we need antialiasing here. Oh well, doesn't hurt. [Jon Aquino 11/21/2003]
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);            
        }

        return graphics;
    }

    public synchronized void copyTo(Graphics2D destination,
        ImageObserver imageObserver) {
        if (getImage() == null) {
            //Nothing we can do. [Jon Aquino]
            return;
        }

        destination.drawImage(getImage(), 0, 0, imageObserver);
    }

    /**
     * @return false if we cannot generate an off-screen panel from the panel
     * because of various conditions during initialization
     */
    private boolean isPanelReady() {
        if (panel.getSize().equals(new Dimension(0, 0))) {
            return false;
        }

        if (getImage() == null) {
            return false;
        }

        return true;
    }

    public synchronized void draw(Drawer drawer) throws Exception {
        if (!isPanelReady()) {
            return;
        }
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);                    
        drawer.draw(g);
    }

    public synchronized GraphicsState getGraphicsState() {
        if (!isPanelReady()) {
            return dummyGraphicsState;
        }

        return new GraphicsState(getGraphics());
    }

    public synchronized void setGraphicsState(GraphicsState gs) {
        if (!isPanelReady()) {
            return;
        }

        gs.restore(getGraphics());
    }

    /**
     * If the panel is not ready, returns null.
     */
    public Object clone() {
        ThreadSafeImage clone = new ThreadSafeImage(panel);

        if (!clone.isPanelReady()) {
            return null;
        }

        copyTo(clone.getGraphics(), null);

        return clone;
    }

    public interface Drawer {
        public void draw(Graphics2D g) throws Exception;
    }
}
