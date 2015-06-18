package org.openjump.core.rasterimage.styler;

import org.openjump.core.rasterimage.RasterImageLayer;

public class RasterClassifier1D {
    
    public static double[] classifyEqualRange(RasterImageLayer rasterImageLayer, int classesCount, int band) {
        
        double[] breaks = new double[classesCount-1];
        double min = rasterImageLayer.getMetadata().getStats().getMin(band);
        double max = rasterImageLayer.getMetadata().getStats().getMax(band);
        double delta = (max - min)/classesCount;
        for (int i = 0; i < breaks.length; i++) {
            breaks[i]=min + (delta*(i+1));
        }  
        
        return breaks;
        
    }
    
    
}
