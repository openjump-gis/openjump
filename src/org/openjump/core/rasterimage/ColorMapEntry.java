package org.openjump.core.rasterimage;

import java.awt.Color;

/**
 *
 * @author AdL
 */
public class ColorMapEntry {

    public ColorMapEntry(double upperValue, Color color) {
        this.upperValue = upperValue;
        this.color = color;
    }

    public ColorMapEntry(ColorMapEntry rangeAndColor) {
        this.upperValue = rangeAndColor.getUpperValue();
        this.color = rangeAndColor.getColor();
    }    
    
    public double getUpperValue() {
        return upperValue;
    }

    public Color getColor() {
        return color;
    }

    private final double upperValue;
    private final Color color;
        
}
