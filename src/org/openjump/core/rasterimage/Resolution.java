package org.openjump.core.rasterimage;

/**
 *
 * @author AdL
 */
public class Resolution {

    public Resolution(double xResolution, double yResolution) {
        this.x = xResolution;
        this.y = yResolution;
        this.z = null;
    }
    
    public Resolution(double xResolution, double yResolution, double zResolution) {
        this.x = xResolution;
        this.y = yResolution;
        this.z = zResolution;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Double getZ() {
        return z;
    }
    
    private final double x;
    private final double y;
    private final Double z;

} 
