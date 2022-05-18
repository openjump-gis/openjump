package org.openjump.core.rasterimage;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resolution)) return false;
        Resolution that = (Resolution) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
