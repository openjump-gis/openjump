package org.openjump.core.rasterimage;

/**
 *
 * @author AdL
 */
public class Stats {

    public Stats(int bandCount) {
        this.bandCount = bandCount;
        this.min = new double[bandCount];
        this.max = new double[bandCount];
        this.mean = new double[bandCount];
        this.stdDev = new double[bandCount];
    }

    public void setStatsForBand(int band, double min, double max, double mean, double stdDev) {
        this.min[band] = min;
        this.max[band] = max;
        this.mean[band] = mean;
        this.stdDev[band] = stdDev;
    }

    public int getBandCount() {
        return bandCount;
    }
    
    public double getMin(int band) {
        return min[band];
    }

    public double getMax(int band) {
        return max[band];
    }

    public double getMean(int band) {
        return mean[band];
    }

    public double getStdDev(int band) {
        return stdDev[band];
    }

    private final int bandCount;
    private final double[] min;
    private final double[] max;
    private final double[] mean;
    private final double[] stdDev;

    public static Stats defaultRGBStats() {
        
        Stats stats  = new Stats(3);
        stats.setStatsForBand(0, 0, 255, 127, 30);
        stats.setStatsForBand(1, 0, 255, 127, 30);
        stats.setStatsForBand(2, 0, 255, 127, 30);
        return stats;
        
    }
    
}
