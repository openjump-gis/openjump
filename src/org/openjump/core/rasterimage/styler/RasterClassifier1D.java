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
    
    public static double[] classifyGivenInterval(RasterImageLayer rasImgLay, int band, double interval) {

        double minVal = rasImgLay.getMetadata().getStats().getMin(band);
        double maxVal = rasImgLay.getMetadata().getStats().getMax(band);
        
        minVal = Math.floor(minVal/interval) * interval;
        maxVal = Math.ceil(maxVal/interval) * interval;
        int intvCount = (int) ((maxVal - minVal) / interval);

        // Following intervals
        double valEnd = minVal;
        double[] breaks = new double[intvCount];
        for (int i = 0; i < intvCount; i++) {
            double valStart = valEnd;
            valEnd = valStart + interval;
            if (valEnd >= maxVal) {
                valEnd = maxVal;
            }
            breaks[i] = valEnd;

        }
        return breaks;        
    }
    
}
