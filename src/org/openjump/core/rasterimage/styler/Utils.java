package org.openjump.core.rasterimage.styler;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.openjump.core.rasterimage.RasterImageLayer;

/**
 * 
 * @author AdL
 */
public class Utils {
    
    public static double[] purgeNoData(Raster rasterData, RasterImageLayer rasterImageLayer) {
        
        /* Purge no data and take only one sample per value */
        Set<Double> withoutNoData_s = new HashSet<Double>();
        
        for(int r=0; r<rasterData.getHeight(); r++) {
            
            for(int c=0; c<rasterData.getWidth(); c++) {
                
                double val;
                if(rasterData.getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT) {
                    val = rasterData.getSampleFloat(c, r, 0);
                } else if(rasterData.getDataBuffer().getDataType() == DataBuffer.TYPE_DOUBLE) {
                    val = rasterData.getSampleDouble(c, r, 0);
                } else {
                    val = rasterData.getSample(c, r, 0);
                }

                if(rasterImageLayer.isNoData(val)) {
                    continue;
                }
                
                withoutNoData_s.add(val);
                
            }
            
        }        
        
        Iterator iter = withoutNoData_s.iterator();
        double[] values = new double[withoutNoData_s.size()];
        int p=0;
        while(iter.hasNext()) {
            values[p] = (Double) iter.next();
            p++;
        }
        
        return values;
        
    }
    
    
}
