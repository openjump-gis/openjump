package org.openjump.core.rasterimage;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;
import org.openjump.core.rasterimage.styler.ColorMapEntry;

/**
 *
 * @author AdL
 */
public class RasterSymbology {
    
    private TreeMap<Double,Color> colorMapEntries_tm;
    private String colorMapType;
    private double transparency = 0;
    
    public RasterSymbology() {
    }
    
    public RasterSymbology (String colorMapType) {
        
        this.colorMapType = colorMapType;
        colorMapEntries_tm = new TreeMap<Double,Color>();
        
    }
    
    public void addColorMapEntry(double upperValue, Color color) {
        colorMapEntries_tm.put(upperValue, color);
    }
    
    private ColorMapEntry getColorMapEntry(double cellValue) {     
        if(colorMapEntries_tm.floorEntry(cellValue) != null) {
            return new ColorMapEntry(
                    colorMapEntries_tm.floorEntry(cellValue).getKey(),
                    colorMapEntries_tm.floorEntry(cellValue).getValue());
        }
        return null;        
    }
    
    private ColorMapEntry getNextColorMapEntry(double cellValue) {     
        if(colorMapEntries_tm.higherEntry(cellValue) != null) {
            return new ColorMapEntry(
                    colorMapEntries_tm.higherEntry(cellValue).getKey(),
                    colorMapEntries_tm.higherEntry(cellValue).getValue());
        }
        return null;        
    }

    public String getColorMapType() {
        return colorMapType;
    }

    public void setColorMapType(String colorMapType) {
        this.colorMapType = colorMapType;
    }
    
    public TreeMap<Double, Color> getColorMapEntries_tm() {
        return colorMapEntries_tm;
    }
    
    public void setColorMapEntries_tm(TreeMap<Double, Color> colorMapEntries_tm) {
        this.colorMapEntries_tm = colorMapEntries_tm;
    }
    
    public ColorMapEntry[] getColorMapEntries() {
        
        ColorMapEntry[] colorMapEntries = new ColorMapEntry[getColorMapEntries_tm().size()];
        int pos = 0;
        for(Map.Entry<Double,Color> colorMapEntry : getColorMapEntries_tm().entrySet()) {
            colorMapEntries[pos] = new ColorMapEntry(colorMapEntry.getKey(), colorMapEntry.getValue());
            pos++;
        }
        return colorMapEntries;
        
    }
    
    public double getTransparency() {
        return transparency;
    }

    public void setTransparency(double transparency) {
        this.transparency = transparency;
    }
    
    public Color getColor(double value) {
        
        if(colorMapType.equals(TYPE_RAMP)) {
            
            ColorMapEntry downColorMapEntry = getColorMapEntry(value);
            ColorMapEntry upColorMapEntry = getNextColorMapEntry(value);
            
            if(downColorMapEntry != null && upColorMapEntry == null) {
                return downColorMapEntry.getColor();
            } else if(downColorMapEntry == null && upColorMapEntry != null) {
                return upColorMapEntry.getColor();
            } else if(downColorMapEntry != null && upColorMapEntry != null) {
            
                if(downColorMapEntry.getColor() == null) {
                    return null;
                } else if(upColorMapEntry.getColor() == null) {
                    return downColorMapEntry.getColor();
                } else {
                    double distDown = value - downColorMapEntry.getUpperValue();
                    double distUp = upColorMapEntry.getUpperValue()- value;

                    double relDist = distDown / (distUp + distDown);

                    Color newColor = calculateNewColor(
                            downColorMapEntry.getColor(),
                            upColorMapEntry.getColor(),
                            relDist);

                    return newColor;
                }
            } else {
                return null;
            }
            
        } else if(colorMapType.equals(TYPE_INTERVALS)) {
            
            ColorMapEntry downColorMapEntry = getColorMapEntry(value);
            if(downColorMapEntry == null) {
                return null;
            }
            return downColorMapEntry.getColor();
            
        } else if(colorMapType.equals(TYPE_SINGLE)) {
            
            ColorMapEntry downColorMapEntry = getColorMapEntry(value);
            if(downColorMapEntry == null) {
                return null;
            }
            return downColorMapEntry.getColor();
            
            
        } else {
            return null;
        }  

    }
    
    private static Color calculateNewColor (Color downColor, Color upColor, double relDist) {
        int red = (int) Math.round((upColor.getRed() - downColor.getRed()) * relDist + downColor.getRed());
        int green = (int) Math.round((upColor.getGreen() - downColor.getGreen()) * relDist + downColor.getGreen());
        int blue = (int) Math.round((upColor.getBlue() - downColor.getBlue()) * relDist + downColor.getBlue());
        
        return new Color(red, green, blue);        
    }
    
    
    public static final String TYPE_RAMP = "RAMP";
    public static final String TYPE_INTERVALS = "INTERVALS";
    public static final String TYPE_SINGLE = "SINGLE";
    
    
}
