package org.openjump.core.rasterimage.styler;

import java.awt.Color;
import java.util.Random;


public class ColorUtils {
    
    public ColorUtils() {
        random = new Random();
    }
    
    public Color interpolateColor(Color startColor, Color endColor, double relDistance) throws Exception {
        
        if(relDistance <0 || relDistance > 1) {
            throw new Exception("Relative distance out of range. Must be 0-1."); //NOI18N
        }
        
        //int red = (int) Math.round((double) c / (double) (colors.length - 1) * 255);
        int red = interpolate(startColor.getRed(), endColor.getRed(), relDistance);
        int green = interpolate(startColor.getGreen(), endColor.getGreen(), relDistance);
        int blue = interpolate(startColor.getBlue(), endColor.getBlue(), relDistance);
        
        return new Color(red, green, blue);
        
    }
    
    private int interpolate(int startValue, int endValue, double relDistance) {
        
        return (int)Math.round(startValue * (1-relDistance) + endValue * (relDistance));
        
    }
    
    public Color randomColor() {
        
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        
        return new Color(red, green, blue);
        
    }
    
    private Random random;
    
}
