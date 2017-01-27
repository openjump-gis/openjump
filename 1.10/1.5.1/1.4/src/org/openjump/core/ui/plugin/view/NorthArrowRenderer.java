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

package org.openjump.core.ui.plugin.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import com.vividsolutions.jump.util.MathUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.renderer.SimpleRenderer;

public class NorthArrowRenderer extends SimpleRenderer {
	public final static String CONTENT_ID = "NORTH_ARROW";
	private final static String ENABLED_KEY = NorthArrowRenderer.class +" - ENABLED";

	private int ARROW_SIZE = 100;
	private Color FILL2 = Color.white;
	private Color FILL1 = new Color(255, 204, 204);
	private int HORIZONTAL_MARGIN = 8;
	private int VERTICAL_MARGIN = 15;
	private Color LINE_COLOR = Color.black;
	private int TEXT_BOTTOM_MARGIN = 3;
	private Color TEXT_COLOR = Color.blue;
	private int TEXT_FONT_SIZE = 15;
	private Font FONT = new Font("Dialog", Font.BOLD, TEXT_FONT_SIZE);
	private Stroke stroke = new BasicStroke();
	private final int ALIGN_BOTTOM_RIGHT = 0;
	private final int ALIGN_TOP_RIGHT = 1;
	private final int ALIGN_TOP_LEFT = 2;
	private final int ALIGN_BOTTOM_LEFT = 3;

	private int alignment = ALIGN_TOP_RIGHT;
	private int style = 1; 

	public NorthArrowRenderer(LayerViewPanel panel) {
		super(CONTENT_ID, panel);
	}

	public static boolean isEnabled(LayerViewPanel panel) {
		return panel.getBlackboard().get(ENABLED_KEY, false);
	}

	public static void setEnabled(boolean enabled, LayerViewPanel panel) {
		panel.getBlackboard().put(ENABLED_KEY, enabled);
	}

	protected void paint(Graphics2D g) {
		paint(g, panel.getViewport().getScale());
	}

	public void paint(Graphics2D g, double scale) {
		if (!isEnabled(panel)) {
			return;
		}
		g.setStroke(stroke);
		int x1 = arrowRight() - ARROW_SIZE / 5;
		int x2 = arrowRight();
		int y1 = arrowTop();
		int y2 = arrowBottom();		
		int x3 = (int) (x1 + 0.5*(x2 - x1));
		int y3 = (int) (y1 + 0.8*(y2 - y1));
		switch (style) {
		case 1: { //the compass rose
			x1 = arrowRight() - ARROW_SIZE;
			x2 = arrowRight();
			y1 = arrowTop();
			y2 = arrowBottom();	
			x3 = (int) (x1 + 0.5*(x2 - x1));
			y3 = (int) (y1 + 0.5*(y2 - y1));
			int width = ARROW_SIZE / 10;
			int x4 = x3 - width;
			int x5 = x3 + width;
			int y4 = y3 - width;
			int y5 = y3 + width;
			//draw drop shadow
			int s = 4;
			int xPoints0[] = {x3+s, x4+s, x1+s, x4+s, x3+s, x5+s, x2+s, x5+s, x3+s}; 
			int yPoints0[] = {y2+s, y5+s, y3+s, y4+s, y1+s, y4+s, y3+s, y5+s, y2+s};
			drawShape(g, Color.LIGHT_GRAY, Color.LIGHT_GRAY, xPoints0, yPoints0, true, false);
			//draw black filled part
			int xPoints[] = {x3, x4, x3, x1, x4, x3, x3, x5, x3, x2, x5, x3, x3}; 
			int yPoints[] = {y2, y5, y3, y3, y4, y3, y1, y4, y3, y3, y5, y3, y2};
			drawShape(g, Color.black, LINE_COLOR, xPoints, yPoints, true, false);
			//draw white filled part
			int xPoints2[] = {x3, x3, x4, x1, x3, x4, x3, x3, x5, x2, x3, x5, x3}; 
			int yPoints2[] = {y2, y3, y5, y3, y3, y4, y1, y3, y4, y3, y3, y5, y2};
			drawShape(g, Color.white, LINE_COLOR, xPoints2, yPoints2, true, false);
			//draw black outline
			int xPoints3[] = {x3, x4, x1, x4, x3, x5, x2, x5, x3}; 
			int yPoints3[] = {y2, y5, y3, y4, y1, y4, y3, y5, y2};
			drawShape(g, Color.white, LINE_COLOR, xPoints3, yPoints3, false, true);
			drawText(g, "N",  TEXT_COLOR, MathUtil.avg(x1, x2), arrowTop() - TEXT_BOTTOM_MARGIN);
			break;
		}
		case 2: { //the inverted V shape half filled narrower version
			int xPoints[] = {x2, x1, x1, x2};  //right half V
			int yPoints[] = {y2, y3, y1, y2};
			drawShape(g, Color.black, LINE_COLOR, xPoints, yPoints, true, true);
			int x0 = x1 - ARROW_SIZE / 5;
			int xPoints2[] = {x0, x1, x1, x0};  //left half V
			int yPoints2[] = {y2, y3, y1, y2};
			drawShape(g, Color.white, LINE_COLOR, xPoints2, yPoints2, true, true);
			drawText(g, "N",  TEXT_COLOR, MathUtil.avg(x0, x2), arrowTop() - TEXT_BOTTOM_MARGIN);
			break;
		}
		case 3: { //the inverted V shape half filled wider version
			x1 = arrowRight() - ARROW_SIZE / 3;
			int xPoints[] = {x2, x1, x1, x2};  //right half V
			int yPoints[] = {y2, y3, y1, y2};
			drawShape(g, Color.black, LINE_COLOR, xPoints, yPoints, true, true);
			int x0 = x1 - ARROW_SIZE / 3;
			int xPoints2[] = {x0, x1, x1, x0};  //left half V
			int yPoints2[] = {y2, y3, y1, y2};
			drawShape(g, Color.white, LINE_COLOR, xPoints2, yPoints2, true, true);
			drawText(g, "N",  TEXT_COLOR, MathUtil.avg(x0, x2), arrowTop() - TEXT_BOTTOM_MARGIN);
			break;
		}
		default: { //shaped like a backwards 4 filled with scale bar color
			y3 = (int) (y1 + 0.6*(y2 - y1));
			int xPoints[] = {x1, x1, x2, x1, x1};  //4
			int yPoints[] = {y2, y3, y3, y1, y2};
			drawShape(g, FILL1, LINE_COLOR, xPoints, yPoints, true, true);
			drawText(g, "N",  TEXT_COLOR, MathUtil.avg(x1, x2), arrowBottom() - TEXT_BOTTOM_MARGIN);
		}
		}
	}

	private void drawText(Graphics2D g, String text, Color textColor, 
			double xCenter, double yBaseline) {
		TextLayout layout = createTextLayout(text, FONT, g);
		layout.draw(
				g,
				(float) (xCenter - (layout.getAdvance() / 2)),
				(float) yBaseline);
	}

	private void drawShape(Graphics2D g, Color fillColor, Color lineColor, 
			int xPoints[],int yPoints[], boolean fill, boolean stroke) {
		GeneralPath polygon = 
			new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);
		polygon.moveTo(xPoints[0], yPoints[0]);

		for (int index = 1; index < xPoints.length; index++) {
			polygon.lineTo(xPoints[index], yPoints[index]);
		};
		polygon.closePath();
		if (fill) {
			g.setColor(fillColor);
			g.fill(polygon);
		}
		if (stroke) {
			g.setColor(lineColor);
			g.draw(polygon);
		}
	}

	private int arrowBottom() {
		int position = 0;
		switch (alignment) {
		case ALIGN_TOP_LEFT: ; //fall through
		case ALIGN_TOP_RIGHT: {
			position = VERTICAL_MARGIN + ARROW_SIZE + TEXT_FONT_SIZE + TEXT_BOTTOM_MARGIN;
			break;
		}
		case ALIGN_BOTTOM_LEFT:  //fall through
		case ALIGN_BOTTOM_RIGHT: ; //fall through
		default: {
			position = panel.getHeight() - VERTICAL_MARGIN;
		}
		}
		return position;
	}

	private int arrowTop() {
		return arrowBottom() - ARROW_SIZE;
	}

	private int arrowRight() {
		int position = 0;
		switch (alignment) {
		case ALIGN_BOTTOM_LEFT: ; //fall through
		case ALIGN_TOP_LEFT: {
			position = ARROW_SIZE / 2 + HORIZONTAL_MARGIN;
			break;
		}
		case ALIGN_TOP_RIGHT:  ;   //fall through
		case ALIGN_BOTTOM_RIGHT: ; //fall through
		default: {
			position = panel.getWidth() - HORIZONTAL_MARGIN;
		}
		}
		return position;
	}

	private TextLayout createTextLayout(String text, Font font, Graphics2D g) {
		return new TextLayout(text, font, g.getFontRenderContext());
	}

	public int getAlignment() {
		return alignment;
	}

	/**
	 * @param alignment: BOTTOM_RIGHT = 0, TOP_RIGHT = 1, TOP_LEFT = 2, BOTTOM_LEFT = 3
	 */
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}

	public int getARROW_SIZE() {
		return ARROW_SIZE;
	}

	public void setARROW_SIZE(int arrow_size) {
		ARROW_SIZE = arrow_size;
	}

	public Color getFILL1() {
		return FILL1;
	}

	public void setFILL1(Color fill1) {
		FILL1 = fill1;
	}

	public Color getFILL2() {
		return FILL2;
	}

	public void setFILL2(Color fill2) {
		FILL2 = fill2;
	}

	public Color getLINE_COLOR() {
		return LINE_COLOR;
	}

	public void setLINE_COLOR(Color line_color) {
		LINE_COLOR = line_color;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	public int getStyle() {
		return style;
	}

	/**
	 * @param style: pass an int related to the ESRI North Arrow style.
	 */
	public void setStyle(int style) {
		this.style = style;
	}

	public int getTEXT_BOTTOM_MARGIN() {
		return TEXT_BOTTOM_MARGIN;
	}

	public void setTEXT_BOTTOM_MARGIN(int text_bottom_margin) {
		TEXT_BOTTOM_MARGIN = text_bottom_margin;
	}

	public Color getTEXT_COLOR() {
		return TEXT_COLOR;
	}

	public void setTEXT_COLOR(Color text_color) {
		TEXT_COLOR = text_color;
	}

	public int getTEXT_FONT_SIZE() {
		return TEXT_FONT_SIZE;
	}

	public void setTEXT_FONT_SIZE(int text_font_size) {
		TEXT_FONT_SIZE = text_font_size;
		FONT = new Font("Dialog", Font.BOLD, TEXT_FONT_SIZE);
	}

	public int getVERTICAL_MARGIN() {
		return VERTICAL_MARGIN;
	}

	public void setVERTICAL_MARGIN(int vertical_margin) {
		VERTICAL_MARGIN = vertical_margin;
	}

}
