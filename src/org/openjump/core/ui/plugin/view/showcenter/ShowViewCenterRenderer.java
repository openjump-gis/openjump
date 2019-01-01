/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
/*****************************************************
 * created:  original version by Vivid Solution
 * last modified:  03.06.2005
 * 01.10.2005 [scale now obtained from other class]
 * 
 * Calculates the actual scale and draws the text
 * and a white rectangle around
 *
 * @author sstein 
 *****************************************************/

package org.openjump.core.ui.plugin.view.showcenter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.SimpleRenderer;

/**
 * Calculates the actual scale and draws the text and a white rectangle around
 *
 * @author sstein
 **/
public class ShowViewCenterRenderer extends SimpleRenderer {

    public final static String CONTENT_ID = "SHOW_CROSSHAIR"; // "SHOW_CENTER";
    ImageIcon icon_3 = GUIUtil.toSmallIcon(new ImageIcon(getClass()
            .getResource("icon3.gif")), 30);
    ImageIcon icon_2 = GUIUtil.toSmallIcon(new ImageIcon(getClass()
            .getResource("icon2.gif")), 30);
    ImageIcon icon = GUIUtil.toSmallIcon(
            new ImageIcon(getClass().getResource("icon.gif")), 30);

    private final static String ENABLED_KEY = ShowViewCenterRenderer.class
            + " - ENABLED";
    private Stroke stroke = new BasicStroke();

    public ShowViewCenterRenderer(LayerViewPanel panel) {
        super(CONTENT_ID, panel);
    }

    @Override
    protected void paint(Graphics2D g) throws Exception {
        if (!isEnabled(panel)) {
            return;
        }
        Viewport view = panel.getViewport();
        // Override dashes set in GridRenderer [Jon Aquino]
        g.setStroke(stroke);

        paintScaleLabel(g, view);
    }

    private void paintScaleLabel(Graphics2D graphics, Viewport viewport)
            throws Exception {
        Integer dimension = (Integer) ShowViewCenterPlugIn.dimensionSpinner
                .getValue();

        Image image = null;
        ImageIcon imageIcon = null;
        if (ShowViewCenterPlugIn.radio_button.isSelected()) {
            imageIcon = GUIUtil.toSmallIcon(new ImageIcon(getClass()
                    .getResource("icon.gif")), dimension);

        } else if (ShowViewCenterPlugIn.radio_button_2.isSelected()) {
            imageIcon = GUIUtil.toSmallIcon(new ImageIcon(getClass()
                    .getResource("icon2.gif")), dimension);

        } else if (ShowViewCenterPlugIn.radio_button_3.isSelected()) {
            imageIcon = GUIUtil.toSmallIcon(new ImageIcon(getClass()
                    .getResource("icon3.gif")), dimension);

        }
        image = imageIcon.getImage();
        graphics.setColor(Color.RED);
        Point2D viewCentre = viewport.toViewPoint(EnvelopeUtil.centre(viewport
                .getEnvelopeInModelCoordinates()));
        graphics.drawImage(image,
                (int) viewCentre.getX() - imageIcon.getIconWidth() / 2,
                (int) viewCentre.getY() - imageIcon.getIconHeight() / 2, null);
    }

    /*********** getters and setters ******************/

    /**
     * 
     * @param panel
     * @return true if the scale is enabled in the LayerViewPanel
     */
    public static boolean isEnabled(LayerViewPanel panel) {
        return panel.getBlackboard().get(ENABLED_KEY, false);
    }

    public static void setEnabled(boolean enabled, LayerViewPanel panel) {
        panel.getBlackboard().put(ENABLED_KEY, enabled);
    }

    /**
     * @param myPlugInContext
     *            The myPlugInContext to set.
     */
    public void setMyPlugInContext(PlugInContext myPlugInContext) {
    }

    public Image makeImage(Shape s) {
        Rectangle r = s.getBounds();
        Image image = new BufferedImage(r.width, r.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = ((BufferedImage) image).createGraphics();
        // move the shape in the region of the image
        gr.translate(-r.x, -r.y);
        gr.draw(s);
        gr.dispose();
        return image;
    }

}
