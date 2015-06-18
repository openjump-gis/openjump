package org.openjump.core.rasterimage.styler;

import java.util.TreeMap;

/**
 * This class represents the style to be associated to a raster image. A style
 * is given as series of value-colour pairs. The values refer to the raster cell
 * values, and the colours to the colours that the cells will take.
 * There are two types of symbolizers: RAMP, INTERVALS. For the RAMP
 * symbolizer, the cell colours will be interpolated from the given value-colour
 * pairs. For the INTERVALS symbolizer, all the cells with value equal or above
 * the value contained in the value-colour pair will take the associated colour.
 * To create a single values symbology, use the INTERVALS symbolizer, providing
 * a value-colour pair for every raster value.
 * @author AdL
 */
public class RasterStyler {

    /**
     * Creates a new raster symbolizer of the given colour map type.
     * @param colorMapType The colour map type to be used in this raster symbolizer.
     * 
     */
    public RasterStyler(ColorMapType2 colorMapType) {
        this.colorMapEntry_tm = new TreeMap<Double,ColorMapEntry>();
        this.colorMapType = colorMapType;
    }
    
    /**
     * Adds a new value-colour pair to the symbology.
     * @param colorMapEntry The new value-colour pair
     * @throws Exception 
     */
    public void addColorMapEntry(ColorMapEntry colorMapEntry) throws Exception {
        
        colorMapEntry_tm.put(colorMapEntry.getUpperValue(), colorMapEntry);
        
    }

    public void addColorMapEntries(ColorMapEntry[] colorMapEntries) throws Exception {
        
        for(ColorMapEntry colorMapEntry : colorMapEntries) {
            colorMapEntry_tm.put(colorMapEntry.getUpperValue(), colorMapEntry);
        }
        
    }
    
    /**
     * Removes a value-colour pair from the symbology.
     * @param cellValue The value to be removed.
     */
    public void removeColorMapEntry(double cellValue) {
        colorMapEntry_tm.remove(cellValue);
    }
    
    /**
     * Returns the colour map entry associated to a cell value.
     * @param cellValue The cell value.
     * @return 
     */
    public ColorMapEntry getColorMapEntry(double cellValue) {

        if(colorMapEntry_tm.floorEntry(cellValue) != null) {
            return colorMapEntry_tm.floorEntry(cellValue).getValue();
        }
        return null;

    }

    /**
     * Returns the colour map entry just above to the given cell value.
     * @param cellValue The given cell value.
     * @return 
     */
    public ColorMapEntry getNextColorMapEntry(double cellValue) {

        if(colorMapEntry_tm.higherEntry(cellValue) != null) {
            return colorMapEntry_tm.higherEntry(cellValue).getValue();
        }
        return null;
        
    }
    
    /**
     * Returns an array of all the colour map entries.
     * @return The colour map entries of the symbolizer.
     */
    public ColorMapEntry[] getColorMapEntries() {
        
        ColorMapEntry[] colorMapEntry = new ColorMapEntry[colorMapEntry_tm.size()];        
        return colorMapEntry_tm.values().toArray(colorMapEntry);
        
    }
    
    /**
     * Returns the colour map type.
     * @return The colour map type.
     */
    public ColorMapType2 getColorMapType() {
        return colorMapType;
    }

    /**
     * Returns the level of transparency: 0 (no transparency) - 1 (transparent).
     * @return The transparency (0-1).
     */
    public double getTransparencyRatio() {
        return transparencyRatio;
    }

    /**
     * Sets the overall transparency: 0 (no transparency) - 1 (transparent).
     * @param transparencyRatio Transparency level (0-1);
     */
    public void setTransparencyRatio(double transparencyRatio) {
        this.transparencyRatio = transparencyRatio;
    }

    @Override
    public boolean equals(Object obj) {
        
        if(obj instanceof RasterStyler) {
            
            RasterStyler otherRasterSymbolizer = (RasterStyler) obj;
            
            // Compare color map type
            if(this.getColorMapType() != otherRasterSymbolizer.getColorMapType()) {
                return false;
            }
            
            // Compare color map entries
            ColorMapEntry[] thisColorMapEntries = this.getColorMapEntries();
            ColorMapEntry[] otherColorMapEntries = otherRasterSymbolizer.getColorMapEntries();
            
            if(thisColorMapEntries.length != otherColorMapEntries.length) {
                return false;
            }
            
            for(int c=0; c<thisColorMapEntries.length; c++) {
                if(thisColorMapEntries[c].getUpperValue() != otherColorMapEntries[c].getUpperValue()) {
                    return false;
                }
                if(!thisColorMapEntries[c].getColor().equals(otherColorMapEntries[c].getColor())) {
                    return false;
                }
            }
            
            return this.getTransparencyRatio() == otherRasterSymbolizer.getTransparencyRatio();
            
        } else {
            return super.equals(obj);
        }
        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.colorMapEntry_tm != null ? this.colorMapEntry_tm.hashCode() : 0);
        hash = 79 * hash + (this.colorMapType != null ? this.colorMapType.hashCode() : 0);
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.transparencyRatio) ^ (Double.doubleToLongBits(this.transparencyRatio) >>> 32));
        return hash;
    }
    
    private final TreeMap<Double,ColorMapEntry> colorMapEntry_tm;
    private final ColorMapType2 colorMapType;
    private double transparencyRatio = 0;
    
    public enum ColorMapType2 {
        RAMP, INTERVALS;
    }
    
}
