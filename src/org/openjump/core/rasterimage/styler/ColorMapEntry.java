package org.openjump.core.rasterimage.styler;

import java.awt.Color;

/**
 * This class represents a cell value-colour pair.
 * @author AdL
 */
public class ColorMapEntry {

    public ColorMapEntry(double quantity, Color color) {
        this.quantity = quantity;
        this.color = color;
    }

    public ColorMapEntry(ColorMapEntry rangeAndColor) {
        this.quantity = rangeAndColor.getUpperValue();
        this.color = rangeAndColor.getColor();
    }
    
    public double getUpperValue() {
        return quantity;
    }

    public Color getColor() {
        return color;
    }
 
    
    private final double quantity;
    private final Color color;
    
}
