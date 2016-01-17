package org.openjump.core.rasterimage;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author AdL
 */
public class Overviews {

        
    public void addOverview(Overview overview) {
        overviews_l.add(overview);
        overviewsCount++;
        if(overview.getOverviewLocation() == OverviewLocation.INTERNAL) {
            internalOverviewsCount++;
        }
        if(overview.getOverviewLocation() == OverviewLocation.EXTERNAL) {
            externalOverviewsCount++;
        }
//        updateSorting();
    }

//    private void updateSorting() {
//
//        double[] resolutions = new double[overviews_l.size()];
//        for(int o=0; o<overviews_l.size(); o++) {             
//            resolutions[o] = overviews_l.get(o).getMinResolution();
//        }
//
//        boolean iterate = true;
//        int rank = 0;
//        while(iterate) {
//            iterate = false;
//            double minVal = Double.MAX_VALUE;
//            int minValIndex = 0;
//            for(int r=0; r<resolutions.length; r++) {
//                if(resolutions[r] < minVal) {
//                    minVal = resolutions[r];
//                    minValIndex = r;
//                    iterate = true;
//                }
//            }
////            rankToOverviewIndex.put(rank, minValIndex);
//            resolutions[minValIndex] = Double.MAX_VALUE;
//            rank++;
//        }
//
//    }

    public int getOverviewsCount() {
        return overviewsCount;
    }

    public int getInternalOverviewsCount() {
        return internalOverviewsCount;
    }

    public int getExternalOverviewsCount() {
        return externalOverviewsCount;
    }

    private int overviewsCount = 0;
    private int internalOverviewsCount = 0;
    private int externalOverviewsCount = 0;
    private final List<Overview> overviews_l = new ArrayList<Overview>();
        
    
    public int pickOverviewLevel(Resolution requestedRes) {
        
        double reqx = requestedRes.getX();
        double reqy = requestedRes.getY();

        double doubleDistance = Double.MAX_VALUE;
        int overviewIndex = 0;
        for(int o=0; o<overviews_l.size(); o++) {
            
            if(overviews_l.get(o).getMinResolution() < reqx &&
                    overviews_l.get(o).getMinResolution() < reqy) {
                
                double tempDoubleDistance = (reqx - overviews_l.get(o).getMinResolution()) + (reqy - overviews_l.get(o).getMinResolution());
                if(tempDoubleDistance < doubleDistance) {
                    doubleDistance = tempDoubleDistance;
                    overviewIndex = o;
                }
                
            }
            
        }
        
        return overviewIndex;
        
    }
    
    public enum OverviewLocation {
        INTERNAL, EXTERNAL;
    }
    
    
}
