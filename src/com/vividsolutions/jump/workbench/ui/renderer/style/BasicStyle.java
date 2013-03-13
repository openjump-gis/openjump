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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.NoninvertibleTransformException;
import java.util.List;

import org.openjump.util.SLDImporter.StrokeFillStyle;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;


public class BasicStyle implements Style, StrokeFillStyle {
	/* Giuseppe Aruta 11-june-2012
	 * Added code from SkyJUMP BasicStyle to enable color chooser plugin
	 */
	public static final String RGB_ATTRIBUTE_NAME = "R_G_B";
	/*
	 * 
	 */
    private boolean renderingFill = true;
    private boolean renderingLine = true;
    private boolean renderingLinePattern = false;
    private boolean renderingFillPattern = false;

		public static final Color       DEFAULT_FILL_COLOR  = new Color(0, 0, 0, 255);
		public static final Color       DEFAULT_LINE_COLOR  = DEFAULT_FILL_COLOR;
		public static final BasicStroke DEFAULT_FILL_STROKE = new BasicStroke(1);

    //The important thing here is the initial alpha. [Jon Aquino]
    private Color fillColor = DEFAULT_FILL_COLOR;
    private Color lineColor = DEFAULT_LINE_COLOR;

    private BasicStroke lineStroke;
    private Stroke fillStroke = DEFAULT_FILL_STROKE;
    private boolean enabled = true;
    private String linePattern = "3";

    private boolean renderingVertices = true;
    
    //Set fill pattern to something, so that the BasicStylePanel combobox won't
    //start empty. [Jon Aquino]
		// Fixing the GUI is a better idea! [s-l-teichmann]
    private Paint fillPattern;

    public BasicStyle(Color fillColor) {
        setFillColor(fillColor);
        setLineColor(Layer.defaultLineColor(fillColor));
        setLineWidth(1);
    }

    public BasicStyle() {
        this(Color.black);
    }

    public boolean isRenderingFillPattern() {
        return renderingFillPattern;
    }

    public BasicStyle setRenderingFillPattern(boolean renderingFillPattern) {
        this.renderingFillPattern = renderingFillPattern;
        return this;
    }

    public Paint getFillPattern() {
        return fillPattern;
    }

    /**
     * Remember to call #setRenderingFillPattern(true).
     */
    public BasicStyle setFillPattern(Paint fillPattern) {
        this.fillPattern = fillPattern;
        if (fillPattern instanceof BasicFillPattern) {
            ((BasicFillPattern) fillPattern).setColor(this.fillColor);
        }        
        return this;
    }

    public String getLinePattern() {
        return linePattern;
    }

    public boolean getRenderingVertices() {
        return renderingVertices;
    }

    public void setRenderingVertices(boolean renderingVertices) {
        this.renderingVertices = renderingVertices;
    }
    
    /**
     * The actual dash pattern used internally will be the given dash pattern
     * multiplied by the line length. Remember to call #setRenderingLinePattern(true).
     * @param linePattern e.g. "5,2,3,2"
     */
    public BasicStyle setLinePattern(String linePattern) {
        this.linePattern = linePattern;
        lineStroke = createLineStroke(lineStroke.getLineWidth());
        return this;
    }

    public void initialize(Layer layer) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void paint(Feature f, Graphics2D g, Viewport viewport)
        throws NoninvertibleTransformException {
    	
    	
    	/* Giuseppe Aruta 11-june-2012
    	 * Added code from SkyJUMP BasicStyle to enable color chooser plugin
    	 */
    	Color fillColor = this.fillColor;
    	Color lineColor = this.lineColor;
    	Color featureColor = getFeatureColor(f);
    	
    	if (featureColor != null)
    	{
    		fillColor = featureColor;
    		lineColor = featureColor.darker();
    	}
    	/*
    	 * End of code 1
    	*/
    	
        if (!renderingVertices && f.getGeometry() instanceof Point) {
            return;
        }
        StyleUtil.paint(f.getGeometry(), g, viewport, renderingFill,
            fillStroke,
            (renderingFillPattern && (fillPattern != null)) ? fillPattern
                                                            : fillColor,
            renderingLine, lineStroke, lineColor);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();

            return null;
        }
    }

    public boolean isRenderingFill() {
        return renderingFill;
    }

    public boolean isRenderingLine() {
        return renderingLine;
    }

    public boolean isRenderingLinePattern() {
        return renderingLinePattern;
    }

    public void setRenderingFill(boolean renderingFill) {
        this.renderingFill = renderingFill;
    }

    public void setRenderingLine(boolean renderingLine) {
        this.renderingLine = renderingLine;
    }

    public BasicStyle setRenderingLinePattern(boolean renderingLinePattern) {
        this.renderingLinePattern = renderingLinePattern;
        lineStroke = createLineStroke(lineStroke.getLineWidth());
        return this;
    }

    public void setFillColor(Color fillColor) {
        setFillColor(fillColor, getAlpha());
    }
    
    private BasicStyle setFillColor(Color fillColor, int alpha) {
        this.fillColor = GUIUtil.alphaColor(fillColor, alpha);
        if (fillPattern instanceof BasicFillPattern) {
            ((BasicFillPattern) fillPattern).setColor(this.fillColor);
        }
        return this;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = GUIUtil.alphaColor(lineColor, getAlpha());
    }

    public void setLineWidth(int lineWidth) {
        //Don't use BasicStroke.JOIN_ROUND or JOIN_BEVEL -- when the line
        //width is 1, one of the corners will not be drawn. [Jon Aquino]
        lineStroke = createLineStroke(lineWidth);
    }
    
    public void setFractionalLineWidth(float lineWidth) {
        lineStroke = createLineStroke(lineWidth);
    }

    private BasicStroke createLineStroke(float lineWidth) {
        return (renderingLinePattern && (linePattern.trim().length() != 0) &&
        (lineWidth > 0))
        ? new BasicStroke(lineWidth, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 1.0f, toArray(linePattern, lineWidth), 0)
        : new BasicStroke(lineWidth, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL);
    }

    public static float[] toArray(String linePattern, float lineWidth) {
        List strings = StringUtil.fromCommaDelimitedString(linePattern);
        float[] array = new float[strings.size()];

        for (int i = 0; i < strings.size(); i++) {
            String string = (String) strings.get(i);
            array[i] = Float.parseFloat(string) * lineWidth;

            if (array[i] <= 0) {
                throw new IllegalArgumentException(I18N.get("ui.renderer.style.BasicStyle.negative-dash-length"));
            }
        }
        return array;
    }

    /**
     * @return 0-255 (255 is opaque)
     */
    public int getAlpha() {
        return fillColor.getAlpha();
    }

    public Color getFillColor() {
        return GUIUtil.alphaColor(fillColor, 255);
    }

    public Color getLineColor() {
        return GUIUtil.alphaColor(lineColor, 255);
    }

    public int getLineWidth() {
        return (int) lineStroke.getLineWidth();
    }

    /**
     * @param alpha 0-255 (255 is opaque)
     */
    public void setAlpha(int alpha) {
        setFillColor(fillColor, alpha);
        lineColor = GUIUtil.alphaColor(lineColor, alpha);
    }

    public BasicStroke getLineStroke() {
        return lineStroke;
    }
    
    /**
     * - Giuseppe Aruta 11-June - 2012 - added code from SkyJUMP to enable color chooser plugIn -
     * - Original comments from SkyJUMP developers -
     * 
     * Looks for an Attriubte field that contains the RGB color code and returns
     * it or null if the RGB hex string is empty or ill-formed. 
     * @param    f   the <code>Feature</code> containing the RGB 
     * 			Attribute to be parsed.
     * @return   the <code>Color</code> represented by the R_B_G hex value with 
     *           the current Alpha value inserted.
     *           Warning: returns null in many "normal" cases.
     */
     public Color getFeatureColor(Feature f)
     {
      	if(f.getSchema().hasAttribute(RGB_ATTRIBUTE_NAME))
     	{
     		String colorStr = (String)f.getAttribute(RGB_ATTRIBUTE_NAME);
     		
     		if ((colorStr != null) && (colorStr.trim().length() == 6))
     		{
     			try
     			{
     				int r = Integer.parseInt(colorStr.substring(0,2), 16);
     				int g = Integer.parseInt(colorStr.substring(2,4), 16);
     				int b = Integer.parseInt(colorStr.substring(4,6), 16);
     				return new Color(r, g, b, getAlpha());
     			}
     			catch (NumberFormatException ex)
     			{
     				return null;
     			}
     		}
     	}
     	
     	return null;
     }
    
}
