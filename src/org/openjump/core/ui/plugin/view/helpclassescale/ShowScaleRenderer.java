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

package org.openjump.core.ui.plugin.view.helpclassescale;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.SimpleRenderer;

/**
* Calculates the actual scale and draws the text and a white rectangle around
*
* @author sstein 
**/
public class ShowScaleRenderer extends SimpleRenderer {
	
    public final static String CONTENT_ID = "SCALE_SHOW";
    /**
     *  Height of the increment boxes, in view-space units.
     */
    private final static int BAR_HEIGHT = 13;
    private final static Color FILL1 = Color.WHITE;

    /**
     *  Distance from the right edge, in view-space units.
     */
    private final static int HORIZONTAL_MARGIN = 100;

    /**
     *  In view-space units; the actual increment may be a bit larger or smaller
     *  than this amount.
     */
    private final static Color LINE_COLOR = Color.GRAY;
    private final static int TEXT_BOTTOM_MARGIN = 1;
    private final static Color TEXT_COLOR = Color.black;


    /**
     *  Distance from the bottom edge, in view-space units.
     */
    private final int FONTSIZE = 12;
    private final static int VERTICAL_MARGIN = 3;
    private final static String ENABLED_KEY = "SCALE_SHOW_ENABLED";
    private Font FONT = new Font("Dialog", Font.PLAIN, FONTSIZE);
    private Stroke stroke = new BasicStroke();
    private PlugInContext myPlugInContext = null;
    
    
    public ShowScaleRenderer(LayerViewPanel panel) {
        super(CONTENT_ID, panel);        
    } 
     
    protected void paint(Graphics2D g) {
        if (!isEnabled(panel)) {
            return;
        }
        //Override dashes set in GridRenderer [Jon Aquino]
        g.setStroke(stroke);
        double screenScale = ScreenScale.getHorizontalMapScale(panel.getViewport()); 
        paintScaleLabel(g, screenScale);
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
   
    private void paintScaleLabel(Graphics2D g, double scale) {
       
    	Integer scaleD = new Integer((int)Math.floor(scale));
        String text = "1 : " + scaleD.toString();
        int length = text.length();

    	//-- draw rectangle
        Rectangle2D.Double shape =
            new Rectangle2D.Double(panel.getWidth()- (length+13)*3.6, 
            						barTop(), (length+12)*3.6-3, barBottom() - barTop());
        g.setColor(FILL1);
        g.fill(shape);
        g.setColor(LINE_COLOR);
        g.draw(shape);
        
        //draw text
        Font font = FONT;
        g.setColor(TEXT_COLOR);

        int textBottomMargin = TEXT_BOTTOM_MARGIN;

        TextLayout layout = createTextLayout(text, font, g);
        layout.draw(g,
                (float) (panel.getWidth()- (length+11)*3.6),
                (float) (barBottom() - textBottomMargin));
        	
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
	 * @param myPlugInContext The myPlugInContext to set.
	 */
	public void setMyPlugInContext(PlugInContext myPlugInContext) {
		this.myPlugInContext = myPlugInContext;
	}
}
