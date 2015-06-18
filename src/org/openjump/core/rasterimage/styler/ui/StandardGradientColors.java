package org.openjump.core.rasterimage.styler.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.openjump.core.rasterimage.styler.ColorMapEntry;

/**
 *
 * @author GeomaticaEAmbiente
 */
public class StandardGradientColors {
    
    public static List<ColorMapEntry[]> getStandardGradientColors(){
       
        List<ColorMapEntry[]> colorMaps_l = new ArrayList<ColorMapEntry[]>();
        
        ColorMapEntry[] colorRange = new ColorMapEntry[2];
        colorRange[0] = new ColorMapEntry(0, Color.WHITE);
        colorRange[1] = new ColorMapEntry(1, Color.BLACK);
        colorMaps_l.add(colorRange);
        
        colorRange = new ColorMapEntry[2];
        colorRange[0] = new ColorMapEntry(0, Color.BLUE);
        colorRange[1] = new ColorMapEntry(1, Color.WHITE);
        colorMaps_l.add(colorRange);
        
        colorRange = new ColorMapEntry[2];
        colorRange[0] = new ColorMapEntry(0, Color.RED);
        colorRange[1] = new ColorMapEntry(1, Color.BLACK);
        colorMaps_l.add(colorRange);
        
        colorRange = new ColorMapEntry[5];
        colorRange[0] = new ColorMapEntry(0, Color.RED);
        colorRange[1] = new ColorMapEntry(0.25, Color.BLACK);
        colorRange[2] = new ColorMapEntry(0.5, Color.BLUE);
        colorRange[3] = new ColorMapEntry(0.8, Color.GREEN);
        colorRange[4] = new ColorMapEntry(1, Color.LIGHT_GRAY);
        colorMaps_l.add(colorRange);
        
        return colorMaps_l;
        
    }

    
}