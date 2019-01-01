package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import javax.swing.JComponent;
import org.openjump.core.rasterimage.styler.ColorMapEntry;

/**
 * This class allows to create a JComponent whose background is stretched between 
 * two colors. The stretched is from top to bottom and with horizonal effect.
 * 
 * @author GeomaticaEAmbiente
 */
public class GradientCanvas extends JComponent{
    
    /**
     * Construct a JComponent whose background is stretched between colorStart and
     * colorEnd.
     * 
     * @param colorMapEntries
     * @param width Width of the stretched effect.
     * @param height Height of the stretched effect.
     * @param type Gradient direction. HORIZONTAL colorStart is on left and colorEnd is on right, VERTICAL
     *  colorStart is at top and colorEnd is bottom.
     */
    public GradientCanvas(ColorMapEntry[] colorMapEntries, int width, int height, GradientType type){
        
        this.colorMapEntries = colorMapEntries;
        this.width = width;
        this.height = height;
        this.type = type;
    
        super.setSize(new Dimension(width, height));
        
    }
    
    /*
    * Stretched for vertical effect from top to bottom and for horizontal effect from left to right.
    */    
    @Override
    public void paint(Graphics g){        
        
        LinearGradientPaint paint = null;
        float[] fractions = new float[colorMapEntries.length];
        Color[] colors = new Color[colorMapEntries.length];
        for(int c=0; c<colors.length; c++) {
            colors[c] = colorMapEntries[c].getColor();
            fractions[c] = (float) (colorMapEntries[c].getUpperValue()/ colorMapEntries[colorMapEntries.length-1].getUpperValue());
        }
                
                
        
        if(type == GradientType.HORIZONTAL){
            paint = new LinearGradientPaint(0, 0, width, height, fractions, colors);
        } else if (type == GradientType.VERTICAL){
            paint = new LinearGradientPaint((width / 2), 0, (width/2), height, fractions, colors); 
        }
              
                
        Graphics2D g2d = (Graphics2D) g;
        Paint oldPaint = g2d.getPaint();
        g2d.setPaint(paint);
        g2d.fillRect(0, 0, (int)width, (int) height);
        g2d.setPaint(oldPaint); 
        
    }

    public GradientType getType() {
        return type;
    }

    public ColorMapEntry[] getColorMapEntries() {
        return colorMapEntries;
    }
    
    private final ColorMapEntry[] colorMapEntries;
    private final float width;
    private final float height;
    
    public enum GradientType {HORIZONTAL, VERTICAL};
    private final GradientType type;

}