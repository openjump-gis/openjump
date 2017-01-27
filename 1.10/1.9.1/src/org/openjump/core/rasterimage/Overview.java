package org.openjump.core.rasterimage;

/**
 *
 * @author AdL
 */
public class Overview {

        public Overview(Overviews.OverviewLocation overviewLocation, Resolution resolution) {
            this.overviewLocation = overviewLocation;
            this.resolution = resolution;
            this.minResolution = Math.min(resolution.getX(), resolution.getY());
            this.maxResolution = Math.max(resolution.getX(), resolution.getY());
        }

        public Overviews.OverviewLocation getOverviewLocation() {
            return overviewLocation;
        }

        public Resolution getResolution() {
            return resolution;
        }
        
        public double getMinResolution() {
            return minResolution;
        }
        
        public double getMaxResolution() {
            return maxResolution;
        }
        
        private final Overviews.OverviewLocation overviewLocation;
        private final Resolution resolution;
        private final double minResolution;
        private final double maxResolution;
        
    }