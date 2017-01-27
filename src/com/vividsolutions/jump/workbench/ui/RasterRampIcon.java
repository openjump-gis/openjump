package com.vividsolutions.jump.workbench.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import javax.swing.Icon;

/**
 *
 * @author AdL
 */
public class RasterRampIcon implements Icon {
    
    public RasterRampIcon(Color[] colors) {
        this.colors = colors;
    }    
    
    @Override
    public int getIconHeight() {
        return 60;
    }
  
    @Override
    public int getIconWidth() {
        return 15;
    }
  
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
//        // store the old color; I like to leave Graphics as I receive them
//        Color old = g.getColor();
//        g.setColor(Color.BLUE);
//        g.fillRect(x, y, getIconWidth(), getIconHeight());
//        g.setColor(old);
        
        float[] fractions = new float[colors.length];
        for(int f=0; f<fractions.length; f++) {
            fractions[f] = f * (1f/(colors.length-1));
        }
        
        Paint gradient = new LinearGradientPaint(
                new Point2D.Double(0, 0),
                new Point2D.Double(0, getIconHeight() - 1),
                fractions,
                colors);
        
        ((Graphics2D)g).setPaint(gradient);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        
    }

    private Color[] colors;
    
}
