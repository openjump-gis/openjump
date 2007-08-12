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

import java.awt.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.SimpleRenderer;

public class ScaleBarRenderer extends SimpleRenderer {
    public final static String CONTENT_ID = "SCALE_BAR";
    /**
     *  Height of the increment boxes, in view-space units.
     */
    private final static int BAR_HEIGHT = 10;
    private final static Color FILL2 = new Color(255, 204, 204);
    private final static Color FILL1 = Color.white;

    /**
     *  Distance from the right edge, in view-space units.
     */
    private final static int HORIZONTAL_MARGIN = 3;

    /**
     *  In view-space units; the actual increment may be a bit larger or smaller
     *  than this amount.
     */
    private final static int IDEAL_INCREMENT = 75;
    private final static Color LINE_COLOR = Color.black;
    private final static int TEXT_BOTTOM_MARGIN = 1;
    private final static int UNIT_TEXT_BOTTOM_MARGIN = 1;
    private final static Color TEXT_COLOR = Color.black;
    private final static Color UNIT_TEXT_COLOR = Color.blue;

    /**
     *  Distance from the bottom edge, in view-space units.
     */
    private final static int VERTICAL_MARGIN = 3;
    private final static String ENABLED_KEY = ScaleBarRenderer.class +" - ENABLED";
    private final static int INCREMENT_COUNT = 5;
    private Font FONT = new Font("Dialog", Font.PLAIN, 10);
    private Font UNIT_FONT = new Font("Dialog", Font.BOLD, 11);

    public ScaleBarRenderer(LayerViewPanel panel) {
        super(CONTENT_ID, panel);
    }

    public static boolean isEnabled(LayerViewPanel panel) {
        return panel.getBlackboard().get(ENABLED_KEY, false);
    }

    public static void setEnabled(boolean enabled, LayerViewPanel panel) {
        panel.getBlackboard().put(ENABLED_KEY, enabled);
    }
    
    private Stroke stroke = new BasicStroke();
    
    protected void paint(Graphics2D g) {
        paint(g, panel.getViewport().getScale());
    }

    public void paint(Graphics2D g, double scale) {
        if (!isEnabled(panel)) {
            return;
        }
        //Override dashes set in GridRenderer [Jon Aquino]
        g.setStroke(stroke);

        RoundQuantity increment =
            new IncrementChooser().chooseGoodIncrement(
                new MetricSystem(1).createUnits(),
                IDEAL_INCREMENT / scale);
        paintIncrements(increment, INCREMENT_COUNT, g, scale);
    }

    private int barBottom() {
        return panel.getHeight() - VERTICAL_MARGIN;
    }

    private int barTop() {
        return barBottom() - BAR_HEIGHT;
    }

    private TextLayout createTextLayout(String text, Font font, Graphics2D g) {
        return new TextLayout(text, font, g.getFontRenderContext());
    }

    private void paintIncrement(int i, RoundQuantity increment, int incrementCount, Graphics2D g, double scale) {
        Rectangle2D.Double shape =
            new Rectangle2D.Double(
                x(i, increment, incrementCount, scale),
                barTop(),
                x(i + 1, increment, incrementCount, scale) - x(i, increment, incrementCount, scale),
                barBottom() - barTop());
        g.setColor(((i % 2) == 0) ? FILL1 : FILL2);
        g.fill(shape);
        g.setColor(LINE_COLOR);
        g.draw(shape);
    }

    private void paintIncrements(RoundQuantity increment, int incrementCount, Graphics2D g, double scale) {
        for (int i = 0; i < incrementCount; i++) {
            paintIncrement(i, increment, incrementCount, g, scale);
            paintLabel(i, increment, incrementCount, g, scale);
        }
    }

    private void paintLabel(int i, RoundQuantity increment, int incrementCount, Graphics2D g, double scale) {
        String text =
                new RoundQuantity(
                    increment.getMantissa() * (i + 1),
                    increment.getExponent(),
                    increment.getUnit()).getAmountString();
        Font font = FONT;
        g.setColor(TEXT_COLOR);

        int textBottomMargin = TEXT_BOTTOM_MARGIN;

        if (i == (incrementCount - 1)) {
            text = increment.getUnit().getName();
            font = UNIT_FONT;
            g.setColor(UNIT_TEXT_COLOR);
            textBottomMargin = UNIT_TEXT_BOTTOM_MARGIN;
        }

        TextLayout layout = createTextLayout(text, font, g);
        double center =
            MathUtil.avg(x(i, increment, incrementCount, scale), x(i + 1, increment, incrementCount, scale));
        layout.draw(
            g,
            (float) (center - (layout.getAdvance() / 2)),
            (float) (barBottom() - textBottomMargin));
    }

    private double x(int i, RoundQuantity increment, int incrementCount, double scale) {
        return HORIZONTAL_MARGIN + (i * increment.getModelValue() * scale);
    }
}
