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
        
     // BWBW
        colorMaps_l.add(createRamp(new Color[] { Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK }));
        
        // DEM-2
        colorMaps_l.add(createRamp(new Color[] { Color.decode("#09114f"),
                Color.decode("#017dff"), Color.decode("#009c01"),
                Color.decode("#5dd331"), Color.decode("#02ff00"),
                Color.decode("#ffff00"), Color.decode("#f6d700"),
                Color.decode("#ff9b33"), Color.decode("#ed7306"),
                Color.decode("#fa210e"), Color.decode("#d20005"),
                Color.decode("#888e46"), Color.decode("#a0a64e"),
                Color.decode("#c3c184"), Color.decode("#e4e1c2") }));
        
        // 36 colors
        colorMaps_l.add(createRamp(new Color[] { Color.decode("#e6004d"),
                Color.decode("#ff0000"), Color.decode("#cc4df2"),
                Color.decode("#cc0000"), Color.decode("#e6cccc"),
                Color.decode("#e6ccd8"), Color.decode("#a600cc"),
                Color.decode("#a64d00"), Color.decode("#ff4dff"),
                Color.decode("#ffa6ff"), Color.decode("#ffe6ff"),
                Color.decode("#ffffa8"), Color.decode("#ffff00"),
                Color.decode("#e6e600"), Color.decode("#e68000"),
                Color.decode("#f2a64d"), Color.decode("#e6a600"),
                Color.decode("#e6e64d"), Color.decode("#ffe6a6"),
                Color.decode("#ffe64d"), Color.decode("#e6cc4d"),
                Color.decode("#f2cca6"), Color.decode("#80ff00"),
                Color.decode("#00a600"), Color.decode("#45ff00"),
                Color.decode("#ccf24d"), Color.decode("#a6ff80"),
                Color.decode("#a6e64d"), Color.decode("#a6f200"),
                Color.decode("#e6e6e6"), Color.decode("#cccccc"),
                Color.decode("#ccffcc"), Color.decode("#000000"),
                Color.decode("#a6e6cc"), Color.decode("#a6a6ff"),
                Color.decode("#4d4dff"),

        }));
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