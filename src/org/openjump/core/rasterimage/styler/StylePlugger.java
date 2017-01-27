package org.openjump.core.rasterimage.styler;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;

/**
 * This utility class plugs a given style into a raster image layer.
 * @author AdL
 */
public class StylePlugger {

    public StylePlugger(RasterImageLayer rasterImageLayer) {
        this.rasterImageLayer = rasterImageLayer;
    }
    
    /**
     * Plugs the given style into the raster image layer.
     * @param rasterSymbolizer The style.
     * @param raster
     */
    public void plug(RasterSymbology rasterSymbolizer, Raster raster) {
        
        int width = raster.getWidth();
        int height = raster.getHeight();
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Double value = raster.getSampleDouble(col, row, 0);                               
                Color newColor = rasterSymbolizer.getColor(value); //getColor(value, rasterSymbolizer);
                if(newColor == null) {
                    /* Transparent cell */
                    newImage.setRGB(col, row, Color.TRANSLUCENT);
                } else {
                    /* Non-transparent cell */
                    newImage.setRGB(col, row, newColor.getRGB());
                }
            }
        }

        rasterImageLayer.setImage(newImage);
        rasterImageLayer.setTransparencyLevel(rasterSymbolizer.getTransparency());
        
    }
    
    private final RasterImageLayer rasterImageLayer;
    
}
