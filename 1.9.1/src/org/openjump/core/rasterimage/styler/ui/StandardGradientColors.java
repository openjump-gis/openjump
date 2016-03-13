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
        
        // White-black
        colorMaps_l.add(createRamp(new Color[]{Color.WHITE, Color.BLACK}));
        
        // Blue-white
        colorMaps_l.add(createRamp(new Color[]{Color.BLUE, Color.WHITE}));
        
        // Red-black
        colorMaps_l.add(createRamp(new Color[]{Color.RED, Color.BLACK}));
        
        // Red-blue
        colorMaps_l.add(createRamp(new Color[]{Color.RED, Color.BLUE}));        
        
        // Green-yellow-red
        colorMaps_l.add(createRamp(new Color[]{Color.GREEN, Color.YELLOW, Color.RED}));
        
        // Blue-green-red
        colorMaps_l.add(createRamp(new Color[]{Color.BLUE, Color.GREEN, Color.RED}));  
        
        // Red-black-blue-green-gray
        colorMaps_l.add(createRamp(new Color[]{Color.RED, Color.BLACK, Color.BLUE, Color.GREEN, Color.LIGHT_GRAY}));
        
        // Rainbow
        colorMaps_l.add(createRamp(new Color[]{
            Color.RED,
            Color.ORANGE,
            Color.YELLOW,
            Color.GREEN,
            Color.BLUE,
            Color.decode("#4B0082"),
            Color.decode("#9400D3")}));
        
        // Aspect
        colorMaps_l.add(createRamp(new Color[]{
            Color.decode("#ff0000"),
            Color.decode("#ff6600"),
            Color.decode("#ffff00"),
            Color.decode("#33ff33"),
            Color.decode("#00ccff"),
            Color.decode("#0066cc"),
            Color.decode("#0000cc"),
            Color.decode("#cc00ff"),
            Color.decode("#ff0000")}));
        
        // DEM
        colorMaps_l.add(createRamp(new Color[]{
            Color.decode("#33ccff"),
            Color.decode("#aeefe8"),
            Color.decode("#ffffb2"),
            Color.decode("#007f3f"),
            Color.decode("#fcb902"),
            Color.decode("#770000"),
            Color.decode("#682f0c"),
            Color.decode("#aaaaaa"),
            Color.decode("#fffcff")}));
        
        return colorMaps_l;
        
    }

    private static ColorMapEntry[] createRamp(Color[] colors) {
        
        ColorMapEntry[] colorMapEntries = new ColorMapEntry[colors.length];
        double step = 1. / (colors.length - 1);
        for(int c=0; c<colors.length; c++) {
            colorMapEntries[c] = new ColorMapEntry(c*step, colors[c]);
        }
        return colorMapEntries;
    }
    
}